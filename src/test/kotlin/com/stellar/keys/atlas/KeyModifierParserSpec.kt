package com.stellar.keys.atlas

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for parsing F3 +, Ctrl +, Shift +, and Alt + shortcuts from names, labels, and keys.
 */
class KeyModifierParserSpec : FunSpec({
    test("parseShortcut should detect F3 + shortcuts and target keys") {
        val p1 = KeyModifierParser.parseShortcut("F3 + B")
        p1.modifier shouldBe KeyBindingModifier.F3
        p1.targetGlfwKey shouldBe 66 // 'B'

        val p2 = KeyModifierParser.parseShortcut("F3+B: Show Hitboxes")
        p2.modifier shouldBe KeyBindingModifier.F3
        p2.targetGlfwKey shouldBe 66

        val p3 = KeyModifierParser.parseShortcut("Toggle Hitboxes (F3 + B)")
        p3.modifier shouldBe KeyBindingModifier.F3
        p3.targetGlfwKey shouldBe 66

        val p4 = KeyModifierParser.parseShortcut("[F3+G] Chunk Borders")
        p4.modifier shouldBe KeyBindingModifier.F3
        p4.targetGlfwKey shouldBe 71 // 'G'

        val p5 = KeyModifierParser.parseShortcut("key.f3_b")
        p5.modifier shouldBe KeyBindingModifier.F3
        p5.targetGlfwKey shouldBe 66

        val p6 = KeyModifierParser.parseShortcut("F3 + Esc")
        p6.modifier shouldBe KeyBindingModifier.F3
        p6.targetGlfwKey shouldBe 256 // ESC

        val p7 = KeyModifierParser.parseShortcut("F3 + F4")
        p7.modifier shouldBe KeyBindingModifier.F3
        p7.targetGlfwKey shouldBe 293 // F4
    }

    test("parseShortcut should detect Ctrl + shortcuts and target keys") {
        val p1 = KeyModifierParser.parseShortcut("Ctrl + C")
        p1.modifier shouldBe KeyBindingModifier.CONTROL
        p1.targetGlfwKey shouldBe 67 // 'C'

        val p2 = KeyModifierParser.parseShortcut("Ctrl+V")
        p2.modifier shouldBe KeyBindingModifier.CONTROL
        p2.targetGlfwKey shouldBe 86 // 'V'

        val p3 = KeyModifierParser.parseShortcut("Control + Z")
        p3.modifier shouldBe KeyBindingModifier.CONTROL
        p3.targetGlfwKey shouldBe 90 // 'Z'

        val p4 = KeyModifierParser.parseShortcut("Ctrl + 1")
        p4.modifier shouldBe KeyBindingModifier.CONTROL
        p4.targetGlfwKey shouldBe 49 // '1'

        val p5 = KeyModifierParser.parseShortcut("key.ctrl_c")
        p5.modifier shouldBe KeyBindingModifier.CONTROL
        p5.targetGlfwKey shouldBe 67
    }

    test("parseShortcut should detect Shift + and Alt + shortcuts") {
        val p1 = KeyModifierParser.parseShortcut("Shift + F3")
        p1.modifier shouldBe KeyBindingModifier.SHIFT
        p1.targetGlfwKey shouldBe 292 // F3

        val p2 = KeyModifierParser.parseShortcut("Alt + F4")
        p2.modifier shouldBe KeyBindingModifier.ALT
        p2.targetGlfwKey shouldBe 293 // F4
    }

    test("parseShortcut should ignore standalone modifier keys without plus or separator") {
        KeyModifierParser.parseShortcut("F3").modifier shouldBe KeyBindingModifier.NONE
        KeyModifierParser.parseShortcut("Control").modifier shouldBe KeyBindingModifier.NONE
        KeyModifierParser.parseShortcut("Ctrl").modifier shouldBe KeyBindingModifier.NONE
        KeyModifierParser.parseShortcut("Shift").modifier shouldBe KeyBindingModifier.NONE
        KeyModifierParser.parseShortcut("Alt").modifier shouldBe KeyBindingModifier.NONE
        KeyModifierParser.parseShortcut("Walk Forward").modifier shouldBe KeyBindingModifier.NONE
    }

    test("isModifierPlaceholderKey should detect when key is the modifier itself") {
        KeyModifierParser.isModifierPlaceholderKey(KeyBindingModifier.F3, 292) shouldBe true
        KeyModifierParser.isModifierPlaceholderKey(KeyBindingModifier.F3, 66) shouldBe false
        KeyModifierParser.isModifierPlaceholderKey(KeyBindingModifier.CONTROL, 341) shouldBe true
        KeyModifierParser.isModifierPlaceholderKey(KeyBindingModifier.CONTROL, 67) shouldBe false
        KeyModifierParser.isModifierPlaceholderKey(KeyBindingModifier.F3, -1) shouldBe true
    }

    test("resolveGlfwKeyFromName should accurately resolve keys") {
        KeyModifierParser.resolveGlfwKeyFromName("A") shouldBe 65
        KeyModifierParser.resolveGlfwKeyFromName("Z") shouldBe 90
        KeyModifierParser.resolveGlfwKeyFromName("0") shouldBe 48
        KeyModifierParser.resolveGlfwKeyFromName("9") shouldBe 57
        KeyModifierParser.resolveGlfwKeyFromName("F1") shouldBe 290
        KeyModifierParser.resolveGlfwKeyFromName("F12") shouldBe 301
        KeyModifierParser.resolveGlfwKeyFromName("ESC") shouldBe 256
        KeyModifierParser.resolveGlfwKeyFromName("ENTER") shouldBe 257
        KeyModifierParser.resolveGlfwKeyFromName("SPACE") shouldBe 32
    }
})
