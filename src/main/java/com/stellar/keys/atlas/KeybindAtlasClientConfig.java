package com.stellar.keys.atlas;

import com.stellar.keys.atlas.layout.KeyboardLayouts;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

public final class KeybindAtlasClientConfig {
   private static KeybindAtlasClientConfig.Backend backend = new KeybindAtlasClientConfig.InMemoryBackend();

   private KeybindAtlasClientConfig() {
   }

   public static void install(KeybindAtlasClientConfig.Backend backend) {
      KeybindAtlasClientConfig.backend = Objects.requireNonNull(backend);
   }

   public static int defaultPixelScale() {
      return backend.defaultPixelScale();
   }

   public static KeyboardLayouts.MainLayoutPreset keyboardLayout() {
      return backend.keyboardLayout();
   }

   public static boolean animationsEnabled() {
      return backend.animationsEnabled();
   }

   public static boolean fKeysEnabled() {
      return backend.fKeysEnabled();
   }

   public static boolean allKeysEnabled() {
      return backend.allKeysEnabled();
   }

   public static boolean alternatingLineColorsEnabled() {
      return backend.alternatingLineColorsEnabled();
   }

   public static void setAlternatingLineColorsEnabled(boolean enabled) {
      backend.setAlternatingLineColorsEnabled(enabled);
   }

   public static float panelOpacity() {
      return backend.panelOpacity();
   }

   public static void setPanelOpacity(float opacity) {
      float clamped = Math.max(0.0F, Math.min(1.0F, (float)Math.round(opacity * 10.0F) / 10.0F));
      backend.setPanelOpacity(clamped);
   }

   public static boolean hiddenBindingsEnabled() {
      return backend.hiddenBindingsEnabled();
   }

   public static boolean categoriesEnabled() {
      return backend.categoriesEnabled();
   }

   public static boolean isHiddenBindingLabel(String label) {
      return hiddenBindingsEnabled() && hiddenBindingLabels().contains(normalizeHiddenBindingLabel(label));
   }

   public static List<KeyCategory> customCategories() {
      List<KeyCategory> categories = new ArrayList<>();
      LinkedHashSet<String> seenIds = new LinkedHashSet<>();

      for (String entry : backend.customCategories()) {
         KeyCategory category = parseCustomCategory(entry);
         if (category != null && category.isCustomCategory() && seenIds.add(category.id())) {
            categories.add(category);
         }
      }

      return categories.isEmpty() ? KeyCategory.defaultCustomCategories() : categories;
   }

   public static List<KeyCategory> availableCategories() {
      List<KeyCategory> categories = new ArrayList<>();
      categories.add(KeyCategory.assignedCategory());
      categories.add(KeyCategory.hiddenCategory());
      categories.addAll(customCategories());
      return categories;
   }

   public static List<KeyCategory.PresetColor> presetCategoryColors() {
      List<KeyCategory.PresetColor> presetColors = new ArrayList<>();
      LinkedHashSet<String> seenIds = new LinkedHashSet<>();
      LinkedHashSet<Integer> seenFillColors = new LinkedHashSet<>();

      for (String entry : backend.presetCategoryColors()) {
         KeyCategory.PresetColor presetColor = parsePresetCategoryColor(entry);
         if (presetColor != null && seenIds.add(presetColor.id()) && seenFillColors.add(Integer.valueOf(presetColor.fillColor()))) {
            presetColors.add(presetColor);
         }
      }

      return presetColors.isEmpty() ? KeyCategory.presetColors() : presetColors;
   }

   public static KeyCategory.PresetColor findPresetCategoryColor(int fillColor) {
      return KeyCategory.presetColor(presetCategoryColors(), fillColor);
   }

   public static String presetCategoryColorName(int fillColor) {
      return KeyCategory.presetColorName(presetCategoryColors(), fillColor);
   }

