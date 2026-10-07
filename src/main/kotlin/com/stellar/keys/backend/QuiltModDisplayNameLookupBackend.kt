package com.stellar.keys.backend

import com.stellar.keys.atlas.ModDisplayNameLookup
import org.quiltmc.loader.api.QuiltLoader
import java.util.Optional

/**
 * Quilt implementation of ModDisplayNameLookup.Backend querying QuiltLoader mod metadata.
 */
class QuiltModDisplayNameLookupBackend : ModDisplayNameLookup.Backend {
    override fun findDisplayName(modId: String): Optional<String> {
        return QuiltLoader.getModContainer(modId)
            .map { it.metadata().name() }
            .filter { it.isNotBlank() }
    }
}
