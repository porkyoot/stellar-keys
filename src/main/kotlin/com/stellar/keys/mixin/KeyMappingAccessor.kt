package com.stellar.keys.mixin

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

/**
 * Mixin accessor to retrieve the bound key from KeyMapping.
 */
@Mixin(KeyMapping::class)
interface KeyMappingAccessor {
    @Accessor("key")
    fun stellarGetKey(): InputConstants.Key
}