   public static KeyCategory findCustomCategory(String categoryId) {
      if (categoryId != null && !categoryId.isBlank()) {
         String normalizedId = KeyCategory.normalizeId(categoryId);

         for (KeyCategory category : customCategories()) {
            if (category.id().equals(normalizedId) || category.id().equalsIgnoreCase(categoryId.trim())) {
               return category;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static KeyCategory findOrCreateCategory(String categoryId, String categoryName) {
      if (categoryId == null || categoryId.isBlank()) {
         return KeyCategory.assignedCategory();
      }
      String normalizedId = KeyCategory.normalizeId(categoryId);
      if ("assigned".equals(normalizedId)) {
         return KeyCategory.assignedCategory();
      }
      if ("hidden".equals(normalizedId)) {
         return KeyCategory.hiddenCategory();
      }
      KeyCategory found = findCustomCategory(normalizedId);
      if (found != null) {
         return found;
      }
      int fillColor = KeyCategory.defaultColorForId(categoryId);
      String name = (categoryName != null && !categoryName.isBlank())
         ? KeyCategory.sanitizeName(categoryName)
         : KeyCategory.sanitizeName(categoryId);
      KeyCategory category = KeyCategory.custom(normalizedId, name, fillColor, KeyVisualStyle.keyAssignedText(), true);
      List<KeyCategory> updated = new ArrayList<>(customCategories());
      updated.add(category);
      setCustomCategories(updated);
      return category;
   }

   public static KeyCategory findCategoryUsingFillColor(int fillColor) {
      for (KeyCategory category : customCategories()) {
         if (category.fillColor() == fillColor) {
            return category;
         }
      }

      return null;
   }

   public static int customCategoriesHash() {
      return customCategories().hashCode();
   }

   public static int keyCategoryAssignmentsHash() {
      return keyCategoryAssignments().hashCode();
   }

   public static String customCategoryIdForKey(String label, int glfwKey) {
      return keyCategoryAssignments().getOrDefault(keyCategoryTarget(label, glfwKey), "");
   }

   public static void clearCustomCategoryForKey(String label, int glfwKey) {
      setCustomCategoryForKey("", label, glfwKey);
   }

   public static void setCustomCategoryForKey(String categoryId, String label, int glfwKey) {
      String target = keyCategoryTarget(label, glfwKey);
      if (!target.isEmpty()) {
         LinkedHashMap<String, String> assignments = keyCategoryAssignments();
         KeyCategory category = findCustomCategory(categoryId);
         if (category == null) {
            assignments.remove(target);
         } else {
            assignments.put(target, category.id());
         }

         setKeyCategoryAssignments(assignments);
      }
   }

   public static KeyCategory createCustomCategory(String requestedName) {
      return createCustomCategory(requestedName, nextCategoryFillColor(customCategories(), presetCategoryColors()));
   }

   public static KeyCategory createCustomCategory(String requestedName, int fillColor) {
      String baseName = KeyCategory.sanitizeName(requestedName);
      if (baseName.isEmpty()) {
         return null;
      } else {
         List<KeyCategory> existing = customCategories();
         KeyCategory.PresetColor presetColor = findPresetCategoryColor(fillColor);
         if (presetColor != null && findCategoryUsingFillColor(presetColor.fillColor()) == null) {
            LinkedHashSet<String> usedNames = new LinkedHashSet<>();
            LinkedHashSet<String> usedIds = new LinkedHashSet<>();

            for (KeyCategory category : existing) {
               usedNames.add(category.name().toLowerCase(Locale.ROOT));
               usedIds.add(category.id());
            }

            String uniqueName = baseName;
            int nameIndex = 2;

            while (usedNames.contains(uniqueName.toLowerCase(Locale.ROOT))) {
               uniqueName = baseName + " " + nameIndex++;
            }

            String baseId = KeyCategory.normalizeId(uniqueName);
            String uniqueId = baseId;
            int idIndex = 2;

            while (usedIds.contains(uniqueId) || "assigned".equals(uniqueId) || "hidden".equals(uniqueId)) {
               uniqueId = baseId + "-" + idIndex++;
            }

            KeyCategory category = KeyCategory.custom(uniqueId, uniqueName, presetColor.fillColor(), KeyVisualStyle.keyAssignedText(), true);
            List<KeyCategory> updated = new ArrayList<>(existing);
            updated.add(category);
            setCustomCategories(updated);
            return category;
         } else {
            return null;
         }
      }
   }

   public static KeyCategory updateCustomCategory(String categoryId, String requestedName, int fillColor) {
      if (categoryId != null && !categoryId.isBlank()) {
         String normalizedId = KeyCategory.normalizeId(categoryId);
         String baseName = KeyCategory.sanitizeName(requestedName);
         if (baseName.isEmpty()) {
            return null;
         } else {
            List<KeyCategory> existing = customCategories();
            KeyCategory currentCategory = null;

            for (KeyCategory category : existing) {
               if (category.id().equals(normalizedId)) {
                  currentCategory = category;
                  break;
               }
            }

            if (currentCategory == null) {
               return null;
            } else {
               KeyCategory.PresetColor presetColor = findPresetCategoryColor(fillColor);
               if (presetColor == null) {
                  return null;
               } else {
                  KeyCategory colorOwner = findCategoryUsingFillColor(fillColor);
                  if (colorOwner != null && !colorOwner.id().equals(normalizedId)) {
                     return null;
                  } else {
                     LinkedHashSet<String> usedNames = new LinkedHashSet<>();

                     for (KeyCategory categoryx : existing) {
                        if (!categoryx.id().equals(normalizedId)) {
                           usedNames.add(categoryx.name().toLowerCase(Locale.ROOT));
                        }
                     }

                     String uniqueName = baseName;
                     int nameIndex = 2;

                     while (usedNames.contains(uniqueName.toLowerCase(Locale.ROOT))) {
                        uniqueName = baseName + " " + nameIndex++;
                     }

                     KeyCategory updatedCategory = KeyCategory.custom(
                        currentCategory.id(), uniqueName, presetColor.fillColor(), KeyVisualStyle.keyAssignedText(), true
                     );
                     List<KeyCategory> updated = new ArrayList<>(existing.size());

                     for (KeyCategory categoryxx : existing) {
                        updated.add(categoryxx.id().equals(normalizedId) ? updatedCategory : categoryxx);
                     }

                     setCustomCategories(updated);
                     return updatedCategory;
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   public static boolean deleteCustomCategory(String categoryId) {
      if (categoryId != null && !categoryId.isBlank()) {
         String normalizedId = KeyCategory.normalizeId(categoryId);
         List<KeyCategory> existing = customCategories();
         List<KeyCategory> updated = new ArrayList<>();
         boolean removed = false;

         for (KeyCategory category : existing) {
            if (category.id().equals(normalizedId)) {
               removed = true;
            } else {
               updated.add(category);
            }
         }

         if (!removed) {
            return false;
         } else {
            setCustomCategories(updated);
            LinkedHashMap<String, String> assignments = keyCategoryAssignments();
            assignments.entrySet().removeIf(entry -> normalizedId.equals(entry.getValue()));
            setKeyCategoryAssignments(assignments);
            return true;
         }
      } else {
         return false;
      }
   }

   public static int keyAssignedFill() {
      return backend.keyAssignedFill();
   }

   public static int keyUnusedFill() {
      return backend.keyUnusedFill();
   }

   public static int keyHiddenFill() {
      return backend.keyHiddenFill();
   }

   public static int keyHoverFill() {
      return backend.keyHoverFill();
   }

   public static int keyAssignedText() {
      return backend.keyAssignedText();
   }

   public static int keyUnusedText() {
      return backend.keyUnusedText();
   }

   public static int keyHiddenText() {
      return backend.keyHiddenText();
   }

   public static int keyHoverText() {
      return backend.keyHoverText();
   }

   public static int boxFill() {
      return forceOpaque(backend.boxFill());
   }

   public static int boxHoverFill() {
      return forceOpaque(backend.boxHoverFill());
   }

   public static int boxBorder() {
      return backend.boxBorder();
   }

   public static int boxHoverBorder() {
      return backend.boxHoverBorder();
   }

   public static int boxText() {
      return backend.boxText();
   }

   public static int boxSourceText() {
      return backend.boxSourceText();
   }

   public static int keyboardFill() {
      return backend.keyboardFill();
   }

   public static int keyboardBorder() {
      return backend.keyboardBorder();
   }

   public static int lineDefault() {
      return backend.lineDefault();
   }

   public static int lineDefaultAlt() {
      return backend.lineDefaultAlt();
   }

   public static int lineHover() {
      return backend.lineHover();
   }

   public static void setDefaultPixelScale(int scale) {
      backend.setDefaultPixelScale(scale <= 1 ? 1 : 2);
   }

   public static void setKeyboardLayout(KeyboardLayouts.MainLayoutPreset layout) {
      backend.setKeyboardLayout(layout == null ? KeyboardLayouts.MainLayoutPreset.US_QWERTY : layout);
   }

   public static void setFKeysEnabled(boolean enabled) {
      backend.setFKeysEnabled(enabled);
   }

   public static void setAllKeysEnabled(boolean enabled) {
      backend.setAllKeysEnabled(enabled);
   }

   public static void setAnimationsEnabled(boolean enabled) {
      backend.setAnimationsEnabled(enabled);
   }

   public static void setHiddenBindingsEnabled(boolean enabled) {
      backend.setHiddenBindingsEnabled(enabled);
   }

   public static void setCategoriesEnabled(boolean enabled) {
      backend.setCategoriesEnabled(enabled);
   }

   public static boolean renderProfilingEnabled() {
      return backend.renderProfilingEnabled();
   }

   public static int maxUnhoveredAssignments() {
      return backend.maxUnhoveredAssignments();
   }

   public static void setMaxUnhoveredAssignments(int max) {
      backend.setMaxUnhoveredAssignments(Math.max(1, Math.min(20, max)));
   }

   public static List<String> disabledMods() {
      return List.copyOf(backend.disabledMods());
   }

   public static void setDisabledMods(List<String> mods) {
      backend.setDisabledMods(mods == null ? List.of() : mods);
   }

   public static List<String> disabledBindings() {
      return List.copyOf(backend.disabledBindings());
   }

   public static void setDisabledBindings(List<String> bindings) {
      backend.setDisabledBindings(bindings == null ? List.of() : bindings);
   }

   public static void restoreDefaults() {
      backend.restoreDefaults();
   }

   public static List<String> configuredHiddenBindingLabels() {
      return new ArrayList<>(hiddenBindingLabels());
   }

   public static int hiddenBindingLabelsHash() {
      return hiddenBindingLabels().hashCode();
   }

   public static void setHiddenBindingLabels(List<String> labels) {
      LinkedHashSet<String> normalizedLabels = new LinkedHashSet<>();
      if (labels != null) {
         for (String label : labels) {
            String normalized = normalizeHiddenBindingLabel(label);
            if (!normalized.isEmpty()) {
               normalizedLabels.add(normalized);
            }
         }
      }

      backend.setHiddenBindingLabels(new ArrayList<>(normalizedLabels));
   }

   public static String hiddenBindingConfigLabel(String label, int glfwKey) {
      String canonical = KeyboardLayouts.canonicalHiddenBindingLabel(glfwKey);
      return !canonical.isEmpty() ? hiddenBindingTarget(canonical, glfwKey) : hiddenBindingTarget(label, glfwKey);
   }

   public static boolean toggleHiddenBindingLabel(String label, int glfwKey) {
      String target = hiddenBindingConfigLabel(label, glfwKey);
      if (target.isEmpty()) {
         return false;
      } else {
         LinkedHashSet<String> labels = new LinkedHashSet<>(hiddenBindingLabels());
         boolean hiddenNow;
         if (labels.contains(target)) {
            labels.remove(target);
            hiddenNow = false;
         } else {
            labels.add(target);
            hiddenNow = true;
         }

         setHiddenBindingLabels(new ArrayList<>(labels));
         return hiddenNow;
      }
   }

   public static boolean setHiddenBindingForKey(String label, int glfwKey, boolean hidden) {
      String target = hiddenBindingConfigLabel(label, glfwKey);
      if (target.isEmpty()) {
         return false;
      } else {
         LinkedHashSet<String> labels = new LinkedHashSet<>(hiddenBindingLabels());
         boolean changed = hidden ? labels.add(target) : labels.remove(target);
         if (changed) {
            setHiddenBindingLabels(new ArrayList<>(labels));
         }

         return changed;
      }
   }

   static String normalizeHiddenBindingLabel(String label) {
      return label == null ? "" : label.trim().toUpperCase(Locale.ROOT);
   }

   public static boolean isHiddenBindingForKey(String label, int glfwKey) {
      if (!hiddenBindingsEnabled()) {
         return false;
      } else {
         Set<String> configured = hiddenBindingLabels();
         String target = hiddenBindingConfigLabel(label, glfwKey);
         return !target.isEmpty() && configured.contains(target);
      }
   }

   private static int forceOpaque(int color) {
      return color & 16777215 | 0xFF000000;
   }

   private static Set<String> hiddenBindingLabels() {
      LinkedHashSet<String> labels = new LinkedHashSet<>();

      for (String label : backend.hiddenBindingLabels()) {
         String normalized = normalizeHiddenBindingLabel(label);
         if (!normalized.isEmpty()) {
            labels.add(normalized);
         }
      }

      return labels;
   }

   private static String hiddenBindingTarget(String label, int glfwKey) {
      String normalized = normalizeHiddenBindingLabel(label);
      if (normalized.isEmpty()) {
         return "";
      } else {
         return isNumpadKey(glfwKey) ? "NUM-" + normalized : normalized;
      }
   }

   private static boolean isNumpadKey(int glfwKey) {
      return glfwKey >= 320 && glfwKey <= 329 || glfwKey == 330 || glfwKey == 331 || glfwKey == 332 || glfwKey == 333 || glfwKey == 334 || glfwKey == 335;
   }

   private static List<String> defaultCustomCategoryEntries() {
      List<String> entries = new ArrayList<>();

      for (KeyCategory category : KeyCategory.defaultCustomCategories()) {
         entries.add(encodeCustomCategory(category));
      }

      return entries;
   }

   private static List<String> defaultPresetCategoryEntries() {
      List<String> entries = new ArrayList<>();

      for (KeyCategory.PresetColor presetColor : KeyCategory.presetColors()) {
         entries.add(encodePresetCategoryColor(presetColor));
      }

      return entries;
   }

   private static LinkedHashMap<String, String> keyCategoryAssignments() {
      LinkedHashSet<String> validCategoryIds = new LinkedHashSet<>();

      for (KeyCategory category : customCategories()) {
         validCategoryIds.add(category.id());
      }

      LinkedHashMap<String, String> assignments = new LinkedHashMap<>();

      for (String entry : backend.keyCategoryAssignments()) {
         KeybindAtlasClientConfig.CategoryAssignment assignment = parseCategoryAssignment(entry);
         if (assignment != null && validCategoryIds.contains(assignment.categoryId)) {
            assignments.put(assignment.target, assignment.categoryId);
         }
      }

      return assignments;
   }

   private static void setCustomCategories(List<KeyCategory> categories) {
      List<String> encoded = new ArrayList<>();
      if (categories != null) {
         LinkedHashSet<String> seenIds = new LinkedHashSet<>();

         for (KeyCategory category : categories) {
            if (category != null && category.isCustomCategory() && seenIds.add(category.id())) {
               encoded.add(encodeCustomCategory(category));
            }
         }
      }

      backend.setCustomCategories(encoded);
   }

   private static void setKeyCategoryAssignments(Map<String, String> assignments) {
      List<String> encoded = new ArrayList<>();
      if (assignments != null) {
         for (Entry<String, String> entry : assignments.entrySet()) {
            String target = entry.getKey();
            String categoryId = entry.getValue();
            if (target != null && !target.isBlank() && categoryId != null && !categoryId.isBlank()) {
               encoded.add(target + "::" + KeyCategory.normalizeId(categoryId));
            }
         }
      }

      backend.setKeyCategoryAssignments(encoded);
   }

   private static String keyCategoryTarget(String label, int glfwKey) {
      return hiddenBindingConfigLabel(label, glfwKey);
   }

   private static KeyCategory parseCustomCategory(String raw) {
      if (raw == null) {
         return null;
      } else {
         String[] parts = raw.split("\\|", 4);
         if (parts.length != 4) {
            return null;
         } else {
            String id = KeyCategory.normalizeId(parts[0]);
            String name = KeyCategory.sanitizeName(parts[1]);
            KeybindAtlasClientConfig.ParsedColor fill = tryParseColor(parts[2]);
            KeybindAtlasClientConfig.ParsedColor text = tryParseColor(parts[3]);
            return !id.isEmpty() && !name.isEmpty() && fill != null && text != null ? KeyCategory.custom(id, name, fill.color, text.color, true) : null;
         }
      }
   }

   private static KeyCategory.PresetColor parsePresetCategoryColor(String raw) {
      if (raw == null) {
         return null;
      } else {
         String[] parts = raw.split("\\|", 3);
         if (parts.length != 3) {
            return null;
         } else {
            String id = KeyCategory.normalizeId(parts[0]);
            String name = KeyCategory.sanitizeName(parts[1]);
            KeybindAtlasClientConfig.ParsedColor fill = tryParseColor(parts[2]);
            return !id.isEmpty() && !name.isEmpty() && fill != null ? KeyCategory.preset(id, name, fill.color) : null;
         }
      }
   }

   private static String encodeCustomCategory(KeyCategory category) {
      return category.id()
         + "|"
         + KeyCategory.sanitizeName(category.name())
         + "|"
         + formatColor(category.fillColor())
         + "|"
         + formatColor(category.textColor());
   }

   private static String encodePresetCategoryColor(KeyCategory.PresetColor presetColor) {
      return presetColor.id() + "|" + KeyCategory.sanitizeName(presetColor.name()) + "|" + formatColor(presetColor.fillColor());
   }

   private static KeybindAtlasClientConfig.CategoryAssignment parseCategoryAssignment(String raw) {
      if (raw == null) {
         return null;
      } else {
         int separatorIndex = raw.indexOf("::");
         if (separatorIndex > 0 && separatorIndex < raw.length() - 2) {
            String target = raw.substring(0, separatorIndex).trim();
            String categoryId = KeyCategory.normalizeId(raw.substring(separatorIndex + 2));
            return !target.isEmpty() && !categoryId.isEmpty() ? new KeybindAtlasClientConfig.CategoryAssignment(target, categoryId) : null;
         } else {
            return null;
         }
      }
   }

   private static int nextCategoryFillColor(List<KeyCategory> existingCategories, List<KeyCategory.PresetColor> presetColors) {
      LinkedHashSet<Integer> usedColors = new LinkedHashSet<>();

      for (KeyCategory category : existingCategories) {
         usedColors.add(Integer.valueOf(category.fillColor()));
      }

      for (KeyCategory.PresetColor presetColor : presetColors) {
         if (!usedColors.contains(Integer.valueOf(presetColor.fillColor()))) {
            return presetColor.fillColor();
         }
      }

      return !presetColors.isEmpty() ? presetColors.get(existingCategories.size() % presetColors.size()).fillColor() : KeyVisualStyle.keyAssignedFill();
   }

   private static KeybindAtlasClientConfig.ParsedColor tryParseColor(String raw) {
      if (raw == null) {
         return null;
      } else {
         String normalized = raw.trim();
         if (normalized.isEmpty()) {
            return null;
         } else {
            String original = normalized;
            if (normalized.startsWith("#")) {
               normalized = normalized.substring(1);
            } else if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
               normalized = normalized.substring(2);
            }

            if (normalized.length() == 6 || normalized.length() == 8) {
               try {
                  long parsed = Long.parseLong(normalized, 16);
                  int color;
                  if (normalized.length() == 6) {
                     color = (int)(4278190080L | parsed);
                  } else {
                     int rgba = (int)parsed;
                     int r = rgba >>> 24 & 0xFF;
                     int g = rgba >>> 16 & 0xFF;
                     int b = rgba >>> 8 & 0xFF;
                     int a = rgba & 0xFF;
                     color = a << 24 | r << 16 | g << 8 | b;
                  }

                  String canonical = formatColor(color);
                  return new KeybindAtlasClientConfig.ParsedColor(color, !canonical.equals(original));
               } catch (NumberFormatException var12) {
               }
            }

            try {
               long parsed = Long.parseLong(original);
               int color = (int)parsed;
               return new KeybindAtlasClientConfig.ParsedColor(color, true);
            } catch (NumberFormatException var11) {
               return null;
            }
         }
      }
   }

   private static String formatColor(int argbColor) {
      int a = argbColor >>> 24 & 0xFF;
      int r = argbColor >>> 16 & 0xFF;
      int g = argbColor >>> 8 & 0xFF;
      int b = argbColor & 0xFF;
      return String.format("#%02X%02X%02X%02X", r, g, b, a);
   }

   private static List<String> copyList(List<String> values) {
      return values == null ? new ArrayList<>() : new ArrayList<>(values);
   }

   public interface Backend {
      int defaultPixelScale();

      void setDefaultPixelScale(int var1);

      KeyboardLayouts.MainLayoutPreset keyboardLayout();

      void setKeyboardLayout(KeyboardLayouts.MainLayoutPreset var1);

      boolean animationsEnabled();

      void setAnimationsEnabled(boolean var1);

      boolean hiddenBindingsEnabled();

      void setHiddenBindingsEnabled(boolean var1);

      boolean categoriesEnabled();

      void setCategoriesEnabled(boolean var1);

      boolean fKeysEnabled();

      void setFKeysEnabled(boolean var1);

      boolean allKeysEnabled();

      void setAllKeysEnabled(boolean var1);

      boolean alternatingLineColorsEnabled();

      void setAlternatingLineColorsEnabled(boolean var1);

      float panelOpacity();

      void setPanelOpacity(float var1);

      boolean renderProfilingEnabled();

      int maxUnhoveredAssignments();

      void setMaxUnhoveredAssignments(int var1);

      List<String> hiddenBindingLabels();

      void setHiddenBindingLabels(List<String> var1);

      List<String> presetCategoryColors();

      void setPresetCategoryColors(List<String> var1);

      List<String> customCategories();

      void setCustomCategories(List<String> var1);

      List<String> keyCategoryAssignments();

      void setKeyCategoryAssignments(List<String> var1);

      List<String> disabledMods();

      void setDisabledMods(List<String> var1);

      List<String> disabledBindings();

      void setDisabledBindings(List<String> var1);

      int keyAssignedFill();

      int keyUnusedFill();

      int keyHiddenFill();

      int keyHoverFill();

      int keyAssignedText();

      int keyUnusedText();

      int keyHiddenText();

      int keyHoverText();

      int boxFill();

      int boxHoverFill();

      int boxBorder();

      int boxHoverBorder();

      int boxText();

      int boxSourceText();

      int keyboardFill();

      int keyboardBorder();

      int lineDefault();

      int lineDefaultAlt();

      int lineHover();

      void restoreDefaults();
   }

   private static final class CategoryAssignment {
      final String target;
      final String categoryId;

      private CategoryAssignment(String target, String categoryId) {
         this.target = target;
         this.categoryId = categoryId;
      }
   }

   private static final class InMemoryBackend implements KeybindAtlasClientConfig.Backend {
      private int defaultPixelScale;
      private KeyboardLayouts.MainLayoutPreset keyboardLayout;
      private boolean animationsEnabled;
      private boolean hiddenBindingsEnabled;
      private boolean categoriesEnabled;
      private boolean fKeysEnabled;
      private boolean allKeysEnabled;
      private boolean alternatingLineColorsEnabled;
      private float panelOpacity;
      private int maxUnhoveredAssignments;
      private List<String> hiddenBindingLabels;
      private List<String> presetCategoryColors;
      private List<String> customCategories;
      private List<String> keyCategoryAssignments;
      private List<String> disabledMods;
      private List<String> disabledBindings;
      private int keyAssignedFill;
      private int keyUnusedFill;
      private int keyHiddenFill;
      private int keyHoverFill;
      private int keyAssignedText;
      private int keyUnusedText;
      private int keyHiddenText;
      private int keyHoverText;
      private int boxFill;
      private int boxHoverFill;
      private int boxBorder;
      private int boxHoverBorder;
      private int boxText;
      private int boxSourceText;
      private int keyboardFill;
      private int keyboardBorder;
      private int lineDefault;
      private int lineDefaultAlt;
      private int lineHover;

      private InMemoryBackend() {
         this.restoreDefaults();
      }

      @Override
      public int defaultPixelScale() {
         return this.defaultPixelScale;
      }

      @Override
      public void setDefaultPixelScale(int scale) {
         this.defaultPixelScale = scale;
      }

      @Override
      public KeyboardLayouts.MainLayoutPreset keyboardLayout() {
         return this.keyboardLayout;
      }

      @Override
      public void setKeyboardLayout(KeyboardLayouts.MainLayoutPreset layout) {
         this.keyboardLayout = layout;
      }

      @Override
      public boolean animationsEnabled() {
         return this.animationsEnabled;
      }

      @Override
      public void setAnimationsEnabled(boolean enabled) {
         this.animationsEnabled = enabled;
      }

      @Override
      public boolean hiddenBindingsEnabled() {
         return this.hiddenBindingsEnabled;
      }

      @Override
      public void setHiddenBindingsEnabled(boolean enabled) {
         this.hiddenBindingsEnabled = enabled;
      }

      @Override
      public boolean categoriesEnabled() {
         return this.categoriesEnabled;
      }

      @Override
      public void setCategoriesEnabled(boolean enabled) {
         this.categoriesEnabled = enabled;
      }

      @Override
      public boolean fKeysEnabled() {
         return this.fKeysEnabled;
      }

      @Override
      public void setFKeysEnabled(boolean enabled) {
         this.fKeysEnabled = enabled;
      }

      @Override
      public boolean allKeysEnabled() {
         return this.allKeysEnabled;
      }

      @Override
      public void setAllKeysEnabled(boolean enabled) {
         this.allKeysEnabled = enabled;
      }

      @Override
      public boolean alternatingLineColorsEnabled() {
         return this.alternatingLineColorsEnabled;
      }

      @Override
      public void setAlternatingLineColorsEnabled(boolean enabled) {
         this.alternatingLineColorsEnabled = enabled;
      }

      @Override
      public float panelOpacity() {
         return this.panelOpacity;
      }

      @Override
      public void setPanelOpacity(float opacity) {
         this.panelOpacity = opacity;
      }

      @Override
      public boolean renderProfilingEnabled() {
         return false;
      }

      @Override
      public int maxUnhoveredAssignments() {
         return this.maxUnhoveredAssignments;
      }

      @Override
      public void setMaxUnhoveredAssignments(int max) {
         this.maxUnhoveredAssignments = max;
      }

      @Override
      public List<String> hiddenBindingLabels() {
         return KeybindAtlasClientConfig.copyList(this.hiddenBindingLabels);
      }

      @Override
      public void setHiddenBindingLabels(List<String> labels) {
         this.hiddenBindingLabels = KeybindAtlasClientConfig.copyList(labels);
      }

      @Override
      public List<String> presetCategoryColors() {
         return KeybindAtlasClientConfig.copyList(this.presetCategoryColors);
      }

      @Override
      public void setPresetCategoryColors(List<String> colors) {
         this.presetCategoryColors = KeybindAtlasClientConfig.copyList(colors);
      }

      @Override
      public List<String> customCategories() {
         return KeybindAtlasClientConfig.copyList(this.customCategories);
      }

      @Override
      public void setCustomCategories(List<String> categories) {
         this.customCategories = KeybindAtlasClientConfig.copyList(categories);
      }

      @Override
      public List<String> keyCategoryAssignments() {
         return KeybindAtlasClientConfig.copyList(this.keyCategoryAssignments);
      }

      @Override
      public void setKeyCategoryAssignments(List<String> assignments) {
         this.keyCategoryAssignments = KeybindAtlasClientConfig.copyList(assignments);
      }

      @Override
      public List<String> disabledMods() {
         return KeybindAtlasClientConfig.copyList(this.disabledMods);
      }

      @Override
      public void setDisabledMods(List<String> mods) {
         this.disabledMods = KeybindAtlasClientConfig.copyList(mods);
      }

      @Override
      public List<String> disabledBindings() {
         return KeybindAtlasClientConfig.copyList(this.disabledBindings);
      }

      @Override
      public void setDisabledBindings(List<String> bindings) {
         this.disabledBindings = KeybindAtlasClientConfig.copyList(bindings);
      }

      @Override
      public int keyAssignedFill() {
         return this.keyAssignedFill;
      }

      @Override
      public int keyUnusedFill() {
         return this.keyUnusedFill;
      }

      @Override
      public int keyHiddenFill() {
         return this.keyHiddenFill;
      }

      @Override
      public int keyHoverFill() {
         return this.keyHoverFill;
      }

      @Override
      public int keyAssignedText() {
         return this.keyAssignedText;
      }

      @Override
      public int keyUnusedText() {
         return this.keyUnusedText;
      }

      @Override
      public int keyHiddenText() {
         return this.keyHiddenText;
      }

      @Override
      public int keyHoverText() {
         return this.keyHoverText;
      }

      @Override
      public int boxFill() {
         return this.boxFill;
      }

      @Override
      public int boxHoverFill() {
         return this.boxHoverFill;
      }

      @Override
      public int boxBorder() {
         return this.boxBorder;
      }

      @Override
      public int boxHoverBorder() {
         return this.boxHoverBorder;
      }

      @Override
      public int boxText() {
         return this.boxText;
      }

      @Override
      public int boxSourceText() {
         return this.boxSourceText;
      }

      @Override
      public int keyboardFill() {
         return this.keyboardFill;
      }

      @Override
      public int keyboardBorder() {
         return this.keyboardBorder;
      }

      @Override
      public int lineDefault() {
         return this.lineDefault;
      }

      @Override
      public int lineDefaultAlt() {
         return this.lineDefaultAlt;
      }

      @Override
      public int lineHover() {
         return this.lineHover;
      }

      @Override
      public void restoreDefaults() {
         this.defaultPixelScale = 1;
         this.keyboardLayout = KeyboardLayouts.MainLayoutPreset.US_QWERTY;
         this.animationsEnabled = true;
         this.hiddenBindingsEnabled = true;
         this.categoriesEnabled = true;
         this.fKeysEnabled = false;
         this.allKeysEnabled = true;
         this.alternatingLineColorsEnabled = true;
         this.panelOpacity = 0.4F;
         this.maxUnhoveredAssignments = 3;
         this.hiddenBindingLabels = KeybindAtlasClientConfig.copyList(KeyboardLayouts.defaultHiddenBindingLabels());
         this.presetCategoryColors = KeybindAtlasClientConfig.copyList(KeybindAtlasClientConfig.defaultPresetCategoryEntries());
         this.customCategories = KeybindAtlasClientConfig.copyList(KeybindAtlasClientConfig.defaultCustomCategoryEntries());
         this.keyCategoryAssignments = new ArrayList<>();
         this.disabledMods = new ArrayList<>();
         this.disabledBindings = new ArrayList<>();
         this.keyAssignedFill = -16686644;
         this.keyUnusedFill = -13684426;
         this.keyHiddenFill = -14737115;
         this.keyHoverFill = -10570753;
         this.keyAssignedText = -1;
         this.keyUnusedText = -6380888;
         this.keyHiddenText = -4868683;
         this.keyHoverText = -3090208;
         this.boxFill = -12960446;
         this.boxHoverFill = -12104878;
         this.boxBorder = -15000544;
         this.boxHoverBorder = -9262869;
         this.boxText = -1710619;
         this.boxSourceText = -6314578;
         this.keyboardFill = -15066080;
         this.keyboardBorder = -15921648;
         this.lineDefault = -7696490;
         this.lineDefaultAlt = -10723224;
         this.lineHover = -7947009;
      }
   }

   private static final class ParsedColor {
      final int color;
      final boolean needsNormalization;

      private ParsedColor(int color, boolean needsNormalization) {
         this.color = color;
         this.needsNormalization = needsNormalization;
      }
   }
}
