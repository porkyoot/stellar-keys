package com.stellar.keys.atlas.edit;

import com.stellar.keys.atlas.KeyBindingAccess;
import com.stellar.keys.atlas.KeyBindingModifier;
import com.stellar.keys.atlas.KeyModifierParser;
import com.stellar.keys.atlas.layout.KeyboardBindingCache;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

public final class EditModeState {
   private final Map<String, EditModeState.OriginalBinding> originalBindings = new LinkedHashMap<>();
   private final List<EditModeState.EditAction> undoHistory = new ArrayList<>();
   private boolean active = false;

   public boolean isActive() {
      return this.active;
   }

   public boolean canUndo() {
      return !this.undoHistory.isEmpty();
   }

   public void enterEditMode(KeyMapping[] keyMappings) {
      this.active = true;
      this.originalBindings.clear();
      this.undoHistory.clear();

      for (KeyMapping km : keyMappings) {
         this.originalBindings.put(km.getName(), new EditModeState.OriginalBinding(KeyBindingAccess.getKey(km), KeyBindingAccess.getModifier(km)));
      }
   }

   public void removeBinding(KeyMapping keyMapping) {
      this.undoHistory.add(new EditModeState.EditAction(keyMapping.getName(), KeyBindingAccess.getKey(keyMapping), KeyBindingAccess.getModifier(keyMapping)));
      KeyBindingAccess.setBinding(keyMapping, KeyBindingModifier.NONE, InputConstants.UNKNOWN);
      KeyMapping.resetMapping();
   }

   public void updateBinding(KeyMapping keyMapping, Key newKey, KeyBindingModifier modifier) {
      this.undoHistory.add(new EditModeState.EditAction(keyMapping.getName(), KeyBindingAccess.getKey(keyMapping), KeyBindingAccess.getModifier(keyMapping)));
      KeyBindingAccess.setBinding(keyMapping, modifier, newKey);
      KeyMapping.resetMapping();
   }

   public void undo(KeyMapping[] keyMappings) {
      if (!this.undoHistory.isEmpty()) {
         EditModeState.EditAction last = this.undoHistory.remove(this.undoHistory.size() - 1);

         for (KeyMapping km : keyMappings) {
            if (km.getName().equals(last.keyMappingName)) {
               KeyBindingAccess.setBinding(km, last.previousModifier, last.previousKey);
               break;
            }
         }

         KeyMapping.resetMapping();
      }
   }

   public void confirm(Options options) {
      this.active = false;
      this.originalBindings.clear();
      this.undoHistory.clear();
      options.save();
   }

   public void exitEditMode() {
      this.active = false;
      this.originalBindings.clear();
      this.undoHistory.clear();
   }

   public void discard(KeyMapping[] keyMappings) {
      for (KeyMapping km : keyMappings) {
         EditModeState.OriginalBinding orig = this.originalBindings.get(km.getName());
         if (orig != null) {
            KeyBindingAccess.setBinding(km, orig.modifier, orig.key);
         }
      }

      KeyMapping.resetMapping();
      this.active = false;
      this.originalBindings.clear();
      this.undoHistory.clear();
   }

   public void resetAllToDefaults(KeyMapping[] keyMappings) {
      for (KeyMapping km : keyMappings) {
         Key currentKey = KeyBindingAccess.getKey(km);
         if (!currentKey.equals(km.getDefaultKey()) || !KeyBindingAccess.getModifier(km).isNone()) {
            this.undoHistory.add(new EditModeState.EditAction(km.getName(), currentKey, KeyBindingAccess.getModifier(km)));
         }
      }

      for (KeyMapping kmx : keyMappings) {
         KeyBindingAccess.setBinding(kmx, KeyBindingModifier.NONE, kmx.getDefaultKey());
      }

      KeyMapping.resetMapping();
   }

   public KeyMapping findKeyMapping(KeyMapping[] keyMappings, String displayLabel, int glfwKey) {
      for (KeyMapping km : keyMappings) {
         String label = KeyboardBindingCache.resolveBindingLabel(km);
         Key inputKey = KeyBindingAccess.getKey(km);
         int key;
         if (inputKey.getType() == Type.MOUSE) {
            key = -100 + inputKey.getValue();
         } else {
            key = inputKey.getValue();
         }

         KeyBindingModifier modifier = KeyBindingAccess.getModifier(km);
         int resolvedKey = !modifier.isNone() ? KeyModifierParser.resolveGlfwKey(km, modifier, key) : key;

         if (label.equals(displayLabel) && (key == glfwKey || resolvedKey == glfwKey)) {
            return km;
         }
      }

      return null;
   }

   public KeyMapping findKeyMappingByName(KeyMapping[] keyMappings, String translationKey) {
      for (KeyMapping km : keyMappings) {
         if (km.getName().equals(translationKey)) {
            return km;
         }
      }

      return null;
   }

   public Map<String, List<EditModeState.UnassignedEntry>> buildUnassignedBindings(KeyMapping[] keyMappings) {
      Map<String, List<EditModeState.UnassignedEntry>> result = new LinkedHashMap<>();

      for (KeyMapping km : keyMappings) {
         if (KeyBindingAccess.getKey(km).equals(InputConstants.UNKNOWN)) {
            String source = KeyboardBindingCache.resolveSourceName(km);
            String label = KeyboardBindingCache.resolveBindingLabel(km);
            result.computeIfAbsent(source, k -> new ArrayList<>()).add(new EditModeState.UnassignedEntry(label, km.getName()));
         }
      }

      for (List<EditModeState.UnassignedEntry> list : result.values()) {
         list.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
      }

      return result;
   }

   public int countUnassigned(KeyMapping[] keyMappings) {
      int count = 0;

      for (KeyMapping km : keyMappings) {
         if (KeyBindingAccess.getKey(km).equals(InputConstants.UNKNOWN)) {
            count++;
         }
      }

      return count;
   }

   private static final class EditAction {
      final String keyMappingName;
      final Key previousKey;
      final KeyBindingModifier previousModifier;

      EditAction(String name, Key key, KeyBindingModifier modifier) {
         this.keyMappingName = name;
         this.previousKey = key;
         this.previousModifier = modifier;
      }
   }

   private static final class OriginalBinding {
      final Key key;
      final KeyBindingModifier modifier;

      OriginalBinding(Key key, KeyBindingModifier modifier) {
         this.key = key;
         this.modifier = modifier;
      }
   }

   public static final class UnassignedEntry {
      final String label;
      final String translationKey;

      UnassignedEntry(String label, String translationKey) {
         this.label = label;
         this.translationKey = translationKey;
      }
   }
}
