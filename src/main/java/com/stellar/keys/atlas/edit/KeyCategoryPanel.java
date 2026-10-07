package com.stellar.keys.atlas.edit;

import com.stellar.keys.atlas.AtlasText;
import com.stellar.keys.atlas.KeyCategory;
import com.stellar.keys.atlas.KeyVisualStyle;
import com.stellar.keys.atlas.render.HoverTooltipState;
import com.stellar.keys.atlas.render.KeyboardRenderer;
import com.stellar.keys.atlas.render.OverlayRenderHelper;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class KeyCategoryPanel {
   private static final int TOGGLE_BTN_W = 132;
   private static final int TOGGLE_BTN_H = 14;
   private static final int PANEL_W = 178;
   private static final int HEADER_H = 14;
   private static final int HEADER_GAP = 4;
   private static final int ROW_H = 20;
   private static final int ROW_GAP = 2;
   private static final int VISIBLE_ROWS = 7;
   private static final int PANEL_PAD = 4;
   private static final int SWATCH_SIZE = 10;
   private static final int EDIT_BTN_SIZE = 10;
   private static final int DELETE_BTN_SIZE = 10;
   private static final int ADD_BTN_SIZE = 12;
   private static final int BOTTOM_MARGIN = 6;
   private static final int SCROLLBAR_W = 5;
   private static final float TITLE_SCALE = 0.8F;
   private static final float NAME_SCALE = 0.75F;
   private static final float SUBTEXT_SCALE = 0.6F;
   private static final int TOGGLE_SIDE_ICON_SIZE = 8;
   private static final int TOGGLE_SIDE_ICON_INSET = 4;
   private static final int ACTION_BTN_HIT_PAD = 2;
   private final int screenWidth;
   private final int screenHeight;
   private final int pixelScale;
   private final HoverTooltipState tooltipState = new HoverTooltipState();
   private boolean expanded;
   private int scrollOffset;
   private float panelProgress;

   public KeyCategoryPanel(int screenWidth, int screenHeight, int pixelScale) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.pixelScale = pixelScale;
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   public void setExpanded(boolean expanded) {
      this.expanded = expanded;
      if (!expanded) {
         this.scrollOffset = 0;
      }
   }

   public void toggle() {
      this.expanded = !this.expanded;
      if (!this.expanded) {
         this.scrollOffset = 0;
      }
   }

   public float panelProgress() {
      return this.panelProgress;
   }

   public void updateAnimations(float step) {
      float target = this.expanded ? 1.0F : 0.0F;
      if (this.panelProgress < target) {
         this.panelProgress = Math.min(target, this.panelProgress + step);
      } else if (this.panelProgress > target) {
         this.panelProgress = Math.max(target, this.panelProgress - step);
      }
   }

   public void scroll(int direction, List<KeyCategory> categories) {
      if (this.expanded) {
         int maxStart = Math.max(0, categories.size() - 7);
         this.scrollOffset = Math.max(0, Math.min(maxStart, this.scrollOffset + direction));
      }
   }

   public boolean isPointOverPanel(int mouseX, int mouseY, List<KeyCategory> categories) {
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      int tbx = this.sx(this.toggleBtnX());
      int tby = this.sy(this.toggleBtnY());
      int tbw = 132 * this.pixelScale;
      int tbh = 14 * this.pixelScale;
      if (OverlayRenderHelper.isMouseOver(smx, smy, tbx, tby, tbw, tbh, 0)) {
         return true;
      } else if (!(this.panelProgress <= 0.0F) && !categories.isEmpty()) {
         int[] geo = this.panelGeometry(categories);
         return OverlayRenderHelper.isMouseOver(smx, smy, this.sx(geo[0]), this.sy(geo[1]), geo[2] * this.pixelScale, geo[3] * this.pixelScale, 0);
      } else {
         return false;
      }
   }

   public KeyCategoryPanel.PanelAction hitTest(int mouseX, int mouseY, List<KeyCategory> categories) {
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      int actionHitPad = 2 * this.pixelScale;
      int tbx = this.sx(this.toggleBtnX());
      int tby = this.sy(this.toggleBtnY());
      int tbw = 132 * this.pixelScale;
      int tbh = 14 * this.pixelScale;
      if (OverlayRenderHelper.isMouseOver(smx, smy, tbx, tby, tbw, tbh, this.pixelScale)) {
         return KeyCategoryPanel.PanelAction.toggle();
      } else if (this.expanded && !(this.panelProgress <= 0.0F) && !categories.isEmpty()) {
         int[] geo = this.panelGeometry(categories);
         int panelX = geo[0];
         int panelY = geo[1];
         int panelW = geo[2];
         int listY = panelY + 4 + 14 + 4;
         int addBtnX = panelX + panelW - 4 - 12;
         int addBtnY = panelY + 4 + 1;
         if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(addBtnX), this.sy(addBtnY), 12 * this.pixelScale, 12 * this.pixelScale, actionHitPad)) {
            return KeyCategoryPanel.PanelAction.create();
         } else {
            int start = this.firstVisibleIndex(categories);
            int visibleRows = Math.min(7, categories.size());

            for (int i = 0; i < visibleRows; i++) {
               int categoryIndex = start + i;
               if (categoryIndex >= categories.size()) {
                  break;
               }

               KeyCategory category = categories.get(categoryIndex);
               int rowX = panelX + 4;
               int rowY = listY + i * 22;
               int rowW = panelW - 8 - (this.hasScrollbar(categories) ? 7 : 0);
               if (category.deletable()) {
                  int deleteX = rowX + rowW - 10 - 2;
                  int editX = deleteX - 10 - 2;
                  int actionY = rowY + 5;
                  if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(editX), this.sy(actionY), 10 * this.pixelScale, 10 * this.pixelScale, actionHitPad)) {
                     return KeyCategoryPanel.PanelAction.edit(category.id());
                  }

                  int deleteY = rowY + 5;
                  if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(deleteX), this.sy(deleteY), 10 * this.pixelScale, 10 * this.pixelScale, actionHitPad)) {
                     return KeyCategoryPanel.PanelAction.delete(category.id());
                  }
               }

               if (OverlayRenderHelper.isMouseOver(smx, smy, this.sx(rowX), this.sy(rowY), rowW * this.pixelScale, 20 * this.pixelScale, 0)) {
                  return KeyCategoryPanel.PanelAction.select(category.id());
               }
            }

            return KeyCategoryPanel.PanelAction.NONE;
         }
      } else {
         return KeyCategoryPanel.PanelAction.NONE;
      }
   }

   public void render(
      GuiGraphicsExtractor guiGraphics,
      KeyboardRenderer renderer,
      Font font,
      int mouseX,
      int mouseY,
      List<KeyCategory> categories,
      String selectedCategoryId,
      List<KeyCategory.PresetColor> presetColors,
      float alpha
   ) {
      if (alpha <= 0.01F) {
         this.tooltipState.update(null, mouseX, mouseY);
      } else {
         KeyCategory selectedCategory = this.selectedCategory(selectedCategoryId, categories);
         int centerX = this.screenWidth / 2;
         int centerY = this.screenHeight / 2;
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         String hoveredTooltipId = null;
         List<Component> hoveredTooltipLines = List.of();
         OverlayRenderHelper.pushScaleTransform(guiGraphics, centerX, centerY, this.pixelScale);
         int tbx = this.sx(this.toggleBtnX());
         int tby = this.sy(this.toggleBtnY());
         int tbw = 132 * this.pixelScale;
         int tbh = 14 * this.pixelScale;
         String toggleLabel = AtlasText.text("ui.categories.toggle", this.displayCategoryName(selectedCategory));
         boolean toggleHovered = OverlayRenderHelper.isMouseOver(smx, smy, tbx, tby, tbw, tbh, this.pixelScale);
         renderer.drawLegendKey(
            guiGraphics,
            tbx,
            tby,
            tbw,
            tbh,
            toggleLabel,
            renderer.keyStyle(selectedCategory.fillColor(), KeyVisualStyle.keyAssignedText(), toggleHovered || this.expanded),
            alpha
         );
         if (!this.expanded) {
            int iconSize = 8 * this.pixelScale;
            int iconY = tby + (tbh - iconSize) / 2;
            int leftIconX = tbx + 4 * this.pixelScale;
            int rightIconX = tbx + tbw - iconSize - 4 * this.pixelScale;
            renderer.drawIcon(guiGraphics, leftIconX, iconY, iconSize, iconSize, "up-arrow", KeyVisualStyle.keyAssignedText(), alpha);
            renderer.drawIcon(guiGraphics, rightIconX, iconY, iconSize, iconSize, "up-arrow", KeyVisualStyle.keyAssignedText(), alpha);
         }

         if (!(this.panelProgress <= 0.0F) && !categories.isEmpty()) {
            int[] geo = this.panelGeometry(categories);
            int panelX = geo[0];
            int panelY = geo[1];
            int panelW = geo[2];
            int panelH = geo[3];
            int listY = panelY + 4 + 14 + 4;
            int scaledPanelX = this.sx(panelX);
            int scaledPanelY = this.sy(panelY);
            int scaledPanelW = panelW * this.pixelScale;
            int scaledPanelH = panelH * this.pixelScale;
            OverlayRenderHelper.blitBlurredBackground(
               guiGraphics, scaledPanelX, scaledPanelY, scaledPanelW, scaledPanelH, panelX, panelY, panelW, panelH, alpha
            );
            OverlayRenderHelper.fillRoundedRect(
               guiGraphics,
               scaledPanelX,
               scaledPanelY,
               scaledPanelW,
               scaledPanelH,
               this.pixelScale,
               OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), KeyVisualStyle.panelAlpha() * alpha)
            );
            OverlayRenderHelper.drawRoundedRectOutline(
               guiGraphics,
               scaledPanelX,
               scaledPanelY,
               scaledPanelW,
               scaledPanelH,
               this.pixelScale,
               OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), alpha),
               this.pixelScale
            );
            guiGraphics.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)this.sx(panelX + 4), (float)this.sy(panelY + 4 + 1));
            guiGraphics.pose().scale((float)this.pixelScale * 0.8F, (float)this.pixelScale * 0.8F);
            guiGraphics.text(font, AtlasText.text("ui.categories.title"), 0, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxText(), alpha), false);
            guiGraphics.pose().popMatrix();
            int addBtnX = panelX + panelW - 4 - 12;
            int addBtnY = panelY + 4 + 1;
            int actionHitPad = 2 * this.pixelScale;
            boolean addHovered = OverlayRenderHelper.isMouseOver(
               smx, smy, this.sx(addBtnX), this.sy(addBtnY), 12 * this.pixelScale, 12 * this.pixelScale, actionHitPad
            );
            renderer.drawHudButton(
               guiGraphics, this.sx(addBtnX), this.sy(addBtnY), 12 * this.pixelScale, 12 * this.pixelScale, "+", addHovered, false, alpha, 2 * this.pixelScale
            );
            int start = this.firstVisibleIndex(categories);
            int visibleRows = Math.min(7, categories.size());
            boolean showScrollbar = this.hasScrollbar(categories);

            for (int i = 0; i < visibleRows; i++) {
               int categoryIndex = start + i;
               if (categoryIndex >= categories.size()) {
                  break;
               }

               KeyCategory category = categories.get(categoryIndex);
               int rowX = panelX + 4;
               int rowY = listY + i * 22;
               int rowW = panelW - 8 - (showScrollbar ? 7 : 0);
               int scaledRowX = this.sx(rowX);
               int scaledRowY = this.sy(rowY);
               int scaledRowW = rowW * this.pixelScale;
               int scaledRowH = 20 * this.pixelScale;
               boolean selected = category.id().equals(selectedCategory.id());
               boolean hovered = OverlayRenderHelper.isMouseOver(smx, smy, scaledRowX, scaledRowY, scaledRowW, scaledRowH, this.pixelScale);
               int rowFill = selected
                  ? OverlayRenderHelper.lighten(KeyVisualStyle.boxFill(), 0.08F)
                  : OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.06F);
               if (hovered && !selected) {
                  rowFill = OverlayRenderHelper.lighten(rowFill, 0.08F);
               }

               guiGraphics.fill(scaledRowX, scaledRowY, scaledRowX + scaledRowW, scaledRowY + scaledRowH, OverlayRenderHelper.withAlpha(rowFill, alpha));
               OverlayRenderHelper.drawRoundedRectOutline(
                  guiGraphics,
                  scaledRowX,
                  scaledRowY,
                  scaledRowW,
                  scaledRowH,
                  this.pixelScale,
                  OverlayRenderHelper.withAlpha(selected ? category.fillColor() : KeyVisualStyle.boxBorder(), alpha),
                  this.pixelScale
               );
               int swatchX = rowX + 3;
               int swatchY = rowY + 5;
               int scaledSwatchX = this.sx(swatchX);
               int scaledSwatchY = this.sy(swatchY);
               int scaledSwatchSize = 10 * this.pixelScale;
               guiGraphics.fill(
                  scaledSwatchX,
                  scaledSwatchY,
                  scaledSwatchX + scaledSwatchSize,
                  scaledSwatchY + scaledSwatchSize,
                  OverlayRenderHelper.withAlpha(category.fillColor(), alpha)
               );
               OverlayRenderHelper.drawRoundedRectOutline(
                  guiGraphics,
                  scaledSwatchX,
                  scaledSwatchY,
                  scaledSwatchSize,
                  scaledSwatchSize,
                  this.pixelScale,
                  OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(category.fillColor(), 0.35F), alpha),
                  this.pixelScale
               );
               int textX = swatchX + 10 + 4;
               int actionButtonsW = category.deletable() ? 32 : 4;
               int textW = rowW - (textX - rowX) - actionButtonsW;
               String displayName = this.trimToWidth(font, this.displayCategoryName(category), 0.75F, textW);
               String colorName = this.trimToWidth(font, this.colorName(category, presetColors), 0.6F, textW);
               guiGraphics.pose().pushMatrix();
               guiGraphics.pose().translate((float)((float)this.sx(textX)), (float)((float)this.sy(rowY + 2)));
               guiGraphics.pose().scale((float)((float)this.pixelScale * 0.75F), (float)((float)this.pixelScale * 0.75F));
               int nameColor = (selected || hovered) ? OverlayRenderHelper.lighten(category.fillColor(), 0.22F) : category.fillColor();
               guiGraphics.text(font, displayName, 0, 0, OverlayRenderHelper.withAlpha(nameColor, alpha), false);
               guiGraphics.pose().popMatrix();
               guiGraphics.pose().pushMatrix();
               guiGraphics.pose().translate((float)((float)this.sx(textX)), (float)((float)this.sy(rowY + 10)));
               guiGraphics.pose().scale((float)((float)this.pixelScale * 0.6F), (float)((float)this.pixelScale * 0.6F));
               guiGraphics.text(font, colorName, 0, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxSourceText(), alpha), false);
               guiGraphics.pose().popMatrix();
               if (hovered) {
                  hoveredTooltipId = "category-row-" + category.id();
                  hoveredTooltipLines = List.of(Component.literal(this.displayCategoryName(category)));
               }

               if (category.deletable()) {
                  int deleteX = rowX + rowW - 10 - 2;
                  int editX = deleteX - 10 - 2;
                  int actionY = rowY + 5;
                  boolean editHovered = OverlayRenderHelper.isMouseOver(
                     smx, smy, this.sx(editX), this.sy(actionY), 10 * this.pixelScale, 10 * this.pixelScale, actionHitPad
                  );
                  renderer.drawHudButton(
                     guiGraphics, this.sx(editX), this.sy(actionY), 10 * this.pixelScale, 10 * this.pixelScale, "✎", editHovered, false, alpha, this.pixelScale
                  );
                  if (editHovered) {
                     hoveredTooltipId = "category-edit-" + category.id();
                     hoveredTooltipLines = List.of(AtlasText.translatable("ui.tooltip.button.edit_category"));
                  }

                  int deleteY = rowY + 5;
                  boolean deleteHovered = OverlayRenderHelper.isMouseOver(
                     smx, smy, this.sx(deleteX), this.sy(deleteY), 10 * this.pixelScale, 10 * this.pixelScale, actionHitPad
                  );
                  renderer.drawHudButton(
                     guiGraphics,
                     this.sx(deleteX),
                     this.sy(deleteY),
                     10 * this.pixelScale,
                     10 * this.pixelScale,
                     "trash",
                     deleteHovered,
                     false,
                     alpha,
                     this.pixelScale
                  );
                  if (deleteHovered) {
                     hoveredTooltipId = "category-delete-" + category.id();
                     hoveredTooltipLines = List.of(AtlasText.translatable("ui.tooltip.button.delete_category"));
                  }
               }
            }

            guiGraphics.disableScissor();
            if (showScrollbar) {
               int visibleH = Math.min(7, categories.size()) * 20 + Math.max(0, Math.min(7, categories.size()) - 1) * 2;
               int virtualH = categories.size() * 20 + Math.max(0, categories.size() - 1) * 2;
               int scaledTrackX = this.sx(panelX + panelW - 5 - 1);
               int scaledTrackY = this.sy(listY);
               int scaledTrackW = 5 * this.pixelScale;
               int scaledTrackH = visibleH * this.pixelScale;
               int thumbH = Math.max(this.pixelScale * 3, scaledTrackH * visibleH / Math.max(1, virtualH));
               int maxScroll = Math.max(1, categories.size() - 7);
               int thumbY = scaledTrackY + (int)((float)this.scrollOffset / (float)maxScroll * (float)(scaledTrackH - thumbH));
               guiGraphics.fill(
                  scaledTrackX,
                  scaledTrackY,
                  scaledTrackX + scaledTrackW,
                  scaledTrackY + scaledTrackH,
                  OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), 0.5F * alpha)
               );
               guiGraphics.fill(
                  scaledTrackX,
                  thumbY,
                  scaledTrackX + scaledTrackW,
                  thumbY + thumbH,
                  OverlayRenderHelper.withAlpha(KeyVisualStyle.boxSourceText(), 0.6F * alpha)
               );
            }

            guiGraphics.pose().popMatrix();
            this.tooltipState.update(hoveredTooltipId, mouseX, mouseY);
            this.tooltipState
               .renderIfReady(guiGraphics, font, this.screenWidth, this.screenHeight, this.pixelScale, mouseX, mouseY, hoveredTooltipId, hoveredTooltipLines);
         } else {
            guiGraphics.pose().popMatrix();
            this.tooltipState.update(null, mouseX, mouseY);
         }
      }
   }

   private int[] panelGeometry(List<KeyCategory> categories) {
      int visibleRows = Math.min(7, Math.max(1, categories.size()));
      int listHeight = visibleRows * 20 + Math.max(0, visibleRows - 1) * 2;
      int fullPanelH = 26 + listHeight;
      int panelH = Math.max(1, (int)((float)fullPanelH * easeInOut(this.panelProgress)));
      int panelX = (this.screenWidth - 178) / 2;
      int panelY = this.toggleBtnY() - panelH - 2;
      return new int[]{panelX, panelY, 178, panelH, fullPanelH};
   }

   private boolean hasScrollbar(List<KeyCategory> categories) {
      return categories.size() > 7;
   }

   private int firstVisibleIndex(List<KeyCategory> categories) {
      int maxStart = Math.max(0, categories.size() - 7);
      if (this.scrollOffset > maxStart) {
         this.scrollOffset = maxStart;
      }

      return Math.max(0, this.scrollOffset);
   }

   private KeyCategory selectedCategory(String selectedCategoryId, List<KeyCategory> categories) {
      for (KeyCategory category : categories) {
         if (category.id().equals(selectedCategoryId)) {
            return category;
         }
      }

      return categories.isEmpty() ? KeyCategory.assignedCategory() : categories.get(0);
   }

   private String colorName(KeyCategory category, List<KeyCategory.PresetColor> presetColors) {
      if (category.isAssignedCategory()) {
         return "Atlas Blue";
      } else {
         return category.isHiddenCategory() ? "Shadow" : KeyCategory.presetColorName(presetColors, category.fillColor());
      }
   }

   private String displayCategoryName(KeyCategory category) {
      if (category != null && category.isAssignedCategory()) {
         return category.name() + " (no category)";
      } else {
         return category == null ? "" : category.name();
      }
   }

   private String trimToWidth(Font font, String text, float scale, int maxWidth) {
      if (maxWidth <= 0) {
         return "";
      } else if ((float)font.width(text) * scale <= (float)maxWidth) {
         return text;
      } else {
         String suffix = "...";
         int maxLength = text.length();

         while (maxLength > 0 && (float)font.width(text.substring(0, maxLength) + suffix) * scale > (float)maxWidth) {
            maxLength--;
         }

         return maxLength <= 0 ? suffix : text.substring(0, maxLength) + suffix;
      }
   }

   private int toggleBtnX() {
      return (this.screenWidth - 132) / 2;
   }

   private int toggleBtnY() {
      return this.screenHeight - 14 - 6;
   }

   private static float easeInOut(float t) {
      return t < 0.5F ? 2.0F * t * t : 1.0F - (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) / 2.0F;
   }

   private int sx(int x) {
      return OverlayRenderHelper.scaleX(x, this.screenWidth / 2, this.pixelScale);
   }

   private int sy(int y) {
      return OverlayRenderHelper.scaleY(y, this.screenHeight / 2, this.pixelScale);
   }

   public static final class PanelAction {
      public static final KeyCategoryPanel.PanelAction NONE = new KeyCategoryPanel.PanelAction(KeyCategoryPanel.PanelAction.Type.NONE, "");
      public final KeyCategoryPanel.PanelAction.Type type;
      public final String categoryId;

      private PanelAction(KeyCategoryPanel.PanelAction.Type type, String categoryId) {
         this.type = type;
         this.categoryId = categoryId;
      }

      public static KeyCategoryPanel.PanelAction toggle() {
         return new KeyCategoryPanel.PanelAction(KeyCategoryPanel.PanelAction.Type.TOGGLE, "");
      }

      public static KeyCategoryPanel.PanelAction select(String categoryId) {
         return new KeyCategoryPanel.PanelAction(KeyCategoryPanel.PanelAction.Type.SELECT_CATEGORY, categoryId);
      }

      public static KeyCategoryPanel.PanelAction create() {
         return new KeyCategoryPanel.PanelAction(KeyCategoryPanel.PanelAction.Type.CREATE_CATEGORY, "");
      }

      public static KeyCategoryPanel.PanelAction edit(String categoryId) {
         return new KeyCategoryPanel.PanelAction(KeyCategoryPanel.PanelAction.Type.EDIT_CATEGORY, categoryId);
      }

      public static KeyCategoryPanel.PanelAction delete(String categoryId) {
         return new KeyCategoryPanel.PanelAction(KeyCategoryPanel.PanelAction.Type.DELETE_CATEGORY, categoryId);
      }

      public static enum Type {
         NONE,
         TOGGLE,
         SELECT_CATEGORY,
         CREATE_CATEGORY,
         EDIT_CATEGORY,
         DELETE_CATEGORY;
      }
   }
}
