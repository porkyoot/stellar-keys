package com.stellar.keys

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

/**
 * Basic unit test verifying Stellar Keys metadata and constants.
 */
class StellarKeysSpec : FunSpec({
    test("stellar keys mod id should be valid") {
        StellarKeysMod.MOD_ID.shouldNotBeBlank()
        StellarKeysMod.MOD_ID shouldBe "stellar_keys"
    }
})
