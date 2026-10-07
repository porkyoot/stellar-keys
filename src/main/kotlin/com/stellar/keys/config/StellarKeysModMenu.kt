package com.stellar.keys.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

/**
 * ModMenu integration for Stellar Keys.
 */
class StellarKeysModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            KeysClothConfigScreen.create(parent)
        }
    }
}
