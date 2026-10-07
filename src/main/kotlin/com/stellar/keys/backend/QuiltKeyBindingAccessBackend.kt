package com.stellar.keys.backend

import com.mojang.blaze3d.platform.InputConstants.Key
import com.stellar.keys.atlas.KeyBindingAccess
import com.stellar.keys.atlas.KeyBindingModifier
import com.stellar.keys.atlas.KeyModifierParser
import net.minecraft.client.KeyMapping

/**
 * Quilt / Fabric implementation of KeyBindingAccess.Backend.
 */
class QuiltKeyBindingAccessBackend : KeyBindingAccess.Backend {
    private val customModifiers = mutableMapOf<String, KeyBindingModifier>()

    override fun getModifier(keyMapping: KeyMapping): KeyBindingModifier {
        val name = keyMapping.name
        val custom = customModifiers[name]
        if (custom != null) {
            return custom
        }
        val parsed = KeyModifierParser.parseModifier(keyMapping)
        return when {
            name == "key.debug.modifier" -> KeyBindingModifier.NONE
            !parsed.isNone -> parsed
            keyMapping.category == KeyMapping.Category.DEBUG || name.startsWith("key.debug.") -> KeyBindingModifier.F3
            name == "key.saveToolbarActivator" || name == "key.loadToolbarActivator" -> KeyBindingModifier.CONTROL
            else -> KeyBindingModifier.NONE
        }
    }

    override fun setBinding(keyMapping: KeyMapping, modifier: KeyBindingModifier, key: Key) {
        keyMapping.setKey(key)
        if (modifier.isNone) {
            customModifiers.remove(keyMapping.name)
        } else {
            customModifiers[keyMapping.name] = modifier
        }
        KeyMapping.resetMapping()
    }

    override fun fromGlfwModifiers(glfwModifiers: Int): KeyBindingModifier {
        return when {
            glfwModifiers and GLFW_MOD_SHIFT != 0 -> KeyBindingModifier.SHIFT
            glfwModifiers and GLFW_MOD_CONTROL != 0 -> KeyBindingModifier.CONTROL
            glfwModifiers and GLFW_MOD_ALT != 0 -> KeyBindingModifier.ALT
            else -> KeyBindingModifier.NONE
        }
    }

    private companion object {
        const val GLFW_MOD_SHIFT = 1
        const val GLFW_MOD_CONTROL = 2
        const val GLFW_MOD_ALT = 4
    }
}
