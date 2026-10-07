package com.stellar.keys.mixin

import net.minecraft.client.gui.components.OptionsList
import net.minecraft.client.gui.screens.options.OptionsSubScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

/**
 * Accessor mixin for retrieving the options list from OptionsSubScreen.
 */
@Mixin(OptionsSubScreen::class)
interface OptionsSubScreenAccessor {
    @Accessor("list")
    fun stellarGetList(): OptionsList?
}
