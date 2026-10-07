package com.stellar.keys.atlas.render;

import com.stellar.keys.atlas.AtlasText;
import com.stellar.keys.atlas.KeyVisualStyle;
import com.stellar.keys.atlas.KeybindAtlasClientConfig;
import com.stellar.keys.atlas.layout.KeyboardLayouts;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class KeyboardDebugOverlay {
   private static final int DBG_H = 14;
   private static final int DBG_NAV_W = 14;
   private static final int DBG_Y = 6;
   private static final int COG_W = 18;
   private static final int COG_H = 18;
   private static final int SETTINGS_PANEL_W = 132;
   private static final int SETTINGS_PANEL_PAD = 4;
   private static final int SETTINGS_ROW_H = 12;
   private static final int SETTINGS_BTN_W = 38;
   private static final int SETTINGS_SEPARATOR_H = 7;
   private static final float SETTINGS_LABEL_SCALE = 0.8F;
   private static final int LAYOUT_MENU_W = 156;
   private static final int LAYOUT_MENU_PAD = 4;
   private static final int LAYOUT_MENU_ROW_GAP = 2;
   private static final int LOG_W = 42;
   private static final int LOG_H = 14;
   private static final int FOLDER_W = 42;
   private static final int FOLDER_H = 14;
   private static final int CLOSE_W = 18;
   private static final int CLOSE_H = 18;
   private static final int CLOSE_X = 6;
   private static final int CLOSE_Y = 6;
   private static final int INFO_W = 18;
   private static final int INFO_H = 18;
   private static final int INFO_X = 28;
   private static final int HUD_MARGIN = 6;
   private static final int HUD_GAP = 4;
   private static final int RESTORE_CW = 100;
   private static final int RESTORE_CH = 40;
   private final int screenWidth;
   private final int screenHeight;
   private final int pixelScale;
   private final HoverTooltipState tooltipState = new HoverTooltipState();

   public KeyboardDebugOverlay(int screenWidth, int screenHeight, int pixelScale) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.pixelScale = pixelScale;
   }

   private static List<KeyboardDebugOverlay.SettingsRow> settingsRows(boolean debugExpanded) {
      List<KeyboardDebugOverlay.SettingsRow> rows = new ArrayList<>();
      rows.add(KeyboardDebugOverlay.SettingsRow.RESOLUTION);
      rows.add(KeyboardDebugOverlay.SettingsRow.ANIMATIONS);
      rows.add(KeyboardDebugOverlay.SettingsRow.ALT_LINE_COLORS);
      rows.add(KeyboardDebugOverlay.SettingsRow.CATEGORIES);
      rows.add(KeyboardDebugOverlay.SettingsRow.PANEL_OPACITY);
      rows.add(KeyboardDebugOverlay.SettingsRow.SEPARATOR);
      rows.add(KeyboardDebugOverlay.SettingsRow.KEYBOARD_LAYOUT);
      rows.add(KeyboardDebugOverlay.SettingsRow.FUNCTION_KEYS);
      rows.add(KeyboardDebugOverlay.SettingsRow.FULL_KEYBOARD);
      rows.add(KeyboardDebugOverlay.SettingsRow.SEPARATOR);
      rows.add(KeyboardDebugOverlay.SettingsRow.DEBUG);
      if (debugExpanded) {
         rows.add(KeyboardDebugOverlay.SettingsRow.LOG);
         rows.add(KeyboardDebugOverlay.SettingsRow.DIR);
      }

      rows.add(KeyboardDebugOverlay.SettingsRow.RESET_SETTINGS);
      return rows;
   }

   private int panelHeight(boolean debugExpanded) {
      List<KeyboardDebugOverlay.SettingsRow> rows = settingsRows(debugExpanded);
      int separators = 0;
      int items = 0;

      for (KeyboardDebugOverlay.SettingsRow row : rows) {
         if (row.separator()) {
            separators++;
         } else {
            items++;
         }
      }

      return 8 + items * 12 + (items - 1) * 2 + separators * 7;
   }

   private int keyboardLayoutRowY(boolean debugExpanded) {
      int rowY = this.panelY() + 4;

      for (KeyboardDebugOverlay.SettingsRow row : settingsRows(debugExpanded)) {
         if (row.separator()) {
            rowY += 7;
         } else {
            if (row == KeyboardDebugOverlay.SettingsRow.KEYBOARD_LAYOUT) {
               return rowY;
            }

            rowY += 14;
         }
      }

      return this.panelY() + 4;
   }

   private int layoutMenuHeight() {
      int count = KeyboardLayouts.MainLayoutPreset.selectableValues().size();
      return 8 + count * 12 + Math.max(0, count - 1) * 2;
   }

   private int[] layoutMenuBounds(boolean debugExpanded) {
      int menuH = this.layoutMenuHeight();
      int minY = 28;
      int maxY = Math.max(minY, this.screenHeight - menuH - 6);
      int menuY = Math.max(minY, Math.min(this.keyboardLayoutRowY(debugExpanded), maxY));
      int menuX = Math.max(6, this.panelX() - 156 - 4);
      return new int[]{menuX, menuY, 156, menuH};
   }

   private void drawSettingsSeparator(GuiGraphicsExtractor guiGraphics, int panelX, int y, int panelWidth, int height) {
      int inset = 5 * this.pixelScale;
      int availableWidth = Math.max(10 * this.pixelScale, panelWidth - 2 * inset);
      int centerX = panelX + panelWidth / 2;
      int lineY = y + Math.max(0, height - this.pixelScale) / 2;
      int halfWidth = availableWidth / 2;
      int centerColor = OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.2F);
      int edgeColor = OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.1F);
      float sepAlpha = 0.9F;
      int bands = Math.max(2, Math.min(8, halfWidth / (2 * this.pixelScale)));
      int leftEdge = centerX - halfWidth;

      for (int b = 0; b < bands; b++) {
         int bandLeft = leftEdge + b * availableWidth / bands;
         int bandRight = leftEdge + (b + 1) * availableWidth / bands;
         if (bandLeft < bandRight) {
            int bandMid = (bandLeft + bandRight) / 2;
            float distance = halfWidth <= 0 ? 1.0F : (float)Math.abs(bandMid - centerX) / (float)halfWidth;
            int blended = OverlayRenderHelper.mixColor(centerColor, edgeColor, distance);
            guiGraphics.fill(bandLeft, lineY, bandRight, lineY + this.pixelScale, OverlayRenderHelper.withAlpha(blended, sepAlpha));
         }
      }
   }

   private int panelX() {
      return this.screenWidth - 132 - 6;
   }

   private int panelY() {
      return 28;
   }

   private int cogBtnX() {
      return this.screenWidth - 18 - 6;
   }

   private int sx(int x) {
      return OverlayRenderHelper.scaleX(x, this.screenWidth / 2, this.pixelScale);
   }

   private int sy(int y) {
      return OverlayRenderHelper.scaleY(y, this.screenHeight / 2, this.pixelScale);
   }

   private static String toggleLabel(boolean active) {
      return AtlasText.text(active ? "ui.common.on" : "ui.common.off");
   }

   private static String actionTooltipId(KeyboardDebugOverlay.Action action) {
      return "debug.action." + action.name();
   }

   private static Component actionTooltip(KeyboardDebugOverlay.Action action) {
      return switch (action) {
         case CLOSE -> AtlasText.translatable("ui.tooltip.button.close");
         case TOGGLE_CATEGORY_INFO -> AtlasText.translatable("ui.tooltip.button.category_info");
         case TOGGLE_CATEGORIES -> AtlasText.translatable("ui.tooltip.setting.categories");
         default -> Component.empty();
         case TOGGLE_SETTINGS -> AtlasText.translatable("ui.tooltip.button.settings");
         case DUMP_DEBUG_LOG -> AtlasText.translatable("ui.tooltip.button.dump_log");
         case OPEN_DEBUG_FOLDER -> AtlasText.translatable("ui.tooltip.button.open_folder");
         case DEBUG_PREV -> AtlasText.translatable("ui.tooltip.button.debug_prev");
         case DEBUG_NEXT -> AtlasText.translatable("ui.tooltip.button.debug_next");
      };
   }

   private static String settingButtonLabel(
      KeyboardDebugOverlay.SettingsRow row,
      int cornerStyle,
      KeyboardLayouts.MainLayoutPreset keyboardLayout,
      boolean animationsEnabled,
      boolean fKeysEnabled,
      boolean categoriesEnabled,
      boolean layoutMenuOpen,
      boolean debugMode
   ) {
      return switch (row) {
         case RESOLUTION -> cornerStyle <= 0 ? "1x" : "2x";
         case ANIMATIONS -> toggleLabel(animationsEnabled);
         case ALT_LINE_COLORS -> toggleLabel(KeybindAtlasClientConfig.alternatingLineColorsEnabled());
         case CATEGORIES -> toggleLabel(categoriesEnabled);
         case PANEL_OPACITY -> String.format(Locale.ROOT, "%.1f", KeybindAtlasClientConfig.panelOpacity());
         default -> "";
         case KEYBOARD_LAYOUT -> keyboardLayout.buttonLabel();
         case FUNCTION_KEYS -> toggleLabel(fKeysEnabled);
         case FULL_KEYBOARD -> toggleLabel(KeybindAtlasClientConfig.allKeysEnabled());
         case DEBUG -> toggleLabel(debugMode);
         case LOG -> AtlasText.text("ui.button.log_short");
         case DIR -> AtlasText.text("ui.button.dir_short");
         case RESET_SETTINGS -> AtlasText.text("ui.button.reset_short");
      };
   }

   private static boolean settingButtonActive(
      KeyboardDebugOverlay.SettingsRow row,
      int cornerStyle,
      boolean animationsEnabled,
      boolean fKeysEnabled,
      boolean categoriesEnabled,
      boolean layoutMenuOpen,
      boolean debugMode
   ) {
      return switch (row) {
         case RESOLUTION -> cornerStyle > 0;
         case ANIMATIONS -> animationsEnabled;
         case ALT_LINE_COLORS -> KeybindAtlasClientConfig.alternatingLineColorsEnabled();
         case CATEGORIES -> categoriesEnabled;
         default -> false;
         case KEYBOARD_LAYOUT -> layoutMenuOpen;
         case FUNCTION_KEYS -> fKeysEnabled;
         case FULL_KEYBOARD -> KeybindAtlasClientConfig.allKeysEnabled();
         case DEBUG -> debugMode;
      };
   }

   private static KeyboardDebugOverlay.Action actionForRow(KeyboardDebugOverlay.SettingsRow row) {
      return switch (row) {
         case RESOLUTION -> KeyboardDebugOverlay.Action.CYCLE_CORNERS;
         case ANIMATIONS -> KeyboardDebugOverlay.Action.TOGGLE_ANIMATIONS;
         case ALT_LINE_COLORS -> KeyboardDebugOverlay.Action.TOGGLE_ALTERNATING_LINE_COLORS;
         case CATEGORIES -> KeyboardDebugOverlay.Action.TOGGLE_CATEGORIES;
         case PANEL_OPACITY -> KeyboardDebugOverlay.Action.CYCLE_PANEL_OPACITY;
         case SEPARATOR -> KeyboardDebugOverlay.Action.NONE;
         case KEYBOARD_LAYOUT -> KeyboardDebugOverlay.Action.TOGGLE_KEYBOARD_LAYOUT_MENU;
         case FUNCTION_KEYS -> KeyboardDebugOverlay.Action.TOGGLE_F_KEYS;
         case FULL_KEYBOARD -> KeyboardDebugOverlay.Action.TOGGLE_ALL_KEYS;
         case DEBUG -> KeyboardDebugOverlay.Action.TOGGLE_DEBUG;
         case LOG -> KeyboardDebugOverlay.Action.DUMP_DEBUG_LOG;
         case DIR -> KeyboardDebugOverlay.Action.OPEN_DEBUG_FOLDER;
         case RESET_SETTINGS -> KeyboardDebugOverlay.Action.RESTORE_DEFAULTS;
      };
   }

   private void drawLayoutMenu(
      GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, int mouseX, int mouseY, KeyboardLayouts.MainLayoutPreset keyboardLayout, boolean debugExpanded
   ) {
      int[] bounds = this.layoutMenuBounds(debugExpanded);
      int menuX = bounds[0];
      int menuY = bounds[1];
      int menuW = bounds[2];
      int menuH = bounds[3];
      int scaledMenuX = this.sx(menuX);
      int scaledMenuY = this.sy(menuY);
      int scaledMenuW = menuW * this.pixelScale;
      int scaledMenuH = menuH * this.pixelScale;
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      OverlayRenderHelper.blitBlurredBackground(guiGraphics, scaledMenuX, scaledMenuY, scaledMenuW, scaledMenuH, menuX, menuY, menuW, menuH);
      float panelAlpha = KeyVisualStyle.panelAlpha();
      OverlayRenderHelper.fillRoundedRect(
         guiGraphics,
         scaledMenuX,
         scaledMenuY,
         scaledMenuW,
         scaledMenuH,
         this.pixelScale,
         OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), panelAlpha)
      );
      OverlayRenderHelper.drawRoundedRectOutline(
         guiGraphics, scaledMenuX, scaledMenuY, scaledMenuW, scaledMenuH, this.pixelScale, KeyVisualStyle.boxBorder(), this.pixelScale
      );
      int buttonX = menuX + 4;
      int buttonW = menuW - 8;
      int rowY = menuY + 4;
      KeyboardLayouts.MainLayoutPreset selectedLayout = keyboardLayout.selectableEquivalent();

      for (KeyboardLayouts.MainLayoutPreset preset : KeyboardLayouts.MainLayoutPreset.selectableValues()) {
         int scaledBtnX = this.sx(buttonX);
         int scaledBtnY = this.sy(rowY);
         int scaledBtnW = buttonW * this.pixelScale;
         int scaledBtnH = 12 * this.pixelScale;
         renderer.drawHudButton(
            guiGraphics,
            scaledBtnX,
            scaledBtnY,
            scaledBtnW,
            scaledBtnH,
            preset.displayName(),
            OverlayRenderHelper.isMouseOver(smx, smy, scaledBtnX, scaledBtnY, scaledBtnW, scaledBtnH, this.pixelScale),
            preset == selectedLayout
         );
         rowY += 14;
      }
   }

   public void render(
      GuiGraphicsExtractor guiGraphics,
      KeyboardRenderer renderer,
      Font font,
      int mouseX,
      int mouseY,
      boolean debugMode,
      int cornerStyle,
      KeyboardLayouts.MainLayoutPreset keyboardLayout,
      boolean animationsEnabled,
      boolean categoriesEnabled,
      boolean showCategoryInfoButton,
      boolean categoryInfoVisible,
      float categoryModeButtonAlpha,
      boolean fKeysEnabled,
      int debugIndex,
      int totalBoxes,
      boolean settingsOpen,
      boolean restorePending,
      boolean debugExpanded,
      boolean layoutMenuOpen,
      float panelProgress
   ) {
      int centerX = this.screenWidth / 2;
      int centerY = this.screenHeight / 2;
      boolean showNavigation = debugMode && totalBoxes > 0;
      OverlayRenderHelper.pushScaleTransform(guiGraphics, centerX, centerY, this.pixelScale);
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      int scaledCloseX = this.sx(6);
      int scaledCloseY = this.sy(6);
      int scaledCloseW = 18 * this.pixelScale;
      int scaledCloseH = 18 * this.pixelScale;
      int groupRight = this.cogBtnX() - 4;
      int totalW = 124;
      int groupLeft = groupRight - totalW;
      int nextX = groupLeft + 14 + 4;
      int logX = nextX + 14 + 4;
      int folderX = logX + 42 + 4;
      int scaledPrevX = this.sx(groupLeft);
      int scaledNextX = this.sx(nextX);
      int scaledLogX = this.sx(logX);
      int scaledFolderX = this.sx(folderX);
      int scaledNavY = this.sy(6);
      int scaledDbgH = 14 * this.pixelScale;
      int scaledNavW = 14 * this.pixelScale;
      String hoveredTooltipId = null;
      Component hoveredTooltip = null;
      boolean closeHovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledCloseX, scaledCloseY, scaledCloseW, scaledCloseH, this.pixelScale);
      renderer.drawHudButton(guiGraphics, scaledCloseX, scaledCloseY, scaledCloseW, scaledCloseH, "X", closeHovered, false);
      if (closeHovered) {
         hoveredTooltipId = actionTooltipId(KeyboardDebugOverlay.Action.CLOSE);
         hoveredTooltip = actionTooltip(KeyboardDebugOverlay.Action.CLOSE);
      }

      if (showCategoryInfoButton || categoryModeButtonAlpha > 0.01F) {
         int scaledInfoX = this.sx(28);
         int scaledInfoY = this.sy(6);
         boolean infoHovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledInfoX, scaledInfoY, 18 * this.pixelScale, 18 * this.pixelScale, this.pixelScale);
         renderer.drawHudButton(
            guiGraphics, scaledInfoX, scaledInfoY, 18 * this.pixelScale, 18 * this.pixelScale, "i", infoHovered, categoryInfoVisible, categoryModeButtonAlpha
         );
         if (showCategoryInfoButton && infoHovered) {
            hoveredTooltipId = actionTooltipId(KeyboardDebugOverlay.Action.TOGGLE_CATEGORY_INFO);
            hoveredTooltip = actionTooltip(KeyboardDebugOverlay.Action.TOGGLE_CATEGORY_INFO);
         }
      }

      int cogX = this.sx(this.cogBtnX());
      int cogY = this.sy(6);
      boolean cogHovered = OverlayRenderHelper.isMouseOver(smx, smy, cogX, cogY, 18 * this.pixelScale, 18 * this.pixelScale, this.pixelScale);
      renderer.drawHudButton(guiGraphics, cogX, cogY, 18 * this.pixelScale, 18 * this.pixelScale, "⚙", cogHovered, false);
      if (cogHovered) {
         hoveredTooltipId = actionTooltipId(KeyboardDebugOverlay.Action.TOGGLE_SETTINGS);
         hoveredTooltip = actionTooltip(KeyboardDebugOverlay.Action.TOGGLE_SETTINGS);
      }

      if (panelProgress > 0.0F) {
         List<KeyboardDebugOverlay.SettingsRow> rowsList = settingsRows(debugExpanded);
         int panelW = 132;
         int fullPanelH = this.panelHeight(debugExpanded);
         int panelX = this.panelX();
         int panelY = this.panelY();
         int scaledPanelX = this.sx(panelX);
         int scaledPanelY = this.sy(panelY);
         int scaledPanelW = panelW * this.pixelScale;
         int scaledFullPanelH = fullPanelH * this.pixelScale;
         int unscaledClippedH = Math.round((float)fullPanelH * panelProgress);
         guiGraphics.enableScissor(panelX, panelY, panelX + panelW, panelY + unscaledClippedH);
         int cr = this.pixelScale;
         OverlayRenderHelper.blitBlurredBackground(guiGraphics, scaledPanelX, scaledPanelY, scaledPanelW, scaledFullPanelH, panelX, panelY, panelW, fullPanelH);
         float panelAlpha = KeyVisualStyle.panelAlpha();
         OverlayRenderHelper.fillRoundedRect(
            guiGraphics,
            scaledPanelX,
            scaledPanelY,
            scaledPanelW,
            scaledFullPanelH,
            cr,
            OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), panelAlpha)
         );
         OverlayRenderHelper.drawRoundedRectOutline(
            guiGraphics, scaledPanelX, scaledPanelY, scaledPanelW, scaledFullPanelH, cr, KeyVisualStyle.boxBorder(), this.pixelScale
         );
         int textColor = KeyVisualStyle.boxText();
         int labelX = panelX + 4;
         int controlX = panelX + panelW - 38 - 4 - 4;
         int rowY = panelY + 4;
         int rowHitX = panelX + 4;
         int rowHitW = panelW - 8;

         for (KeyboardDebugOverlay.SettingsRow row : rowsList) {
            if (row.separator()) {
               this.drawSettingsSeparator(guiGraphics, scaledPanelX, this.sy(rowY), scaledPanelW, 7 * this.pixelScale);
               rowY += 7;
            } else {
               boolean rowHovered = OverlayRenderHelper.isMouseOver(
                  smx, smy, this.sx(rowHitX), this.sy(rowY), rowHitW * this.pixelScale, 12 * this.pixelScale, this.pixelScale
               );
               if (rowHovered) {
                  hoveredTooltipId = row.tooltipId();
                  hoveredTooltip = row.tooltip();
               }

               guiGraphics.pose().pushMatrix();
               int scaledLabelX = this.sx(labelX);
               int scaledRowY = this.sy(rowY);
               guiGraphics.pose().translate((float)((float)scaledLabelX), (float)((float)scaledRowY));
               guiGraphics.pose().scale((float)((float)this.pixelScale * 0.8F), (float)((float)this.pixelScale * 0.8F));
               guiGraphics.text(font, row.label(), 0, 0, textColor, false);
               guiGraphics.pose().popMatrix();
               String btnLabel = settingButtonLabel(
                  row, cornerStyle, keyboardLayout, animationsEnabled, fKeysEnabled, categoriesEnabled, layoutMenuOpen, debugMode
               );
               boolean active = settingButtonActive(row, cornerStyle, animationsEnabled, fKeysEnabled, categoriesEnabled, layoutMenuOpen, debugMode);
               int scaledBtnX = this.sx(controlX);
               int scaledBtnY = this.sy(rowY);
               renderer.drawHudButton(
                  guiGraphics,
                  scaledBtnX,
                  scaledBtnY,
                  38 * this.pixelScale,
                  12 * this.pixelScale,
                  btnLabel,
                  OverlayRenderHelper.isMouseOver(smx, smy, scaledBtnX, scaledBtnY, 38 * this.pixelScale, 12 * this.pixelScale, this.pixelScale),
                  active
               );
               rowY += 14;
            }
         }

         if (restorePending) {
         }

         guiGraphics.disableScissor();
         if (layoutMenuOpen) {
            this.drawLayoutMenu(guiGraphics, renderer, mouseX, mouseY, keyboardLayout, debugExpanded);
         }
      }

      if (showNavigation) {
         boolean prevHovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledPrevX, scaledNavY, scaledNavW, scaledDbgH, this.pixelScale);
         boolean nextHovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledNextX, scaledNavY, scaledNavW, scaledDbgH, this.pixelScale);
         renderer.drawHudButton(guiGraphics, scaledPrevX, scaledNavY, scaledNavW, scaledDbgH, "<", prevHovered, false);
         renderer.drawHudButton(guiGraphics, scaledNextX, scaledNavY, scaledNavW, scaledDbgH, ">", nextHovered, false);
         if (prevHovered) {
            hoveredTooltipId = actionTooltipId(KeyboardDebugOverlay.Action.DEBUG_PREV);
            hoveredTooltip = actionTooltip(KeyboardDebugOverlay.Action.DEBUG_PREV);
         } else if (nextHovered) {
            hoveredTooltipId = actionTooltipId(KeyboardDebugOverlay.Action.DEBUG_NEXT);
            hoveredTooltip = actionTooltip(KeyboardDebugOverlay.Action.DEBUG_NEXT);
         }

         int shownCount = debugIndex >= 0 ? debugIndex + 1 : 0;
         String indexText = String.format("[%d/%d]", shownCount, totalBoxes);
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)((double)scaledPrevX - 30.0 * (double)this.pixelScale), (float)((double)scaledNavY + 3.0 * (double)this.pixelScale));
         guiGraphics.pose().scale((float)((float)this.pixelScale), (float)((float)this.pixelScale));
         guiGraphics.centeredText(font, indexText, 0, 0, KeyVisualStyle.boxText());
         guiGraphics.pose().popMatrix();
      }

      if (debugMode) {
         boolean logHovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledLogX, scaledNavY, 42 * this.pixelScale, 14 * this.pixelScale, this.pixelScale);
         boolean dirHovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledFolderX, scaledNavY, 42 * this.pixelScale, 14 * this.pixelScale, this.pixelScale);
         renderer.drawHudButton(
            guiGraphics, scaledLogX, scaledNavY, 42 * this.pixelScale, 14 * this.pixelScale, AtlasText.text("ui.button.log_short"), logHovered, false
         );
         renderer.drawHudButton(
            guiGraphics, scaledFolderX, scaledNavY, 42 * this.pixelScale, 14 * this.pixelScale, AtlasText.text("ui.button.dir_short"), dirHovered, false
         );
         if (logHovered) {
            hoveredTooltipId = actionTooltipId(KeyboardDebugOverlay.Action.DUMP_DEBUG_LOG);
            hoveredTooltip = actionTooltip(KeyboardDebugOverlay.Action.DUMP_DEBUG_LOG);
         } else if (dirHovered) {
            hoveredTooltipId = actionTooltipId(KeyboardDebugOverlay.Action.OPEN_DEBUG_FOLDER);
            hoveredTooltip = actionTooltip(KeyboardDebugOverlay.Action.OPEN_DEBUG_FOLDER);
         }
      }

      this.tooltipState.update(hoveredTooltipId, mouseX, mouseY);
      if (hoveredTooltipId != null && hoveredTooltip != null) {
         this.tooltipState
            .renderIfReady(guiGraphics, font, this.screenWidth, this.screenHeight, this.pixelScale, mouseX, mouseY, hoveredTooltipId, List.of(hoveredTooltip));
      }

      guiGraphics.pose().popMatrix();
   }

   public KeyboardDebugOverlay.Interaction hitTest(
      int mouseX,
      int mouseY,
      boolean debugMode,
      boolean hasBoxes,
      boolean settingsOpen,
      boolean restorePending,
      boolean debugExpanded,
      boolean layoutMenuOpen,
      float panelProgress,
      boolean showCategoryInfoButton
   ) {
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(6), this.sy(6), 18 * this.pixelScale, 18 * this.pixelScale, this.pixelScale)) {
         return KeyboardDebugOverlay.Interaction.of(KeyboardDebugOverlay.Action.CLOSE);
      } else if (showCategoryInfoButton
         && OverlayRenderHelper.isMouseOver(smx, smy, this.sx(28), this.sy(6), 18 * this.pixelScale, 18 * this.pixelScale, this.pixelScale)) {
         return KeyboardDebugOverlay.Interaction.of(KeyboardDebugOverlay.Action.TOGGLE_CATEGORY_INFO);
      } else if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(this.cogBtnX()), this.sy(6), 18 * this.pixelScale, 18 * this.pixelScale, this.pixelScale)) {
         return KeyboardDebugOverlay.Interaction.of(KeyboardDebugOverlay.Action.TOGGLE_SETTINGS);
      } else {
         if (settingsOpen && panelProgress >= 1.0F) {
            if (layoutMenuOpen) {
               int[] menuBounds = this.layoutMenuBounds(debugExpanded);
               int buttonX = menuBounds[0] + 4;
               int buttonW = menuBounds[2] - 8;
               int rowY = menuBounds[1] + 4;

               for (KeyboardLayouts.MainLayoutPreset preset : KeyboardLayouts.MainLayoutPreset.selectableValues()) {
                  int scaledBtnX = this.sx(buttonX);
                  int scaledBtnY = this.sy(rowY);
                  int scaledBtnW = buttonW * this.pixelScale;
                  int scaledBtnH = 12 * this.pixelScale;
                  if (OverlayRenderHelper.isMouseOver(smx, smy, scaledBtnX, scaledBtnY, scaledBtnW, scaledBtnH, this.pixelScale)) {
                     return KeyboardDebugOverlay.Interaction.selectLayout(preset);
                  }

                  rowY += 14;
               }
            }

            List<KeyboardDebugOverlay.SettingsRow> rowsList = settingsRows(debugExpanded);
            int panelX = this.panelX();
            int panelY = this.panelY();
            int controlX = panelX + 132 - 38 - 4 - 4;
            int ry = panelY + 4;

            for (KeyboardDebugOverlay.SettingsRow row : rowsList) {
               if (row.separator()) {
                  ry += 7;
               } else {
                  int scaledBtnX = this.sx(controlX);
                  int scaledBtnY = this.sy(ry);
                  int scaledBtnW = 38 * this.pixelScale;
                  int scaledBtnH = 12 * this.pixelScale;
                  if (OverlayRenderHelper.isMouseOver(smx, smy, scaledBtnX, scaledBtnY, scaledBtnW, scaledBtnH, this.pixelScale)) {
                     return KeyboardDebugOverlay.Interaction.of(actionForRow(row));
                  }

                  ry += 14;
               }
            }

            if (restorePending) {
            }
         }

         if (debugMode) {
            int groupRight = this.cogBtnX() - 4;
            int totalW = 124;
            int groupLeft = groupRight - totalW;
            int nextX = groupLeft + 14 + 4;
            int logX = nextX + 14 + 4;
            int folderX = logX + 42 + 4;
            int scaledNavY = this.sy(6);
            if (hasBoxes) {
               if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(groupLeft), scaledNavY, 14 * this.pixelScale, 14 * this.pixelScale, this.pixelScale)) {
                  return KeyboardDebugOverlay.Interaction.of(KeyboardDebugOverlay.Action.DEBUG_PREV);
               }

               if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(nextX), scaledNavY, 14 * this.pixelScale, 14 * this.pixelScale, this.pixelScale)) {
                  return KeyboardDebugOverlay.Interaction.of(KeyboardDebugOverlay.Action.DEBUG_NEXT);
               }
            }

            if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(logX), scaledNavY, 42 * this.pixelScale, 14 * this.pixelScale, this.pixelScale)) {
               return KeyboardDebugOverlay.Interaction.of(KeyboardDebugOverlay.Action.DUMP_DEBUG_LOG);
            }

            if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(folderX), scaledNavY, 42 * this.pixelScale, 14 * this.pixelScale, this.pixelScale)) {
               return KeyboardDebugOverlay.Interaction.of(KeyboardDebugOverlay.Action.OPEN_DEBUG_FOLDER);
            }
         }

         return KeyboardDebugOverlay.Interaction.NONE;
      }
   }

   public boolean isPointOverPanel(int mouseX, int mouseY, boolean settingsOpen, boolean debugExpanded, boolean restorePending, boolean layoutMenuOpen) {
      if (!settingsOpen) {
         return false;
      } else {
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         int panelW = 132;
         int fullPanelH = this.panelHeight(debugExpanded);
         int scaledPanelX = this.sx(this.panelX());
         int scaledPanelY = this.sy(this.panelY());
         int scaledPanelW = panelW * this.pixelScale;
         int scaledPanelH = fullPanelH * this.pixelScale;
         if (OverlayRenderHelper.isMouseOver(smx, smy, scaledPanelX, scaledPanelY, scaledPanelW, scaledPanelH, 0)) {
            return true;
         } else {
            if (layoutMenuOpen) {
               int[] menuBounds = this.layoutMenuBounds(debugExpanded);
               int scaledMenuX = this.sx(menuBounds[0]);
               int scaledMenuY = this.sy(menuBounds[1]);
               int scaledMenuW = menuBounds[2] * this.pixelScale;
               int scaledMenuH = menuBounds[3] * this.pixelScale;
               if (OverlayRenderHelper.isMouseOver(smx, smy, scaledMenuX, scaledMenuY, scaledMenuW, scaledMenuH, 0)) {
                  return true;
               }
            }

            if (restorePending) {
               int cx = this.panelX() + (panelW - 100) / 2;
               int cy = this.panelY() + (fullPanelH - 40) / 2;
               int scaledCx = this.sx(cx);
               int scaledCy = this.sy(cy);
               if (OverlayRenderHelper.isMouseOver(smx, smy, scaledCx, scaledCy, 100 * this.pixelScale, 40 * this.pixelScale, 0)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public int[] panelScaledBounds(boolean settingsOpen, boolean debugExpanded) {
      if (!settingsOpen) {
         return null;
      } else {
         int fullPanelH = this.panelHeight(debugExpanded);
         return new int[]{this.sx(this.panelX()), this.sy(this.panelY()), 132 * this.pixelScale, fullPanelH * this.pixelScale};
      }
   }

   static enum Action {
      NONE,
      CLOSE,
      TOGGLE_CATEGORY_INFO,
      TOGGLE_CATEGORIES,
      TOGGLE_DEBUG,
      TOGGLE_SETTINGS,
      TOGGLE_DEBUG_PANEL,
      RESTORE_DEFAULTS,
      CYCLE_CORNERS,
      TOGGLE_KEYBOARD_LAYOUT_MENU,
      SELECT_KEYBOARD_LAYOUT,
      TOGGLE_ANIMATIONS,
      TOGGLE_F_KEYS,
      TOGGLE_ALL_KEYS,
      TOGGLE_ALTERNATING_LINE_COLORS,
      CYCLE_PANEL_OPACITY,
      DUMP_DEBUG_LOG,
      OPEN_DEBUG_FOLDER,
      DEBUG_PREV,
      DEBUG_NEXT;
   }

   static final class Interaction {
      static final KeyboardDebugOverlay.Interaction NONE = new KeyboardDebugOverlay.Interaction(KeyboardDebugOverlay.Action.NONE, null);
      final KeyboardDebugOverlay.Action action;
      final KeyboardLayouts.MainLayoutPreset layoutPreset;

      private Interaction(KeyboardDebugOverlay.Action action, KeyboardLayouts.MainLayoutPreset layoutPreset) {
         this.action = action;
         this.layoutPreset = layoutPreset;
      }

      static KeyboardDebugOverlay.Interaction of(KeyboardDebugOverlay.Action action) {
         return action == KeyboardDebugOverlay.Action.NONE ? NONE : new KeyboardDebugOverlay.Interaction(action, null);
      }

      static KeyboardDebugOverlay.Interaction selectLayout(KeyboardLayouts.MainLayoutPreset layoutPreset) {
         return new KeyboardDebugOverlay.Interaction(KeyboardDebugOverlay.Action.SELECT_KEYBOARD_LAYOUT, layoutPreset);
      }
   }

   private static enum SettingsRow {
      RESOLUTION("ui.debug.setting.resolution", "ui.tooltip.setting.resolution"),
      ANIMATIONS("ui.debug.setting.animations", "ui.tooltip.setting.animations"),
      ALT_LINE_COLORS("ui.debug.setting.alt_line_colors", "ui.tooltip.setting.alt_line_colors"),
      CATEGORIES("ui.debug.setting.categories", "ui.tooltip.setting.categories"),
      PANEL_OPACITY("ui.debug.setting.panel_opacity", "ui.tooltip.setting.panel_opacity"),
      SEPARATOR(null, null),
      KEYBOARD_LAYOUT("ui.debug.setting.keyboard_layout", "ui.tooltip.setting.keyboard_layout"),
      FUNCTION_KEYS("ui.debug.setting.function_keys", "ui.tooltip.setting.function_keys"),
      FULL_KEYBOARD("ui.debug.setting.full_keyboard", "ui.tooltip.setting.full_keyboard"),
      DEBUG("ui.debug.setting.debug", "ui.tooltip.setting.debug"),
      LOG("ui.debug.setting.log", "ui.tooltip.setting.log"),
      DIR("ui.debug.setting.dir", "ui.tooltip.setting.dir"),
      RESET_SETTINGS("ui.debug.setting.reset_settings", "ui.tooltip.setting.reset_settings");

      private final String labelKey;
      private final String tooltipKey;

      private SettingsRow(String labelKey, String tooltipKey) {
         this.labelKey = labelKey;
         this.tooltipKey = tooltipKey;
      }

      boolean separator() {
         return this == SEPARATOR;
      }

      String label() {
         return this.labelKey == null ? "" : AtlasText.text(this.labelKey);
      }

      Component tooltip() {
         return this.tooltipKey == null ? Component.empty() : AtlasText.translatable(this.tooltipKey);
      }

      String tooltipId() {
         return "debug.setting." + this.name();
      }
   }
}
