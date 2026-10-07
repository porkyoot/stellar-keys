package com.stellar.keys.atlas.edit;

import com.stellar.keys.atlas.AtlasText;
import com.stellar.keys.atlas.KeyVisualStyle;
import com.stellar.keys.atlas.render.KeyboardRenderer;
import com.stellar.keys.atlas.render.OverlayRenderHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class UnassignedBindingsPanel {
   private static final int TOGGLE_BTN_W = 120;
   private static final int TOGGLE_BTN_H = 14;
   private static final int PANEL_W = 160;
   private static final int ROW_H = 11;
   private static final int PANEL_PAD = 4;
   private static final int SCROLLBAR_W = 5;
   private static final int VISIBLE_ROWS = 10;
   private static final float MOD_LABEL_SCALE = 0.8F;
   private static final float BINDING_LABEL_SCALE = 0.7F;
   private static final int BOTTOM_MARGIN = 6;
   private static final int CHECKBOX_SIZE = 7;
   private static final int TOGGLE_SIDE_ICON_SIZE = 8;
   private static final int TOGGLE_SIDE_ICON_INSET = 4;
   private final int screenWidth;
   private final int screenHeight;
   private final int pixelScale;
   private boolean expanded = false;
   private int scrollOffset = 0;
   private Map<String, Boolean> modExpanded = new LinkedHashMap<>();
   private float panelProgress = 0.0F;
   private Map<String, Float> modExpandProgress = new LinkedHashMap<>();
   private boolean draggingScrollbar = false;
   private int scrollbarDragOffset = 0;

   public UnassignedBindingsPanel(int screenWidth, int screenHeight, int pixelScale) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.pixelScale = pixelScale;
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   public void setExpanded(boolean expanded) {
      this.expanded = expanded;
   }

   public void toggle() {
      this.expanded = !this.expanded;
      this.scrollOffset = 0;
   }

   public float panelProgress() {
      return this.panelProgress;
   }

   public void updateAnimations(float step, Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod) {
      float target = this.expanded ? 1.0F : 0.0F;
      this.panelProgress = moveTowards(this.panelProgress, target, step);
      if (unassignedByMod != null) {
         for (String mod : unassignedByMod.keySet()) {
            float modTarget = Boolean.TRUE.equals(this.modExpanded.get(mod)) ? 1.0F : 0.0F;
            float current = this.modExpandProgress.getOrDefault(mod, 0.0F);
            this.modExpandProgress.put(mod, moveTowards(current, modTarget, step));
         }
      }
   }

   private static float moveTowards(float current, float target, float step) {
      if (current < target) {
         return Math.min(target, current + step);
      } else {
         return current > target ? Math.max(target, current - step) : current;
      }
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

   private int toggleBtnX() {
      return (this.screenWidth - 120) / 2;
   }

   private int toggleBtnY() {
      return this.screenHeight - 14 - 6;
   }

   public void render(
      GuiGraphicsExtractor guiGraphics,
      KeyboardRenderer renderer,
      Font font,
      int mouseX,
      int mouseY,
      Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod,
      int totalUnassigned,
      float alpha
   ) {
      int centerX = this.screenWidth / 2;
      int centerY = this.screenHeight / 2;
      OverlayRenderHelper.pushScaleTransform(guiGraphics, centerX, centerY, this.pixelScale);
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      int tbx = this.sx(this.toggleBtnX());
      int tby = this.sy(this.toggleBtnY());
      int tbw = 120 * this.pixelScale;
      int tbh = 14 * this.pixelScale;
      String label = AtlasText.text("ui.unassigned.toggle", totalUnassigned);
      boolean allowHover = alpha >= 1.0F;
      boolean toggleHovered = allowHover && OverlayRenderHelper.isMouseOver(smx, smy, tbx, tby, tbw, tbh, this.pixelScale);
      renderer.drawHudButton(guiGraphics, tbx, tby, tbw, tbh, label, toggleHovered, this.expanded, alpha);
      if (!this.expanded) {
         int iconSize = 8 * this.pixelScale;
         int iconY = tby + (tbh - iconSize) / 2;
         int leftIconX = tbx + 4 * this.pixelScale;
         int rightIconX = tbx + tbw - iconSize - 4 * this.pixelScale;
         int iconColor = toggleHovered ? KeyVisualStyle.keyHoverText() : KeyVisualStyle.keyUnusedText();
         renderer.drawIcon(guiGraphics, leftIconX, iconY, iconSize, iconSize, "up-arrow", iconColor, alpha);
         renderer.drawIcon(guiGraphics, rightIconX, iconY, iconSize, iconSize, "up-arrow", iconColor, alpha);
      }

      if (this.panelProgress > 0.0F && unassignedByMod != null && !unassignedByMod.isEmpty()) {
         this.renderPanel(guiGraphics, renderer, font, smx, smy, unassignedByMod, alpha);
      }

      guiGraphics.pose().popMatrix();
   }

   private int computeVirtualHeight(Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod) {
      int h = 0;

      for (Entry<String, List<EditModeState.UnassignedEntry>> modEntry : unassignedByMod.entrySet()) {
         h += 11;
         float ep = this.modExpandProgress.getOrDefault(modEntry.getKey(), 0.0F);
         if (ep > 0.0F) {
            h += (int)((float)(modEntry.getValue().size() * 11) * ep);
         }
      }

      return h;
   }

   private int[] panelGeometry(Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod) {
      int virtualH = this.computeVirtualHeight(unassignedByMod);
      int visibleH = Math.min(110, virtualH);
      int fullPanelContentH = visibleH + 8;
      int panelContentH = Math.max(1, (int)((float)fullPanelContentH * easeInOut(this.panelProgress)));
      int panelW = 160;
      int panelX = (this.screenWidth - panelW) / 2;
      int panelY = this.toggleBtnY() - panelContentH - 2;
      return new int[]{panelX, panelY, panelW, panelContentH, virtualH, visibleH};
   }

   private void renderPanel(
      GuiGraphicsExtractor guiGraphics,
      KeyboardRenderer renderer,
      Font font,
      int smx,
      int smy,
      Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod,
      float alpha
   ) {
      int[] geo = this.panelGeometry(unassignedByMod);
      int panelX = geo[0];
      int panelY = geo[1];
      int panelW = geo[2];
      int panelContentH = geo[3];
      int virtualH = geo[4];
      int visibleH = geo[5];
      int scaledPanelX = this.sx(panelX);
      int scaledPanelY = this.sy(panelY);
      int scaledPanelW = panelW * this.pixelScale;
      int scaledPanelH = panelContentH * this.pixelScale;
      int cr = this.pixelScale;
      OverlayRenderHelper.blitBlurredBackground(
         guiGraphics, scaledPanelX, scaledPanelY, scaledPanelW, scaledPanelH, panelX, panelY, panelW, panelContentH, alpha
      );
      float panelAlpha = KeyVisualStyle.panelAlpha() * alpha;
      OverlayRenderHelper.fillRoundedRect(
         guiGraphics,
         scaledPanelX,
         scaledPanelY,
         scaledPanelW,
         scaledPanelH,
         cr,
         OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), panelAlpha)
      );
      OverlayRenderHelper.drawRoundedRectOutline(
         guiGraphics,
         scaledPanelX,
         scaledPanelY,
         scaledPanelW,
         scaledPanelH,
         cr,
         OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), alpha),
         this.pixelScale
      );
      guiGraphics.enableScissor(panelX, panelY, panelX + panelW, panelY + panelContentH);
      int maxScrollPx = Math.max(0, virtualH - visibleH);
      int scrollPx = Math.min(this.scrollOffset * 11, maxScrollPx);
      boolean hasScrollbar = virtualH > visibleH;
      int drawY = panelY + 4 - scrollPx;

      for (Entry<String, List<EditModeState.UnassignedEntry>> modEntry : unassignedByMod.entrySet()) {
         String modName = modEntry.getKey();
         List<EditModeState.UnassignedEntry> entries = modEntry.getValue();
         if (drawY + 11 > panelY && drawY < panelY + panelContentH) {
            int ry = this.sy(drawY);
            String arrow = Boolean.TRUE.equals(this.modExpanded.get(modName)) ? "▾" : "▸";
            int arrowColor = OverlayRenderHelper.withAlpha(-7829368, alpha);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)((float)this.sx(panelX + 4)), (float)((float)(ry + this.pixelScale)));
            guiGraphics.pose().scale((float)((float)this.pixelScale * 0.8F), (float)((float)this.pixelScale * 0.8F));
            guiGraphics.text(font, arrow, 0, 0, arrowColor, false);
            int arrowAdvance = font.width(arrow + " ");
            guiGraphics.text(font, modName, arrowAdvance, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxText(), alpha), false);
            guiGraphics.pose().popMatrix();
         }

         drawY += 11;
         float expandProg = this.modExpandProgress.getOrDefault(modName, 0.0F);
         if (expandProg > 0.0F) {
            int totalBindingH = entries.size() * 11;
            int visibleBindingH = (int)((float)totalBindingH * expandProg);
            int bTop = Math.max(panelY, drawY);
            int bBottom = Math.min(panelY + panelContentH, drawY + visibleBindingH);
            if (bBottom > bTop) {
               guiGraphics.enableScissor(panelX, bTop, panelX + panelW, bBottom);
               int bindY = drawY;

               for (EditModeState.UnassignedEntry ue : entries) {
                  if (bindY + 11 > panelY && bindY < panelY + panelContentH) {
                     String bindingText = ue.label;
                     if (bindingText.length() > 44) {
                        bindingText = bindingText.substring(0, 41) + "...";
                     }

                     int bry = this.sy(bindY);
                     guiGraphics.pose().pushMatrix();
                     guiGraphics.pose().translate((float)((float)this.sx(panelX + 4 + 8)), (float)((float)(bry + this.pixelScale)));
                     guiGraphics.pose().scale((float)((float)this.pixelScale * 0.7F), (float)((float)this.pixelScale * 0.7F));
                     guiGraphics.text(font, bindingText, 0, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxSourceText(), alpha), false);
                     guiGraphics.pose().popMatrix();
                     int plusX = this.sx(panelX + panelW - 4 - 7 - (hasScrollbar ? 7 : 0));
                     int plusW = 7 * this.pixelScale;
                     int plusH = 7 * this.pixelScale;
                     boolean plusHov = alpha >= 1.0F && OverlayRenderHelper.isMouseOver(smx, smy, plusX, bry, plusW, plusH, this.pixelScale);
                     this.drawPlusBox(guiGraphics, plusX, bry + this.pixelScale, plusW, plusH, plusHov, alpha);
                  }

                  bindY += 11;
               }

               guiGraphics.disableScissor();
            }

            drawY += visibleBindingH;
         }
      }

      guiGraphics.disableScissor();
      if (hasScrollbar) {
         int sbX = this.sx(panelX + panelW - 5 - 1);
         int sbW = 5 * this.pixelScale;
         int trackH = scaledPanelH - 8 * this.pixelScale;
         int trackY = scaledPanelY + 4 * this.pixelScale;
         int thumbH = Math.max(this.pixelScale * 3, trackH * visibleH / virtualH);
         int maxScroll = Math.max(1, maxScrollPx);
         int thumbY = trackY + (int)((float)scrollPx / (float)maxScroll * (float)(trackH - thumbH));
         guiGraphics.fill(sbX, trackY, sbX + sbW, trackY + trackH, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), 0.5F * alpha));
         guiGraphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxSourceText(), 0.6F * alpha));
      }
   }

   public UnassignedBindingsPanel.PanelAction hitTest(int mouseX, int mouseY, Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod) {
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      int tbx = this.sx(this.toggleBtnX());
      int tby = this.sy(this.toggleBtnY());
      int tbw = 120 * this.pixelScale;
      int tbh = 14 * this.pixelScale;
      if (OverlayRenderHelper.isMouseOver(smx, smy, tbx, tby, tbw, tbh, this.pixelScale)) {
         return UnassignedBindingsPanel.PanelAction.toggle();
      } else if (this.expanded && unassignedByMod != null && !unassignedByMod.isEmpty()) {
         int[] geo = this.panelGeometry(unassignedByMod);
         int panelX = geo[0];
         int panelY = geo[1];
         int panelW = geo[2];
         int panelContentH = geo[3];
         int virtualH = geo[4];
         int visibleH = geo[5];
         int maxScrollPx = Math.max(0, virtualH - visibleH);
         int scrollPx = Math.min(this.scrollOffset * 11, maxScrollPx);
         boolean hasScrollbar = virtualH > visibleH;
         int drawY = panelY + 4 - scrollPx;
         int modIndex = 0;

         for (Entry<String, List<EditModeState.UnassignedEntry>> modEntry : unassignedByMod.entrySet()) {
            String modName = modEntry.getKey();
            List<EditModeState.UnassignedEntry> entries = modEntry.getValue();
            if (drawY + 11 > panelY && drawY < panelY + panelContentH) {
               int ry = this.sy(drawY);
               int rowX = this.sx(panelX + 4);
               int rowW = (panelW - 8) * this.pixelScale;
               int rowH = 11 * this.pixelScale;
               if (OverlayRenderHelper.isMouseOver(smx, smy, rowX, ry, rowW, rowH, 0)) {
                  return UnassignedBindingsPanel.PanelAction.expandMod(modIndex);
               }
            }

            drawY += 11;
            float expandProg = this.modExpandProgress.getOrDefault(modName, 0.0F);
            if (expandProg > 0.0F) {
               int totalBindingH = entries.size() * 11;
               int visibleBindingH = (int)((float)totalBindingH * expandProg);
               int bindY = drawY;

               for (EditModeState.UnassignedEntry ue : entries) {
                  if (bindY < drawY + visibleBindingH && bindY + 11 > panelY && bindY < panelY + panelContentH) {
                     int bry = this.sy(bindY);
                     int plusX = this.sx(panelX + panelW - 4 - 7 - (hasScrollbar ? 7 : 0));
                     int plusW = 7 * this.pixelScale;
                     int plusH = 7 * this.pixelScale;
                     if (OverlayRenderHelper.isMouseOver(smx, smy, plusX, bry, plusW, plusH, this.pixelScale)) {
                        return UnassignedBindingsPanel.PanelAction.assign(ue.translationKey);
                     }
                  }

                  bindY += 11;
               }

               drawY += visibleBindingH;
            }

            modIndex++;
         }

         return UnassignedBindingsPanel.PanelAction.none();
      } else {
         return UnassignedBindingsPanel.PanelAction.none();
      }
   }

   public void scroll(int direction) {
      this.scrollOffset = Math.max(0, this.scrollOffset - direction);
   }

   public boolean beginScrollbarDrag(int mouseX, int mouseY, Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod) {
      int[] geo = this.scrollbarGeometry(unassignedByMod);
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

   public boolean dragScrollbarTo(int mouseY, Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod) {
      if (!this.draggingScrollbar) {
         return false;
      } else {
         int[] geo = this.scrollbarGeometry(unassignedByMod);
         if (geo == null) {
            this.endScrollbarDrag();
            return false;
         } else {
            int smy = this.sy(mouseY);
            int thumbMinY = geo[1];
            int thumbMaxY = geo[1] + geo[3] - geo[5];
            int thumbY = Math.max(thumbMinY, Math.min(smy - this.scrollbarDragOffset, thumbMaxY));
            int thumbRange = Math.max(1, geo[3] - geo[5]);
            float ratio = (float)(thumbY - thumbMinY) / (float)thumbRange;
            int scrollPx = Math.round(ratio * (float)geo[6]);
            int maxRows = Math.max(0, (int)Math.ceil((double)geo[6] / 11.0));
            this.scrollOffset = Math.max(0, Math.min(maxRows, Math.round((float)scrollPx / 11.0F)));
            return true;
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

   public boolean isPointOverPanel(int mouseX, int mouseY, Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod) {
      int smx = this.sx(mouseX);
      int smy = this.sy(mouseY);
      int tbx = this.sx(this.toggleBtnX());
      int tby = this.sy(this.toggleBtnY());
      int tbw = 120 * this.pixelScale;
      int tbh = 14 * this.pixelScale;
      if (OverlayRenderHelper.isMouseOver(smx, smy, tbx, tby, tbw, tbh, 0)) {
         return true;
      } else if (!(this.panelProgress <= 0.0F) && unassignedByMod != null && !unassignedByMod.isEmpty()) {
         int[] geo = this.panelGeometry(unassignedByMod);
         int panelX = geo[0];
         int panelY = geo[1];
         int panelW = geo[2];
         int panelContentH = geo[3];
         int scaledPanelX = this.sx(panelX);
         int scaledPanelY = this.sy(panelY);
         int scaledPanelW = panelW * this.pixelScale;
         int scaledPanelH = panelContentH * this.pixelScale;
         return OverlayRenderHelper.isMouseOver(smx, smy, scaledPanelX, scaledPanelY, scaledPanelW, scaledPanelH, 0);
      } else {
         return false;
      }
   }

   public void setModExpanded(int modIndex, List<String> modNames) {
      if (modIndex >= 0 && modIndex < modNames.size()) {
         String mod = modNames.get(modIndex);
         Boolean prev = this.modExpanded.get(mod);
         this.modExpanded.put(mod, prev == null || !prev);
      }
   }

   private int[] scrollbarGeometry(Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod) {
      if (!(this.panelProgress <= 0.0F) && unassignedByMod != null && !unassignedByMod.isEmpty()) {
         int[] geo = this.panelGeometry(unassignedByMod);
         int panelX = geo[0];
         int panelY = geo[1];
         int panelW = geo[2];
         int panelContentH = geo[3];
         int virtualH = geo[4];
         int visibleH = geo[5];
         if (virtualH <= visibleH) {
            return null;
         } else {
            int scaledPanelY = this.sy(panelY);
            int scaledPanelH = panelContentH * this.pixelScale;
            int maxScrollPx = Math.max(0, virtualH - visibleH);
            int scrollPx = Math.min(this.scrollOffset * 11, maxScrollPx);
            int sbX = this.sx(panelX + panelW - 5 - 1);
            int sbW = 5 * this.pixelScale;
            int trackH = scaledPanelH - 8 * this.pixelScale;
            int trackY = scaledPanelY + 4 * this.pixelScale;
            int thumbH = Math.max(this.pixelScale * 3, trackH * visibleH / virtualH);
            int maxScroll = Math.max(1, maxScrollPx);
            int thumbY = trackY + (int)((float)scrollPx / (float)maxScroll * (float)(trackH - thumbH));
            return new int[]{sbX, trackY, sbW, trackH, thumbY, thumbH, maxScrollPx};
         }
      } else {
         return null;
      }
   }

   private void drawPlusBox(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h, boolean hovered, float alpha) {
      int bgColor = OverlayRenderHelper.withAlpha(KeyVisualStyle.boxFill(), 0.8F * alpha);
      int borderColor = OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), (hovered ? 1.0F : 0.5F) * alpha);
      guiGraphics.fill(x, y, x + w, y + h, bgColor);
      OverlayRenderHelper.drawRectOutline(guiGraphics, x, y, w, h, borderColor, this.pixelScale);
      int margin = this.pixelScale * 2;
      int crossColor = OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), alpha);
      int strokeThick = this.pixelScale;
      int halfStroke = strokeThick / 2;
      int cx = x + w / 2 - halfStroke;
      int cy = y + h / 2 - halfStroke;
      guiGraphics.fill(x + margin, cy, x + w - margin, cy + strokeThick, crossColor);
      guiGraphics.fill(cx, y + margin, cx + strokeThick, y + h - margin, crossColor);
   }

   public static final class PanelAction {
      public final UnassignedBindingsPanel.PanelAction.Type type;
      public final int modIndex;
      public final String translationKey;

      private PanelAction(UnassignedBindingsPanel.PanelAction.Type type, int modIndex, String translationKey) {
         this.type = type;
         this.modIndex = modIndex;
         this.translationKey = translationKey;
      }

      static UnassignedBindingsPanel.PanelAction none() {
         return new UnassignedBindingsPanel.PanelAction(UnassignedBindingsPanel.PanelAction.Type.NONE, -1, null);
      }

      static UnassignedBindingsPanel.PanelAction toggle() {
         return new UnassignedBindingsPanel.PanelAction(UnassignedBindingsPanel.PanelAction.Type.TOGGLE, -1, null);
      }

      static UnassignedBindingsPanel.PanelAction expandMod(int modIndex) {
         return new UnassignedBindingsPanel.PanelAction(UnassignedBindingsPanel.PanelAction.Type.EXPAND_MOD, modIndex, null);
      }

      static UnassignedBindingsPanel.PanelAction assign(String translationKey) {
         return new UnassignedBindingsPanel.PanelAction(UnassignedBindingsPanel.PanelAction.Type.ASSIGN, -1, translationKey);
      }

      public static enum Type {
         NONE,
         TOGGLE,
         EXPAND_MOD,
         ASSIGN;
      }
   }
}
