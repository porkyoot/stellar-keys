package com.stellar.keys.atlas.edit;

import com.stellar.keys.atlas.AtlasText;
import com.stellar.keys.atlas.KeyVisualStyle;
import com.stellar.keys.atlas.render.KeyboardRenderer;
import com.stellar.keys.atlas.render.OverlayRenderHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ConfirmationDialog {
   private static final int DIALOG_W = 170;
   private static final int DIALOG_H = 52;
   private static final int BTN_W = 46;
   private static final int BTN_H = 12;
   private static final int BTN_GAP = 6;
   private static final int PAD = 8;
   private final int screenWidth;
   private final int screenHeight;
   private final int pixelScale;
   private String title = "";
   private boolean visible = false;
   private long openTimeNanos = 0L;
   private boolean closing = false;
   private long closeTimeNanos = 0L;

   public ConfirmationDialog(int screenWidth, int screenHeight, int pixelScale) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.pixelScale = pixelScale;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void open(String title) {
      this.title = title;
      this.visible = true;
      this.openTimeNanos = System.nanoTime();
   }

   public void close() {
      if (this.visible && !this.closing) {
         this.closing = true;
         this.closeTimeNanos = System.nanoTime();
      }
   }

   private int sx(int x) {
      return OverlayRenderHelper.scaleX(x, this.screenWidth / 2, this.pixelScale);
   }

   private int sy(int y) {
      return OverlayRenderHelper.scaleY(y, this.screenHeight / 2, this.pixelScale);
   }

   private int dialogX() {
      return (this.screenWidth - 170) / 2;
   }

   private int dialogY() {
      return (this.screenHeight - 52) / 2;
   }

   public void render(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, Font font, int mouseX, int mouseY) {
      if (this.visible) {
         float elapsedMs = (float)(System.nanoTime() - this.openTimeNanos) / 1000000.0F;
         float fadeAlpha = Math.min(1.0F, elapsedMs / 200.0F);
         fadeAlpha = 1.0F - (1.0F - fadeAlpha) * (1.0F - fadeAlpha);
         if (this.closing) {
            float closeMs = (float)(System.nanoTime() - this.closeTimeNanos) / 1000000.0F;
            float closeAlpha = Math.max(0.0F, 1.0F - closeMs / 300.0F);
            closeAlpha *= closeAlpha;
            fadeAlpha *= closeAlpha;
            if (fadeAlpha < 0.02F) {
               this.visible = false;
               this.closing = false;
               return;
            }
         }

         int centerX = this.screenWidth / 2;
         int centerY = this.screenHeight / 2;
         OverlayRenderHelper.pushScaleTransform(guiGraphics, centerX, centerY, this.pixelScale);
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         int overlayLeft = this.sx(0);
         int overlayTop = this.sy(0);
         int overlayRight = this.sx(this.screenWidth);
         int overlayBottom = this.sy(this.screenHeight);
         int bgAlpha = Math.round(96.0F * fadeAlpha);
         guiGraphics.fill(overlayLeft, overlayTop, overlayRight, overlayBottom, bgAlpha << 24);
         int dw = 170 * this.pixelScale;
         int dh = 52 * this.pixelScale;
         int dx = this.sx(this.dialogX());
         int dy = this.sy(this.dialogY());
         int cr = this.pixelScale;
         int boxFill = OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F);
         OverlayRenderHelper.blitBlurredBackground(guiGraphics, dx, dy, dw, dh, this.dialogX(), this.dialogY(), 170, 52, fadeAlpha);
         OverlayRenderHelper.fillRoundedRect(guiGraphics, dx, dy, dw, dh, cr, OverlayRenderHelper.withAlpha(boxFill, fadeAlpha * KeyVisualStyle.panelAlpha()));
         OverlayRenderHelper.drawRoundedRectOutline(
            guiGraphics, dx, dy, dw, dh, cr, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), fadeAlpha), this.pixelScale
         );
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)dx + (float)dw / 2.0F, (float)(dy + 8 * this.pixelScale));
         guiGraphics.pose().scale((float)this.pixelScale * 0.85F, (float)this.pixelScale * 0.85F);
         guiGraphics.centeredText(font, this.title, 0, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxText(), fadeAlpha));
         guiGraphics.pose().popMatrix();
         int btnW = 46 * this.pixelScale;
         int btnH = 12 * this.pixelScale;
         int btnGap = 6 * this.pixelScale;
         int totalBtnW = btnW * 2 + btnGap;
         int btnStartX = dx + (dw - totalBtnW) / 2;
         int btnY = dy + dh - btnH - 8 * this.pixelScale;
         boolean allowHover = !this.closing;
         renderer.drawHudButton(
            guiGraphics,
            btnStartX,
            btnY,
            btnW,
            btnH,
            AtlasText.text("ui.button.no"),
            allowHover && OverlayRenderHelper.isMouseOver(smx, smy, btnStartX, btnY, btnW, btnH, this.pixelScale),
            false,
            fadeAlpha
         );
         renderer.drawHudButton(
            guiGraphics,
            btnStartX + btnW + btnGap,
            btnY,
            btnW,
            btnH,
            AtlasText.text("ui.button.yes"),
            allowHover && OverlayRenderHelper.isMouseOver(smx, smy, btnStartX + btnW + btnGap, btnY, btnW, btnH, this.pixelScale),
            true,
            fadeAlpha
         );
         OverlayRenderHelper.popScaleTransform(guiGraphics);
      }
   }

   public ConfirmationDialog.Result hitTest(int mouseX, int mouseY) {
      if (this.visible && !this.closing) {
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         int dw = 170 * this.pixelScale;
         int dh = 52 * this.pixelScale;
         int dx = this.sx(this.dialogX());
         int dy = this.sy(this.dialogY());
         int btnW = 46 * this.pixelScale;
         int btnH = 12 * this.pixelScale;
         int btnGap = 6 * this.pixelScale;
         int totalBtnW = btnW * 2 + btnGap;
         int btnStartX = dx + (dw - totalBtnW) / 2;
         int btnY = dy + dh - btnH - 8 * this.pixelScale;
         if (OverlayRenderHelper.isMouseOver(smx, smy, btnStartX, btnY, btnW, btnH, this.pixelScale)) {
            return ConfirmationDialog.Result.CANCEL;
         } else {
            return OverlayRenderHelper.isMouseOver(smx, smy, btnStartX + btnW + btnGap, btnY, btnW, btnH, this.pixelScale)
               ? ConfirmationDialog.Result.CONFIRM
               : ConfirmationDialog.Result.NONE;
         }
      } else {
         return ConfirmationDialog.Result.NONE;
      }
   }

   public boolean isPointOverDialog(int mouseX, int mouseY) {
      if (this.visible && !this.closing) {
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         int dx = this.sx(this.dialogX());
         int dy = this.sy(this.dialogY());
         int dw = 170 * this.pixelScale;
         int dh = 52 * this.pixelScale;
         return smx >= dx && smx < dx + dw && smy >= dy && smy < dy + dh;
      } else {
         return false;
      }
   }

   public static enum Result {
      NONE,
      CONFIRM,
      CANCEL;
   }
}
