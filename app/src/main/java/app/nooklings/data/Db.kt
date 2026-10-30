package app.nooklings.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JarDao {
    @Query("SELECT * FROM jars ORDER BY at")
    fun all(): Flow<List<Jar>>

    @Query("SELECT * FROM jars ORDER BY at")
    suspend fun list(): List<Jar>

    @Query("SELECT * FROM jars WHERE id = :id")
    suspend fun get(id: Long): Jar?

    @Insert
    suspend fun insert(j: Jar): Long

    @Insert
    suspend fun insertAll(j: List<Jar>)

    @Update
    suspend fun update(j: Jar)

    @Delete
    suspend fun delete(j: Jar)

    @Query("SELECT COUNT(*) FROM jars")
    suspend fun count(): Int

    @Query("SELECT * FROM openings ORDER BY at")
    fun openings(): Flow<List<Opening>>

    @Query("SELECT * FROM openings ORDER BY at")
    suspend fun openingList(): List<Opening>

    @Insert
    suspend fun open(o: Opening): Long

    @Query("DELETE FROM openings WHERE jar = :id")
    suspend fun forgetOpenings(id: Long)
}

@Database(entities = [Jar::class, Opening::class], version = 1, exportSchema = true)
abstract class NookDb : RoomDatabase() {
    abstract fun jars(): JarDao

    companion object {
        fun create(context: Context): NookDb = Room.databaseBuilder(context, NookDb::class.java, "nooklings.db").build()
    }
}
