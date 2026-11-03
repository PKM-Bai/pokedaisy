package com.pokedaisy.app.companion.data

import java.util.concurrent.ConcurrentHashMap

/** ROM-owned names for the hash-verified Unbound 2.1.1.1 Chinese build only. */
object UnboundChineseBattleNames {
    @Volatile private var enabled = false
    private val speciesCache = ConcurrentHashMap<Int, String>()
    private val moveCache = ConcurrentHashMap<Int, String>()
    fun use(enabled: Boolean) {
        this.enabled = enabled
        speciesCache.clear()
        moveCache.clear()
    }
    private fun read(id: Int, range: IntRange, base: Long, stride: Int, cache: ConcurrentHashMap<Int, String>): String? {
        if (!enabled || id !in range) return null
        cache[id]?.let { return it }
        val value = runCatching {
            val raw = PokedexSource.reader.readCoreMemory(base + id.toLong() * stride, stride)
            if (raw.none { (it.toInt() and 0xff) == 0xff }) return@runCatching null
            Gen3Text.decode(raw, charmap = CjkTables.ELITE_REDUX_CN)
                .trim().takeIf { it.isNotEmpty() && Gen3Text.UNKNOWN !in it }
        }.getOrNull() ?: return null
        cache[id] = value
        return value
    }
    fun fallback(original: String): String = if (enabled) untranslatedGameText(original) else original
    fun species(id: Int): String? = read(id, 1..1293, 0x0966A98CL, 11, speciesCache)
    fun move(id: Int): String? = read(id, 1..922, 0x08A40A10L, 13, moveCache)
}

/** Only use a player's nickname when the battle record identifies a single party member. */
internal fun battleNickname(b: BattleMon, party: List<Mon>): String? =
    party.filter { it.species == b.species && it.level == b.level && it.hp == b.hp }
        .singleOrNull()?.nickname
