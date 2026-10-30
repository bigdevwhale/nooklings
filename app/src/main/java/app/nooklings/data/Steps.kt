package app.nooklings.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.nooklings.container
import app.nooklings.domain.Catch
import app.nooklings.domain.Tag
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Walks are caught by the phone's step counter: enough steps in a short while make a jar on their own. */
object Steps {
    const val MIN_WALK = 2500
    const val MIN_MANUAL = 300
    const val WINDOW = 3 * HOUR

    fun available(c: Context) = (c.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    /** Steps since the phone booted, or null if the counter doesn't answer. */
    suspend fun total(c: Context): Long? {
        val sm = c.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val s = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        return withTimeoutOrNull(5000) {
            suspendCancellableCoroutine { k ->
                val l = object : SensorEventListener {
                    override fun onSensorChanged(e: SensorEvent) { sm.unregisterListener(this); if (k.isActive) k.resume(e.values[0].toLong()) }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sm.registerListener(l, s, SensorManager.SENSOR_DELAY_NORMAL)
                k.invokeOnCancellation { sm.unregisterListener(l) }
            }
        }
    }

    /** What a reading means: a walk to roll (or null) and the new base and its time. */
    data class Verdict(val walk: Int?, val base: Long, val baseAt: Long)

    fun judge(total: Long, base: Long, baseAt: Long, now: Long): Verdict = when {
        base < 0 || total < base -> Verdict(null, total, now)
        total - base >= MIN_WALK -> Verdict((total - base).toInt(), total, now)
        now - baseAt > WINDOW -> Verdict(null, total, now)
        else -> Verdict(null, base, baseAt)
    }

    fun schedule(c: Context, on: Boolean) {
        val wm = WorkManager.getInstance(c)
        if (on) wm.enqueueUniquePeriodicWork("walks", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<WalkWorker>(1, TimeUnit.HOURS).build())
        else wm.cancelUniqueWork("walks")
    }
}

class WalkWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.now()
        if (!s.autoWalks) return Result.success()
        val total = Steps.total(applicationContext) ?: return Result.success()
        val now = System.currentTimeMillis()
        val v = Steps.judge(total, s.stepBase, s.stepBaseAt, now)
        c.settings.update { it.copy(stepBase = v.base, stepBaseAt = v.baseAt) }
        v.walk?.let { n -> c.roll(c.draft(Tag.WALK, Catch.WALK, applicationContext.getString(app.nooklings.R.string.label_walk)).copy(steps = n)) }
        return Result.success()
    }
}
