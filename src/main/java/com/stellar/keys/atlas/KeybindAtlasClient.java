package com.stellar.keys.atlas;

import com.stellar.keys.atlas.render.KeyboardScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class KeybindAtlasClient {
   private static KeyMapping openKeyboardKey;

   private KeybindAtlasClient() {
   }

   public static void setOpenKeyboardKey(KeyMapping key) {
      openKeyboardKey = key;
   }

   public static KeyMapping openKeyboardKey() {
      if (openKeyboardKey == null) {
         openKeyboardKey = new KeyMapping("key.stellar_keys.open_keyboard", 75, KeyMapping.Category.MISC);
      }

      return openKeyboardKey;
   }

   public static void handleOpenKeyboard(Minecraft minecraft) {
      if (minecraft.gui.screen() == null && openKeyboardKey().consumeClick()) {
         minecraft.setScreenAndShow(new KeyboardScreen());
      }
   }

   public static boolean shouldCancelGuiOverlay(Minecraft minecraft) {
      return minecraft.gui.screen() instanceof KeyboardScreen;
   }
}
