package com.stellar.keys.atlas;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class AtlasText {
   private static final String PREFIX = "keybindatlas.";

   private AtlasText() {
   }

   public static String key(String path) {
      return "keybindatlas." + path;
   }

   public static MutableComponent translatable(String path, Object... args) {
      return Component.translatable(key(path), args);
   }

   public static String text(String path, Object... args) {
      return translatable(path, args).getString();
   }
}
