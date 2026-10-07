package com.stellar.keys.atlas;

public final class KeyVisualStyle {
   public static final float PANEL_ALPHA = 0.4F;
   public static final int BACKGROUND_OVERLAY = -1073741824;
   public static final int PANEL_CORNER = 3;
   public static final float PANEL_DARKEN = 0.12F;
   public static final int HUD_MARGIN = 6;
   public static final int HUD_GAP = 4;
   public static final int PANEL_PAD = 4;
   public static final float LABEL_SCALE = 0.8F;
   public static final float MOD_LABEL_SCALE = 0.8F;
   public static final float BINDING_LABEL_SCALE = 0.7F;
   public static final int ARROW_COLOR = -7829368;
   public static final float FADE_IN_MS = 200.0F;
   public static final float FADE_OUT_MS = 300.0F;
   public static final float SEPARATOR_CENTER_DARKEN = 0.2F;
   public static final float SEPARATOR_EDGE_DARKEN = 0.1F;
   public static final float DIALOG_TITLE_SCALE = 0.85F;
   static final int DEFAULT_KEY_ASSIGNED_FILL = -16686644;
   static final int DEFAULT_KEY_UNUSED_FILL = -13684426;
   static final int DEFAULT_KEY_HIDDEN_FILL = -14737115;
   static final int DEFAULT_KEY_HOVER_FILL = -10570753;
   static final int DEFAULT_KEY_ASSIGNED_TEXT = -1;
   static final int DEFAULT_KEY_UNUSED_TEXT = -6380888;
   static final int DEFAULT_KEY_HIDDEN_TEXT = -4868683;
   static final int DEFAULT_KEY_HOVER_TEXT = -3090208;
   static final int DEFAULT_BOX_FILL = -12960446;
   static final int DEFAULT_BOX_HOVER_FILL = -12104878;
   static final int DEFAULT_BOX_BORDER = -15000544;
   static final int DEFAULT_BOX_HOVER_BORDER = -9262869;
   static final int DEFAULT_BOX_TEXT = -1710619;
   static final int DEFAULT_BOX_SOURCE_TEXT = -6314578;
   static final int DEFAULT_KEYBOARD_FILL = -15066080;
   static final int DEFAULT_KEYBOARD_BORDER = -15921648;
   static final int DEFAULT_LINE_DEFAULT = -7696490;
   static final int DEFAULT_LINE_DEFAULT_ALT = -10723224;
   static final int DEFAULT_LINE_HOVER = -7947009;

   static final int DEFAULT_KEY_DIRECT_CONFLICT_FILL = -2937041;
   static final int DEFAULT_KEY_DIRECT_CONFLICT_TEXT = -1;
   static final int DEFAULT_KEY_SOFT_CONFLICT_FILL = -1395960;
   static final int DEFAULT_KEY_SOFT_CONFLICT_TEXT = -1;

   public static float panelAlpha() {
      return KeybindAtlasClientConfig.panelOpacity();
   }

   private KeyVisualStyle() {
   }

   public static int keyDirectConflictFill() {
      return DEFAULT_KEY_DIRECT_CONFLICT_FILL;
   }

   public static int keyDirectConflictText() {
      return DEFAULT_KEY_DIRECT_CONFLICT_TEXT;
   }

   public static int keySoftConflictFill() {
      return DEFAULT_KEY_SOFT_CONFLICT_FILL;
   }

   public static int keySoftConflictText() {
      return DEFAULT_KEY_SOFT_CONFLICT_TEXT;
   }

   public static int keyAssignedFill() {
      return KeybindAtlasClientConfig.keyAssignedFill();
   }

   public static int keyUnusedFill() {
      return KeybindAtlasClientConfig.keyUnusedFill();
   }

   public static int keyHiddenFill() {
      return KeybindAtlasClientConfig.keyHiddenFill();
   }

   public static int keyHoverFill() {
      return KeybindAtlasClientConfig.keyHoverFill();
   }

   public static int keyAssignedText() {
      return KeybindAtlasClientConfig.keyAssignedText();
   }

   public static int keyUnusedText() {
      return KeybindAtlasClientConfig.keyUnusedText();
   }

   public static int keyHiddenText() {
      return KeybindAtlasClientConfig.keyHiddenText();
   }

   public static int keyHoverText() {
      return KeybindAtlasClientConfig.keyHoverText();
   }

   public static int boxFill() {
      return KeybindAtlasClientConfig.boxFill();
   }

   public static int boxHoverFill() {
      return KeybindAtlasClientConfig.boxHoverFill();
   }

   public static int boxBorder() {
      return KeybindAtlasClientConfig.boxBorder();
   }

   public static int boxHoverBorder() {
      return KeybindAtlasClientConfig.boxHoverBorder();
   }

   public static int boxText() {
      return KeybindAtlasClientConfig.boxText();
   }

   public static int boxSourceText() {
      return KeybindAtlasClientConfig.boxSourceText();
   }

   public static int keyboardFill() {
      return KeybindAtlasClientConfig.keyboardFill();
   }

   public static int keyboardBorder() {
      return KeybindAtlasClientConfig.keyboardBorder();
   }

   public static int lineDefault() {
      return KeybindAtlasClientConfig.lineDefault();
   }

   public static int lineDefaultAlt() {
      return KeybindAtlasClientConfig.lineDefaultAlt();
   }

   public static int lineHover() {
      return KeybindAtlasClientConfig.lineHover();
   }
}
