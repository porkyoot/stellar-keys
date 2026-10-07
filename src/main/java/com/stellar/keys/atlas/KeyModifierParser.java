package com.stellar.keys.atlas;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/**
 * Parser for shortcuts that specify modifiers (F3 +, Ctrl +, etc.) in their translation keys,
 * display names, or bound key representations.
 */
public final class KeyModifierParser {
   private static final Pattern MODIFIER_KEY_PATTERN = Pattern.compile(
      "(?i)(?:^|[^a-z0-9])(f3|ctrl|control|shift|alt)\\s*([\\+_\\-])\\s*(?:(?:f3|ctrl|control|shift|alt)\\s*[\\+_\\-]\\s*)*([a-z0-9_]+)?"
   );

   public record ParsedShortcut(KeyBindingModifier modifier, int targetGlfwKey) {
      public boolean hasModifier() {
         return this.modifier != null && !this.modifier.isNone();
      }

      public boolean hasTargetKey() {
         return this.targetGlfwKey > 0;
      }
   }

   private KeyModifierParser() {
   }

   public static ParsedShortcut parseShortcut(String text) {
      if (text == null || text.isBlank()) {
         return new ParsedShortcut(KeyBindingModifier.NONE, -1);
      }
      Matcher matcher = MODIFIER_KEY_PATTERN.matcher(text);
      if (matcher.find()) {
         String modToken = matcher.group(1).toLowerCase(Locale.ROOT);
         KeyBindingModifier modifier = switch (modToken) {
            case "f3" -> KeyBindingModifier.F3;
            case "ctrl", "control" -> KeyBindingModifier.CONTROL;
            case "shift" -> KeyBindingModifier.SHIFT;
            case "alt" -> KeyBindingModifier.ALT;
            default -> KeyBindingModifier.NONE;
         };
         int targetKey = resolveGlfwKeyFromName(matcher.group(3));
         return new ParsedShortcut(modifier, targetKey);
      }
      return new ParsedShortcut(KeyBindingModifier.NONE, -1);
   }

   public static KeyBindingModifier parseModifier(String text) {
      return parseShortcut(text).modifier();
   }

   public static ParsedShortcut parseShortcut(KeyMapping keyMapping) {
      if (keyMapping == null) {
         return new ParsedShortcut(KeyBindingModifier.NONE, -1);
      }
      String name = keyMapping.getName();
      if ("key.debug.modifier".equals(name)) {
         return new ParsedShortcut(KeyBindingModifier.NONE, -1);
      }
      if ("key.saveToolbarActivator".equals(name) || "key.loadToolbarActivator".equals(name)) {
         return new ParsedShortcut(KeyBindingModifier.CONTROL, -1);
      }

      KeyBindingModifier foundModifier = KeyBindingModifier.NONE;
      int foundTargetKey = -1;

      // 1. Try keyMapping.getName()
      ParsedShortcut fromName = parseShortcut(name);
      if (fromName.hasModifier()) {
         foundModifier = fromName.modifier();
         if (fromName.hasTargetKey()) {
            foundTargetKey = fromName.targetGlfwKey();
         }
      }

      // 2. Try display / localized label
      if (!foundModifier.isNone() && foundTargetKey > 0) {
         return new ParsedShortcut(foundModifier, foundTargetKey);
      }
      try {
         String label = Component.translatable(name).getString();
         if (label != null && !label.isBlank() && !label.equals(name)) {
            ParsedShortcut fromLabel = parseShortcut(label);
            if (fromLabel.hasModifier()) {
               foundModifier = fromLabel.modifier();
               if (fromLabel.hasTargetKey()) {
                  foundTargetKey = fromLabel.targetGlfwKey();
               }
            }
         }
      } catch (Exception ignored) {
      }

      // 3. Try bound key display name and name
      if (!foundModifier.isNone() && foundTargetKey > 0) {
         return new ParsedShortcut(foundModifier, foundTargetKey);
      }
      try {
         var key = KeyBindingAccess.getKey(keyMapping);
         if (key != null) {
            String keyDisplayName = key.getDisplayName() != null ? key.getDisplayName().getString() : null;
            if (keyDisplayName != null && !keyDisplayName.isBlank()) {
               ParsedShortcut fromKeyDisplay = parseShortcut(keyDisplayName);
               if (fromKeyDisplay.hasModifier()) {
                  foundModifier = fromKeyDisplay.modifier();
                  if (fromKeyDisplay.hasTargetKey()) {
                     foundTargetKey = fromKeyDisplay.targetGlfwKey();
                  }
               }
            }
            if (foundTargetKey <= 0) {
               ParsedShortcut fromKeyName = parseShortcut(key.getName());
               if (fromKeyName.hasModifier()) {
                  if (foundModifier.isNone()) {
                     foundModifier = fromKeyName.modifier();
                  }
                  if (fromKeyName.hasTargetKey()) {
                     foundTargetKey = fromKeyName.targetGlfwKey();
                  }
               }
            }
         }
      } catch (Exception ignored) {
      }

      // 4. Fallback for DEBUG category or key.debug. prefix
      if (foundModifier.isNone()) {
         if (keyMapping.getCategory() == KeyMapping.Category.DEBUG || (name != null && name.startsWith("key.debug."))) {
            foundModifier = KeyBindingModifier.F3;
         }
      }

      return new ParsedShortcut(foundModifier, foundTargetKey);
   }

