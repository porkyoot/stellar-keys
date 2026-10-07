package com.stellar.keys.atlas

import com.stellar.keys.atlas.layout.KeyAssignmentInfo
import com.stellar.keys.atlas.layout.KeyboardBindingCache
import com.stellar.keys.atlas.render.KeyboardRenderer
import com.stellar.keys.atlas.render.OverlayRenderHelper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for direct conflicts, category outline styling, and full fill conflict styling.
 */
class ConflictAndCategorySpec : FunSpec({
    test("KeyVisualStyle should expose red for direct conflict") {
        KeyVisualStyle.keyDirectConflictFill() shouldBe -2_937_041 // Red (0xFFD32F2F)
        KeyVisualStyle.keyDirectConflictText() shouldBe -1 // White text
    }

    test("KeyRenderStyle outline should preserve outline color and dark assigned fill") {
        val emerald = -14_704_819
        val assignedFill = KeyVisualStyle.keyAssignedFill()
        val assignedText = KeyVisualStyle.keyAssignedText()

        val style = KeyboardRenderer.KeyRenderStyle.outline(emerald, assignedFill, assignedText, false)
        style.hasOutline() shouldBe true
        style.outlineColor() shouldBe emerald
        style.fillColor() shouldBe assignedFill
        style.textColor() shouldBe assignedText

        val hoveredStyle = KeyboardRenderer.KeyRenderStyle.outline(emerald, assignedFill, assignedText, true)
        hoveredStyle.hasOutline() shouldBe true
        hoveredStyle.outlineColor() shouldBe OverlayRenderHelper.lighten(emerald, 0.22F)
        hoveredStyle.fillColor() shouldBe OverlayRenderHelper.lighten(assignedFill, 0.22F)
        hoveredStyle.textColor() shouldBe OverlayRenderHelper.lighten(assignedText, 0.22F)

        // Keycap style uses category color for outline, while keycap text remains standard assignedText
        val categoryKeyStyle = KeyboardRenderer.KeyRenderStyle.outline(emerald, assignedFill, assignedText, false)
        categoryKeyStyle.outlineColor() shouldBe emerald
        categoryKeyStyle.fillColor() shouldBe assignedFill
        categoryKeyStyle.textColor() shouldBe assignedText

        val hoveredCategoryKeyStyle = KeyboardRenderer.KeyRenderStyle.outline(emerald, assignedFill, assignedText, true)
        hoveredCategoryKeyStyle.outlineColor() shouldBe OverlayRenderHelper.lighten(emerald, 0.22F)
        hoveredCategoryKeyStyle.fillColor() shouldBe OverlayRenderHelper.lighten(assignedFill, 0.22F)
        hoveredCategoryKeyStyle.textColor() shouldBe OverlayRenderHelper.lighten(assignedText, 0.22F)
    }

    test("Action label background and text colors match category") {
        val movementCat = KeybindAtlasClientConfig.findOrCreateCategory("movement", "Movement")
        movementCat.fillColor() shouldBe -14_704_819
        // High contrast text auto-calculated for emerald fill
        movementCat.textColor() shouldBe -1 // White text on dark emerald
    }

    test("KeybindAtlasClientConfig should unify categories via findOrCreateCategory") {
        val movementCat = KeybindAtlasClientConfig.findOrCreateCategory("movement", "Movement")
        movementCat.id() shouldBe "movement"
        movementCat.fillColor() shouldBe -14_704_819
        KeybindAtlasClientConfig.findCustomCategory("movement") shouldBe movementCat
    }

    test("KeybindAtlasClientConfig customCategories should cache and include all default categories") {
        val cats1 = KeybindAtlasClientConfig.customCategories()
        val cats2 = KeybindAtlasClientConfig.customCategories()
        (cats1 === cats2) shouldBe true
        cats1.any { it.id() == "gameplay" } shouldBe true
        cats1.any { it.id() == "inventory" } shouldBe true
    }

    test("KeyRenderStyle fullFill for conflicts should have no custom outline") {
        val conflictFill = KeyVisualStyle.keyDirectConflictFill()
        val conflictText = KeyVisualStyle.keyDirectConflictText()

        val style = KeyboardRenderer.KeyRenderStyle.fullFill(conflictFill, conflictText, false)
        style.hasOutline() shouldBe false
        style.outlineColor() shouldBe 0
        style.fillColor() shouldBe conflictFill
        style.textColor() shouldBe conflictText
    }

    test("KeyAssignmentInfo should store category and localized category name") {
        val info = KeyAssignmentInfo("Forward", "Minecraft", null, KeyBindingModifier.NONE, "movement", "Movement")
        info.label shouldBe "Forward"
        info.source shouldBe "Minecraft"
        info.modifier shouldBe KeyBindingModifier.NONE
        info.category shouldBe "movement"
        info.categoryName shouldBe "Movement"
    }

    test("KeyboardBindingCache should detect direct conflicts on same layer") {
        val cache = KeyboardBindingCache.shared()
        cache.markDirty()

        // Initially no bindings
        cache.hasBinding(87) shouldBe false
        cache.hasDirectConflict(87) shouldBe false
    }
})
