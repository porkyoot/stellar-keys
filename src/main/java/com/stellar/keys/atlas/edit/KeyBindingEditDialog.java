package com.stellar.keys.atlas.edit;

import com.stellar.keys.atlas.AtlasText;
import com.stellar.keys.atlas.KeyBindingModifier;
import com.stellar.keys.atlas.KeyVisualStyle;
import com.stellar.keys.atlas.render.KeyboardRenderer;
import com.stellar.keys.atlas.render.OverlayRenderHelper;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class KeyBindingEditDialog {
   private static final int DIALOG_W = 170;
   private static final int DIALOG_H_CHOOSE = 72;
   private static final int DIALOG_H_CAPTURE = 52;
   private static final int DIALOG_H_CAPTURED = 72;
   private static final int BTN_W = 46;
   private static final int BTN_H = 12;
   private static final int BTN_GAP = 6;
   private static final int PAD = 8;
   private static final float LABEL_SCALE = 0.7F;
   private final int screenWidth;
   private final int screenHeight;
   private final int pixelScale;
   private String bindingLabel;
   private String currentKeyName;
   private KeyBindingEditDialog.State state = KeyBindingEditDialog.State.CHOOSE;
   private Key capturedKey = null;
   private KeyBindingModifier capturedModifier = KeyBindingModifier.NONE;
   private boolean visible = false;
   private boolean forNewAssignment = false;
   private long openTimeNanos = 0L;
   private boolean closing = false;
   private long closeTimeNanos = 0L;
   private int sourceGlfwKey = -1;

   public KeyBindingEditDialog(int screenWidth, int screenHeight, int pixelScale) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.pixelScale = pixelScale;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public boolean isCapturing() {
      return this.visible && !this.closing && this.state == KeyBindingEditDialog.State.CAPTURE;
   }

   public KeyBindingEditDialog.State getState() {
      return this.state;
   }

   public String getBindingLabel() {
      return this.bindingLabel;
   }

   public int getSourceGlfwKey() {
      return this.sourceGlfwKey;
   }

   public boolean isForNewAssignment() {
      return this.forNewAssignment;
   }

   public Key getCapturedKey() {
      return this.capturedKey;
   }

   public KeyBindingModifier getCapturedModifier() {
      return this.capturedModifier;
   }

   public void openForEdit(String bindingLabel, String currentKeyName, int sourceGlfwKey) {
      this.bindingLabel = bindingLabel;
      this.currentKeyName = currentKeyName;
      this.state = KeyBindingEditDialog.State.CHOOSE;
      this.capturedKey = null;
      this.capturedModifier = KeyBindingModifier.NONE;
      this.visible = true;
      this.forNewAssignment = false;
      this.sourceGlfwKey = sourceGlfwKey;
      this.openTimeNanos = System.nanoTime();
   }

   public void openForNewAssignment(String bindingLabel, String translationKey) {
      this.bindingLabel = bindingLabel;
      this.currentKeyName = AtlasText.text("ui.key.none");
      this.state = KeyBindingEditDialog.State.CAPTURE;
      this.capturedKey = null;
      this.capturedModifier = KeyBindingModifier.NONE;
      this.visible = true;
      this.forNewAssignment = true;
      this.sourceGlfwKey = -1;
      this.openTimeNanos = System.nanoTime();
   }

   public void close() {
      if (this.visible && !this.closing) {
         this.closing = true;
         this.closeTimeNanos = System.nanoTime();
      }
   }

   public void startCapture() {
      this.state = KeyBindingEditDialog.State.CAPTURE;
      this.capturedKey = null;
      this.capturedModifier = KeyBindingModifier.NONE;
   }

   public void setCapturedKey(Key key, KeyBindingModifier modifier) {
      this.capturedKey = key;
      this.capturedModifier = modifier;
      this.state = KeyBindingEditDialog.State.CAPTURED;
   }

   private int sx(int x) {
      return OverlayRenderHelper.scaleX(x, this.screenWidth / 2, this.pixelScale);
   }

   private int sy(int y) {
      return OverlayRenderHelper.scaleY(y, this.screenHeight / 2, this.pixelScale);
   }

   private int dialogH() {
      return switch (this.state) {
         case CHOOSE -> 72;
         case CAPTURE -> 52;
         case CAPTURED -> 72;
      };
   }

   private int dialogX() {
      return (this.screenWidth - 170) / 2;
   }

   private int dialogY() {
      return (this.screenHeight - this.dialogH()) / 2;
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
               this.state = KeyBindingEditDialog.State.CHOOSE;
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
         int dh = this.dialogH() * this.pixelScale;
         int dx = this.sx(this.dialogX());
         int dy = this.sy(this.dialogY());
         int cr = this.pixelScale;
         int boxFill = OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F);
         OverlayRenderHelper.blitBlurredBackground(guiGraphics, dx, dy, dw, dh, this.dialogX(), this.dialogY(), 170, this.dialogH(), fadeAlpha);
         OverlayRenderHelper.fillRoundedRect(guiGraphics, dx, dy, dw, dh, cr, OverlayRenderHelper.withAlpha(boxFill, fadeAlpha * KeyVisualStyle.panelAlpha()));
         OverlayRenderHelper.drawRoundedRectOutline(
            guiGraphics, dx, dy, dw, dh, cr, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), fadeAlpha), this.pixelScale
         );
         String title;
         if (this.state == KeyBindingEditDialog.State.CAPTURE) {
            title = AtlasText.text("ui.dialog.press_key");
         } else {
            title = AtlasText.text("ui.dialog.edit_title", this.bindingLabel);
            if (title.length() > 30) {
               title = title.substring(0, 27) + "...";
            }
         }

         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)dx + (float)dw / 2.0F, (float)(dy + 8 * this.pixelScale));
         guiGraphics.pose().scale((float)this.pixelScale * 0.85F, (float)this.pixelScale * 0.85F);
         guiGraphics.centeredText(font, title, 0, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxText(), fadeAlpha));
         guiGraphics.pose().popMatrix();
         switch (this.state) {
            case CHOOSE:
               this.renderChooseState(guiGraphics, renderer, font, dx, dy, dw, dh, smx, smy, fadeAlpha);
               break;
            case CAPTURE:
               this.renderCaptureState(guiGraphics, font, dx, dy, dw, dh, fadeAlpha);
               break;
            case CAPTURED:
               this.renderCapturedState(guiGraphics, renderer, font, dx, dy, dw, dh, smx, smy, fadeAlpha);
         }

         OverlayRenderHelper.popScaleTransform(guiGraphics);
      }
   }

   private void renderChooseState(
      GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, Font font, int dx, int dy, int dw, int dh, int smx, int smy, float fadeAlpha
   ) {
      String info = AtlasText.text("ui.dialog.current_binding", this.currentKeyName);
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().translate((float)dx + (float)dw / 2.0F, (float)(dy + 22 * this.pixelScale));
      guiGraphics.pose().scale((float)this.pixelScale * 0.7F, (float)this.pixelScale * 0.7F);
      guiGraphics.centeredText(font, info, 0, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxSourceText(), fadeAlpha));
      guiGraphics.pose().popMatrix();
      int btnW = 46 * this.pixelScale;
      int btnH = 12 * this.pixelScale;
      int btnGap = 6 * this.pixelScale;
      int totalBtnW = btnW * 3 + btnGap * 2;
      int btnStartX = dx + (dw - totalBtnW) / 2;
      int btnY = dy + dh - btnH - 8 * this.pixelScale;
      boolean allowHover = !this.closing;
      renderer.drawHudButton(
         guiGraphics,
         btnStartX,
         btnY,
         btnW,
         btnH,
         AtlasText.text("ui.button.remove"),
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
         AtlasText.text("ui.button.update"),
         allowHover && OverlayRenderHelper.isMouseOver(smx, smy, btnStartX + btnW + btnGap, btnY, btnW, btnH, this.pixelScale),
         true,
         fadeAlpha
      );
      renderer.drawHudButton(
         guiGraphics,
         btnStartX + 2 * (btnW + btnGap),
         btnY,
         btnW,
         btnH,
         AtlasText.text("ui.button.cancel"),
         allowHover && OverlayRenderHelper.isMouseOver(smx, smy, btnStartX + 2 * (btnW + btnGap), btnY, btnW, btnH, this.pixelScale),
         false,
         fadeAlpha
      );
   }

   private void renderCaptureState(GuiGraphicsExtractor guiGraphics, Font font, int dx, int dy, int dw, int dh, float fadeAlpha) {
      String hint = AtlasText.text("ui.dialog.escape_to_cancel");
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().translate((float)dx + (float)dw / 2.0F, (float)(dy + dh - 16 * this.pixelScale));
      guiGraphics.pose().scale((float)this.pixelScale * 0.7F, (float)this.pixelScale * 0.7F);
      guiGraphics.centeredText(font, hint, 0, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxSourceText(), fadeAlpha));
      guiGraphics.pose().popMatrix();
   }

   private void renderCapturedState(
      GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, Font font, int dx, int dy, int dw, int dh, int smx, int smy, float fadeAlpha
   ) {
      String keyName = this.formatCapturedKey();
      String info = AtlasText.text("ui.dialog.new_binding", keyName);
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().translate((float)dx + (float)dw / 2.0F, (float)(dy + 22 * this.pixelScale));
      guiGraphics.pose().scale((float)this.pixelScale * 0.7F, (float)this.pixelScale * 0.7F);
      guiGraphics.centeredText(font, info, 0, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxText(), fadeAlpha));
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
         AtlasText.text("ui.button.confirm"),
         allowHover && OverlayRenderHelper.isMouseOver(smx, smy, btnStartX, btnY, btnW, btnH, this.pixelScale),
         true,
         fadeAlpha
      );
      renderer.drawHudButton(
         guiGraphics,
         btnStartX + btnW + btnGap,
         btnY,
         btnW,
         btnH,
         AtlasText.text("ui.button.cancel"),
         allowHover && OverlayRenderHelper.isMouseOver(smx, smy, btnStartX + btnW + btnGap, btnY, btnW, btnH, this.pixelScale),
         false,
         fadeAlpha
      );
   }

   public String formatCapturedKey() {
      if (this.capturedKey == null) {
         return AtlasText.text("ui.key.none");
      } else {
         String keyName = this.capturedKey.getDisplayName().getString();
         return this.capturedModifier.formatKeyName(keyName);
      }
   }

   public KeyBindingEditDialog.Result hitTest(int mouseX, int mouseY) {
      if (this.visible && !this.closing) {
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         int dw = 170 * this.pixelScale;
         int dh = this.dialogH() * this.pixelScale;
         int dx = this.sx(this.dialogX());
         int dy = this.sy(this.dialogY());
         switch (this.state) {
            case CHOOSE:
               int btnWx = 46 * this.pixelScale;
               int btnHx = 12 * this.pixelScale;
               int btnGapx = 6 * this.pixelScale;
               int totalBtnWx = btnWx * 3 + btnGapx * 2;
               int btnStartXx = dx + (dw - totalBtnWx) / 2;
               int btnYx = dy + dh - btnHx - 8 * this.pixelScale;
               if (OverlayRenderHelper.isMouseOver(smx, smy, btnStartXx, btnYx, btnWx, btnHx, this.pixelScale)) {
                  return KeyBindingEditDialog.Result.REMOVE;
               } else if (OverlayRenderHelper.isMouseOver(smx, smy, btnStartXx + btnWx + btnGapx, btnYx, btnWx, btnHx, this.pixelScale)) {
                  return KeyBindingEditDialog.Result.START_CAPTURE;
               } else {
                  if (OverlayRenderHelper.isMouseOver(smx, smy, btnStartXx + 2 * (btnWx + btnGapx), btnYx, btnWx, btnHx, this.pixelScale)) {
                     return KeyBindingEditDialog.Result.CANCEL;
                  }

                  return KeyBindingEditDialog.Result.NONE;
               }
            case CAPTURE:
               return KeyBindingEditDialog.Result.NONE;
            case CAPTURED:
               int btnW = 46 * this.pixelScale;
               int btnH = 12 * this.pixelScale;
               int btnGap = 6 * this.pixelScale;
               int totalBtnW = btnW * 2 + btnGap;
               int btnStartX = dx + (dw - totalBtnW) / 2;
               int btnY = dy + dh - btnH - 8 * this.pixelScale;
               if (OverlayRenderHelper.isMouseOver(smx, smy, btnStartX, btnY, btnW, btnH, this.pixelScale)) {
                  return KeyBindingEditDialog.Result.CONFIRM_UPDATE;
               } else {
                  if (OverlayRenderHelper.isMouseOver(smx, smy, btnStartX + btnW + btnGap, btnY, btnW, btnH, this.pixelScale)) {
                     return KeyBindingEditDialog.Result.CANCEL;
                  }

                  return KeyBindingEditDialog.Result.NONE;
               }
            default:
               return KeyBindingEditDialog.Result.NONE;
         }
      } else {
         return KeyBindingEditDialog.Result.NONE;
      }
   }

   public boolean isPointOverDialog(int mouseX, int mouseY) {
      if (this.visible && !this.closing) {
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         int dx = this.sx(this.dialogX());
         int dy = this.sy(this.dialogY());
         return OverlayRenderHelper.isMouseOver(smx, smy, dx, dy, 170 * this.pixelScale, this.dialogH() * this.pixelScale, 0);
      } else {
         return false;
      }
   }

   public static enum Result {
      NONE,
      REMOVE,
      CONFIRM_UPDATE,
      CANCEL,
      START_CAPTURE;
   }

   static enum State {
      CHOOSE,
      CAPTURE,
      CAPTURED;
   }
}
