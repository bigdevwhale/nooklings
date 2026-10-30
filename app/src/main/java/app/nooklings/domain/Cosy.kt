package app.nooklings.domain

/** What a lodger brings when a dream of theirs comes true; it stays in the room for good. */
enum class Gift { RUG, CANDLE, POSTCARD, ROWAN, LEAF, GARLAND }

/** The six lodgers. Each eats one kind of cosiness and lives in its own spot in the room. */
enum class Pet(val gift: Gift) {
    CAT(Gift.RUG), DORMOUSE(Gift.CANDLE), MOTH(Gift.POSTCARD), BULLFINCH(Gift.ROWAN), HEDGEHOG(Gift.LEAF), RACCOON(Gift.GARLAND);

    val eats: List<Tag> get() = Tag.entries.filter { it.pet == this }
}

/** What a caught moment was. The colour fills the jar; the pet is who eats it. */
enum class Tag(val color: Int, val pet: Pet) {
    SKY(0x6A9AD8, Pet.HEDGEHOG),
    WALK(0x7AB060, Pet.HEDGEHOG),
    TEA(0xD98A2B, Pet.CAT),
    WARM(0xFFB04A, Pet.CAT),
    QUIET(0xC8D4EA, Pet.DORMOUSE),
    NEW(0xC9A86A, Pet.MOTH),
    PEOPLE(0xE0605A, Pet.BULLFINCH),
    HANDS(0x9A7AD0, Pet.RACCOON),
}

/** How a moment was caught: ten seconds of sound, a photo of light, minutes of quiet, a walk, or two jars cooked into jam. */
enum class Catch { SOUND, LIGHT, QUIET, WALK, BREW }

/** A lodger's dream: a riddle about a kind of cosiness. Bringing that kind makes it come true. */
data class Dream(val pet: Pet, val tag: Tag, val ru: String, val en: String) {
    val key: String get() = "${pet.name}:${Dreams.of(pet).indexOf(this)}"
    fun text(ru: Boolean) = if (ru) this.ru else en
}

object Dreams {
    private val all = listOf(
        Dream(Pet.CAT, Tag.TEA, "Коту снится, как пахнет горячий хлеб.", "The cat dreams of the smell of warm bread."),
        Dream(Pet.CAT, Tag.WARM, "Кот мечтает о солнечном пятне на полу.", "The cat dreams of a patch of sun on the floor."),
        Dream(Pet.CAT, Tag.TEA, "Коту снится свисток чайника.", "The cat dreams of a kettle whistling."),
        Dream(Pet.DORMOUSE, Tag.QUIET, "Соне снится тишина, в которой слышно часы.", "The dormouse dreams of a quiet where you can hear the clock."),
        Dream(Pet.DORMOUSE, Tag.QUIET, "Соне снится, как беззвучно падает снег.", "The dormouse dreams of snow falling without a sound."),
        Dream(Pet.MOTH, Tag.NEW, "Моли снится запах старой бумаги.", "The moth dreams of the smell of old paper."),
        Dream(Pet.MOTH, Tag.NEW, "Моль хочет туда, где ты ещё не был.", "The moth wants to go where you've never been."),
        Dream(Pet.MOTH, Tag.NEW, "Моли снится незнакомая улица.", "The moth dreams of a street you don't know."),
        Dream(Pet.BULLFINCH, Tag.PEOPLE, "Снегирю снится, как кто-то смеётся.", "The bullfinch dreams of someone laughing."),
        Dream(Pet.BULLFINCH, Tag.PEOPLE, "Снегирь ждёт голос, который давно не слышал.", "The bullfinch is waiting for a voice he hasn't heard in a while."),
        Dream(Pet.HEDGEHOG, Tag.SKY, "Ёжику снится мокрая листва.", "The hedgehog dreams of wet leaves."),
        Dream(Pet.HEDGEHOG, Tag.WALK, "Ёжику хочется уйти далеко-далеко.", "The hedgehog wants to walk far, far away."),
        Dream(Pet.HEDGEHOG, Tag.SKY, "Ёжику снится ветер.", "The hedgehog dreams of the wind."),
        Dream(Pet.RACCOON, Tag.HANDS, "Енотику снится запах муки.", "The raccoon dreams of the smell of flour."),
        Dream(Pet.RACCOON, Tag.HANDS, "Енотик хочет что-нибудь починить.", "The raccoon wants to mend something."),
    )

    fun of(p: Pet) = all.filter { it.pet == p }

    fun byKey(key: String?): Dream? {
        val (p, i) = key?.split(':')?.takeIf { it.size == 2 } ?: return null
        return runCatching { of(Pet.valueOf(p))[i.toInt()] }.getOrNull()
    }

    /** A new dream for one of [present], not the lodger who dreamt last. */
    fun roll(present: Collection<Pet>, last: Pet?, r: Double, r2: Double): Dream? {
        val who = present.filter { it != last }.ifEmpty { present.toList() }
        if (who.isEmpty()) return null
        val p = who[(r * who.size).toInt().coerceIn(0, who.size - 1)]
        val ds = of(p)
        return ds[(r2 * ds.size).toInt().coerceIn(0, ds.size - 1)]
    }
}
