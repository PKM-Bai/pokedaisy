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
    @Test fun `item name from the Chinese ROM decodes`() {
        Gen3Text.cjk = CjkTables.forSha(sha)
        assertEquals("精灵球", Gen3Text.decode(bytes("07 5b 08 bc 0b 14 ff")))
    }
}
