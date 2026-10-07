package com.stellar.keys.atlas;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import java.util.Objects;
import net.minecraft.client.KeyMapping;

public final class KeyBindingAccess {
   private static KeyBindingAccess.Backend backend = new KeyBindingAccess.MissingBackend();

   private KeyBindingAccess() {
   }

   public static void install(KeyBindingAccess.Backend backend) {
      KeyBindingAccess.backend = Objects.requireNonNull(backend);
   }

   public static KeyBindingModifier getModifier(KeyMapping keyMapping) {
      return backend.getModifier(keyMapping);
   }

   public static Key getKey(KeyMapping keyMapping) {
      return InputConstants.getKey(keyMapping.saveString());
   }

   public static void setBinding(KeyMapping keyMapping, KeyBindingModifier modifier, Key key) {
      backend.setBinding(keyMapping, modifier, key);
   }

   public static KeyBindingModifier fromGlfwModifiers(int glfwModifiers) {
      return backend.fromGlfwModifiers(glfwModifiers);
   }

   public interface Backend {
      KeyBindingModifier getModifier(KeyMapping var1);

      void setBinding(KeyMapping var1, KeyBindingModifier var2, Key var3);

      KeyBindingModifier fromGlfwModifiers(int var1);
   }

   private static final class MissingBackend implements KeyBindingAccess.Backend {
      @Override
      public KeyBindingModifier getModifier(KeyMapping keyMapping) {
         throw new IllegalStateException("KeyBindingAccess backend has not been installed");
      }

      @Override
      public void setBinding(KeyMapping keyMapping, KeyBindingModifier modifier, Key key) {
         throw new IllegalStateException("KeyBindingAccess backend has not been installed");
      }

      @Override
      public KeyBindingModifier fromGlfwModifiers(int glfwModifiers) {
         throw new IllegalStateException("KeyBindingAccess backend has not been installed");
      }
   }
}
