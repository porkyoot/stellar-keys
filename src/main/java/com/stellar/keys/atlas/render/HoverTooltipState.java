package com.stellar.keys.atlas.render;

import com.stellar.keys.atlas.KeyVisualStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class HoverTooltipState {
   private static final long HOVER_DELAY_NANOS = 500000000L;
   private static final int MAX_TEXT_WIDTH = 180;
   private static final int OFFSET_X = 10;
   private static final int OFFSET_Y = 12;
   private static final int PAD_X = 6;
   private static final int PAD_Y = 5;
   private static final int LINE_GAP = 2;
   private String hoveredTargetId;
   private int lastMouseX = Integer.MIN_VALUE;
   private int lastMouseY = Integer.MIN_VALUE;
   private long hoverStartNanos;

   public void update(String targetId, int mouseX, int mouseY) {
      if (targetId != null && !targetId.isBlank()) {
         boolean targetChanged = !Objects.equals(this.hoveredTargetId, targetId);
         boolean mouseMoved = mouseX != this.lastMouseX || mouseY != this.lastMouseY;
         if (targetChanged || mouseMoved) {
            this.hoveredTargetId = targetId;
            this.hoverStartNanos = System.nanoTime();
         }

         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
      } else {
         this.clear();
      }
   }

   public void renderIfReady(
      GuiGraphicsExtractor guiGraphics, Font font, int screenWidth, int screenHeight, int pixelScale, int mouseX, int mouseY, String targetId, List<Component> lines
   ) {
      if (Objects.equals(this.hoveredTargetId, targetId) && lines != null && !lines.isEmpty()) {
         if (System.nanoTime() - this.hoverStartNanos >= 500000000L) {
            float textScale = (float)pixelScale * 0.8F;
            int wrapWidth = Math.max(24, Math.round(180.0F / Math.max(0.01F, textScale)));
            List<FormattedCharSequence> wrappedLines = new ArrayList<>();

            for (Component line : lines) {
               wrappedLines.addAll(font.split(line, wrapWidth));
            }

            if (!wrappedLines.isEmpty()) {
               int maxLineWidth = 0;

               for (FormattedCharSequence line : wrappedLines) {
                  maxLineWidth = Math.max(maxLineWidth, Math.round((float)font.width(line) * textScale));
               }

               int lineHeight = Math.max(pixelScale, Math.round(9.0F * textScale));
               int boxWidth = maxLineWidth + 12 * pixelScale;
               int boxHeight = wrappedLines.size() * lineHeight + Math.max(0, wrappedLines.size() - 1) * 2 * pixelScale + 10 * pixelScale;
               int centerX = screenWidth / 2;
               int centerY = screenHeight / 2;
               int scaledMouseX = OverlayRenderHelper.scaleX(mouseX, centerX, pixelScale);
               int scaledMouseY = OverlayRenderHelper.scaleY(mouseY, centerY, pixelScale);
               int scaledLeft = OverlayRenderHelper.scaleX(0, centerX, pixelScale);
               int scaledTop = OverlayRenderHelper.scaleY(0, centerY, pixelScale);
               int scaledRight = OverlayRenderHelper.scaleX(screenWidth, centerX, pixelScale);
               int scaledBottom = OverlayRenderHelper.scaleY(screenHeight, centerY, pixelScale);
               int boxX = scaledMouseX + 10 * pixelScale;
               int boxY = scaledMouseY + 12 * pixelScale;
               if (boxX + boxWidth > scaledRight - pixelScale) {
                  boxX = scaledMouseX - boxWidth - 10 * pixelScale;
               }

               if (boxY + boxHeight > scaledBottom - pixelScale) {
                  boxY = scaledMouseY - boxHeight - 12 * pixelScale;
               }

               boxX = Math.max(scaledLeft + pixelScale, Math.min(boxX, scaledRight - boxWidth - pixelScale));
               boxY = Math.max(scaledTop + pixelScale, Math.min(boxY, scaledBottom - boxHeight - pixelScale));
               int blurSampleX = Math.max(0, Math.round((float)boxX / (float)Math.max(1, pixelScale)));
               int blurSampleY = Math.max(0, Math.round((float)boxY / (float)Math.max(1, pixelScale)));
               int blurSampleW = Math.max(1, Math.round((float)boxWidth / (float)Math.max(1, pixelScale)));
               int blurSampleH = Math.max(1, Math.round((float)boxHeight / (float)Math.max(1, pixelScale)));
               int fillColor = OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), KeyVisualStyle.panelAlpha());
               OverlayRenderHelper.blitBlurredBackground(guiGraphics, boxX, boxY, boxWidth, boxHeight, blurSampleX, blurSampleY, blurSampleW, blurSampleH);
               OverlayRenderHelper.fillRoundedRect(guiGraphics, boxX, boxY, boxWidth, boxHeight, pixelScale, fillColor);
               OverlayRenderHelper.drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, pixelScale, KeyVisualStyle.boxBorder(), pixelScale);
               int textX = boxX + 6 * pixelScale;
               int textY = boxY + 5 * pixelScale;

               for (FormattedCharSequence line : wrappedLines) {
                  guiGraphics.pose().pushMatrix();
                  guiGraphics.pose().translate((float)textX, (float)textY);
                  guiGraphics.pose().scale(textScale, textScale);
                  guiGraphics.text(font, line, 0, 0, KeyVisualStyle.boxText(), false);
                  guiGraphics.pose().popMatrix();
                  textY += lineHeight + 2 * pixelScale;
               }
            }
         }
      }
   }

   private void clear() {
      this.hoveredTargetId = null;
      this.lastMouseX = Integer.MIN_VALUE;
      this.lastMouseY = Integer.MIN_VALUE;
      this.hoverStartNanos = 0L;
   }
}