   public static KeyBindingModifier parseModifier(KeyMapping keyMapping) {
      return parseShortcut(keyMapping).modifier();
   }

   public static int resolveGlfwKey(KeyMapping keyMapping, KeyBindingModifier modifier, int defaultGlfwKey) {
      if (defaultGlfwKey > 0 && !isModifierPlaceholderKey(modifier, defaultGlfwKey)) {
         return defaultGlfwKey;
      }
      ParsedShortcut parsed = parseShortcut(keyMapping);
      if (parsed.hasTargetKey()) {
         return parsed.targetGlfwKey();
      }
      return defaultGlfwKey;
   }

   public static boolean isModifierPlaceholderKey(KeyBindingModifier modifier, int glfwKey) {
      if (glfwKey <= 0) {
         return true;
      }
      if (modifier == null) {
         return false;
      }
      return switch (modifier) {
         case F3 -> glfwKey == 292; // GLFW_KEY_F3
         case CONTROL -> glfwKey == 341 || glfwKey == 345; // GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL
         case SHIFT -> glfwKey == 340 || glfwKey == 344; // GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT
         case ALT -> glfwKey == 342 || glfwKey == 346; // GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT
         case NONE -> false;
      };
   }

   public static int resolveGlfwKeyFromName(String keyName) {
      if (keyName == null || keyName.isBlank()) {
         return -1;
      }
      String clean = keyName.trim().toUpperCase(Locale.ROOT);
      if (clean.length() == 1) {
         char ch = clean.charAt(0);
         if (ch >= 'A' && ch <= 'Z') {
            return ch;
         }
         if (ch >= '0' && ch <= '9') {
            return ch;
         }
         return switch (ch) {
            case ' ' -> 32;
            case '-' -> 45;
            case '=' -> 61;
            case '[' -> 91;
            case ']' -> 93;
            case '\\' -> 92;
            case ';' -> 59;
            case '\'' -> 39;
            case '`' -> 96;
            case ',' -> 44;
            case '.' -> 46;
            case '/' -> 47;
            default -> -1;
         };
      }
      if (clean.startsWith("F") && clean.length() <= 3) {
         try {
            int fNum = Integer.parseInt(clean.substring(1));
            if (fNum >= 1 && fNum <= 12) {
               return 290 + (fNum - 1);
            }
         } catch (NumberFormatException ignored) {
         }
      }
      return switch (clean) {
         case "ESC", "ESCAPE" -> 256;
         case "ENTER", "RETURN" -> 257;
         case "TAB" -> 258;
         case "BACKSPACE", "BS" -> 259;
         case "INSERT", "INS" -> 260;
         case "DELETE", "DEL" -> 261;
         case "RIGHT" -> 262;
         case "LEFT" -> 263;
         case "DOWN" -> 264;
         case "UP" -> 265;
         case "PAGE_UP", "PAGEUP", "PGUP" -> 266;
         case "PAGE_DOWN", "PAGEDOWN", "PGDN" -> 267;
         case "HOME" -> 268;
         case "END" -> 269;
         case "CAPS_LOCK", "CAPSLOCK", "CAPS" -> 280;
         case "SPACE" -> 32;
         default -> -1;
      };
   }
}
