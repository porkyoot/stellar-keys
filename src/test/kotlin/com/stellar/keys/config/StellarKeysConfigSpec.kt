package com.stellar.keys.config

import com.stellar.core.config.ConfigManager
import com.stellar.keys.StellarKeysMod
import com.stellar.keys.atlas.KeybindAtlasClientConfig
import com.stellar.keys.atlas.layout.KeyboardLayouts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

/**
 * Unit tests for StellarKeysConfig and Quilt reflective integration.
 */
class StellarKeysConfigSpec : FunSpec({
    test("config should register and initialize with expected defaults") {
        val config = ConfigManager.register(
            StellarKeysMod.MOD_ID,
            "test_config",
            StellarKeysConfig::class.java,
        )

        config.defaultPixelScale() shouldBe 1
        config.keyboardLayout() shouldBe KeyboardLayouts.MainLayoutPreset.US_QWERTY
        config.animationsEnabled() shouldBe true
        config.hiddenBindingsEnabled() shouldBe true
        config.categoriesEnabled() shouldBe true
        config.fKeysEnabled() shouldBe false
        config.allKeysEnabled() shouldBe true
        config.panelOpacity() shouldBe 0.4f
        config.maxUnhoveredAssignments() shouldBe 3
    }

    test("formatColor should format ARGB integer into hex string") {
        val hex = StellarKeysConfig.formatColor(-1) // 0xFFFFFFFF
        hex shouldBe "#FFFFFFFF"

        val colorHex = StellarKeysConfig.formatColor(-16_777_216) // opaque black
        colorHex shouldBe "#000000FF"
        colorHex.shouldStartWith("#")
    }

    test("backend should install into KeybindAtlasClientConfig correctly") {
        val config = ConfigManager.register(
            StellarKeysMod.MOD_ID,
            "test_backend_config",
            StellarKeysConfig::class.java,
        )
        KeybindAtlasClientConfig.install(config)
        KeybindAtlasClientConfig.defaultPixelScale() shouldBe 1
        KeybindAtlasClientConfig.keyboardLayout() shouldBe KeyboardLayouts.MainLayoutPreset.US_QWERTY
    }
})
