package com.stellar.keys.mixin

import com.stellar.keys.atlas.render.KeyboardScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.options.controls.ControlsScreen
import net.minecraft.network.chat.Component
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private const val BUTTON_WIDTH = 310
private const val BUTTON_HEIGHT = 20

/**
 * Mixin injecting the Keyboard Atlas button into the Controls options screen.
 */
@Mixin(ControlsScreen::class)
abstract class ControlsScreenMixin {
    @Suppress(
        "MagicNumber",
        "UnusedPrivateMember",
        "UnusedParameter",
    )
    @Inject(method = ["addOptions"], at = [At("HEAD")])
    private fun stellarKeysAddAtlasButton(ci: CallbackInfo) {
        val screen = this as Any as? ControlsScreen ?: return
        val optionsList = (screen as OptionsSubScreenAccessor).stellarGetList() ?: return

        val button = Button.builder(
            Component.translatable("key.stellar_keys.open_keyboard"),
        ) {
            Minecraft.getInstance().setScreenAndShow(KeyboardScreen(screen))
        }.bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build()

        optionsList.addBig(button)
    }
}
