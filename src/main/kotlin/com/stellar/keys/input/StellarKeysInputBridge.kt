package com.stellar.keys.input

import com.stellar.core.input.KeyMappingRegistry
import com.stellar.keys.atlas.KeybindAtlasClient
import com.stellar.keys.atlas.render.KeyboardScreen
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

/**
 * Client-side input bridge connecting Minecraft raw input events to Stellar Keys.
 */
object StellarKeysInputBridge {
    val openKeyboardKeyMapping: KeyMapping = KeyMappingRegistry.register(
        name = "key.stellar_keys.open_keyboard",
        keyCode = GLFW.GLFW_KEY_K,
        category = KeyMappingRegistry.CATEGORY_STELLAR,
    )

    fun initialize() {
        KeybindAtlasClient.setOpenKeyboardKey(openKeyboardKeyMapping)
    }

    fun openKeyboard(minecraft: Minecraft) {
        if (minecraft.gui.screen() == null) {
            minecraft.setScreenAndShow(KeyboardScreen())
        }
    }
}
