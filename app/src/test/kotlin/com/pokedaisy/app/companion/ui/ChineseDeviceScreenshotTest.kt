package com.pokedaisy.app.companion.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pokedaisy.app.DocsApi
import com.pokedaisy.app.DocsStore
import com.pokedaisy.app.companion.i18n.AppLanguage
import com.pokedaisy.app.companion.i18n.L10n
import com.pokedaisy.app.companion.ui.theme.AppTheme
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Chinese UI at known screen sizes; RG DS density is an assumption until measured on a device. */
@RunWith(Parameterized::class)
class ChineseDeviceScreenshotTest(private val device: String, private val config: DeviceConfig, private val window: Boolean) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun devices() = listOf(
            arrayOf("Thor", DeviceConfig.PIXEL_5.copy(screenWidth = 1240, screenHeight = 1080, orientation = ScreenOrientation.LANDSCAPE), false),
            arrayOf("DuoLite", DeviceConfig.PIXEL_5.copy(screenWidth = 1280, screenHeight = 960, orientation = ScreenOrientation.LANDSCAPE), false),
            arrayOf("RgDs", DeviceConfig.PIXEL_5.copy(screenWidth = 640, screenHeight = 480, density = Density.HIGH, xdpi = 200, ydpi = 200, orientation = ScreenOrientation.LANDSCAPE), false),
            arrayOf("Single", DeviceConfig.PIXEL_5.copy(screenWidth = 1920, screenHeight = 1080, density = Density.create(369), orientation = ScreenOrientation.LANDSCAPE), true),
        )
    }

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = config, theme = "android:Theme.Material.NoActionBar")

    @Before fun setUp() {
        CompanionScreenshotTest().game()
        L10n.apply(AppLanguage.ZH_CN.code, null)
    }

    @After fun tearDown() = L10n.apply("EN", null)

    private fun shot(content: @Composable () -> Unit) = paparazzi.snapshot(device) {
        if (window) {
            Box(Modifier.fillMaxSize().background(Color(0xFF3A5A4A))) {
                SidePanelHost(window = true, onClose = {}, compactText = true, content = content)
            }
        } else content()
    }

    private fun tab(name: String) = shot {
        CompanionScreen(SampleCompanion.snapshot, SampleCompanion.Slots, SampleCompanion.Settings(), initialTab = name)
    }

    @Test fun settings() = tab("SETTINGS")
    @Test fun party() = tab("PARTY")
    @Test fun guide() = tab("GUIDE")

    private fun exampleDex() = com.pokedaisy.app.companion.data.DexEntry(
        national = 25, species = 25, category = "MOUSE", heightDm = 4, weightHg = 60,
        description = "ROM-specific description. This effect may differ between game versions.",
        type1 = 0, type2 = 0, baseStats = listOf(35, 55, 40, 50, 50, 90),
        abilities = listOf("STATIC", "CUSTOM ABILITY"), hiddenAbility = "HIDDEN ABILITY",
        catchRate = 190, genderRatio = 127, eggGroups = emptyList(),
    )

    private fun dexInfo(entry: com.pokedaisy.app.companion.data.DexEntry) = shot { AppTheme {
        val m = rememberGbaTextMetrics()
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            DexInfoSection(entry, m, m, Modifier.fillMaxWidth(0.58f).fillMaxHeight())
        }
    } }

    @Test fun unverifiedDexInfo() = dexInfo(exampleDex())

    // Isolate the lower rows so the small screen captures both ability labels and their values.
    @Test fun unverifiedDexAbilities() = dexInfo(exampleDex().copy(description = "", baseStats = emptyList()))

    @Test fun manual() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }
        val manual = DocsApi.parse(File(root, "app/src/main/assets/docs-zh-hans.json").readText(Charsets.UTF_8))
        val source = object : DocsSource {
            override fun cached() = manual
            override fun refresh() = DocsStore.Refresh.Unchanged
            override fun image(manual: DocsApi.Manual, image: DocsApi.Block.Image): Bitmap? = null
        }
        val ui = DocsUiState().apply { this.manual = manual; loadedLanguage = AppLanguage.ZH_CN; started = true; open("settings-reference") }
        shot { AppTheme { DocsReader(ui, rememberGbaTextMetrics(), source) } }
    }
}
