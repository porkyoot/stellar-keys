package com.stellar.keys.atlas;

import java.util.Objects;
import java.util.Optional;

public final class ModDisplayNameLookup {
   private static ModDisplayNameLookup.Backend backend = modId -> Optional.empty();

   private ModDisplayNameLookup() {
   }

   public static void install(ModDisplayNameLookup.Backend backend) {
      ModDisplayNameLookup.backend = Objects.requireNonNull(backend);
   }

   public static Optional<String> findDisplayName(String modId) {
      return backend.findDisplayName(modId);
   }

   public interface Backend {
      Optional<String> findDisplayName(String var1);
   }
}
