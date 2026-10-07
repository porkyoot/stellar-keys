package com.stellar.keys.atlas

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for KeyCategory parsing, normalization, and palette presets.
 */
class KeyCategorySpec : FunSpec({
    test("default categories should be initialized properly") {
        val assigned = KeyCategory.assignedCategory()
        assigned.id() shouldBe "assigned"
        assigned.name() shouldBe "Assigned"
        assigned.isAssignedCategory() shouldBe true
        assigned.deletable() shouldBe false

        val hidden = KeyCategory.hiddenCategory()
        hidden.id() shouldBe "hidden"
        hidden.isHiddenCategory shouldBe true
        hidden.deletable() shouldBe false
    }

    test("preset colors should contain core palette entries") {
        val presets = KeyCategory.presetColors()
        presets.shouldNotBeEmpty()
        presets.any { it.name() == "Crimson" } shouldBe true
        presets.any { it.name() == "Emerald" } shouldBe true
        presets.any { it.name() == "Amber" } shouldBe true
    }

    test("normalizeId should convert arbitrary strings into valid identifiers") {
        KeyCategory.normalizeId("My Custom Category!") shouldBe "my-custom-category"
        KeyCategory.normalizeId("   Combat 123   ") shouldBe "combat-123"
        KeyCategory.normalizeId("assigned") shouldBe "assigned-category"
        KeyCategory.normalizeId("hidden") shouldBe "hidden-category"
        KeyCategory.normalizeId("") shouldBe "category"
    }

    test("sanitizeName should strip invalid characters and enforce length limits") {
        KeyCategory.sanitizeName("Combat | Actions") shouldBe "Combat / Actions"
        KeyCategory.sanitizeName("    Lots    of   spaces   ") shouldBe "Lots of spaces"
    }

    test("autoTextColor should choose high contrast foreground color") {
        val darkBg = -16_777_216 // black
        val lightBg = -1 // white
        KeyCategory.autoTextColor(darkBg) shouldBe -1 // white text
        KeyCategory.autoTextColor(lightBg) shouldNotBe -1 // dark text
    }

    test("defaultCustomCategories should include standard Minecraft categories") {
        val defaults = KeyCategory.defaultCustomCategories()
        defaults.shouldNotBeEmpty()
        defaults.any { it.id() == "movement" } shouldBe true
        defaults.any { it.id() == "gameplay" } shouldBe true
        defaults.any { it.id() == "inventory" } shouldBe true
        defaults.any { it.id() == "multiplayer" } shouldBe true
    }

    test("defaultColorForId should return known colors for standard categories") {
        KeyCategory.defaultColorForId("movement") shouldBe -14_704_819
        KeyCategory.defaultColorForId("gameplay") shouldBe -4_834_233
        KeyCategory.defaultColorForId("inventory") shouldBe -429_290
        KeyCategory.defaultColorForId("multiplayer") shouldBe -7_256_618
    }
})
