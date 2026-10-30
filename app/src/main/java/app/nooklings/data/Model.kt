package app.nooklings.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.nooklings.domain.Catch
import app.nooklings.domain.Tag
import app.nooklings.domain.Wx

const val DAY = 86_400_000L
const val HOUR = 3_600_000L

/**
 * A caught moment rolled into a jar. [color] is the light from a photo (0 keeps the tag's colour);
 * [photo] and [sound] are file names in the app's own folders; [tag2] is set on jam.
 */
@Entity(tableName = "jars")
data class Jar(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tag: Tag,
    val tag2: Tag? = null,
    val kind: Catch,
    val label: String,
    val at: Long,
    val wx: Wx? = null,
    val temp: Int? = null,
    val place: String = "",
    val color: Int = 0,
    val photo: String? = null,
    val sound: String? = null,
    val steps: Int = 0,
    val rare: Boolean = false,
) {
    val tags: List<Tag> get() = listOfNotNull(tag, tag2).distinct()
    val tint: Int get() = if (color != 0) color else tag.color
}

/** A jar opened to listen: everyone gathers by the stove and gets a little warmer. */
@Entity(tableName = "openings", indices = [Index("jar")])
data class Opening(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jar: Long,
    val at: Long,
)
