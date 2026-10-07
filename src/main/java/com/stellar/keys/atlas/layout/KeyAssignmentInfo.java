package com.stellar.keys.atlas.layout;

import com.stellar.keys.atlas.KeyBindingModifier;

public final class KeyAssignmentInfo {
   public final String label;
   public final String source;
   public final String modifierPrefix;
   public final KeyBindingModifier modifier;
   public final String category;
   public final String categoryName;

   public KeyAssignmentInfo(String label, String source) {
      this(label, source, null, KeyBindingModifier.NONE, "misc", "Miscellaneous");
   }

   public KeyAssignmentInfo(String label, String source, String modifierPrefix) {
      this(label, source, modifierPrefix, KeyBindingModifier.NONE, "misc", "Miscellaneous");
   }

   public KeyAssignmentInfo(String label, String source, String modifierPrefix, KeyBindingModifier modifier) {
      this(label, source, modifierPrefix, modifier, "misc", "Miscellaneous");
   }

   public KeyAssignmentInfo(String label, String source, String modifierPrefix, KeyBindingModifier modifier, String category, String categoryName) {
      this.label = label;
      this.source = source;
      this.modifierPrefix = modifierPrefix;
      this.modifier = modifier != null ? modifier : KeyBindingModifier.NONE;
      this.category = category != null && !category.isBlank() ? category : "misc";
      this.categoryName = categoryName != null && !categoryName.isBlank() ? categoryName : "Miscellaneous";
   }
}
