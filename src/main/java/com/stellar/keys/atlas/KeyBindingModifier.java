package com.stellar.keys.atlas;

public enum KeyBindingModifier {
   NONE,
   F3,
   CONTROL,
   SHIFT,
   ALT;

   public boolean isNone() {
      return this == NONE;
   }

   public String localizedLabel() {
      return switch (this) {
         case NONE -> "";
         case F3 -> "F3";
         case CONTROL -> AtlasText.text("ui.modifier.ctrl");
         case SHIFT -> AtlasText.text("ui.modifier.shift");
         case ALT -> AtlasText.text("ui.modifier.alt");
      };
   }

   public String layerDisplayName() {
      return switch (this) {
         case NONE -> AtlasText.text("ui.layer.base");
         case F3 -> "F3 +";
         case CONTROL -> "Ctrl +";
         case SHIFT -> "Shift +";
         case ALT -> "Alt +";
      };
   }

   public String formatKeyName(String keyName) {
      return this.isNone() ? keyName : this.localizedLabel() + " + " + keyName;
   }
}
