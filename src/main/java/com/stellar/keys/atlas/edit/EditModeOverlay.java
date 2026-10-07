package com.stellar.keys.atlas.edit;

import com.stellar.keys.atlas.AtlasText;
import com.stellar.keys.atlas.render.HoverTooltipState;
import com.stellar.keys.atlas.render.KeyboardRenderer;
import com.stellar.keys.atlas.render.OverlayRenderHelper;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class EditModeOverlay {
   private static final int EDIT_BTN_W = 18;
   private static final int EDIT_BTN_H = 18;
   private static final int TOOLBAR_BTN_W = 44;
   private static final int TOOLBAR_BTN_H = 14;
   private static final int HUD_MARGIN = 6;
   private static final int HUD_GAP = 4;
   private static final int COG_W = 18;
   private static final int BTN_Y = 6;
   private static final String HIDDEN_KEYS_ICON = "eye";
   private static final String CATEGORIES_ICON = "C";
   private final int screenWidth;
   private final int screenHeight;
   private final int pixelScale;
   private float toolbarProgress = 0.0F;
   private final HoverTooltipState tooltipState = new HoverTooltipState();

   public EditModeOverlay(int screenWidth, int screenHeight, int pixelScale) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.pixelScale = pixelScale;
   }

   public void updateAnimations(float step, boolean editActive) {
      float target = editActive ? 1.0F : 0.0F;
      if (this.toolbarProgress < target) {
         this.toolbarProgress = Math.min(target, this.toolbarProgress + step);
      } else if (this.toolbarProgress > target) {
         this.toolbarProgress = Math.max(target, this.toolbarProgress - step);
      }
   }

   public float toolbarProgress() {
      return this.toolbarProgress;
   }

   private static float easeInOut(float t) {
      return t < 0.5F ? 2.0F * t * t : 1.0F - (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) / 2.0F;
   }

   private int editBtnX() {
      return this.screenWidth - 18 - 6 - 4 - 18;
   }

   private int hiddenKeysBtnX() {
      return this.editBtnX() - 4 - 18;
   }

   private int categoriesBtnX() {
      return this.hiddenKeysBtnX() - 4 - 18;
   }

   private int sx(int x) {
      return OverlayRenderHelper.scaleX(x, this.screenWidth / 2, this.pixelScale);
   }

   private int sy(int y) {
      return OverlayRenderHelper.scaleY(y, this.screenHeight / 2, this.pixelScale);
   }

   public void render(
      GuiGraphicsExtractor guiGraphics,
      KeyboardRenderer renderer,
      Font font,
      int mouseX,
      int mouseY,
      boolean editActive,
      boolean canUndo,
      boolean hiddenKeysModeActive,
      boolean categoriesEnabled
   ) {
      int centerX = this.screenWidth / 2;
      int centerY = this.screenHeight / 2;
      OverlayRenderHelper.pushScaleTransform(guiGraphics, centerX, centerY, this.pixelScale);
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      String hoveredTooltipId = null;
      Component hoveredTooltip = null;
      if (!editActive) {
         if (hiddenKeysModeActive) {
            int categoriesX = this.sx(this.categoriesBtnX());
            int categoriesY = this.sy(6);
            int categoriesW = 18 * this.pixelScale;
            int categoriesH = 18 * this.pixelScale;
            boolean categoriesHovered = OverlayRenderHelper.isMouseOver(smx, smy, categoriesX, categoriesY, categoriesW, categoriesH, this.pixelScale);
            renderer.drawHudButton(guiGraphics, categoriesX, categoriesY, categoriesW, categoriesH, "C", categoriesHovered, categoriesEnabled);
            if (categoriesHovered) {
               hoveredTooltipId = "edit.categories";
               hoveredTooltip = AtlasText.translatable("ui.tooltip.setting.categories");
            }
         }

         int hiddenX = this.sx(this.hiddenKeysBtnX());
         int hiddenY = this.sy(6);
         int hiddenW = 18 * this.pixelScale;
         int hiddenH = 18 * this.pixelScale;
         boolean hiddenHovered = OverlayRenderHelper.isMouseOver(smx, smy, hiddenX, hiddenY, hiddenW, hiddenH, this.pixelScale);
         renderer.drawHudButton(guiGraphics, hiddenX, hiddenY, hiddenW, hiddenH, "eye", hiddenHovered, hiddenKeysModeActive);
         if (hiddenHovered) {
            hoveredTooltipId = "edit.hidden_keys";
            hoveredTooltip = AtlasText.translatable("ui.tooltip.button.hidden_keys");
         }
      }

      int editX = this.sx(this.editBtnX());
      int editY = this.sy(6);
      int editW = 18 * this.pixelScale;
      int editH = 18 * this.pixelScale;
      boolean editHovered = OverlayRenderHelper.isMouseOver(smx, smy, editX, editY, editW, editH, this.pixelScale);
      renderer.drawHudButton(guiGraphics, editX, editY, editW, editH, "✎", editHovered, editActive);
      if (editHovered) {
         hoveredTooltipId = "edit.mode";
         hoveredTooltip = AtlasText.translatable("ui.tooltip.button.edit_mode");
      }

      float easedProgress = easeInOut(this.toolbarProgress);
      if (easedProgress > 0.01F) {
         int baseX = this.editBtnX() - 4;
         int btnY = this.sy(8);
         int btnW = 44 * this.pixelScale;
         int btnH = 14 * this.pixelScale;
         int applyX = this.sx(baseX - 44);
         boolean applyHovered = OverlayRenderHelper.isMouseOver(smx, smy, applyX, btnY, btnW, btnH, this.pixelScale) && editActive;
         renderer.drawHudButton(guiGraphics, applyX, btnY, btnW, btnH, AtlasText.text("ui.button.apply"), applyHovered, true, easedProgress);
         if (applyHovered) {
            hoveredTooltipId = "edit.apply";
            hoveredTooltip = AtlasText.translatable("ui.tooltip.button.apply_changes");
         }

         int discardX = this.sx(baseX - 88 - 4);
         boolean discardHovered = OverlayRenderHelper.isMouseOver(smx, smy, discardX, btnY, btnW, btnH, this.pixelScale) && editActive;
         renderer.drawHudButton(guiGraphics, discardX, btnY, btnW, btnH, AtlasText.text("ui.button.discard"), discardHovered, false, easedProgress);
         if (discardHovered) {
            hoveredTooltipId = "edit.discard";
            hoveredTooltip = AtlasText.translatable("ui.tooltip.button.discard_changes");
         }

         int undoX = this.sx(baseX - 132 - 8);
         boolean undoHovered = OverlayRenderHelper.isMouseOver(smx, smy, undoX, btnY, btnW, btnH, this.pixelScale) && editActive;
         renderer.drawHudButton(guiGraphics, undoX, btnY, btnW, btnH, AtlasText.text("ui.button.undo"), undoHovered && canUndo, false, easedProgress);
         if (undoHovered) {
            hoveredTooltipId = "edit.undo";
            hoveredTooltip = AtlasText.translatable("ui.tooltip.button.undo");
         }

         if (!canUndo && editActive) {
            guiGraphics.fill(undoX, btnY, undoX + btnW, btnY + btnH, OverlayRenderHelper.withAlpha(-16777216, 0.5F * easedProgress));
         }

         int resetX = this.sx(baseX - 176 - 12);
         boolean resetHovered = OverlayRenderHelper.isMouseOver(smx, smy, resetX, btnY, btnW, btnH, this.pixelScale) && editActive;
         renderer.drawHudButton(guiGraphics, resetX, btnY, btnW, btnH, AtlasText.text("ui.button.reset"), resetHovered, false, easedProgress);
         if (resetHovered) {
            hoveredTooltipId = "edit.reset";
            hoveredTooltip = AtlasText.translatable("ui.tooltip.button.reset_bindings");
         }
      }

      this.tooltipState.update(hoveredTooltipId, mouseX, mouseY);
      if (hoveredTooltipId != null && hoveredTooltip != null) {
         this.tooltipState
            .renderIfReady(guiGraphics, font, this.screenWidth, this.screenHeight, this.pixelScale, mouseX, mouseY, hoveredTooltipId, List.of(hoveredTooltip));
      }

      OverlayRenderHelper.popScaleTransform(guiGraphics);
   }

   public EditModeOverlay.Action hitTest(int mouseX, int mouseY, boolean editActive, boolean canUndo, boolean hiddenKeysModeActive) {
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      if (!editActive) {
         if (hiddenKeysModeActive) {
            int categoriesX = this.sx(this.categoriesBtnX());
            int categoriesY = this.sy(6);
            if (OverlayRenderHelper.isMouseOver(smx, smy, categoriesX, categoriesY, 18 * this.pixelScale, 18 * this.pixelScale, this.pixelScale)) {
               return EditModeOverlay.Action.TOGGLE_CATEGORIES;
            }
         }

         int hiddenX = this.sx(this.hiddenKeysBtnX());
         int hiddenY = this.sy(6);
         if (OverlayRenderHelper.isMouseOver(smx, smy, hiddenX, hiddenY, 18 * this.pixelScale, 18 * this.pixelScale, this.pixelScale)) {
            return EditModeOverlay.Action.TOGGLE_HIDDEN_KEYS;
         }
      }

      int editX = this.sx(this.editBtnX());
      int editY = this.sy(6);
      if (OverlayRenderHelper.isMouseOver(smx, smy, editX, editY, 18 * this.pixelScale, 18 * this.pixelScale, this.pixelScale)) {
         return EditModeOverlay.Action.TOGGLE_EDIT;
      } else {
         if (editActive && this.toolbarProgress >= 1.0F) {
            int baseX = this.editBtnX() - 4;
            int btnY = this.sy(8);
            int btnW = 44 * this.pixelScale;
            int btnH = 14 * this.pixelScale;
            int applyX = this.sx(baseX - 44);
            if (OverlayRenderHelper.isMouseOver(smx, smy, applyX, btnY, btnW, btnH, this.pixelScale)) {
               return EditModeOverlay.Action.APPLY;
            }

            int discardX = this.sx(baseX - 88 - 4);
            if (OverlayRenderHelper.isMouseOver(smx, smy, discardX, btnY, btnW, btnH, this.pixelScale)) {
               return EditModeOverlay.Action.DISCARD;
            }

            int undoX = this.sx(baseX - 132 - 8);
            if (canUndo && OverlayRenderHelper.isMouseOver(smx, smy, undoX, btnY, btnW, btnH, this.pixelScale)) {
               return EditModeOverlay.Action.UNDO;
            }

            int resetX = this.sx(baseX - 176 - 12);
            if (OverlayRenderHelper.isMouseOver(smx, smy, resetX, btnY, btnW, btnH, this.pixelScale)) {
               return EditModeOverlay.Action.RESET;
            }
         }

         return EditModeOverlay.Action.NONE;
      }
   }

   public boolean isPointOverToolbar(int mouseX, int mouseY, boolean editActive, boolean hiddenKeysModeActive) {
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      if (!editActive) {
         if (hiddenKeysModeActive) {
            int categoriesX = this.sx(this.categoriesBtnX());
            int categoriesY = this.sy(6);
            if (OverlayRenderHelper.isMouseOver(smx, smy, categoriesX, categoriesY, 18 * this.pixelScale, 18 * this.pixelScale, 0)) {
               return true;
            }
         }

         int hiddenX = this.sx(this.hiddenKeysBtnX());
         int hiddenY = this.sy(6);
         if (OverlayRenderHelper.isMouseOver(smx, smy, hiddenX, hiddenY, 18 * this.pixelScale, 18 * this.pixelScale, 0)) {
            return true;
         }
      }

      int editX = this.sx(this.editBtnX());
      int editY = this.sy(6);
      if (OverlayRenderHelper.isMouseOver(smx, smy, editX, editY, 18 * this.pixelScale, 18 * this.pixelScale, 0)) {
         return true;
      } else {
         if (editActive && this.toolbarProgress >= 1.0F) {
            int baseX = this.editBtnX() - 4;
            int btnY = this.sy(8);
            int btnH = 14 * this.pixelScale;
            int resetX = this.sx(baseX - 176 - 12);
            int applyEndX = this.sx(baseX);
            int totalW = applyEndX - resetX;
            if (OverlayRenderHelper.isMouseOver(smx, smy, resetX, btnY, totalW, btnH, 0)) {
               return true;
            }
         }

         return false;
      }
   }

   public static enum Action {
      NONE,
      TOGGLE_EDIT,
      TOGGLE_HIDDEN_KEYS,
      TOGGLE_CATEGORIES,
      UNDO,
      DISCARD,
      APPLY,
      RESET;
   }
}
