package com.stellar.keys.mixin

import com.stellar.keys.atlas.KeybindAtlasClient
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Mixin hooking Minecraft client tick loop to process the open keyboard hotkey.
 */
@Mixin(Minecraft::class)
abstract class MinecraftTickMixin {
    @Suppress("UnusedPrivateMember", "UnusedParameter")
    @Inject(method = ["tick"], at = [At("TAIL")])
    private fun stellarKeysOnTick(ci: CallbackInfo) {
        val mc = this as Any as? Minecraft ?: return
        KeybindAtlasClient.handleOpenKeyboard(mc)
    }
}
