package com.stellar.keys.atlas.render;

import com.stellar.keys.atlas.KeyVisualStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class OverlayRenderHelper {

   private OverlayRenderHelper() {
   }

   public static void setPanelBlurTarget(Object target, int screenW, int screenH) {
   }

   public static void blitBlurredBackground(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h) {
      blitBlurredBackground(guiGraphics, x, y, w, h, x, y, w, h);
   }

   public static void blitBlurredBackground(GuiGraphicsExtractor guiGraphics, int vx, int vy, int vw, int vh, int uvX, int uvY, int uvW, int uvH) {
      blitBlurredBackground(guiGraphics, vx, vy, vw, vh, uvX, uvY, uvW, uvH, 1.0F);
   }

   public static void blitBlurredBackground(GuiGraphicsExtractor guiGraphics, int vx, int vy, int vw, int vh, int uvX, int uvY, int uvW, int uvH, float alpha) {
      if (alpha > 0.01F) {
         guiGraphics.fill(vx, vy, vx + vw, vy + vh, withAlpha(KeyVisualStyle.boxFill(), alpha * 0.6F));
      }
   }

   public static boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height, int pad) {
      return mouseX >= x - pad && mouseX < x + width + pad && mouseY >= y - pad && mouseY < y + height + pad;
   }

   public static void drawRectOutline(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int color, int thickness) {
      int t = Math.max(1, thickness);
      guiGraphics.fill(x, y, x + width, y + t, color);
      guiGraphics.fill(x, y + height - t, x + width, y + height, color);
      guiGraphics.fill(x, y + t, x + t, y + height - t, color);
      guiGraphics.fill(x + width - t, y + t, x + width, y + height - t, color);
   }

   public static void fillRoundedRect(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h, int r, int color) {
      if (r <= 0) {
         guiGraphics.fill(x, y, x + w, y + h, color);
      } else {
         int cr = Math.min(r, Math.min(w / 2, h / 2));
         guiGraphics.fill(x + cr, y, x + w - cr, y + h, color);
         guiGraphics.fill(x, y + cr, x + cr, y + h - cr, color);
         guiGraphics.fill(x + w - cr, y + cr, x + w, y + h - cr, color);

         for (int yi = 0; yi < cr; yi++) {
            double dy = (double)(cr - yi) - 0.5;
            int xi = Math.max(0, (int)Math.ceil((double)cr - Math.sqrt(Math.max(0.0, (double)cr * (double)cr - dy * dy)) - 0.5));
            guiGraphics.fill(x + xi, y + yi, x + cr, y + yi + 1, color);
            guiGraphics.fill(x + w - cr, y + yi, x + w - xi, y + yi + 1, color);
            guiGraphics.fill(x + xi, y + h - 1 - yi, x + cr, y + h - yi, color);
            guiGraphics.fill(x + w - cr, y + h - 1 - yi, x + w - xi, y + h - yi, color);
         }
      }
   }

   public static void drawRoundedRectOutline(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h, int r, int color, int thickness) {
      int t = Math.max(1, thickness);
      int cr = Math.min(r, Math.min(w / 2, h / 2));
      if (cr <= 0) {
         drawRectOutline(guiGraphics, x, y, w, h, color, t);
      } else {
         guiGraphics.fill(x + cr, y, x + w - cr, y + t, color);
         guiGraphics.fill(x + cr, y + h - t, x + w - cr, y + h, color);
         guiGraphics.fill(x, y + cr, x + t, y + h - cr, color);
         guiGraphics.fill(x + w - t, y + cr, x + w, y + h - cr, color);
         int prevXi = cr;

         for (int yi = 0; yi < cr; yi++) {
            double dy = (double)(cr - yi) - 0.5;
            int xi = Math.max(0, (int)Math.ceil((double)cr - Math.sqrt(Math.max(0.0, (double)cr * (double)cr - dy * dy)) - 0.5));
            int spanEnd = Math.max(xi + t, prevXi);
            guiGraphics.fill(x + xi, y + yi, x + spanEnd, y + yi + 1, color);
            guiGraphics.fill(x + w - spanEnd, y + yi, x + w - xi, y + yi + 1, color);
            guiGraphics.fill(x + xi, y + h - 1 - yi, x + spanEnd, y + h - yi, color);
            guiGraphics.fill(x + w - spanEnd, y + h - 1 - yi, x + w - xi, y + h - yi, color);
            prevXi = xi;
         }
      }
   }

   public static int withAlpha(int color, float alpha) {
      int a = Math.round((float)(color >>> 24 & 0xFF) * alpha);
      return color & 16777215 | a << 24;
   }

   public static int darken(int color, float amount) {
      int a = color >>> 24 & 0xFF;
      int r = color >>> 16 & 0xFF;
      int g = color >>> 8 & 0xFF;
      int b = color & 0xFF;
      r = Math.max(0, Math.round((float)r * (1.0F - amount)));
      g = Math.max(0, Math.round((float)g * (1.0F - amount)));
      b = Math.max(0, Math.round((float)b * (1.0F - amount)));
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int lighten(int color, float amount) {
      int a = color >>> 24 & 0xFF;
      int r = color >>> 16 & 0xFF;
      int g = color >>> 8 & 0xFF;
      int b = color & 0xFF;
      r = Math.min(255, Math.round((float)r + (255.0F - (float)r) * amount));
      g = Math.min(255, Math.round((float)g + (255.0F - (float)g) * amount));
      b = Math.min(255, Math.round((float)b + (255.0F - (float)b) * amount));
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int mixColor(int startColor, int endColor, float amount) {
      float clamped = Math.max(0.0F, Math.min(1.0F, amount));
      int sa = startColor >>> 24 & 0xFF;
      int sr = startColor >>> 16 & 0xFF;
      int sg = startColor >>> 8 & 0xFF;
      int sb = startColor & 0xFF;
      int ea = endColor >>> 24 & 0xFF;
      int er = endColor >>> 16 & 0xFF;
      int eg = endColor >>> 8 & 0xFF;
      int eb = endColor & 0xFF;
      int a = Math.round((float)sa + (float)(ea - sa) * clamped);
      int r = Math.round((float)sr + (float)(er - sr) * clamped);
      int g = Math.round((float)sg + (float)(eg - sg) * clamped);
      int b = Math.round((float)sb + (float)(eb - sb) * clamped);
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int opaque(int color) {
      return color & 16777215 | 0xFF000000;
   }

   public static int scaleX(int x, int centerX, int pixelScale) {
      return centerX + (x - centerX) * pixelScale;
   }

   public static int scaleY(int y, int centerY, int pixelScale) {
      return centerY + (y - centerY) * pixelScale;
   }

   public static void pushScaleTransform(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, int pixelScale) {
      guiGraphics.pose().pushMatrix();
      if (pixelScale > 1) {
         float inv = 1.0F / (float)pixelScale;
         guiGraphics.pose().translate((float)centerX, (float)centerY);
         guiGraphics.pose().scale(inv, inv);
         guiGraphics.pose().translate((float)(-centerX), (float)(-centerY));
      }
   }

   public static void popScaleTransform(GuiGraphicsExtractor guiGraphics) {
      guiGraphics.pose().popMatrix();
   }

   public static void pushPose(GuiGraphicsExtractor guiGraphics) {
      guiGraphics.pose().pushMatrix();
   }

   public static void popPose(GuiGraphicsExtractor guiGraphics) {
      guiGraphics.pose().popMatrix();
   }

   public static void translate(GuiGraphicsExtractor guiGraphics, float x, float y) {
      guiGraphics.pose().translate(x, y);
   }

   public static void scale(GuiGraphicsExtractor guiGraphics, float sx, float sy) {
      guiGraphics.pose().scale(sx, sy);
   }

   public static void drawString(GuiGraphicsExtractor guiGraphics, Font font, String text, int x, int y, int color, boolean shadow) {
      guiGraphics.text(font, text, x, y, color, shadow);
   }

   public static void drawString(GuiGraphicsExtractor guiGraphics, Font font, Component text, int x, int y, int color, boolean shadow) {
      guiGraphics.text(font, text, x, y, color, shadow);
   }

   public static void drawCenteredString(GuiGraphicsExtractor guiGraphics, Font font, String text, int x, int y, int color) {
      guiGraphics.centeredText(font, text, x, y, color);
   }

   public static void drawCenteredString(GuiGraphicsExtractor guiGraphics, Font font, Component text, int x, int y, int color) {
      guiGraphics.centeredText(font, text, x, y, color);
   }

   public static void drawPixelArtPattern(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int color, String[] pattern) {
      int GRID = pattern.length;
      int cell = Math.max(1, Math.min(width, height) / GRID);
      int iconW = cell * GRID;
      int iconH = cell * GRID;
      int startX = x + (width - iconW) / 2;
      int startY = y + (height - iconH) / 2;

      for (int ry = 0; ry < GRID; ry++) {
         String row = pattern[ry];

         for (int rx = 0; rx < GRID && rx < row.length(); rx++) {
            if (row.charAt(rx) == '1') {
               int px = startX + rx * cell;
               int py = startY + ry * cell;
               guiGraphics.fill(px, py, px + cell, py + cell, color);
            }
         }
      }
   }

   public static int polylineLength(int[] points) {
      int total = 0;

      for (int i = 0; i + 3 < points.length; i += 2) {
         total += Math.abs(points[i + 2] - points[i]) + Math.abs(points[i + 3] - points[i + 1]);
      }

      return total;
   }

   public static Map<Integer, Integer> buildCenterOutRowPriority(Collection<Integer> rowYs) {
      List<Integer> rows = new ArrayList<>(rowYs);
      rows.sort(Integer::compareTo);
      Map<Integer, Integer> priority = new LinkedHashMap<>();
      if (rows.isEmpty()) {
         return priority;
      } else {
         int centerIndex = rows.size() / 2;
         int rank = 0;
         priority.put(rows.get(centerIndex), rank++);

         for (int offset = 1; offset < rows.size(); offset++) {
            int upper = centerIndex - offset;
            int lower = centerIndex + offset;
            if (upper >= 0) {
               priority.put(rows.get(upper), rank++);
            }

            if (lower < rows.size()) {
               priority.put(rows.get(lower), rank++);
            }
         }

         return priority;
      }
   }
}
