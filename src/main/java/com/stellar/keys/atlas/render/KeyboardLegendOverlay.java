package com.stellar.keys.atlas.render;

import com.stellar.keys.atlas.AtlasText;
import com.stellar.keys.atlas.KeyVisualStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class KeyboardLegendOverlay {
   private static final int HUD_MARGIN = 6;
   private static final int PANEL_PAD_X = 4;
   private static final int PANEL_PAD_Y = 4;
   private static final int SAMPLE_W = 8;
   private static final int SAMPLE_H = 8;
   private static final int SAMPLE_LABEL_GAP = 4;
   private static final int ROW_GAP = 2;
   private static final int LEGEND_COLUMN_GAP = 10;
   private static final int LEGEND_ROWS_PER_COLUMN = 5;
   private static final float LABEL_SCALE = 0.8F;
   private static final int MOD_BTN_W = 18;
   private static final int MOD_BTN_H = 18;
   private static final float MOD_LABEL_SCALE = 0.8F;
   private static final int MOD_VISIBLE_ROWS = 8;
   private static final int MOD_PANEL_W = 134;
   private static final int MOD_ROW_GAP = 1;
   private static final int SCROLLBAR_W = 6;
   private static final int SCROLLBAR_MIN_THUMB = 8;
   private static final int BULK_BTN_W = 63;
   private static final int BULK_BTN_H = 10;
   private static final int BULK_ROW_H = 12;
   private static final int ARROW_W = 7;
   private static final int BINDING_INDENT = 6;
   private static final float BINDING_LABEL_SCALE = 0.7F;
   private static final int CHECKBOX_SIZE = 7;
   private final int screenWidth;
   private final int screenHeight;
   private final int pixelScale;
   private final HoverTooltipState tooltipState = new HoverTooltipState();
   private boolean draggingScrollbar;
   private int scrollbarDragOffset;

   public KeyboardLegendOverlay(int screenWidth, int screenHeight, int pixelScale) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.pixelScale = pixelScale;
   }

   private int sx(int x) {
      return OverlayRenderHelper.scaleX(x, this.screenWidth / 2, this.pixelScale);
   }

   private int sy(int y) {
      return OverlayRenderHelper.scaleY(y, this.screenHeight / 2, this.pixelScale);
   }

   private int modBtnX() {
      return this.screenWidth - 6 - 18;
   }

   private int modBtnY() {
      return this.screenHeight - 18 - 6;
   }

   private int modRowH(Font font) {
      return Math.max(8, Math.round(9.0F * 0.8F));
   }

   private int modPanelContentH(Font font) {
      int rowH = this.modRowH(font);
      return 21 + 8 * rowH + 7;
   }

   private int[] legendPanelBounds(Font font, List<KeyboardLegendOverlay.LegendEntry> entries) {
      if (entries.isEmpty()) {
         return new int[]{6, this.screenHeight - 6, 0, 0};
      } else {
         int rowHeight = this.legendRowHeight(font);
         int rowCount = this.legendRowsPerColumn(entries);
         int[] columnWidths = this.legendColumnWidths(font, entries);
         int panelWidth = 8;

         for (int columnWidth : columnWidths) {
            panelWidth += columnWidth;
         }

         panelWidth += Math.max(0, columnWidths.length - 1) * 10;
         int panelHeight = 8 + rowCount * rowHeight + Math.max(0, rowCount - 1) * 2;
         return new int[]{6, this.screenHeight - panelHeight - 6, panelWidth, panelHeight};
      }
   }

   private int legendColumnCount(List<KeyboardLegendOverlay.LegendEntry> entries) {
      return Math.max(1, (entries.size() + 5 - 1) / 5);
   }

   private int legendRowsPerColumn(List<KeyboardLegendOverlay.LegendEntry> entries) {
      int columnCount = this.legendColumnCount(entries);
      return Math.max(1, (entries.size() + columnCount - 1) / columnCount);
   }

   private int legendRowHeight(Font font) {
      int textHeight = Math.max(1, Math.round(9.0F * 0.8F));
      return Math.max(8, textHeight);
   }

   private int[] legendColumnWidths(Font font, List<KeyboardLegendOverlay.LegendEntry> entries) {
      int columnCount = this.legendColumnCount(entries);
      int rowsPerColumn = this.legendRowsPerColumn(entries);
      int[] widths = new int[columnCount];

      for (int column = 0; column < columnCount; column++) {
         int maxLabelWidth = 0;
         int start = column * rowsPerColumn;
         int end = Math.min(entries.size(), start + rowsPerColumn);

         for (int index = start; index < end; index++) {
            maxLabelWidth = Math.max(maxLabelWidth, Math.round((float)font.width(entries.get(index).name()) * 0.8F));
         }

         widths[column] = 12 + maxLabelWidth;
      }

      return widths;
   }

   private int[] legendRowBounds(Font font, List<KeyboardLegendOverlay.LegendEntry> entries, int index) {
      int[] panelBounds = this.legendPanelBounds(font, entries);
      int rowsPerColumn = this.legendRowsPerColumn(entries);
      int[] columnWidths = this.legendColumnWidths(font, entries);
      int rowHeight = this.legendRowHeight(font);
      int column = index / rowsPerColumn;
      int row = index % rowsPerColumn;
      int rowX = panelBounds[0] + 4;

      for (int i = 0; i < column; i++) {
         rowX += columnWidths[i] + 10;
      }

      int rowY = panelBounds[1] + 4 + row * (rowHeight + 2);
      return new int[]{rowX, rowY, columnWidths[column], rowHeight};
   }

   String hoveredLegendCategoryId(int mouseX, int mouseY, Font font, List<KeyboardLegendOverlay.LegendEntry> entries) {
      if (entries.isEmpty()) {
         return "";
      } else {
         int[] legendBounds = this.legendPanelBounds(font, entries);
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         if (!OverlayRenderHelper.isMouseOver(
            smx, smy, this.sx(legendBounds[0]), this.sy(legendBounds[1]), legendBounds[2] * this.pixelScale, legendBounds[3] * this.pixelScale, 0
         )) {
            return "";
         } else {
            for (int index = 0; index < entries.size(); index++) {
               int[] rowBounds = this.legendRowBounds(font, entries, index);
               if (OverlayRenderHelper.isMouseOver(
                  smx, smy, this.sx(rowBounds[0]), this.sy(rowBounds[1]), rowBounds[2] * this.pixelScale, rowBounds[3] * this.pixelScale, 0
               )) {
                  return entries.get(index).categoryId();
               }
            }

            return "";
         }
      }
   }

   private static String ellipsize(String text, Font font, float scale, int maxWidth) {
      if ((float)font.width(text) * scale <= (float)maxWidth) {
         return text;
      } else {
         String ell = "...";
         int maxLen = text.length();

         while (maxLen > 0 && (float)font.width(text.substring(0, maxLen) + ell) * scale > (float)maxWidth) {
            maxLen--;
         }

         return maxLen <= 0 ? ell : text.substring(0, maxLen) + ell;
      }
   }

   public void render(
      GuiGraphicsExtractor guiGraphics,
      KeyboardRenderer renderer,
      Font font,
      List<KeyboardLegendOverlay.LegendEntry> legendEntries,
      List<String> modSources,
      Map<String, Boolean> modEnabled,
      Map<String, Boolean> modExpanded,
      Map<String, List<String>> bindingsByMod,
      Map<String, Map<String, Boolean>> bindingEnabled,
      Map<String, Float> modExpandProgress,
      boolean modPanelOpen,
      int mouseX,
      int mouseY,
      int scrollIndex,
      float modPanelProgress,
      boolean showFilterButton
   ) {
      int centerX = this.screenWidth / 2;
      int centerY = this.screenHeight / 2;
      String hoveredTooltipId = null;
      Component hoveredTooltip = null;
      OverlayRenderHelper.pushScaleTransform(guiGraphics, centerX, centerY, this.pixelScale);
      int cr = this.pixelScale;
      if (!legendEntries.isEmpty()) {
         int[] legendBounds = this.legendPanelBounds(font, legendEntries);
         int panelX = legendBounds[0];
         int panelY = legendBounds[1];
         int panelWidth = legendBounds[2];
         int panelHeight = legendBounds[3];
         int scaledPanelX = this.sx(panelX);
         int scaledPanelY = this.sy(panelY);
         int scaledPanelW = panelWidth * this.pixelScale;
         int scaledPanelH = panelHeight * this.pixelScale;
         OverlayRenderHelper.blitBlurredBackground(guiGraphics, scaledPanelX, scaledPanelY, scaledPanelW, scaledPanelH, panelX, panelY, panelWidth, panelHeight);
         float legendAlpha = KeyVisualStyle.panelAlpha();
         OverlayRenderHelper.fillRoundedRect(
            guiGraphics,
            scaledPanelX,
            scaledPanelY,
            scaledPanelW,
            scaledPanelH,
            cr,
            OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), legendAlpha)
         );
         OverlayRenderHelper.drawRoundedRectOutline(
            guiGraphics, scaledPanelX, scaledPanelY, scaledPanelW, scaledPanelH, cr, KeyVisualStyle.boxBorder(), this.pixelScale
         );
         String hoveredCategoryId = this.hoveredLegendCategoryId(mouseX, mouseY, font, legendEntries);

         for (int index = 0; index < legendEntries.size(); index++) {
            KeyboardLegendOverlay.LegendEntry entry = legendEntries.get(index);
            int[] rowBounds = this.legendRowBounds(font, legendEntries, index);
            int rowX = rowBounds[0];
            int rowY = rowBounds[1];
            int rowW = rowBounds[2];
            int rowH = rowBounds[3];
            boolean rowHovered = entry.categoryId().equals(hoveredCategoryId);
            int sampleY = rowY + (rowH - 8) / 2;
            int labelX = rowX + 8 + 4;
            int textY = rowY + (rowH - 9) / 2;
            if (rowHovered) {
               int scaledRowX = this.sx(rowX - 2);
               int scaledRowY = this.sy(rowY - 1);
               int scaledRowW = (rowW + 4) * this.pixelScale;
               int scaledRowH = (rowH + 2) * this.pixelScale;
               int swatchColor = entry.style.hasOutline() ? entry.style.outlineColor() : entry.style.fillColor();
               int highlightFill = OverlayRenderHelper.withAlpha(OverlayRenderHelper.mixColor(KeyVisualStyle.boxFill(), swatchColor, 0.35F), 0.7F);
               int highlightBorder = OverlayRenderHelper.mixColor(KeyVisualStyle.boxBorder(), swatchColor, 0.7F);
               OverlayRenderHelper.fillRoundedRect(guiGraphics, scaledRowX, scaledRowY, scaledRowW, scaledRowH, this.pixelScale, highlightFill);
               OverlayRenderHelper.drawRoundedRectOutline(
                  guiGraphics, scaledRowX, scaledRowY, scaledRowW, scaledRowH, this.pixelScale, highlightBorder, this.pixelScale
               );
            }

            renderer.drawLegendKey(guiGraphics, this.sx(rowX), this.sy(sampleY), 8 * this.pixelScale, 8 * this.pixelScale, entry.sampleLabel, entry.style);
            int swatchColor = entry.style.hasOutline() ? entry.style.outlineColor() : entry.style.fillColor();
            int textColor = swatchColor != 0
               ? (rowHovered ? OverlayRenderHelper.lighten(swatchColor, 0.22F) : swatchColor)
               : (rowHovered ? KeyVisualStyle.keyHoverText() : KeyVisualStyle.boxText());
            this.drawLabel(guiGraphics, font, entry.name(), this.sx(labelX), this.sy(textY), textColor);
         }
      }

      int btnY = this.modBtnY();
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      if (showFilterButton) {
         int btnX = this.modBtnX();
         int scaledBtnX = this.sx(btnX);
         int scaledBtnY = this.sy(btnY);
         int scaledBtnW = 18 * this.pixelScale;
         int scaledBtnH = 18 * this.pixelScale;
         boolean filterHovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledBtnX, scaledBtnY, scaledBtnW, scaledBtnH, this.pixelScale);
         renderer.drawHudButton(guiGraphics, scaledBtnX, scaledBtnY, scaledBtnW, scaledBtnH, "M", filterHovered, false);
         if (filterHovered) {
            hoveredTooltipId = "legend.filter";
            hoveredTooltip = AtlasText.translatable("ui.tooltip.button.filter");
         }
      }

      if (modPanelProgress > 0.0F && modSources != null && !modSources.isEmpty()) {
         List<KeyboardLegendOverlay.DisplayRow> displayRows = buildDisplayRows(modSources, modExpanded, bindingsByMod, modExpandProgress);
         int totalRows = displayRows.size();
         int rowH = this.modRowH(font);
         int fullModPanelH = this.modPanelContentH(font);
         int modPanelX = this.screenWidth - 134 - 6;
         int modPanelY = btnY - 1 - fullModPanelH;
         int scaledModPanelX = this.sx(modPanelX);
         int scaledModPanelY = this.sy(modPanelY);
         int scaledModPanelW = 134 * this.pixelScale;
         int scaledFullModPanelH = fullModPanelH * this.pixelScale;
         int unscaledClippedH = Math.round((float)fullModPanelH * modPanelProgress);
         guiGraphics.enableScissor(modPanelX, modPanelY + fullModPanelH - unscaledClippedH, modPanelX + 134, modPanelY + fullModPanelH);
         OverlayRenderHelper.blitBlurredBackground(
            guiGraphics, scaledModPanelX, scaledModPanelY, scaledModPanelW, scaledFullModPanelH, modPanelX, modPanelY, 134, fullModPanelH
         );
         float filterAlpha = KeyVisualStyle.panelAlpha();
         OverlayRenderHelper.fillRoundedRect(
            guiGraphics,
            scaledModPanelX,
            scaledModPanelY,
            scaledModPanelW,
            scaledFullModPanelH,
            cr,
            OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), filterAlpha)
         );
         OverlayRenderHelper.drawRoundedRectOutline(
            guiGraphics, scaledModPanelX, scaledModPanelY, scaledModPanelW, scaledFullModPanelH, cr, KeyVisualStyle.boxBorder(), this.pixelScale
         );
         int bulkY = modPanelY + 4;
         int totalBulkW = 130;
         int allBtnX = modPanelX + (134 - totalBulkW) / 2;
         int noneBtnX = allBtnX + 63 + 4;
         int scaledAllBtnX = this.sx(allBtnX);
         int scaledNoneBtnX = this.sx(noneBtnX);
         int scaledBulkY = this.sy(bulkY);
         int scaledBulkW = 63 * this.pixelScale;
         int scaledBulkH = 10 * this.pixelScale;
         boolean enableAllHovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledAllBtnX, scaledBulkY, scaledBulkW, scaledBulkH, this.pixelScale);
         boolean disableAllHovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledNoneBtnX, scaledBulkY, scaledBulkW, scaledBulkH, this.pixelScale);
         renderer.drawHudButton(
            guiGraphics, scaledAllBtnX, scaledBulkY, scaledBulkW, scaledBulkH, AtlasText.text("ui.button.select_all"), enableAllHovered, false
         );
         renderer.drawHudButton(
            guiGraphics, scaledNoneBtnX, scaledBulkY, scaledBulkW, scaledBulkH, AtlasText.text("ui.button.unselect_all"), disableAllHovered, false
         );
         if (enableAllHovered) {
            hoveredTooltipId = "legend.enable_all";
            hoveredTooltip = AtlasText.translatable("ui.tooltip.button.enable_all_mods");
         } else if (disableAllHovered) {
            hoveredTooltipId = "legend.disable_all";
            hoveredTooltip = AtlasText.translatable("ui.tooltip.button.disable_all_mods");
         }

         int textColor2 = KeyVisualStyle.boxText();
         int dimTextColor = OverlayRenderHelper.darken(textColor2, 0.35F) | 0xFF000000;
         int arrowColor = -7829368;
         int toggleX = modPanelX + 134 - 7 - 4 - 8;
         int rowY2 = modPanelY + 4 + 12 + 1;
         int maxStart = Math.max(0, totalRows - 8);
         int start = Math.max(0, Math.min(scrollIndex, maxStart));
         boolean subRowScissorActive = false;

         for (int vi = 0; vi < 8; vi++) {
            int idx = start + vi;
            if (idx >= totalRows) {
               break;
            }

            KeyboardLegendOverlay.DisplayRow row = displayRows.get(idx);
            String mod = modSources.get(row.modIndex);
            switch (row.type) {
               case MOD: {
                  if (subRowScissorActive) {
                     guiGraphics.disableScissor();
                     subRowScissorActive = false;
                  }

                  boolean expanded = Boolean.TRUE.equals(modExpanded.get(mod));
                  String arrow = expanded ? "▾" : "▸";
                  int arrowX = modPanelX + 4;
                  guiGraphics.pose().pushMatrix();
                  guiGraphics.pose().translate((float)((float)this.sx(arrowX)), (float)((float)this.sy(rowY2)));
                  guiGraphics.pose().scale((float)((float)this.pixelScale * 0.8F), (float)((float)this.pixelScale * 0.8F));
                  guiGraphics.text(font, arrow, 0, 0, arrowColor, false);
                  guiGraphics.pose().popMatrix();
                  int labelX2 = modPanelX + 4 + 7;
                  int availableUnscaled = Math.max(0, toggleX - labelX2 - 4);
                  String drawText = ellipsize(mod, font, 0.8F, availableUnscaled);
                  guiGraphics.pose().pushMatrix();
                  guiGraphics.pose().translate((float)((float)this.sx(labelX2)), (float)((float)this.sy(rowY2)));
                  guiGraphics.pose().scale((float)((float)this.pixelScale * 0.8F), (float)((float)this.pixelScale * 0.8F));
                  guiGraphics.text(font, drawText, 0, 0, textColor2, false);
                  guiGraphics.pose().popMatrix();
                  boolean active = modEnabled != null && Boolean.TRUE.equals(modEnabled.get(mod));
                  this.drawCheckbox(guiGraphics, this.sx(toggleX), this.sy(rowY2), 7 * this.pixelScale, 7 * this.pixelScale, active);
                  break;
               }
               case SELECT_ALL: {
                  float expandProgress = modExpandProgress != null ? modExpandProgress.getOrDefault(mod, 1.0F) : 1.0F;
                  if (expandProgress < 1.0F && !subRowScissorActive) {
                     int subCount = countSubRowsForMod(displayRows, idx);
                     int subTotalH = subCount * (rowH + 1);
                     int clipH = Math.max(0, Math.round((float)subTotalH * expandProgress));
                     guiGraphics.enableScissor(modPanelX, rowY2, modPanelX + 134, rowY2 + clipH);
                     subRowScissorActive = true;
                  }

                  int labelX2 = modPanelX + 4 + 7 + 6;
                  int availableUnscaled = Math.max(0, toggleX - labelX2 - 4);
                  String drawText = ellipsize(AtlasText.text("ui.button.select_all"), font, 0.7F, availableUnscaled);
                  guiGraphics.pose().pushMatrix();
                  guiGraphics.pose().translate((float)((float)this.sx(labelX2)), (float)((float)this.sy(rowY2)));
                  guiGraphics.pose().scale((float)((float)this.pixelScale * 0.7F), (float)((float)this.pixelScale * 0.7F));
                  guiGraphics.text(font, drawText, 0, 0, dimTextColor, false);
                  guiGraphics.pose().popMatrix();
                  boolean allOn = isAllBindingsEnabled(mod, bindingsByMod, bindingEnabled);
                  this.drawCheckbox(guiGraphics, this.sx(toggleX), this.sy(rowY2), 7 * this.pixelScale, 7 * this.pixelScale, allOn);
                  break;
               }
               case BINDING: {
                  int labelX2 = modPanelX + 4 + 7 + 6;
                  int availableUnscaled = Math.max(0, toggleX - labelX2 - 4);
                  String drawText = ellipsize(row.bindingLabel, font, 0.7F, availableUnscaled);
                  guiGraphics.pose().pushMatrix();
                  guiGraphics.pose().translate((float)((float)this.sx(labelX2)), (float)((float)this.sy(rowY2)));
                  guiGraphics.pose().scale((float)((float)this.pixelScale * 0.7F), (float)((float)this.pixelScale * 0.7F));
                  guiGraphics.text(font, drawText, 0, 0, dimTextColor, false);
                  guiGraphics.pose().popMatrix();
                  boolean bindOn = isBindingEnabled(mod, row.bindingLabel, bindingEnabled);
                  this.drawCheckbox(guiGraphics, this.sx(toggleX), this.sy(rowY2), 7 * this.pixelScale, 7 * this.pixelScale, bindOn);
               }
            }

            rowY2 += rowH + 1;
         }

         if (subRowScissorActive) {
            guiGraphics.disableScissor();
         }

         if (totalRows > 8) {
            int sbX = modPanelX + 134 - 6 - 4;
            int sbY = modPanelY + 4 + 12 + 1;
            int sbH = 8 * rowH + 7;
            int scaledSbX = this.sx(sbX);
            int scaledSbY = this.sy(sbY);
            int scaledSbW = 6 * this.pixelScale;
            int scaledSbH = sbH * this.pixelScale;
            guiGraphics.fill(
               scaledSbX,
               scaledSbY,
               scaledSbX + scaledSbW,
               scaledSbY + scaledSbH,
               OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.28F), 1.0F)
            );
            float frac = 8.0F / (float)totalRows;
            int thumbH = Math.max(8, Math.round((float)scaledSbH * frac));
            int thumbRange = Math.max(1, scaledSbH - thumbH);
            int thumbY = scaledSbY + Math.round((float)start / (float)maxStart * (float)thumbRange);
            guiGraphics.fill(scaledSbX, thumbY, scaledSbX + scaledSbW, thumbY + thumbH, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), 1.0F));
         }

         guiGraphics.disableScissor();
      }

      this.tooltipState.update(hoveredTooltipId, mouseX, mouseY);
      if (hoveredTooltipId != null && hoveredTooltip != null) {
         this.tooltipState
            .renderIfReady(guiGraphics, font, this.screenWidth, this.screenHeight, this.pixelScale, mouseX, mouseY, hoveredTooltipId, List.of(hoveredTooltip));
      }

      guiGraphics.pose().popMatrix();
   }

   public KeyboardLegendOverlay.ModAction hitTest(
      int mouseX,
      int mouseY,
      Font font,
      List<String> modSources,
      Map<String, Boolean> modExpanded,
      Map<String, List<String>> bindingsByMod,
      boolean modPanelOpen,
      int scrollIndex,
      float modPanelProgress
   ) {
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      int scaledBtnX = this.sx(this.modBtnX());
      int scaledBtnY = this.sy(this.modBtnY());
      int scaledBtnW = 18 * this.pixelScale;
      int scaledBtnH = 18 * this.pixelScale;
      if (OverlayRenderHelper.isMouseOver(smx, smy, scaledBtnX, scaledBtnY, scaledBtnW, scaledBtnH, 0)) {
         return new KeyboardLegendOverlay.ModAction(KeyboardLegendOverlay.ModAction.Type.TOGGLE_BUTTON, -1);
      } else {
         if (modPanelOpen && modPanelProgress >= 1.0F && modSources != null && !modSources.isEmpty()) {
            List<KeyboardLegendOverlay.DisplayRow> displayRows = buildDisplayRows(modSources, modExpanded, bindingsByMod);
            int totalRows = displayRows.size();
            int rowH = this.modRowH(font);
            int fullModPanelH = this.modPanelContentH(font);
            int modPanelX = this.screenWidth - 134 - 6;
            int modPanelY = this.modBtnY() - 1 - fullModPanelH;
            int bulkY = modPanelY + 4;
            int totalBulkW = 130;
            int allBtnX = modPanelX + (134 - totalBulkW) / 2;
            int noneBtnX = allBtnX + 63 + 4;
            if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(allBtnX), this.sy(bulkY), 63 * this.pixelScale, 10 * this.pixelScale, 0)) {
               return new KeyboardLegendOverlay.ModAction(KeyboardLegendOverlay.ModAction.Type.ENABLE_ALL, -1);
            }

            if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(noneBtnX), this.sy(bulkY), 63 * this.pixelScale, 10 * this.pixelScale, 0)) {
               return new KeyboardLegendOverlay.ModAction(KeyboardLegendOverlay.ModAction.Type.DISABLE_ALL, -1);
            }

            int toggleX = modPanelX + 134 - 7 - 4 - 8;
            int maxStart = Math.max(0, totalRows - 8);
            int start = Math.max(0, Math.min(scrollIndex, maxStart));

            for (int vi = 0; vi < 8; vi++) {
               int idx = start + vi;
               if (idx >= totalRows) {
                  break;
               }

               KeyboardLegendOverlay.DisplayRow row = displayRows.get(idx);
               int ry = modPanelY + 4 + 12 + 1 + vi * (rowH + 1);
               int scaledToggleX = this.sx(toggleX);
               int scaledToggleY = this.sy(ry);
               int scaledToggleW = 7 * this.pixelScale;
               int scaledToggleH = 7 * this.pixelScale;
               int scaledRowH = rowH * this.pixelScale;
               switch (row.type) {
                  case MOD:
                     if (OverlayRenderHelper.isMouseOver(smx, smy, scaledToggleX, scaledToggleY, scaledToggleW, scaledToggleH, 0)) {
                        return new KeyboardLegendOverlay.ModAction(KeyboardLegendOverlay.ModAction.Type.TOGGLE_MOD, row.modIndex);
                     }

                     int rowLeftxx = this.sx(modPanelX + 4);
                     if (smx >= rowLeftxx && smx < scaledToggleX && smy >= scaledToggleY && smy < scaledToggleY + scaledRowH) {
                        return new KeyboardLegendOverlay.ModAction(KeyboardLegendOverlay.ModAction.Type.EXPAND_MOD, row.modIndex);
                     }
                     break;
                  case SELECT_ALL:
                     int rowLeftx = this.sx(modPanelX + 4);
                     int rowRightx = this.sx(modPanelX + 134 - 4);
                     if (smx >= rowLeftx && smx < rowRightx && smy >= scaledToggleY && smy < scaledToggleY + scaledRowH) {
                        return new KeyboardLegendOverlay.ModAction(KeyboardLegendOverlay.ModAction.Type.SELECT_ALL_BINDINGS, row.modIndex);
                     }
                     break;
                  case BINDING:
                     int rowLeft = this.sx(modPanelX + 4);
                     int rowRight = this.sx(modPanelX + 134 - 4);
                     if (smx >= rowLeft && smx < rowRight && smy >= scaledToggleY && smy < scaledToggleY + scaledRowH) {
                        return new KeyboardLegendOverlay.ModAction(KeyboardLegendOverlay.ModAction.Type.TOGGLE_BINDING, row.modIndex, row.bindingLabel);
                     }
               }
            }
         }

         return new KeyboardLegendOverlay.ModAction(KeyboardLegendOverlay.ModAction.Type.NONE, -1);
      }
   }

   public boolean beginScrollbarDrag(
      int mouseX,
      int mouseY,
      Font font,
      List<String> modSources,
      Map<String, Boolean> modExpanded,
      Map<String, List<String>> bindingsByMod,
      Map<String, Float> modExpandProgress,
      int scrollIndex,
      float modPanelProgress
   ) {
      int[] geo = this.modScrollbarGeometry(font, modSources, modExpanded, bindingsByMod, modExpandProgress, scrollIndex, modPanelProgress);
      if (geo == null) {
         return false;
      } else {
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         if (OverlayRenderHelper.isMouseOver(smx, smy, geo[0], geo[4], geo[2], geo[5], 0)) {
            this.draggingScrollbar = true;
            this.scrollbarDragOffset = smy - geo[4];
            return true;
         } else if (OverlayRenderHelper.isMouseOver(smx, smy, geo[0], geo[1], geo[2], geo[3], 0)) {
            this.draggingScrollbar = true;
            this.scrollbarDragOffset = geo[5] / 2;
            return true;
         } else {
            return false;
         }
      }
   }

   public int dragScrollbarTo(
      int mouseY,
      Font font,
      List<String> modSources,
      Map<String, Boolean> modExpanded,
      Map<String, List<String>> bindingsByMod,
      Map<String, Float> modExpandProgress,
      int scrollIndex,
      float modPanelProgress
   ) {
      if (!this.draggingScrollbar) {
         return scrollIndex;
      } else {
         int[] geo = this.modScrollbarGeometry(font, modSources, modExpanded, bindingsByMod, modExpandProgress, scrollIndex, modPanelProgress);
         if (geo == null) {
            this.endScrollbarDrag();
            return scrollIndex;
         } else {
            int smy = this.sy(mouseY);
            int thumbMinY = geo[1];
            int thumbMaxY = geo[1] + geo[3] - geo[5];
            int thumbY = Math.max(thumbMinY, Math.min(smy - this.scrollbarDragOffset, thumbMaxY));
            int thumbRange = Math.max(1, geo[3] - geo[5]);
            float ratio = (float)(thumbY - thumbMinY) / (float)thumbRange;
            return Math.round(ratio * (float)geo[6]);
         }
      }
   }

   public boolean isDraggingScrollbar() {
      return this.draggingScrollbar;
   }

   public void endScrollbarDrag() {
      this.draggingScrollbar = false;
      this.scrollbarDragOffset = 0;
   }

   private void drawLabel(GuiGraphicsExtractor guiGraphics, Font font, String text, int x, int y, int color) {
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().translate((float)((float)x), (float)((float)y));
      guiGraphics.pose().scale((float)((float)this.pixelScale * 0.8F), (float)((float)this.pixelScale * 0.8F));
      guiGraphics.text(font, text, 0, 0, color, false);
      guiGraphics.pose().popMatrix();
   }

   private void drawCheckbox(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h, boolean checked) {
      int bgColor = OverlayRenderHelper.withAlpha(KeyVisualStyle.boxFill(), 0.8F);
      int borderColor = OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), checked ? 1.0F : 0.5F);
      guiGraphics.fill(x, y, x + w, y + h, bgColor);
      OverlayRenderHelper.drawRectOutline(guiGraphics, x, y, w, h, borderColor, this.pixelScale);
      if (checked) {
         int inset = Math.max(this.pixelScale, this.pixelScale * 2);
         int checkColor = OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), 1.0F);
         guiGraphics.fill(x + inset, y + inset, x + w - inset, y + h - inset, checkColor);
      }
   }

   private static int countSubRowsForMod(List<KeyboardLegendOverlay.DisplayRow> rows, int startIdx) {
      int count = 0;

      for (int i = startIdx; i < rows.size(); i++) {
         KeyboardLegendOverlay.DisplayRow.Type t = rows.get(i).type;
         if (t != KeyboardLegendOverlay.DisplayRow.Type.SELECT_ALL && t != KeyboardLegendOverlay.DisplayRow.Type.BINDING) {
            break;
         }

         count++;
      }

      return count;
   }

   private static boolean isBindingEnabled(String mod, String bindingLabel, Map<String, Map<String, Boolean>> bindingEnabled) {
      if (bindingEnabled == null) {
         return true;
      } else {
         Map<String, Boolean> modBindings = bindingEnabled.get(mod);
         if (modBindings == null) {
            return true;
         } else {
            Boolean val = modBindings.get(bindingLabel);
            return val == null || val;
         }
      }
   }

   private static boolean isAllBindingsEnabled(String mod, Map<String, List<String>> bindingsByMod, Map<String, Map<String, Boolean>> bindingEnabled) {
      List<String> bindings = bindingsByMod.getOrDefault(mod, List.of());
      if (bindings.isEmpty()) {
         return true;
      } else {
         for (String b : bindings) {
            if (!isBindingEnabled(mod, b, bindingEnabled)) {
               return false;
            }
         }

         return true;
      }
   }

   public static List<KeyboardLegendOverlay.DisplayRow> buildDisplayRows(
      List<String> modSources, Map<String, Boolean> modExpanded, Map<String, List<String>> bindingsByMod
   ) {
      return buildDisplayRows(modSources, modExpanded, bindingsByMod, null);
   }

   public static List<KeyboardLegendOverlay.DisplayRow> buildDisplayRows(
      List<String> modSources, Map<String, Boolean> modExpanded, Map<String, List<String>> bindingsByMod, Map<String, Float> modExpandProgress
   ) {
      List<KeyboardLegendOverlay.DisplayRow> rows = new ArrayList<>();

      for (int i = 0; i < modSources.size(); i++) {
         String mod = modSources.get(i);
         rows.add(new KeyboardLegendOverlay.DisplayRow(KeyboardLegendOverlay.DisplayRow.Type.MOD, i, mod, null));
         boolean expanded = modExpanded != null && Boolean.TRUE.equals(modExpanded.get(mod));
         boolean animating = modExpandProgress != null && modExpandProgress.getOrDefault(mod, 0.0F) > 0.0F;
         if (expanded || animating) {
            rows.add(new KeyboardLegendOverlay.DisplayRow(KeyboardLegendOverlay.DisplayRow.Type.SELECT_ALL, i, AtlasText.text("ui.button.select_all"), null));

            for (String binding : (bindingsByMod != null ? bindingsByMod.getOrDefault(mod, List.<String>of()) : List.<String>of())) {
               rows.add(new KeyboardLegendOverlay.DisplayRow(KeyboardLegendOverlay.DisplayRow.Type.BINDING, i, binding, binding));
            }
         }
      }

      return rows;
   }

   public static int displayRowCount(List<String> modSources, Map<String, Boolean> modExpanded, Map<String, List<String>> bindingsByMod) {
      return displayRowCount(modSources, modExpanded, bindingsByMod, null);
   }

   public static int displayRowCount(
      List<String> modSources, Map<String, Boolean> modExpanded, Map<String, List<String>> bindingsByMod, Map<String, Float> modExpandProgress
   ) {
      int count = 0;

      for (String mod : modSources) {
         count++;
         boolean expanded = modExpanded != null && Boolean.TRUE.equals(modExpanded.get(mod));
         boolean animating = modExpandProgress != null && modExpandProgress.getOrDefault(mod, 0.0F) > 0.0F;
         if (expanded || animating) {
            count = ++count + (bindingsByMod != null ? bindingsByMod.getOrDefault(mod, List.of()).size() : 0);
         }
      }

      return count;
   }

   private int[] modScrollbarGeometry(
      Font font,
      List<String> modSources,
      Map<String, Boolean> modExpanded,
      Map<String, List<String>> bindingsByMod,
      Map<String, Float> modExpandProgress,
      int scrollIndex,
      float modPanelProgress
   ) {
      if (!(modPanelProgress <= 0.0F) && modSources != null && !modSources.isEmpty()) {
         int totalRows = displayRowCount(modSources, modExpanded, bindingsByMod, modExpandProgress);
         if (totalRows <= 8) {
            return null;
         } else {
            int rowH = this.modRowH(font);
            int fullModPanelH = this.modPanelContentH(font);
            int modPanelX = this.screenWidth - 134 - 6;
            int modPanelY = this.modBtnY() - 1 - fullModPanelH;
            int maxStart = Math.max(0, totalRows - 8);
            int start = Math.max(0, Math.min(scrollIndex, maxStart));
            int sbX = this.sx(modPanelX + 134 - 6 - 4);
            int sbY = this.sy(modPanelY + 4 + 12 + 1);
            int sbW = 6 * this.pixelScale;
            int sbH = (8 * rowH + 7) * this.pixelScale;
            float frac = 8.0F / (float)totalRows;
            int thumbH = Math.max(8, Math.round((float)sbH * frac));
            int thumbRange = Math.max(1, sbH - thumbH);
            int thumbY = sbY + Math.round((float)start / (float)maxStart * (float)thumbRange);
            return new int[]{sbX, sbY, sbW, sbH, thumbY, thumbH, maxStart};
         }
      } else {
         return null;
      }
   }

   public boolean isPointOverModFilter(int mouseX, int mouseY, Font font, boolean modFilterOpen, float modPanelProgress) {
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      int scaledBtnX = this.sx(this.modBtnX());
      int scaledBtnY = this.sy(this.modBtnY());
      int scaledBtnW = 18 * this.pixelScale;
      int scaledBtnH = 18 * this.pixelScale;
      if (OverlayRenderHelper.isMouseOver(smx, smy, scaledBtnX, scaledBtnY, scaledBtnW, scaledBtnH, 0)) {
         return true;
      } else {
         if (modFilterOpen && modPanelProgress > 0.0F) {
            int fullModPanelH = this.modPanelContentH(font);
            int modPanelX = this.screenWidth - 134 - 6;
            int modPanelY = this.modBtnY() - 1 - fullModPanelH;
            int scaledPanelX = this.sx(modPanelX);
            int scaledPanelY = this.sy(modPanelY);
            int scaledPanelW = 134 * this.pixelScale;
            int scaledPanelH = fullModPanelH * this.pixelScale;
            if (OverlayRenderHelper.isMouseOver(smx, smy, scaledPanelX, scaledPanelY, scaledPanelW, scaledPanelH, 0)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isPointOverLegendPanel(int mouseX, int mouseY, Font font, List<KeyboardLegendOverlay.LegendEntry> entries) {
      if (entries.isEmpty()) {
         return false;
      } else {
         int[] bounds = this.legendPanelBounds(font, entries);
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         return OverlayRenderHelper.isMouseOver(smx, smy, this.sx(bounds[0]), this.sy(bounds[1]), bounds[2] * this.pixelScale, bounds[3] * this.pixelScale, 0);
      }
   }

   static final class DisplayRow {
      final KeyboardLegendOverlay.DisplayRow.Type type;
      final int modIndex;
      final String text;
      final String bindingLabel;

      DisplayRow(KeyboardLegendOverlay.DisplayRow.Type type, int modIndex, String text, String bindingLabel) {
         this.type = type;
         this.modIndex = modIndex;
         this.text = text;
         this.bindingLabel = bindingLabel;
      }

      static enum Type {
         MOD,
         SELECT_ALL,
         BINDING;
      }
   }

   static final class LegendEntry {
      final String categoryId;
      final String sampleLabel;
      final String name;
      final KeyboardRenderer.KeyRenderStyle style;

      private LegendEntry(String categoryId, String sampleLabel, String name, KeyboardRenderer.KeyRenderStyle style) {
         this.categoryId = categoryId;
         this.sampleLabel = sampleLabel;
         this.name = name;
         this.style = style;
      }

      static KeyboardLegendOverlay.LegendEntry standard(String categoryId, String name, KeyboardRenderer.KeySpriteVariant variant) {
         return new KeyboardLegendOverlay.LegendEntry(categoryId, "", name, KeyboardRenderer.KeyRenderStyle.standard(variant));
      }

      static KeyboardLegendOverlay.LegendEntry custom(String categoryId, String name, int fillColor, int textColor) {
         return new KeyboardLegendOverlay.LegendEntry(categoryId, "", name, KeyboardRenderer.KeyRenderStyle.custom(fillColor, textColor, false));
      }

      static KeyboardLegendOverlay.LegendEntry outline(String categoryId, String name, int outlineColor, int fillColor, int textColor) {
         return new KeyboardLegendOverlay.LegendEntry(
            categoryId, "", name, KeyboardRenderer.KeyRenderStyle.outline(outlineColor, fillColor, textColor, false)
         );
      }

      String name() {
         return this.name;
      }

      String categoryId() {
         return this.categoryId;
      }
   }

   static final class ModAction {
      final KeyboardLegendOverlay.ModAction.Type type;
      final int modIndex;
      final String bindingLabel;

      ModAction(KeyboardLegendOverlay.ModAction.Type type, int modIndex) {
         this(type, modIndex, null);
      }

      ModAction(KeyboardLegendOverlay.ModAction.Type type, int modIndex, String bindingLabel) {
         this.type = type;
         this.modIndex = modIndex;
         this.bindingLabel = bindingLabel;
      }

      static enum Type {
         NONE,
         TOGGLE_BUTTON,
         TOGGLE_MOD,
         ENABLE_ALL,
         DISABLE_ALL,
         EXPAND_MOD,
         TOGGLE_BINDING,
         SELECT_ALL_BINDINGS;
      }
   }
}
