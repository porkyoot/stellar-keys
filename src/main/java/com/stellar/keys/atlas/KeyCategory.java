package com.stellar.keys.atlas;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class KeyCategory {
   public static final String DEFAULT_ID = "assigned";
   public static final String HIDDEN_ID = "hidden";
   private static final List<KeyCategory.PresetColor> PRESET_COLORS = List.of(
      preset("crimson", "Crimson", -4834233),
      preset("purple", "Purple", -7256618),
      preset("emerald", "Emerald", -14704819),
      preset("tangerine", "Tangerine", -429290),
      preset("amber", "Amber", -680437),
      preset("cyan", "Cyan", -16337196),
      preset("rose", "Rose", -1292135),
      preset("lime", "Lime", -8074218),
      preset("teal", "Teal", -15419226),
      preset("copper", "Copper", -4959479)
   );
   private static final List<KeyCategory> DEFAULT_CUSTOM_CATEGORIES = List.of(
      custom("movement", "Movement", -14704819),
      custom("gameplay", "Gameplay", -4834233),
      custom("inventory", "Inventory", -429290),
      custom("creative", "Creative", -680437),
      custom("multiplayer", "Multiplayer", -7256618),
      custom("ui", "Interface", -16337196),
      custom("spectator", "Spectator", -15419226),
      custom("misc", "Miscellaneous", -4959479)
   );
   private final String id;
   private final String name;
   private final int fillColor;
   private final int textColor;
   private final boolean deletable;
   private final boolean hiddenCategory;

   private KeyCategory(String id, String name, int fillColor, int textColor, boolean deletable, boolean hiddenCategory) {
      this.id = id;
      this.name = name;
      this.fillColor = fillColor;
      this.textColor = textColor;
      this.deletable = deletable;
      this.hiddenCategory = hiddenCategory;
   }

   public static KeyCategory assignedCategory() {
      return new KeyCategory("assigned", "Assigned", KeyVisualStyle.keyAssignedFill(), KeyVisualStyle.keyAssignedText(), false, false);
   }

   public static KeyCategory hiddenCategory() {
      return new KeyCategory("hidden", "Hidden keybind", KeyVisualStyle.keyHiddenFill(), KeyVisualStyle.keyHiddenText(), false, true);
   }

   public static KeyCategory custom(String id, String name, int fillColor) {
      return custom(id, name, fillColor, autoTextColor(fillColor), true);
   }

   public static KeyCategory.PresetColor preset(String id, String name, int fillColor) {
      return new KeyCategory.PresetColor(normalizeId(id), sanitizeName(name), fillColor);
   }

   public static KeyCategory custom(String id, String name, int fillColor, int textColor, boolean deletable) {
      return new KeyCategory(normalizeId(id), sanitizeName(name), fillColor, textColor, deletable, false);
   }

   public static List<KeyCategory> defaultCustomCategories() {
      return DEFAULT_CUSTOM_CATEGORIES;
   }

   public static List<KeyCategory.PresetColor> presetColors() {
      return PRESET_COLORS;
   }

   public static KeyCategory.PresetColor presetColor(int fillColor) {
      return presetColor(PRESET_COLORS, fillColor);
   }

   public static KeyCategory.PresetColor presetColor(List<KeyCategory.PresetColor> presetColors, int fillColor) {
      if (presetColors == null) {
         return null;
      } else {
         for (KeyCategory.PresetColor presetColor : presetColors) {
            if (presetColor.fillColor() == fillColor) {
               return presetColor;
            }
         }

         return null;
      }
   }

   public static String presetColorName(int fillColor) {
      return presetColorName(PRESET_COLORS, fillColor);
   }

   public static String presetColorName(List<KeyCategory.PresetColor> presetColors, int fillColor) {
      KeyCategory.PresetColor presetColor = presetColor(presetColors, fillColor);
      return presetColor != null ? presetColor.name() : String.format(Locale.ROOT, "#%06X", fillColor & 16777215);
   }

   public static int defaultColorForId(String id) {
      if (id == null) {
         return PRESET_COLORS.get(0).fillColor();
      }
      return switch (id.toLowerCase(Locale.ROOT)) {
         case "movement" -> -14704819;
         case "gameplay" -> -4834233;
         case "inventory" -> -429290;
         case "creative" -> -680437;
         case "multiplayer" -> -7256618;
         case "ui" -> -16337196;
         case "spectator" -> -15419226;
         case "misc" -> -4959479;
         case "debug" -> -8074218;
         default -> {
            int colorIndex = Math.abs(id.hashCode()) % PRESET_COLORS.size();
            yield PRESET_COLORS.get(colorIndex).fillColor();
         }
      };
   }

   public static String normalizeId(String raw) {
      if (raw == null) {
         return "category";
      } else {
         String lower = raw.trim().toLowerCase(Locale.ROOT);
         StringBuilder builder = new StringBuilder(lower.length());
         boolean lastDash = false;

         for (int index = 0; index < lower.length(); index++) {
            char current = lower.charAt(index);
            if ((current < 'a' || current > 'z') && (current < '0' || current > '9')) {
               if (!lastDash && builder.length() > 0) {
                  builder.append('-');
                  lastDash = true;
               }
            } else {
               builder.append(current);
               lastDash = false;
            }
         }

         int length = builder.length();
         if (length > 0 && builder.charAt(length - 1) == '-') {
            builder.deleteCharAt(length - 1);
         }

         String normalized = builder.toString();
         if (normalized.isEmpty()) {
            return "category";
         } else {
            return !"assigned".equals(normalized) && !"hidden".equals(normalized) ? normalized : normalized + "-category";
         }
      }
   }

   public static String sanitizeName(String raw) {
      if (raw == null) {
         return "";
      } else {
         String sanitized = raw.replace('|', '/').trim().replaceAll("\\s+", " ");
         if (sanitized.length() > 24) {
            sanitized = sanitized.substring(0, 24).trim();
         }

         return sanitized;
      }
   }

   public static int autoTextColor(int fillColor) {
      int red = fillColor >>> 16 & 0xFF;
      int green = fillColor >>> 8 & 0xFF;
      int blue = fillColor & 0xFF;
      double luminance = (0.2126 * (double)red + 0.7152 * (double)green + 0.0722 * (double)blue) / 255.0;
      return luminance >= 0.58 ? -15658216 : -1;
   }

   public String id() {
      return this.id;
   }

   public String name() {
      return this.name;
   }

   public int fillColor() {
      return this.fillColor;
   }

   public int textColor() {
      return this.textColor;
   }

   public boolean deletable() {
      return this.deletable;
   }

   public boolean isHiddenCategory() {
      return this.hiddenCategory;
   }

   public boolean isAssignedCategory() {
      return "assigned".equals(this.id);
   }

   public boolean isCustomCategory() {
      return !this.isAssignedCategory() && !this.hiddenCategory;
   }

   @Override
   public boolean equals(Object other) {
      if (this == other) {
         return true;
      } else {
         return !(other instanceof KeyCategory that)
            ? false
            : this.fillColor == that.fillColor
               && this.textColor == that.textColor
               && this.deletable == that.deletable
               && this.hiddenCategory == that.hiddenCategory
               && this.id.equals(that.id)
               && this.name.equals(that.name);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.id, this.name, this.fillColor, this.textColor, this.deletable, this.hiddenCategory);
   }

   public static final class PresetColor {
      private final String id;
      private final String name;
      private final int fillColor;

      private PresetColor(String id, String name, int fillColor) {
         this.id = id;
         this.name = name;
         this.fillColor = fillColor;
      }

      public String id() {
         return this.id;
      }

      public String name() {
         return this.name;
      }

      public int fillColor() {
         return this.fillColor;
      }
   }
}
