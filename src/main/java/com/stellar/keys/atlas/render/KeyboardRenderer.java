package com.stellar.keys.atlas.render;

import com.stellar.keys.atlas.KeyVisualStyle;
import com.stellar.keys.atlas.box.BoxPosition;
import com.stellar.keys.atlas.box.BoxTextLayout;
import com.stellar.keys.atlas.box.BoxTextRow;
import com.stellar.keys.atlas.layout.KeyboardKey;
import com.stellar.keys.atlas.layout.KeyboardKeyShape;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public final class KeyboardRenderer implements AutoCloseable {
   private final Minecraft minecraft;
   private final TextureManager textureManager;
   private final Font font;
   private final int pixelScale;
   private final String texturePrefix;
   private final Map<KeyboardRenderer.KeySpriteKey, KeyboardRenderer.SpriteTexture> keySprites = new HashMap<>();
   private final Map<KeyboardRenderer.PanelSpriteKey, KeyboardRenderer.SpriteTexture> panelSprites = new HashMap<>();
   private final Map<KeyboardRenderer.HousingSpriteKey, KeyboardRenderer.SpriteTexture> housingSprites = new HashMap<>();
   private final Map<KeyboardRenderer.MouseSpriteKey, KeyboardRenderer.SpriteTexture> mouseSprites = new HashMap<>();
   private final Map<KeyboardRenderer.IconSpriteKey, KeyboardRenderer.SpriteTexture> iconSprites = new HashMap<>();
   private final Map<KeyboardRenderer.CenteredLabelKey, KeyboardRenderer.CenteredLabelLayout> centeredLabelLayouts = new HashMap<>();
   private int nextTextureId = 0;

   public KeyboardRenderer(Minecraft minecraft, Font font, int pixelScale) {
      this.minecraft = minecraft;
      this.textureManager = minecraft.getTextureManager();
      this.font = font;
      this.pixelScale = pixelScale;
      this.texturePrefix = "keybindatlas/keyboard_" + Integer.toUnsignedString(System.identityHashCode(this), 16);
   }

   public boolean matches(Minecraft minecraft, Font font, int pixelScale) {
      return this.minecraft == minecraft && this.font == font && this.pixelScale == pixelScale;
   }

   public void drawKeyboardHousing(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height) {
      this.housingSprite(width, height).blit(guiGraphics, x, y);
   }

   void prewarmHousingSprite(int width, int height) {
      this.housingSprite(width, height);
   }

   public void drawMouseDevice(
      GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, List<KeyboardKey> mouseKeys, Function<KeyboardKey, KeyboardRenderer.KeyRenderStyle> styleFn
   ) {
      this.drawMouseDevice(guiGraphics, x, y, width, height, mouseKeys, styleFn, 1.0F);
   }

   public void drawMouseDevice(
      GuiGraphicsExtractor guiGraphics,
      int x,
      int y,
      int width,
      int height,
      List<KeyboardKey> mouseKeys,
      Function<KeyboardKey, KeyboardRenderer.KeyRenderStyle> styleFn,
      float alpha
   ) {
      KeyboardRenderer.SpriteTexture sprite = this.resolveMouseSprite(width, height, mouseKeys, styleFn);
      sprite.blit(guiGraphics, x, y, alpha);
   }

   void prewarmMouseDevice(int width, int height, List<KeyboardKey> mouseKeys, Function<KeyboardKey, KeyboardRenderer.KeyRenderStyle> styleFn) {
      this.resolveMouseSprite(width, height, mouseKeys, styleFn);
   }

   private KeyboardRenderer.SpriteTexture resolveMouseSprite(
      int xWidth, int yHeight, List<KeyboardKey> mouseKeys, Function<KeyboardKey, KeyboardRenderer.KeyRenderStyle> styleFn
   ) {
      int borderColor = KeyVisualStyle.keyboardBorder();
      int bodyFill = KeyVisualStyle.keyboardFill();
      int lmbFill = bodyFill;
      int rmbFill = bodyFill;
      int mmbFill = bodyFill;
      int mb4Fill = bodyFill;
      int mb5Fill = bodyFill;
      int mbo = -100;

      for (KeyboardKey mk : mouseKeys) {
         KeyboardRenderer.KeyRenderStyle style = styleFn.apply(mk);
         int fill = style.hasOutline() ? style.outlineColor() : style.fillColor();
         if (mk.glfwKey == mbo + 0) {
            lmbFill = fill;
         } else if (mk.glfwKey == mbo + 1) {
            rmbFill = fill;
         } else if (mk.glfwKey == mbo + 2) {
            mmbFill = fill;
         } else if (mk.glfwKey == mbo + 3) {
            mb4Fill = fill;
         } else if (mk.glfwKey == mbo + 4) {
            mb5Fill = fill;
         }
      }

      KeyboardRenderer.MouseSpriteKey key = new KeyboardRenderer.MouseSpriteKey(
         xWidth, yHeight, borderColor, bodyFill, lmbFill, rmbFill, mmbFill, mb4Fill, mb5Fill
      );
      return this.mouseSprites.computeIfAbsent(key, this::buildMouseSprite);
   }

   private KeyboardRenderer.SpriteTexture buildMouseSprite(KeyboardRenderer.MouseSpriteKey key) {
      int w = key.width;
      int h = key.height;
      NativeImage image = new NativeImage(w, h, true);
      float scX = (float)w / 62.0F;
      float scY = (float)h / 82.0F;
      int divColor = OverlayRenderHelper.darken(key.bodyFill, 0.3F);
      float[] offsets = new float[]{0.25F, 0.75F};

      for (int py = 0; py < h; py++) {
         for (int px = 0; px < w; px++) {
            int aSum = 0;
            int rSum = 0;
            int gSum = 0;
            int bSum = 0;

            for (float oy : offsets) {
               for (float ox : offsets) {
                  float ny = ((float)py + oy) / scY;
                  float nx = ((float)px + ox) / scX;
                  int c = this.mousePixelColor(nx, ny, key.borderColor, key.bodyFill, divColor, key.lmbFill, key.rmbFill, key.mmbFill, key.mb4Fill, key.mb5Fill);
                  aSum += c >> 24 & 0xFF;
                  rSum += c >> 16 & 0xFF;
                  gSum += c >> 8 & 0xFF;
                  bSum += c & 0xFF;
               }
            }

            int a = aSum >> 2;
            int r = rSum >> 2;
            int g = gSum >> 2;
            int b = bSum >> 2;
            if (a > 0) {
               int c = a << 24 | r << 16 | g << 8 | b;
               image.setPixelABGR(px, py, this.argbToAbgr(c));
            }
         }
      }

      return this.registerSprite("mouse", image, 0, 0);
   }

   private float[] mouseBodyBounds(float ny) {
      float cx = 30.5F;
      float fullHW = 20.0F;
      if (ny < 0.0F) {
         return null;
      } else if (ny < 7.0F) {
         float t = ny / 7.0F;
         float smooth = (float)Math.sin((double)t * Math.PI / 2.0);
         float hw = 10.0F + (fullHW - 10.0F) * smooth;
         return new float[]{cx - hw, cx + hw};
      } else if (ny < 62.0F) {
         return new float[]{cx - fullHW, cx + fullHW};
      } else if (ny < 82.0F) {
         float t = (ny - 62.0F) / 20.0F;
         float hw = fullHW * (float)Math.sqrt(1.0 - (double)(t * t));
         return hw < 0.5F ? null : new float[]{cx - hw, cx + hw};
      } else {
         return null;
      }
   }

   private int mousePixelColor(float nx, float ny, int borderColor, int bodyFill, int divColor, int lmbFill, int rmbFill, int mmbFill, int mb4Fill, int mb5Fill) {
      if (ny >= 32.0F && ny < 42.0F && nx >= 6.5F && nx < 10.5F) {
         if (ny < 33.0F) {
            if (nx < 8.0F) {
               return 0;
            } else {
               return nx < 8.5F ? borderColor : mb5Fill;
            }
         } else {
            return nx < 7.5F ? borderColor : mb5Fill;
         }
      } else if (!(ny >= 43.0F) || !(ny < 53.0F) || !(nx >= 6.5F) || !(nx < 10.5F)) {
         float[] bounds = this.mouseBodyBounds(ny);
         if (bounds == null) {
            return 0;
         } else {
            float bL = bounds[0];
            float bR = bounds[1];
            if (nx < bL - 0.5F || nx > bR + 0.5F) {
               return 0;
            } else if (nx < bL + 0.5F || nx > bR - 0.5F) {
               return borderColor;
            } else if (!(ny >= 0.5F) || !(ny < 44.5F)) {
               float innerL = bL + 1.5F;
               float innerR = bR - 1.5F;
               if (nx < innerL || nx > innerR) {
                  return OverlayRenderHelper.darken(bodyFill, 0.25F);
               } else if (ny >= 58.0F) {
                  float t = Math.min(1.0F, (ny - 58.0F) / 22.0F);
                  float shade = 0.05F + 0.4F * t * t;
                  return OverlayRenderHelper.darken(bodyFill, shade);
               } else {
                  return bodyFill;
               }
            } else if (nx >= 28.0F && nx < 34.0F) {
               if (ny >= 17.0F && ny < 32.0F) {
                  float wL = 29.0F;
                  float wR = 33.0F;
                  if (ny < 18.0F || ny >= 31.0F) {
                     wL = 30.0F;
                     wR = 32.0F;
                  }

                  if (nx >= wL && nx < wR) {
                     return mmbFill;
                  }
               }

               return divColor;
            } else {
               return nx < 28.0F ? lmbFill : rmbFill;
            }
         }
      } else if (ny > 52.0F) {
         if (nx < 8.0F) {
            return 0;
         } else {
            return nx < 8.5F ? borderColor : mb4Fill;
         }
      } else {
         return nx < 7.5F ? borderColor : mb4Fill;
      }
   }

   public void drawKey(GuiGraphicsExtractor guiGraphics, KeyboardKey key, boolean hasBinding, boolean hiddenBinding, boolean hovered) {
      this.drawKey(guiGraphics, key, this.keyStyle(hasBinding, hiddenBinding, hovered), true);
   }

   public void drawKey(GuiGraphicsExtractor guiGraphics, KeyboardKey key, KeyboardRenderer.KeyRenderStyle style, boolean showLabel) {
      this.drawKey(guiGraphics, key, style, showLabel, 1.0F);
   }

   public void drawKey(GuiGraphicsExtractor guiGraphics, KeyboardKey key, KeyboardRenderer.KeyRenderStyle style, boolean showLabel, float alpha) {
      this.keySprite(key.width, key.height, key.shape, style).blit(guiGraphics, key.x, key.y, alpha);
      if (showLabel) {
         this.drawScaledCenteredLabel(guiGraphics, key.label, key.x, key.y, key.width, key.height, OverlayRenderHelper.withAlpha(style.textColor(), alpha));
      }
   }

   public void drawDynamicBox(
      GuiGraphicsExtractor guiGraphics,
      int x,
      int y,
      BoxTextLayout layout,
      int boxFillColor,
      int boxTextColor,
      boolean hovered,
      float alpha,
      float textAlpha
   ) {
      if (!(alpha <= 0.01F)) {
         int width = layout.dimensions.width;
         int height = layout.dimensions.height;
         int defaultFill = hovered ? KeyVisualStyle.boxHoverFill() : KeyVisualStyle.boxFill();
         int fillColor = boxFillColor != 0 ? (hovered ? OverlayRenderHelper.lighten(boxFillColor, 0.15F) : boxFillColor) : defaultFill;
         guiGraphics.pose().pushMatrix();

         guiGraphics.fill(x, y, x + width, y + height, OverlayRenderHelper.withAlpha(fillColor, alpha));
         this.panelSprite(width, height, boxFillColor, hovered).blit(guiGraphics, x, y, alpha);
         if (!layout.rows.isEmpty() && textAlpha > 0.01F) {
            int textPad = 3 * this.pixelScale;

            for (BoxTextRow row : layout.rows) {
               int rowColor = row.color;
               if (boxTextColor != 0) {
                  if (row.kind == BoxTextRow.Kind.TEXT && row.color == KeyVisualStyle.boxText()) {
                     rowColor = boxTextColor;
                  } else if (row.kind == BoxTextRow.Kind.RIGHT_TEXT) {
                     rowColor = boxTextColor;
                  } else if (row.kind == BoxTextRow.Kind.TEXT && row.color == KeyVisualStyle.boxSourceText()) {
                     rowColor = OverlayRenderHelper.mixColor(boxTextColor, fillColor, 0.25F);
                  } else if (row.kind == BoxTextRow.Kind.SEPARATOR) {
                     rowColor = OverlayRenderHelper.mixColor(boxTextColor, fillColor, 0.4F);
                  }
               }
               if (row.kind == BoxTextRow.Kind.SEPARATOR) {
                  this.drawBoxSeparator(guiGraphics, x, y, width, row, fillColor, textAlpha);
               } else if (row.kind == BoxTextRow.Kind.RIGHT_TEXT) {
                  guiGraphics.pose().pushMatrix();
                  guiGraphics.pose().translate((float)(x + width - textPad), (float)(y + row.yOffset));
                  guiGraphics.pose().scale(row.scale, row.scale);
                  guiGraphics.text(this.font, row.text, -this.font.width(row.text), 0, OverlayRenderHelper.withAlpha(rowColor, textAlpha), false);
                  guiGraphics.pose().popMatrix();
               } else {
                  guiGraphics.pose().pushMatrix();
                  guiGraphics.pose().translate((float)x + (float)width / 2.0F, (float)(y + row.yOffset));
                  guiGraphics.pose().scale(row.scale, row.scale);
                  guiGraphics.centeredText(this.font, row.text, 0, 0, OverlayRenderHelper.withAlpha(rowColor, textAlpha));
                  guiGraphics.pose().popMatrix();
               }
            }
         }

         guiGraphics.pose().popMatrix();
      }
   }

   public void drawDynamicBox(GuiGraphicsExtractor guiGraphics, int x, int y, BoxTextLayout layout, boolean hovered, float alpha, float textAlpha) {
      this.drawDynamicBox(guiGraphics, x, y, layout, 0, 0, hovered, alpha, textAlpha);
   }

   private void drawBoxSeparator(GuiGraphicsExtractor guiGraphics, int x, int y, int width, BoxTextRow row, int fillColor, float alpha) {
      int availableWidth = Math.max(10 * this.pixelScale, width - 10 * this.pixelScale);
      int centerX = x + width / 2;
      int lineY = y + row.yOffset + Math.max(0, row.height - this.pixelScale) / 2;
      int halfWidth = availableWidth / 2;
      int baseFill = fillColor != 0 ? fillColor : KeyVisualStyle.boxFill();
      int centerColor = OverlayRenderHelper.darken(baseFill, 0.2F);
      int edgeColor = OverlayRenderHelper.darken(baseFill, 0.1F);
      float sepAlpha = alpha * 0.9F;
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

   public void drawHudButton(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, String label, boolean hovered, boolean active) {
      this.drawHudButton(guiGraphics, x, y, width, height, label, hovered, active, 1.0F);
   }

   public void drawHudButton(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, String label, boolean hovered, boolean active, float alpha) {
      this.drawHudButton(guiGraphics, x, y, width, height, label, hovered, active, alpha, 0);
   }

   public void drawHudButton(
      GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, String label, boolean hovered, boolean active, float alpha, int iconInset
   ) {
      if (!(alpha <= 0.01F)) {
         KeyboardRenderer.KeySpriteVariant variant = hovered
            ? (active ? KeyboardRenderer.KeySpriteVariant.ASSIGNED_HOVER : KeyboardRenderer.KeySpriteVariant.UNUSED_HOVER)
            : (active ? KeyboardRenderer.KeySpriteVariant.ASSIGNED : KeyboardRenderer.KeySpriteVariant.UNUSED);
         this.keySprite(width, height, KeyboardKeyShape.rectangle(), this.keyStyle(variant)).blit(guiGraphics, x, y, alpha);
         String[] pattern = PixelArtIcons.patternFor(label, Math.min(width, height));
         if (pattern != null) {
            int inset = Math.max(0, iconInset);
            int innerWidth = Math.max(1, width - inset * 2);
            int innerHeight = Math.max(1, height - inset * 2);
            this.iconSprite(innerWidth, innerHeight, pattern, variant.textColor()).blit(guiGraphics, x + inset, y + inset, alpha);
         } else {
            int textColor = OverlayRenderHelper.withAlpha(variant.textColor(), alpha);
            this.drawScaledCenteredLabel(guiGraphics, label, x, y, width, height, textColor);
         }
      }
   }

   public void drawLegendKey(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, String label, KeyboardRenderer.LegendKeyType type) {
      KeyboardRenderer.KeySpriteVariant variant = switch (type) {
         case ASSIGNED -> KeyboardRenderer.KeySpriteVariant.ASSIGNED;
         case UNUSED -> KeyboardRenderer.KeySpriteVariant.UNUSED;
         case HIDDEN_BINDING -> KeyboardRenderer.KeySpriteVariant.HIDDEN;
      };
      this.drawLegendKey(guiGraphics, x, y, width, height, label, this.keyStyle(variant));
   }

   public void drawLegendKey(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, String label, KeyboardRenderer.KeyRenderStyle style) {
      this.drawLegendKey(guiGraphics, x, y, width, height, label, style, 1.0F);
   }

   public void drawLegendKey(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, String label, KeyboardRenderer.KeyRenderStyle style, float alpha) {
      if (!(alpha <= 0.01F)) {
         this.keySprite(width, height, KeyboardKeyShape.rectangle(), style).blit(guiGraphics, x, y, alpha);
         this.drawScaledCenteredLabel(guiGraphics, label, x, y, width, height, OverlayRenderHelper.withAlpha(style.textColor(), alpha));
      }
   }

   public void drawIcon(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, String label, int color, float alpha) {
      this.drawIcon(guiGraphics, x, y, width, height, label, color, alpha, 0);
   }

   public void drawIcon(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, String label, int color, float alpha, int inset) {
      if (!(alpha <= 0.01F)) {
         String[] pattern = PixelArtIcons.patternFor(label, Math.min(width, height));
         if (pattern != null) {
            int innerInset = Math.max(0, inset);
            int innerWidth = Math.max(1, width - innerInset * 2);
            int innerHeight = Math.max(1, height - innerInset * 2);
            this.iconSprite(innerWidth, innerHeight, pattern, color).blit(guiGraphics, x + innerInset, y + innerInset, alpha);
         }
      }
   }

   void prewarmKeySprite(int width, int height, KeyboardRenderer.KeySpriteVariant variant) {
      this.keySprite(width, height, KeyboardKeyShape.rectangle(), variant);
   }

   void prewarmKeySprite(int width, int height, KeyboardRenderer.KeyRenderStyle style) {
      this.keySprite(width, height, KeyboardKeyShape.rectangle(), style);
   }

   void prewarmPanelSprite(int width, int height, int fillColor, boolean hovered) {
      this.panelSprite(width, height, fillColor, hovered);
   }

   void prewarmPanelSprite(int width, int height, boolean hovered) {
      this.prewarmPanelSprite(width, height, 0, hovered);
   }

   void prewarmCenteredLabel(String label, int width, int height) {
      this.centeredLabelLayouts.computeIfAbsent(new KeyboardRenderer.CenteredLabelKey(label, width, height), this::buildCenteredLabelLayout);
   }

   public void beginPolyLineBatch(GuiGraphicsExtractor guiGraphics) {
   }

   public void endPolyLineBatch() {
   }

   public void drawPolyLineProgress(GuiGraphicsExtractor guiGraphics, int[] points, int color, float progress, int totalLength) {
      if (points.length >= 4 && !(progress <= 0.0F)) {
         if (progress >= 1.0F) {
            this.drawPolyLine(guiGraphics, points, points.length, color);
         } else if (totalLength > 0) {
            int target = Math.max(1, Math.round((float)totalLength * progress));
            int[] clipped = new int[points.length];
            int clippedLength = 2;
            clipped[0] = points[0];
            clipped[1] = points[1];
            int remaining = target;

            for (int i = 0; i + 3 < points.length; i += 2) {
               int ax = points[i];
               int ay = points[i + 1];
               int bx = points[i + 2];
               int by = points[i + 3];
               int segment = Math.abs(bx - ax) + Math.abs(by - ay);
               if (segment > 0) {
                  if (remaining < segment) {
                     int px = ax;
                     int py = ay;
                     if (ax != bx) {
                        px = ax + (bx > ax ? remaining : -remaining);
                     } else {
                        py = ay + (by > ay ? remaining : -remaining);
                     }

                     clipped[clippedLength++] = px;
                     clipped[clippedLength++] = py;
                     break;
                  }

                  clipped[clippedLength++] = bx;
                  clipped[clippedLength++] = by;
                  remaining -= segment;
                  if (remaining == 0) {
                     break;
                  }
               }
            }

            if (clippedLength >= 4) {
               this.drawPolyLine(guiGraphics, clipped, clippedLength, color);
            }
         }
      }
   }

   public boolean isMouseInBox(int mouseX, int mouseY, BoxPosition boxPos) {
      return mouseX >= boxPos.boxX - this.pixelScale
         && mouseX < boxPos.boxX + boxPos.boxWidth + this.pixelScale
         && mouseY >= boxPos.boxY - this.pixelScale
         && mouseY < boxPos.boxY + boxPos.boxHeight + this.pixelScale;
   }

   @Override
   public void close() {
      this.releaseSprites(this.keySprites);
      this.releaseSprites(this.panelSprites);
      this.releaseSprites(this.housingSprites);
      this.releaseSprites(this.mouseSprites);
      this.releaseSprites(this.iconSprites);
      this.centeredLabelLayouts.clear();
   }

   private void releaseSprites(Map<?, KeyboardRenderer.SpriteTexture> cache) {
      for (KeyboardRenderer.SpriteTexture sprite : cache.values()) {
         this.textureManager.release(sprite.location);
      }

      cache.clear();
   }

   private void drawScaledCenteredLabel(GuiGraphicsExtractor guiGraphics, String label, int x, int y, int width, int height, int textColor) {
      KeyboardRenderer.CenteredLabelLayout layout = this.centeredLabelLayouts
         .computeIfAbsent(new KeyboardRenderer.CenteredLabelKey(label, width, height), this::buildCenteredLabelLayout);
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().translate((float)x + (float)width / 2.0F, (float)y + layout.yOffset);
      guiGraphics.pose().scale(layout.scale, layout.scale);
      guiGraphics.centeredText(this.font, label, 0, 0, textColor);
      guiGraphics.pose().popMatrix();
   }

   private KeyboardRenderer.CenteredLabelLayout buildCenteredLabelLayout(KeyboardRenderer.CenteredLabelKey key) {
      int textWidth = this.font.width(key.label);
      int maxWidth = key.width - 4 * this.pixelScale;
      float scale = (float)this.pixelScale;
      if (textWidth > 0 && (float)textWidth * scale > (float)maxWidth && maxWidth > 0) {
         scale = (float)maxWidth / (float)textWidth;
      }

      return new KeyboardRenderer.CenteredLabelLayout(scale, (float)key.height / 2.0F - 4.0F * (float)this.pixelScale);
   }

   public KeyboardRenderer.KeySpriteVariant keyVariant(boolean hasBinding, boolean hiddenBinding, boolean hovered) {
      if (hiddenBinding) {
         return hovered ? KeyboardRenderer.KeySpriteVariant.HIDDEN_HOVER : KeyboardRenderer.KeySpriteVariant.HIDDEN;
      } else if (hasBinding) {
         return hovered ? KeyboardRenderer.KeySpriteVariant.ASSIGNED_HOVER : KeyboardRenderer.KeySpriteVariant.ASSIGNED;
      } else {
         return hovered ? KeyboardRenderer.KeySpriteVariant.UNUSED_HOVER : KeyboardRenderer.KeySpriteVariant.UNUSED;
      }
   }

   public KeyboardRenderer.KeyRenderStyle keyStyle(boolean hasBinding, boolean hiddenBinding, boolean hovered) {
      return this.keyStyle(this.keyVariant(hasBinding, hiddenBinding, hovered));
   }

   public KeyboardRenderer.KeyRenderStyle keyStyle(KeyboardRenderer.KeySpriteVariant variant) {
      return KeyboardRenderer.KeyRenderStyle.standard(variant);
   }

   public KeyboardRenderer.KeyRenderStyle keyStyle(int fillColor, int textColor, boolean hovered) {
      return KeyboardRenderer.KeyRenderStyle.custom(fillColor, textColor, hovered);
   }

   public KeyboardRenderer.KeyRenderStyle keyStyle(int outlineColor, int fillColor, int textColor, boolean hovered) {
      return KeyboardRenderer.KeyRenderStyle.outline(outlineColor, fillColor, textColor, hovered);
   }

   private KeyboardRenderer.SpriteTexture keySprite(int width, int height, KeyboardKeyShape shape, KeyboardRenderer.KeySpriteVariant variant) {
      return this.keySprites.computeIfAbsent(new KeyboardRenderer.KeySpriteKey(width, height, shape, variant), this::buildKeySprite);
   }

   private KeyboardRenderer.SpriteTexture keySprite(int width, int height, KeyboardKeyShape shape, KeyboardRenderer.KeyRenderStyle style) {
      return this.keySprites.computeIfAbsent(new KeyboardRenderer.KeySpriteKey(width, height, shape, style), this::buildKeySprite);
   }

   private KeyboardRenderer.SpriteTexture panelSprite(int width, int height, int fillColor, boolean hovered) {
      return this.panelSprites.computeIfAbsent(new KeyboardRenderer.PanelSpriteKey(width, height, fillColor, hovered), this::buildPanelSprite);
   }

   private KeyboardRenderer.SpriteTexture panelSprite(int width, int height, boolean hovered) {
      return this.panelSprite(width, height, 0, hovered);
   }

   private KeyboardRenderer.SpriteTexture housingSprite(int width, int height) {
      return this.housingSprites.computeIfAbsent(new KeyboardRenderer.HousingSpriteKey(width, height), this::buildHousingSprite);
   }

   private KeyboardRenderer.SpriteTexture iconSprite(int width, int height, String[] pattern, int color) {
      return this.iconSprites.computeIfAbsent(new KeyboardRenderer.IconSpriteKey(width, height, pattern, color), this::buildIconSprite);
   }

   private KeyboardRenderer.SpriteTexture buildKeySprite(KeyboardRenderer.KeySpriteKey key) {
      int padding = this.pixelScale;
      int spriteWidth = key.width + padding * 2;
      int spriteHeight = key.height + padding * 2;
      NativeImage image = new NativeImage(spriteWidth, spriteHeight, true);
      int fillColor = key.fillColor;
      int borderColor = key.outlineColor != 0 ? key.outlineColor : OverlayRenderHelper.darken(fillColor, key.borderDark);
      int bottomBorderColor = key.outlineColor != 0 ? OverlayRenderHelper.darken(key.outlineColor, 0.2F) : OverlayRenderHelper.darken(borderColor, 0.15F);
      int topColor = OverlayRenderHelper.lighten(fillColor, key.topLight * 0.5F);
      int wallColor = OverlayRenderHelper.darken(fillColor, 0.25F);
      if (key.shape.isRectangle()) {
         for (ShapeGeometry.FillOp op : ShapeGeometry.shadedKeyShape(key.width, key.height, this.pixelScale, this.pixelScale)) {
            this.fillImageRect(
               image,
               padding + op.x1,
               padding + op.y1,
               padding + op.x2,
               padding + op.y2,
               ShapeGeometry.fillColor(op.role, fillColor, topColor, wallColor, borderColor, bottomBorderColor)
            );
         }
      } else {
         this.drawSectionedKeySprite(image, key, fillColor, topColor, wallColor, borderColor, bottomBorderColor);
      }

      return this.registerSprite("key", image, padding, padding);
   }

   private void drawSectionedKeySprite(
      NativeImage image, KeyboardRenderer.KeySpriteKey key, int fillColor, int topColor, int wallColor, int borderColor, int bottomBorderColor
   ) {
      int padding = this.pixelScale;
      int borderReach = Math.max(1, this.pixelScale);
      int fillAbgr = this.argbToAbgr(fillColor);
      int topAbgr = this.argbToAbgr(topColor);
      int wallAbgr = this.argbToAbgr(wallColor);
      int borderAbgr = this.argbToAbgr(borderColor);
      int bottomBorderAbgr = this.argbToAbgr(bottomBorderColor);

      for (int py = 0; py < image.getHeight(); py++) {
         int localY = py - padding;

         for (int px = 0; px < image.getWidth(); px++) {
            int localX = px - padding;
            int color;
            if (this.containsSectionedKeyPixel(key, localX, localY)) {
               color = this.sectionedInteriorAbgr(key, localX, localY, fillAbgr, topAbgr, wallAbgr);
            } else {
               if (!this.isNearSectionedKey(key, localX, localY, borderReach)) {
                  continue;
               }

               color = this.isSectionedBottomBorderPixel(key, localX, localY, borderReach) ? bottomBorderAbgr : borderAbgr;
            }

            image.setPixelABGR(px, py, color);
         }
      }
   }

   private int sectionedInteriorAbgr(KeyboardRenderer.KeySpriteKey key, int localX, int localY, int fillAbgr, int topAbgr, int wallAbgr) {
      int left = this.sectionedLeftAt(key, localY);
      int right = this.sectionedRightAt(key, localY);
      int sideThickness = Math.max(1, this.pixelScale);
      int bottomThickness = Math.max(2, this.pixelScale + 1);
      if (localX < left + sideThickness || localX >= right - sideThickness || localY >= key.height - bottomThickness) {
         return wallAbgr;
      } else {
         return localY < this.pixelScale ? topAbgr : fillAbgr;
      }
   }

   private boolean isNearSectionedKey(KeyboardRenderer.KeySpriteKey key, int localX, int localY, int reach) {
      for (int dy = -reach; dy <= reach; dy++) {
         for (int dx = -reach; dx <= reach; dx++) {
            if (Math.max(Math.abs(dx), Math.abs(dy)) <= reach && this.containsSectionedKeyPixel(key, localX + dx, localY + dy)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isSectionedBottomBorderPixel(KeyboardRenderer.KeySpriteKey key, int localX, int localY, int reach) {
      if (localY >= key.height) {
         return true;
      } else if (localY < key.height - reach) {
         return false;
      } else {
         for (int dy = 1; dy <= reach; dy++) {
            if (this.containsSectionedKeyPixel(key, localX, localY - dy)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean containsSectionedKeyPixel(KeyboardRenderer.KeySpriteKey key, int localX, int localY) {
      if (localY >= 0 && localY < key.height) {
         int left = this.sectionedLeftAt(key, localY);
         int right = this.sectionedRightAt(key, localY);
         return localX >= left && localX < right;
      } else {
         return false;
      }
   }

   private int sectionedLeftAt(KeyboardRenderer.KeySpriteKey key, int localY) {
      KeyboardKeyShape shape = key.shape;
      int splitY = shape.effectiveSplitY(key.height);
      int left = localY < splitY ? shape.topInsetLeft : shape.bottomInsetLeft;
      int cornerTrim = Math.max(this.topCornerTrim(localY), this.bottomCornerTrim(key.height, localY));
      return left + cornerTrim;
   }

   private int sectionedRightAt(KeyboardRenderer.KeySpriteKey key, int localY) {
      KeyboardKeyShape shape = key.shape;
      int splitY = shape.effectiveSplitY(key.height);
      int right = key.width - (localY < splitY ? shape.topInsetRight : shape.bottomInsetRight);
      int cornerTrim = Math.max(this.topCornerTrim(localY), this.bottomCornerTrim(key.height, localY));
      return right - cornerTrim;
   }

   private int topCornerTrim(int localY) {
      return Math.max(0, this.pixelScale - localY);
   }

   private int bottomCornerTrim(int height, int localY) {
      return Math.max(0, this.pixelScale - (height - 1 - localY));
   }

   private KeyboardRenderer.SpriteTexture buildPanelSprite(KeyboardRenderer.PanelSpriteKey key) {
      int padding = this.pixelScale;
      int spriteWidth = key.width + padding * 2;
      int spriteHeight = key.height + padding * 2;
      NativeImage image = new NativeImage(spriteWidth, spriteHeight, true);
      int defaultFill = key.hovered ? KeyVisualStyle.boxHoverFill() : KeyVisualStyle.boxFill();
      int defaultBorder = key.hovered ? KeyVisualStyle.boxHoverBorder() : KeyVisualStyle.boxBorder();
      int fillColor = key.fillColor != 0 ? (key.hovered ? OverlayRenderHelper.lighten(key.fillColor, 0.15F) : key.fillColor) : defaultFill;
      int borderColor = key.fillColor != 0 ? OverlayRenderHelper.darken(fillColor, 0.25F) : defaultBorder;
      int bottomBorderColor = OverlayRenderHelper.darken(borderColor, 0.1F);
      int topColor = OverlayRenderHelper.lighten(fillColor, 0.09F);
      int wallColor = OverlayRenderHelper.darken(fillColor, 0.14F);

      for (ShapeGeometry.FillOp op : ShapeGeometry.panelShape(key.width, key.height, this.pixelScale, this.pixelScale)) {
         int color = ShapeGeometry.fillColor(op.role, fillColor, topColor, wallColor, borderColor, bottomBorderColor);
         this.fillImageRect(image, padding + op.x1, padding + op.y1, padding + op.x2, padding + op.y2, color);
      }

      return this.registerSprite("box", image, padding, padding);
   }

   private KeyboardRenderer.SpriteTexture buildHousingSprite(KeyboardRenderer.HousingSpriteKey key) {
      NativeImage image = new NativeImage(key.width, key.height, true);
      int borderInset = Math.max(1, this.pixelScale);
      this.fillRoundedRect(image, 0, 0, key.width, key.height, this.pixelScale, KeyVisualStyle.keyboardBorder());
      this.fillRoundedRect(
         image,
         borderInset,
         borderInset,
         key.width - borderInset * 2,
         key.height - borderInset * 2,
         Math.max(0, this.pixelScale - borderInset),
         KeyVisualStyle.keyboardFill()
      );
      return this.registerSprite("housing", image, 0, 0);
   }

   private KeyboardRenderer.SpriteTexture buildIconSprite(KeyboardRenderer.IconSpriteKey key) {
      NativeImage image = new NativeImage(key.width, key.height, true);
      String[] pattern = key.pattern;
      int GRID = pattern.length;
      int cell = Math.max(1, Math.min(key.width, key.height) / GRID);
      int iconW = cell * GRID;
      int iconH = cell * GRID;
      int startX = (key.width - iconW) / 2;
      int startY = (key.height - iconH) / 2;
      int abgr = this.argbToAbgr(key.color);

      for (int ry = 0; ry < GRID; ry++) {
         String row = pattern[ry];

         for (int rx = 0; rx < GRID && rx < row.length(); rx++) {
            if (row.charAt(rx) == '1') {
               int px = startX + rx * cell;
               int py = startY + ry * cell;

               for (int dy = 0; dy < cell; dy++) {
                  for (int dx = 0; dx < cell; dx++) {
                     int ix = px + dx;
                     int iy = py + dy;
                     if (ix >= 0 && ix < key.width && iy >= 0 && iy < key.height) {
                        image.setPixelABGR(ix, iy, abgr);
                     }
                  }
               }
            }
         }
      }

      return this.registerSprite("icon", image, 0, 0);
   }

   private KeyboardRenderer.SpriteTexture registerSprite(String kind, NativeImage image, int originX, int originY) {
      DynamicTexture texture = new DynamicTexture(() -> "atlas_" + kind, image);
      texture.upload();
      Identifier location = Identifier.fromNamespaceAndPath("stellar_keys", this.texturePrefix + "/" + kind + "_" + this.nextTextureId++);
      this.textureManager.register(location, texture);
      return new KeyboardRenderer.SpriteTexture(location, image.getWidth(), image.getHeight(), originX, originY);
   }

   private void fillImageRect(NativeImage image, int x1, int y1, int x2, int y2, int argb) {
      int clampedX1 = Math.max(0, x1);
      int clampedY1 = Math.max(0, y1);
      int clampedX2 = Math.min(image.getWidth(), x2);
      int clampedY2 = Math.min(image.getHeight(), y2);
      if (clampedX1 < clampedX2 && clampedY1 < clampedY2) {
         int abgr = this.argbToAbgr(argb);

         for (int row = clampedY1; row < clampedY2; row++) {
            for (int col = clampedX1; col < clampedX2; col++) {
               image.setPixelABGR(col, row, abgr);
            }
         }
      }
   }

   private int argbToAbgr(int color) {
      int a = color >>> 24 & 0xFF;
      int r = color >>> 16 & 0xFF;
      int g = color >>> 8 & 0xFF;
      int b = color & 0xFF;
      return a << 24 | b << 16 | g << 8 | r;
   }

   private void drawPolyLine(GuiGraphicsExtractor guiGraphics, int[] points, int pointCount, int color) {
      for (int i = 0; i + 3 < pointCount; i += 2) {
         int ax = points[i];
         int ay = points[i + 1];
         int bx = points[i + 2];
         int by = points[i + 3];
         boolean skipStart = i > 0;
         boolean skipEnd = i + 4 < pointCount;
         if (skipStart) {
            int prevX = points[i - 2];
            int prevY = points[i - 1];
            if (prevY == ay && prevY == points[i - 1] || prevX == ax && prevX == points[i - 2]) {
               skipStart = false;
            }
         }

         if (skipEnd) {
            int nextX = points[i + 4];
            int nextY = points[i + 5];
            if (nextY == by && nextY == points[i + 3] || nextX == bx && nextX == points[i + 2]) {
               skipEnd = false;
            }
         }

         if (ay == by) {
            int lo = Math.min(ax, bx);
            int hi = Math.max(ax, bx);
            if (skipStart) {
               if (ax < bx) {
                  lo += this.pixelScale;
               } else {
                  hi -= this.pixelScale;
               }
            }

            if (skipEnd) {
               if (ax < bx) {
                  hi -= this.pixelScale;
               } else {
                  lo += this.pixelScale;
               }
            }

            if (lo <= hi) {
               guiGraphics.fill(lo, ay, hi + this.pixelScale, ay + this.pixelScale, color);
            }
         } else if (ax == bx) {
            int lox = Math.min(ay, by);
            int hix = Math.max(ay, by);
            if (skipStart) {
               if (ay < by) {
                  lox += this.pixelScale;
               } else {
                  hix -= this.pixelScale;
               }
            }

            if (skipEnd) {
               if (ay < by) {
                  hix -= this.pixelScale;
               } else {
                  lox += this.pixelScale;
               }
            }

            if (lox <= hix) {
               guiGraphics.fill(ax, lox, ax + this.pixelScale, hix + this.pixelScale, color);
            }
         }
      }

      for (int i = 2; i + 2 < pointCount; i += 2) {
         int px = points[i - 2];
         int py = points[i - 1];
         int cx = points[i];
         int cy = points[i + 1];
         int nx = points[i + 2];
         int ny = points[i + 3];
         int dxIn = Integer.signum(cx - px);
         int dyIn = Integer.signum(cy - py);
         int dxOut = Integer.signum(nx - cx);
         int dyOut = Integer.signum(ny - cy);
         int curveType = this.bendCurveType(dxIn, dyIn, dxOut, dyOut);
         if (curveType >= 0 && this.pixelScale > 1) {
            emitCornerCurve(guiGraphics, cx, cy, this.pixelScale, curveType, color);
         }
      }
   }

   private static void emitCornerCurve(GuiGraphicsExtractor guiGraphics, int gapX, int gapY, int scale, int type, int color) {
      switch (type) {
         case 0:
            for (int j = 1; j < scale; j++) {
               guiGraphics.fill(gapX + scale - j, gapY + j, gapX + scale, gapY + j + 1, color);
            }
            break;
         case 1:
            for (int j = 1; j < scale; j++) {
               guiGraphics.fill(gapX, gapY + j, gapX + j, gapY + j + 1, color);
            }
            break;
         case 2:
            for (int j = 1; j < scale; j++) {
               guiGraphics.fill(gapX + scale - j, gapY + scale - 1 - j, gapX + scale, gapY + scale - j, color);
            }
            break;
         case 3:
            for (int j = 1; j < scale; j++) {
               guiGraphics.fill(gapX, gapY + scale - 1 - j, gapX + j, gapY + scale - j, color);
            }
      }
   }

   private void fillRoundedRect(NativeImage image, int x, int y, int width, int height, int radius, int color) {
      if (width > 0 && height > 0) {
         int cappedRadius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
         if (cappedRadius <= 0) {
            this.fillImageRect(image, x, y, x + width, y + height, color);
         } else {
            this.fillImageRect(image, x + cappedRadius, y, x + width - cappedRadius, y + height, color);
            this.fillImageRect(image, x, y + cappedRadius, x + cappedRadius, y + height - cappedRadius, color);
            this.fillImageRect(image, x + width - cappedRadius, y + cappedRadius, x + width, y + height - cappedRadius, color);

            for (int row = 0; row < cappedRadius; row++) {
               int inset = cappedRadius - row;
               this.fillImageRect(image, x + inset, y + row, x + width - inset, y + row + 1, color);
               this.fillImageRect(image, x + inset, y + height - row - 1, x + width - inset, y + height - row, color);
            }
         }
      }
   }

   private int bendCurveType(int dxIn, int dyIn, int dxOut, int dyOut) {
      if ((dxIn <= 0 || dyOut <= 0) && (dyIn >= 0 || dxOut >= 0)) {
         if ((dxIn <= 0 || dyOut >= 0) && (dyIn <= 0 || dxOut >= 0)) {
            if ((dxIn >= 0 || dyOut <= 0) && (dyIn >= 0 || dxOut <= 0)) {
               return (dxIn >= 0 || dyOut >= 0) && (dyIn <= 0 || dxOut <= 0) ? -1 : 2;
            } else {
               return 0;
            }
         } else {
            return 3;
         }
      } else {
         return 1;
      }
   }

   private static final class CenteredLabelKey {
      final String label;
      final int width;
      final int height;

      CenteredLabelKey(String label, int width, int height) {
         this.label = label;
         this.width = width;
         this.height = height;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardRenderer.CenteredLabelKey that)
               ? false
               : this.width == that.width && this.height == that.height && this.label.equals(that.label);
         }
      }

      @Override
      public int hashCode() {
         int result = this.label.hashCode();
         result = 31 * result + this.width;
         return 31 * result + this.height;
      }
   }

   private static final class CenteredLabelLayout {
      final float scale;
      final float yOffset;

      CenteredLabelLayout(float scale, float yOffset) {
         this.scale = scale;
         this.yOffset = yOffset;
      }
   }

   private static final class HousingSpriteKey {
      final int width;
      final int height;

      HousingSpriteKey(int width, int height) {
         this.width = width;
         this.height = height;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardRenderer.HousingSpriteKey that) ? false : this.width == that.width && this.height == that.height;
         }
      }

      @Override
      public int hashCode() {
         int result = this.width;
         return 31 * result + this.height;
      }
   }

   private static final class IconSpriteKey {
      final int width;
      final int height;
      final String[] pattern;
      final int color;

      IconSpriteKey(int width, int height, String[] pattern, int color) {
         this.width = width;
         this.height = height;
         this.pattern = pattern;
         this.color = color;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardRenderer.IconSpriteKey that)
               ? false
               : this.width == that.width && this.height == that.height && this.pattern == that.pattern && this.color == that.color;
         }
      }

      @Override
      public int hashCode() {
         int result = this.width;
         result = 31 * result + this.height;
         result = 31 * result + System.identityHashCode(this.pattern);
         return 31 * result + this.color;
      }
   }

   public static final class KeyRenderStyle {
      private final int fillColor;
      private final int textColor;
      private final int outlineColor;
      private final float topLight;
      private final float borderDark;

      private KeyRenderStyle(int fillColor, int textColor, int outlineColor, float topLight, float borderDark) {
         this.fillColor = fillColor;
         this.textColor = textColor;
         this.outlineColor = outlineColor;
         this.topLight = topLight;
         this.borderDark = borderDark;
      }

      public static KeyboardRenderer.KeyRenderStyle standard(KeyboardRenderer.KeySpriteVariant variant) {
         return new KeyboardRenderer.KeyRenderStyle(variant.fillColor(), variant.textColor(), 0, variant.topLight, variant.borderDark);
      }

      public static KeyboardRenderer.KeyRenderStyle custom(int fillColor, int textColor, boolean hovered) {
         return fullFill(fillColor, textColor, hovered);
      }

      public static KeyboardRenderer.KeyRenderStyle fullFill(int fillColor, int textColor, boolean hovered) {
         return hovered
            ? new KeyboardRenderer.KeyRenderStyle(
               OverlayRenderHelper.lighten(fillColor, 0.22F),
               textColor,
               0,
               KeyboardRenderer.KeySpriteVariant.ASSIGNED_HOVER.topLight,
               KeyboardRenderer.KeySpriteVariant.ASSIGNED_HOVER.borderDark
            )
            : new KeyboardRenderer.KeyRenderStyle(
               fillColor, textColor, 0, KeyboardRenderer.KeySpriteVariant.ASSIGNED.topLight, KeyboardRenderer.KeySpriteVariant.ASSIGNED.borderDark
            );
      }

      public static KeyboardRenderer.KeyRenderStyle outline(int outlineColor, int fillColor, int textColor, boolean hovered) {
         return hovered
            ? new KeyboardRenderer.KeyRenderStyle(
               OverlayRenderHelper.lighten(fillColor, 0.22F),
               OverlayRenderHelper.lighten(textColor, 0.22F),
               OverlayRenderHelper.lighten(outlineColor, 0.22F),
               KeyboardRenderer.KeySpriteVariant.ASSIGNED_HOVER.topLight,
               KeyboardRenderer.KeySpriteVariant.ASSIGNED_HOVER.borderDark
            )
            : new KeyboardRenderer.KeyRenderStyle(
               fillColor,
               textColor,
               outlineColor,
               KeyboardRenderer.KeySpriteVariant.ASSIGNED.topLight,
               KeyboardRenderer.KeySpriteVariant.ASSIGNED.borderDark
            );
      }

      public int fillColor() {
         return this.fillColor;
      }

      public int textColor() {
         return this.textColor;
      }

      public int outlineColor() {
         return this.outlineColor;
      }

      public boolean hasOutline() {
         return this.outlineColor != 0;
      }

      float topLight() {
         return this.topLight;
      }

      float borderDark() {
         return this.borderDark;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardRenderer.KeyRenderStyle that)
               ? false
               : this.fillColor == that.fillColor
                  && this.textColor == that.textColor
                  && this.outlineColor == that.outlineColor
                  && Float.floatToIntBits(this.topLight) == Float.floatToIntBits(that.topLight)
                  && Float.floatToIntBits(this.borderDark) == Float.floatToIntBits(that.borderDark);
         }
      }

      @Override
      public int hashCode() {
         int result = this.fillColor;
         result = 31 * result + this.textColor;
         result = 31 * result + this.outlineColor;
         result = 31 * result + Float.floatToIntBits(this.topLight);
         return 31 * result + Float.floatToIntBits(this.borderDark);
      }
   }

   private static final class KeySpriteKey {
      final int width;
      final int height;
      final KeyboardKeyShape shape;
      final int fillColor;
      final int textColor;
      final int outlineColor;
      final float topLight;
      final float borderDark;

      KeySpriteKey(int width, int height, KeyboardKeyShape shape, KeyboardRenderer.KeySpriteVariant variant) {
         this(width, height, shape, KeyboardRenderer.KeyRenderStyle.standard(variant));
      }

      KeySpriteKey(int width, int height, KeyboardKeyShape shape, KeyboardRenderer.KeyRenderStyle style) {
         this.width = width;
         this.height = height;
         this.shape = shape == null ? KeyboardKeyShape.rectangle() : shape;
         this.fillColor = style.fillColor();
         this.textColor = style.textColor();
         this.outlineColor = style.outlineColor();
         this.topLight = style.topLight();
         this.borderDark = style.borderDark();
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardRenderer.KeySpriteKey that)
               ? false
               : this.width == that.width
                  && this.height == that.height
                  && this.shape.equals(that.shape)
                  && this.fillColor == that.fillColor
                  && this.textColor == that.textColor
                  && this.outlineColor == that.outlineColor
                  && Float.floatToIntBits(this.topLight) == Float.floatToIntBits(that.topLight)
                  && Float.floatToIntBits(this.borderDark) == Float.floatToIntBits(that.borderDark);
         }
      }

      @Override
      public int hashCode() {
         int result = this.width;
         result = 31 * result + this.height;
         result = 31 * result + this.shape.hashCode();
         result = 31 * result + this.fillColor;
         result = 31 * result + this.textColor;
         result = 31 * result + this.outlineColor;
         result = 31 * result + Float.floatToIntBits(this.topLight);
         return 31 * result + Float.floatToIntBits(this.borderDark);
      }
   }

   static enum KeySpriteVariant {
      ASSIGNED(0.15F, 0.56F),
      UNUSED(0.13F, 0.5F),
      HIDDEN(0.09F, 0.46F),
      ASSIGNED_HOVER(0.2F, 0.52F),
      UNUSED_HOVER(0.18F, 0.46F),
      HIDDEN_HOVER(0.14F, 0.42F);

      final float topLight;
      final float borderDark;

      private KeySpriteVariant(float topLight, float borderDark) {
         this.topLight = topLight;
         this.borderDark = borderDark;
      }

      int fillColor() {
         return switch (this) {
            case ASSIGNED -> KeyVisualStyle.keyAssignedFill();
            case UNUSED -> KeyVisualStyle.keyUnusedFill();
            case HIDDEN -> KeyVisualStyle.keyHiddenFill();
            case ASSIGNED_HOVER -> OverlayRenderHelper.lighten(KeyVisualStyle.keyAssignedFill(), 0.22F);
            case UNUSED_HOVER -> OverlayRenderHelper.lighten(KeyVisualStyle.keyUnusedFill(), 0.22F);
            case HIDDEN_HOVER -> OverlayRenderHelper.lighten(KeyVisualStyle.keyHiddenFill(), 0.22F);
         };
      }

      int textColor() {
         return switch (this) {
            case ASSIGNED, ASSIGNED_HOVER -> KeyVisualStyle.keyAssignedText();
            case UNUSED, UNUSED_HOVER -> KeyVisualStyle.keyUnusedText();
            case HIDDEN, HIDDEN_HOVER -> KeyVisualStyle.keyHiddenText();
         };
      }
   }

   static enum LegendKeyType {
      ASSIGNED,
      UNUSED,
      HIDDEN_BINDING;
   }

   private static final class MouseSpriteKey {
      final int width;
      final int height;
      final int borderColor;
      final int bodyFill;
      final int lmbFill;
      final int rmbFill;
      final int mmbFill;
      final int mb4Fill;
      final int mb5Fill;

      MouseSpriteKey(int width, int height, int borderColor, int bodyFill, int lmbFill, int rmbFill, int mmbFill, int mb4Fill, int mb5Fill) {
         this.width = width;
         this.height = height;
         this.borderColor = borderColor;
         this.bodyFill = bodyFill;
         this.lmbFill = lmbFill;
         this.rmbFill = rmbFill;
         this.mmbFill = mmbFill;
         this.mb4Fill = mb4Fill;
         this.mb5Fill = mb5Fill;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardRenderer.MouseSpriteKey that)
               ? false
               : this.width == that.width
                  && this.height == that.height
                  && this.borderColor == that.borderColor
                  && this.bodyFill == that.bodyFill
                  && this.lmbFill == that.lmbFill
                  && this.rmbFill == that.rmbFill
                  && this.mmbFill == that.mmbFill
                  && this.mb4Fill == that.mb4Fill
                  && this.mb5Fill == that.mb5Fill;
         }
      }

      @Override
      public int hashCode() {
         int h = this.width;
         h = 31 * h + this.height;
         h = 31 * h + this.borderColor;
         h = 31 * h + this.bodyFill;
         h = 31 * h + this.lmbFill;
         h = 31 * h + this.rmbFill;
         h = 31 * h + this.mmbFill;
         h = 31 * h + this.mb4Fill;
         return 31 * h + this.mb5Fill;
      }
   }

   private static final class PanelSpriteKey {
      final int width;
      final int height;
      final int fillColor;
      final boolean hovered;

      PanelSpriteKey(int width, int height, int fillColor, boolean hovered) {
         this.width = width;
         this.height = height;
         this.fillColor = fillColor;
         this.hovered = hovered;
      }

      PanelSpriteKey(int width, int height, boolean hovered) {
         this(width, height, 0, hovered);
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardRenderer.PanelSpriteKey that)
               ? false
               : this.width == that.width && this.height == that.height && this.fillColor == that.fillColor && this.hovered == that.hovered;
         }
      }

      @Override
      public int hashCode() {
         int result = this.width;
         result = 31 * result + this.height;
         result = 31 * result + this.fillColor;
         return 31 * result + (this.hovered ? 1 : 0);
      }
   }

   private static final class SpriteTexture {
      final Identifier location;
      final int width;
      final int height;
      final int originX;
      final int originY;

      SpriteTexture(Identifier location, int width, int height, int originX, int originY) {
         this.location = location;
         this.width = width;
         this.height = height;
         this.originX = originX;
         this.originY = originY;
      }

      void blit(GuiGraphicsExtractor guiGraphics, int x, int y) {
         this.blit(guiGraphics, x, y, 1.0F);
      }

      void blit(GuiGraphicsExtractor guiGraphics, int x, int y, float alpha) {
         if (!(alpha <= 0.01F)) {
            int x1 = x - this.originX;
            int y1 = y - this.originY;
            if (alpha < 0.999F) {
               int tint = OverlayRenderHelper.withAlpha(0xFFFFFFFF, alpha);
               guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.location, x1, y1, 0.0F, 0.0F, this.width, this.height, this.width, this.height, tint);
            } else {
               guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.location, x1, y1, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
            }
         }
      }
   }
}
