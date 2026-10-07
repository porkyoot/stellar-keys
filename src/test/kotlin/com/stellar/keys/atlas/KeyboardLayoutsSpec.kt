package com.stellar.keys.atlas

import com.stellar.keys.atlas.layout.KeyboardLayouts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for KeyboardLayouts presets and default hidden labels.
 */
class KeyboardLayoutsSpec : FunSpec({
    test("presets should create valid non-empty keyboard layouts") {
        for (preset in KeyboardLayouts.MainLayoutPreset.entries) {
            val layout = KeyboardLayouts.buildCenteredMainLayout(800, 600, 1, false, preset)
            layout shouldNotBe null
            layout.keys.shouldNotBeEmpty()
            layout.housingWidth shouldBeGreaterThan 0
            layout.housingHeight shouldBeGreaterThan 0
        }
    }

    test("default hidden binding labels should contain movement and digit keys") {
        val labels = KeyboardLayouts.defaultHiddenBindingLabels()
        labels.shouldNotBeEmpty()
        labels.contains("W") shouldBe true
        labels.contains("A") shouldBe true
        labels.contains("S") shouldBe true
        labels.contains("D") shouldBe true
        labels.contains("1") shouldBe true
        labels.contains("/") shouldBe true
    }

    test("canonicalHiddenBindingLabel should resolve standard key names") {
        KeyboardLayouts.canonicalHiddenBindingLabel(87) shouldBe "W"
        KeyboardLayouts.canonicalHiddenBindingLabel(32) shouldBe "SPACE"
        KeyboardLayouts.canonicalHiddenBindingLabel(257) shouldBe "ENTER"
        KeyboardLayouts.canonicalHiddenBindingLabel(258) shouldBe "TAB"
        KeyboardLayouts.canonicalHiddenBindingLabel(259) shouldBe "BKSP"
    }
})
