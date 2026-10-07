package com.stellar.keys.atlas.layout;

import com.stellar.keys.atlas.KeyBindingAccess;
import com.stellar.keys.atlas.KeyBindingModifier;
import com.stellar.keys.atlas.KeyModifierParser;
import com.stellar.keys.atlas.ModDisplayNameLookup;
import com.stellar.keys.atlas.box.BoxDimensions;
import com.stellar.keys.atlas.box.BoxTextLayout;
import com.stellar.keys.atlas.box.BoxTextLayoutFactory;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class KeyboardBindingCache {
   public static final int MOUSE_BUTTON_OFFSET = -100;
   private static final KeyboardBindingCache SHARED_INSTANCE = new KeyboardBindingCache();
   private static final String VANILLA_SOURCE = "Minecraft";
   private static final Map<String, String> DISPLAY_LABEL_CACHE = new HashMap<>();
   private static final Map<String, String> SOURCE_NAME_CACHE = new HashMap<>();
   private static final Set<String> VANILLA_NAME_SEGMENTS = Set.of(
      "advancements",
      "attack",
      "back",
      "chat",
      "command",
      "drop",
      "forward",
      "fullscreen",
      "hotbar",
      "inventory",
      "jump",
      "left",
      "loadToolbarActivator",
      "pickItem",
      "playerlist",
      "right",
      "saveToolbarActivator",
      "screenshot",
      "smoothCamera",
      "socialInteractions",
      "spectatorOutlines",
      "sprint",
      "swapOffhand",
      "sneak",
      "togglePerspective",
      "use"
   );
   private static final Set<String> VANILLA_CATEGORY_SEGMENTS = Set.of("creative", "gameplay", "inventory", "misc", "movement", "multiplayer", "ui");
   private final Map<Integer, List<KeyAssignmentInfo>> assignmentsByKey = new HashMap<>();
   private final Map<KeyboardBindingCache.LayoutCacheKey, BoxTextLayout> layoutByState = new HashMap<>();
   private KeyBindingModifier currentLayer = KeyBindingModifier.NONE;
   private int keymapSignature = Integer.MIN_VALUE;
   private int pendingKeymapSignature = Integer.MIN_VALUE;
   private KeyMapping[] pendingKeyMappings = new KeyMapping[0];
   private int pendingKeymapIndex = 0;
   private int pendingKeymapTotal = 0;
   private BoxTextLayout emptyLayout = new BoxTextLayout(List.of(), new BoxDimensions(0, 0));

   public static KeyboardBindingCache shared() {
      return SHARED_INSTANCE;
   }

   public void markDirty() {
      this.assignmentsByKey.clear();
      this.layoutByState.clear();
      DISPLAY_LABEL_CACHE.clear();
      SOURCE_NAME_CACHE.clear();
      this.keymapSignature = Integer.MIN_VALUE;
      this.pendingKeymapSignature = Integer.MIN_VALUE;
      this.pendingKeyMappings = new KeyMapping[0];
      this.pendingKeymapIndex = 0;
      this.pendingKeymapTotal = 0;
   }

   public void clearLayoutCache() {
      this.layoutByState.clear();
   }

   public KeyboardBindingCache.RefreshResult refreshIfNeeded(KeyMapping[] keyMappings, int maxEntries) {
      int nextSignature = computeKeymapSignature(keyMappings);
      if (nextSignature != this.keymapSignature && nextSignature != this.pendingKeymapSignature) {
         this.beginIncrementalRebuild(nextSignature, keyMappings);
      }

      if (this.pendingKeymapSignature == Integer.MIN_VALUE) {
         return new KeyboardBindingCache.RefreshResult(false, true, 0, 0);
      } else {
         int totalMappings = this.pendingKeymapTotal;
         boolean changed = maxEntries > 0 && this.processIncrementalRebuild(maxEntries);
         boolean complete = this.pendingKeymapSignature == Integer.MIN_VALUE;
         int processedMappings = complete ? totalMappings : this.pendingKeymapIndex;
         return new KeyboardBindingCache.RefreshResult(changed, complete, processedMappings, totalMappings);
      }
   }

   public KeyBindingModifier getCurrentLayer() {
      return this.currentLayer;
   }

   public void setCurrentLayer(KeyBindingModifier layer) {
      KeyBindingModifier next = layer != null ? layer : KeyBindingModifier.NONE;
      if (this.currentLayer != next) {
         this.currentLayer = next;
         this.layoutByState.clear();
      }
   }

   public boolean hasBinding(int glfwKey) {
      return this.hasBinding(glfwKey, this.currentLayer);
   }

   public boolean hasBinding(int glfwKey, KeyBindingModifier layer) {
      List<KeyAssignmentInfo> list = this.assignmentsByKey.get(glfwKey);
      if (list == null || list.isEmpty()) {
         return false;
      }
      for (KeyAssignmentInfo info : list) {
         if (info.modifier == layer) {
            return true;
         }
      }
      return false;
   }

   public BoxTextLayout getBoxTextLayout(int glfwKey, boolean hovered, boolean editMode, int visibleBindingBoxCount, BoxTextLayoutFactory layoutFactory) {
      List<KeyAssignmentInfo> assignments = this.assignmentsForKey(glfwKey, this.currentLayer);
      if (assignments != null && !assignments.isEmpty()) {
         KeyboardBindingCache.LayoutCacheKey cacheKey = new KeyboardBindingCache.LayoutCacheKey(glfwKey, this.currentLayer, hovered, editMode, visibleBindingBoxCount);
         return this.layoutByState.computeIfAbsent(cacheKey, ignored -> layoutFactory.create(assignments, hovered, editMode, visibleBindingBoxCount));
      } else {
         return this.emptyLayout;
      }
   }

   public int getBindingCountForLayer(KeyBindingModifier layer) {
      int count = 0;
      for (List<KeyAssignmentInfo> list : this.assignmentsByKey.values()) {
         for (KeyAssignmentInfo info : list) {
            if (info.modifier == layer) {
               count++;
            }
         }
      }
      return count;
   }

   public List<KeyAssignmentInfo> allAssignmentsForKey(int glfwKey) {
      List<KeyAssignmentInfo> list = this.assignmentsByKey.get(glfwKey);
      return list != null ? list : List.of();
   }

   public boolean hasBindingAnyLayer(int glfwKey) {
      List<KeyAssignmentInfo> list = this.assignmentsByKey.get(glfwKey);
      return list != null && !list.isEmpty();
   }

   public Set<KeyBindingModifier> layersForKey(int glfwKey) {
      List<KeyAssignmentInfo> list = this.assignmentsByKey.get(glfwKey);
      if (list == null || list.isEmpty()) {
         return Set.of();
      }
      Set<KeyBindingModifier> set = new LinkedHashSet<>();
      for (KeyAssignmentInfo info : list) {
         set.add(info.modifier);
      }
      return set;
   }

   public boolean hasDirectConflict(int glfwKey, KeyBindingModifier layer) {
      return this.assignmentsForKey(glfwKey, layer).size() > 1;
   }

   public boolean hasDirectConflict(int glfwKey) {
      return this.hasDirectConflict(glfwKey, this.currentLayer);
   }

   public boolean hasDirectConflictAnyLayer(int glfwKey) {
      List<KeyAssignmentInfo> list = this.assignmentsByKey.get(glfwKey);
      if (list == null || list.size() < 2) {
         return false;
      }
      Map<KeyBindingModifier, Integer> counts = new HashMap<>();
      for (KeyAssignmentInfo info : list) {
         int c = counts.merge(info.modifier, 1, Integer::sum);
         if (c > 1) {
            return true;
         }
      }
      return false;
   }

   public boolean hasSoftConflict(int glfwKey) {
      Set<KeyBindingModifier> layers = this.layersForKey(glfwKey);
      if (layers.isEmpty()) {
         return false;
      }
      if (layers.size() > 1) {
         return true;
      }
      return !layers.contains(this.currentLayer);
   }

   public int getConflictCountForLayer(KeyBindingModifier layer) {
      int count = 0;
      for (List<KeyAssignmentInfo> list : this.assignmentsByKey.values()) {
         int inLayer = 0;
         for (KeyAssignmentInfo info : list) {
            if (info.modifier == layer) {
               inLayer++;
            }
         }
         if (inLayer > 1) {
            count++;
         }
      }
      return count;
   }

   public int completedSignature() {
      return this.keymapSignature * 31 + this.currentLayer.ordinal();
   }

   private void beginIncrementalRebuild(int nextSignature, KeyMapping[] keyMappings) {
      this.assignmentsByKey.clear();
      this.layoutByState.clear();
      DISPLAY_LABEL_CACHE.clear();
      SOURCE_NAME_CACHE.clear();
      this.pendingKeymapSignature = nextSignature;
      this.pendingKeyMappings = keyMappings;
      this.pendingKeymapIndex = 0;
      this.pendingKeymapTotal = keyMappings.length;
   }

   private boolean processIncrementalRebuild(int maxEntries) {
      if (this.pendingKeymapSignature == Integer.MIN_VALUE) {
         return false;
      } else if (this.pendingKeymapIndex >= this.pendingKeymapTotal) {
         this.keymapSignature = this.pendingKeymapSignature;
         this.pendingKeymapSignature = Integer.MIN_VALUE;
         this.pendingKeyMappings = new KeyMapping[0];
         this.pendingKeymapTotal = 0;
         this.pendingKeymapIndex = 0;
         return true;
      } else {
         for (int limit = Math.min(this.pendingKeymapTotal, this.pendingKeymapIndex + maxEntries); this.pendingKeymapIndex < limit; this.pendingKeymapIndex++) {
            this.addKeyMapping(this.pendingKeyMappings[this.pendingKeymapIndex]);
         }

         this.layoutByState.clear();
         if (this.pendingKeymapIndex >= this.pendingKeymapTotal) {
            this.keymapSignature = this.pendingKeymapSignature;
            this.pendingKeymapSignature = Integer.MIN_VALUE;
            this.pendingKeyMappings = new KeyMapping[0];
            this.pendingKeymapTotal = 0;
            this.pendingKeymapIndex = 0;
         }

         return true;
      }
   }

   private void addKeyMapping(KeyMapping keyMapping) {
      KeyBindingModifier modifier = KeyBindingAccess.getModifier(keyMapping);
      if (modifier == null || modifier.isNone()) {
         modifier = KeyModifierParser.parseModifier(keyMapping);
      }
      int glfwKey = this.resolveGlfwKey(KeyBindingAccess.getKey(keyMapping));
      if (!modifier.isNone()) {
         glfwKey = KeyModifierParser.resolveGlfwKey(keyMapping, modifier, glfwKey);
      }
      List<KeyAssignmentInfo> assignments = this.assignmentsByKey.get(glfwKey);
      if (assignments == null) {
         assignments = new ArrayList<>();
         this.assignmentsByKey.put(glfwKey, assignments);
      }

      String categoryId = resolveCategoryId(keyMapping);
      String categoryName = resolveCategoryName(keyMapping);
      assignments.add(new KeyAssignmentInfo(
         resolveBindingLabel(keyMapping),
         resolveSourceName(keyMapping),
         extractModifierPrefix(keyMapping, modifier),
         modifier,
         categoryId,
         categoryName
      ));
   }

   public static String resolveCategoryId(KeyMapping keyMapping) {
      if (keyMapping.getCategory() != null && keyMapping.getCategory().id() != null) {
         return keyMapping.getCategory().id().getPath();
      }
      return "misc";
   }

   public static String resolveCategoryName(KeyMapping keyMapping) {
      if (keyMapping.getCategory() != null) {
         Component label = keyMapping.getCategory().label();
         if (label != null) {
            return label.getString();
         }
      }
      return "Miscellaneous";
   }

   private int resolveGlfwKey(Key inputKey) {
      return inputKey.getType() == Type.MOUSE ? -100 + inputKey.getValue() : inputKey.getValue();
   }

   private static int computeKeymapSignature(KeyMapping[] keyMappings) {
      int hash = currentLanguageToken().hashCode();

      for (KeyMapping keyMapping : keyMappings) {
         hash = 31 * hash + keyMapping.getName().hashCode();
         hash = 31 * hash + keyMapping.getCategory().hashCode();
         Key inputKey = KeyBindingAccess.getKey(keyMapping);
         if (inputKey.getType() == Type.MOUSE) {
            hash = 31 * hash + -100 + inputKey.getValue();
         } else {
            hash = 31 * hash + inputKey.getValue();
         }

         KeyBindingModifier modifier = KeyBindingAccess.getModifier(keyMapping);
         if (modifier == null || modifier.isNone()) {
            modifier = KeyModifierParser.parseModifier(keyMapping);
         }
         hash = 31 * hash + modifier.ordinal();
      }

      return hash;
   }

   private static String currentLanguageToken() {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft != null && minecraft.options != null && minecraft.options.languageCode != null ? minecraft.options.languageCode : "en_us";
   }

   public static String resolveBindingLabel(KeyMapping keyMapping) {
      return DISPLAY_LABEL_CACHE.computeIfAbsent(keyMapping.getName(), translationKey -> Component.translatable(translationKey).getString());
   }

   public static String resolveSourceName(KeyMapping keyMapping) {
      String modId = resolveModId(keyMapping);
      return modId != null && !"minecraft".equals(modId)
         ? SOURCE_NAME_CACHE.computeIfAbsent(modId, KeyboardBindingCache::resolveSourceNameForModId)
         : "Minecraft";
   }

   private static String resolveModId(KeyMapping keyMapping) {
      String modId = null;
      if (keyMapping.getCategory() != null && keyMapping.getCategory().id() != null) {
         String ns = keyMapping.getCategory().id().getNamespace();
         if (!"minecraft".equals(ns)) {
            modId = ns;
         }
      }
      if (modId == null) {
         modId = extractNameModId(keyMapping.getName());
      }

      return modId;
   }

   private static String resolveSourceNameForModId(String modId) {
      return ModDisplayNameLookup.findDisplayName(modId).orElseGet(() -> humanizeModId(modId));
   }

   private static String extractCategoryModId(String translationKey) {
      if (translationKey != null && !translationKey.isBlank()) {
         String[] parts = translationKey.split("\\.");
         if (parts.length >= 3 && "key".equals(parts[0]) && "categories".equals(parts[1])) {
            String candidate = sanitizeModId(parts[2]);
            return candidate != null && !VANILLA_CATEGORY_SEGMENTS.contains(candidate) ? candidate : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String extractNameModId(String translationKey) {
      if (translationKey != null && !translationKey.isBlank()) {
         String[] parts = translationKey.split("\\.");
         if (parts.length >= 3 && "key".equals(parts[0])) {
            String candidate = sanitizeModId(parts[1]);
            return candidate != null && !VANILLA_NAME_SEGMENTS.contains(candidate) ? candidate : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String sanitizeModId(String candidate) {
      if (candidate != null && !candidate.isBlank()) {
         for (int i = 0; i < candidate.length(); i++) {
            char ch = candidate.charAt(i);
            boolean valid = ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '_' || ch == '-';
            if (!valid) {
               return null;
            }
         }

         return candidate;
      } else {
         return null;
      }
   }

   private static String humanizeModId(String modId) {
      String[] parts = modId.split("[_-]");
      StringBuilder result = new StringBuilder();

      for (String part : parts) {
         if (!part.isEmpty()) {
            if (result.length() > 0) {
               result.append(' ');
            }

            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
               result.append(part.substring(1));
            }
         }
      }

      return result.length() > 0 ? result.toString() : modId;
   }

   private static String extractModifierPrefix(KeyMapping keyMapping, KeyBindingModifier modifier) {
      if (modifier != null && !modifier.isNone()) {
         return modifier.localizedLabel();
      }
      KeyBindingModifier parsed = KeyModifierParser.parseModifier(keyMapping);
      return parsed.isNone() ? null : parsed.localizedLabel();
   }

   public List<String> sourcesWithAssignments() {
      Set<String> set = new LinkedHashSet<>();

      for (List<KeyAssignmentInfo> list : this.assignmentsByKey.values()) {
         for (KeyAssignmentInfo info : list) {
            set.add(info.source);
         }
      }

      return new ArrayList<>(set);
   }

   public List<KeyAssignmentInfo> assignmentsForKey(int glfwKey) {
      return this.assignmentsForKey(glfwKey, this.currentLayer);
   }

   public List<KeyAssignmentInfo> assignmentsForKey(int glfwKey, KeyBindingModifier layer) {
      List<KeyAssignmentInfo> list = this.assignmentsByKey.get(glfwKey);
      if (list == null || list.isEmpty()) {
         return List.of();
      }
      List<KeyAssignmentInfo> filtered = new ArrayList<>();
      for (KeyAssignmentInfo info : list) {
         if (info.modifier == layer) {
            filtered.add(info);
         }
      }
      return filtered;
   }

   public Map<String, List<String>> bindingsBySource() {
      Map<String, LinkedHashSet<String>> map = new LinkedHashMap<>();

      for (List<KeyAssignmentInfo> list : this.assignmentsByKey.values()) {
         for (KeyAssignmentInfo info : list) {
            map.computeIfAbsent(info.source, k -> new LinkedHashSet<>()).add(info.label);
         }
      }

      Map<String, List<String>> result = new LinkedHashMap<>();

      for (Entry<String, LinkedHashSet<String>> entry : map.entrySet()) {
         List<String> sorted = new ArrayList<>(entry.getValue());
         Collections.sort(sorted);
         result.put(entry.getKey(), sorted);
      }

      return result;
   }

   private static final class LayoutCacheKey {
      final int glfwKey;
      final KeyBindingModifier layer;
      final boolean hovered;
      final boolean editMode;
      final int visibleBindingBoxCount;

      private LayoutCacheKey(int glfwKey, KeyBindingModifier layer, boolean hovered, boolean editMode, int visibleBindingBoxCount) {
         this.glfwKey = glfwKey;
         this.layer = layer;
         this.hovered = hovered;
         this.editMode = editMode;
         this.visibleBindingBoxCount = visibleBindingBoxCount;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardBindingCache.LayoutCacheKey that)
               ? false
               : this.glfwKey == that.glfwKey
                  && this.layer == that.layer
                  && this.hovered == that.hovered
                  && this.editMode == that.editMode
                  && this.visibleBindingBoxCount == that.visibleBindingBoxCount;
         }
      }

      @Override
      public int hashCode() {
         int result = this.glfwKey;
         result = 31 * result + (this.layer != null ? this.layer.hashCode() : 0);
         result = 31 * result + (this.hovered ? 1 : 0);
         result = 31 * result + (this.editMode ? 1 : 0);
         return 31 * result + this.visibleBindingBoxCount;
      }
   }

   public static final class RefreshResult {
      public final boolean changed;
      public final boolean complete;
      public final int processedMappings;
      public final int totalMappings;

      private RefreshResult(boolean changed, boolean complete, int processedMappings, int totalMappings) {
         this.changed = changed;
         this.complete = complete;
         this.processedMappings = processedMappings;
         this.totalMappings = totalMappings;
      }
   }
}
