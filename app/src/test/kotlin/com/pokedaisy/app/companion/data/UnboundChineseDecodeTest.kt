package com.pokedaisy.app.companion.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Names matched against the original party screen of this exact ROM build. */
class UnboundChineseDecodeTest {
    private val sha = "40104331f1899b23cd5b6cee9612b775e6bb4234"
    @After fun reset() { Gen3Text.cjk = null }
    private fun bytes(hex: String) = hex.split(" ").map { it.toInt(16).toByte() }.toByteArray()
    @Test fun `known party names use the same Chinese encoding`() {
        Gen3Text.cjk = CjkTables.forSha(sha)
        assertSame(CjkTables.ELITE_REDUX_CN, Gen3Text.cjk)
        for ((hex, expected) in listOf(
            "0d 04 0e af 08 b8 ff" to "铁哑铃",
            "09 6c 09 d9 01 b6 ff" to "迷你冰",
            "0e 92 0d 17 11 0b ff" to "雪童子",
            "09 b1 0d e6 08 79 0c 04 ff" to "幕下力士",
            "04 c9 01 67 0b 14 ff" to "海豹球",
        )) {
            val encoded = bytes(hex)
            assertEquals(expected, Gen3Text.decode(encoded))
            // The production party reader validates the language byte and nickname field.
            val mon = ByteArray(100)
            encoded.copyInto(mon, 8)
            mon[0x12] = 2
            assertEquals(expected, gen3Nickname(mon, 0, VANILLA_PARTY_MON))
        }
    }
    @Test fun `ROM item names are enabled only for the selected version and matching item id`() {
        val previousReader = PokedexSource.reader
        val rom = ByteArray(44 * 6)
        bytes("07 5b 08 bc 0b 14 ff").copyInto(rom, 4 * 44)
        rom[4 * 44 + 14] = 4
        PokedexSource.reader = RomFileReader(rom)
        try {
            Gen3Text.cjk = CjkTables.forSha(sha)
            val table = vanillaItems(0x08000000, count = 6)
            RomItemText.use(table)
            assertEquals(null, RomItemText.name(4))
            RomItemText.use(table, chineseNames = true)
            assertEquals("精灵球", RomItemText.name(4))
            assertEquals(null, RomItemText.name(5))
            assertEquals(null, RomItemText.name(6))
            RomItemText.use(null)
            assertEquals(null, RomItemText.name(4))
        } finally {
            RomItemText.use(null)
            PokedexSource.reader = previousReader
        }
    }

    @Test fun `known ROM item names match the verified version`() {
        val dir = System.getenv("POKEDAISY_SAMPLE_DIR")?.let { java.io.File(it) }
        org.junit.Assume.assumeTrue("set POKEDAISY_SAMPLE_DIR to test local samples", dir?.isDirectory == true)
        val file = dir!!.walkTopDown().firstOrNull { f ->
            if (!f.isFile || !f.extension.equals("gba", true)) false else {
                val digest = java.security.MessageDigest.getInstance("SHA-1")
                f.inputStream().use { input ->
                    val buffer = ByteArray(65536)
                    while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
                }
                digest.digest().joinToString("") { "%02x".format(it) } == sha
            }
        }
        org.junit.Assume.assumeTrue("matching Unbound Chinese sample is not present", file != null)
        val previousReader = PokedexSource.reader
        try {
            PokedexSource.reader = RomFileReader(file!!.readBytes())
            Gen3Text.cjk = CjkTables.forSha(sha)
            RomItemText.use(vanillaItems(0x08876200), chineseNames = true)
            val expected = listOf("大师球", "高级球", "超级球", "精灵球", "狩猎球", "捕网球", "潜水球", "巢穴球", "重复球", "计时球", "豪华球", "纪念球", "伤药", "解毒药", "灼伤药", "解冻药", "解眠药", "解麻药", "全复药", "全满药", "厉害伤药", "好伤药", "万灵药", "活力碎片", "活力块", "美味之水", "劲爽汽水", "果汁牛奶", "哞哞鲜奶", "元气粉", "元气根", "万能粉", "复活草")
            expected.forEachIndexed { index, name -> assertEquals("item ${index + 1}", name, RomItemText.name(index + 1)) }
        } finally {
            RomItemText.use(null)
            PokedexSource.reader = previousReader
        }
    }

    @Test fun `item name from the Chinese ROM decodes`() {
        Gen3Text.cjk = CjkTables.forSha(sha)
        assertEquals("精灵球", Gen3Text.decode(bytes("07 5b 08 bc 0b 14 ff")))
    }
}
