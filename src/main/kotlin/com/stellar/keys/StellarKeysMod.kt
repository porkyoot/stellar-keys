package com.stellar.keys

import com.stellar.core.StellarCore
import com.stellar.core.config.ConfigManager
import com.stellar.keys.atlas.KeyBindingAccess
import com.stellar.keys.atlas.KeybindAtlasClientConfig
import com.stellar.keys.atlas.ModDisplayNameLookup
import com.stellar.keys.backend.QuiltKeyBindingAccessBackend
import com.stellar.keys.backend.QuiltModDisplayNameLookupBackend
import com.stellar.keys.config.StellarKeysConfig
import com.stellar.keys.input.StellarKeysInputBridge
import net.fabricmc.api.ClientModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Dedicated client entry point for the Stellar Keys mod (100% client-side).
 */
class StellarKeysMod : ClientModInitializer {
    private val logger: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitializeClient() {
        StellarCore.logInfo("Initializing Stellar Keys under namespace ${StellarCore.NAMESPACE}")
        logger.info("Initializing client mod Stellar Keys")

        val config = ConfigManager.register(MOD_ID, "main", StellarKeysConfig::class.java)
        KeybindAtlasClientConfig.install(config)
        KeyBindingAccess.install(QuiltKeyBindingAccessBackend())
        ModDisplayNameLookup.install(QuiltModDisplayNameLookupBackend())

        StellarKeysInputBridge.initialize()
    }

    companion object {
        const val MOD_ID: String = "stellar_keys"
    }
}
