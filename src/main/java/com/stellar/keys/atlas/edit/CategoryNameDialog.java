package com.stellar.keys.atlas.edit;

import com.stellar.keys.atlas.AtlasText;
import com.stellar.keys.atlas.KeyCategory;
import com.stellar.keys.atlas.KeyVisualStyle;
import com.stellar.keys.atlas.render.HoverTooltipState;
import com.stellar.keys.atlas.render.KeyboardRenderer;
import com.stellar.keys.atlas.render.OverlayRenderHelper;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class CategoryNameDialog {
   private static final int DIALOG_MIN_H = 118;
   private static final int DIALOG_W = 194;
   private static final int INPUT_H = 14;
   private static final int SWATCH_SIZE = 16;
   private static final int SWATCH_GAP = 6;
   private static final int SWATCH_COLS = 7;
   private static final int BTN_W = 52;
   private static final int BTN_H = 12;
   private static final int BTN_GAP = 6;
   private static final int PAD = 8;
   private static final int MAX_NAME_LENGTH = 24;
   private final int screenWidth;
   private final int screenHeight;
   private final int pixelScale;
   private boolean visible;
   private boolean closing;
   private long openTimeNanos;
   private long closeTimeNanos;
   private String inputText = "";
   private int selectedFillColor = 0;
   private String editingCategoryId = "";
   private int currentDialogHeight = 118;
   private final HoverTooltipState tooltipState = new HoverTooltipState();

   public CategoryNameDialog(int screenWidth, int screenHeight, int pixelScale) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.pixelScale = pixelScale;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void open(int defaultFillColor) {
      this.openInternal("", "", defaultFillColor);
   }

   public void openForEdit(KeyCategory category) {
      if (category != null) {
         this.openInternal(category.id(), category.name(), category.fillColor());
      }
   }

   public void close() {
      if (this.visible && !this.closing) {
         this.closing = true;
         this.closeTimeNanos = System.nanoTime();
      }
   }

   public String enteredName() {
      return this.inputText.trim();
   }

   public boolean isEditingCategory() {
      return !this.editingCategoryId.isBlank();
   }

   public String editingCategoryId() {
      return this.editingCategoryId;
   }

   public int selectedFillColor() {
      return this.selectedFillColor;
   }

   public boolean charTyped(char codePoint, int modifiers) {
      if (!this.visible || this.closing) {
         return false;
      } else if (Character.isISOControl(codePoint)) {
         return false;
      } else if (this.inputText.length() >= 24) {
         return true;
      } else {
         if (codePoint == '|') {
            codePoint = '/';
         }

         this.inputText = this.inputText + codePoint;
         return true;
      }
   }

   public CategoryNameDialog.Result keyPressed(
      int keyCode, int scanCode, int modifiers, List<KeyCategory.PresetColor> presetColors, Map<Integer, String> usedColorOwners
   ) {
      if (!this.visible || this.closing) {
         return CategoryNameDialog.Result.NONE;
      } else if (keyCode == 256) {
         return CategoryNameDialog.Result.CANCEL;
      } else if (keyCode != 257 && keyCode != 335) {
         if (keyCode == 259 && !this.inputText.isEmpty()) {
            this.inputText = this.inputText.substring(0, this.inputText.length() - 1);
         }

         return CategoryNameDialog.Result.NONE;
      } else {
         return this.canCreate(presetColors, usedColorOwners) ? CategoryNameDialog.Result.CREATE : CategoryNameDialog.Result.NONE;
      }
   }

   public CategoryNameDialog.Result hitTest(int mouseX, int mouseY, List<KeyCategory.PresetColor> presetColors, Map<Integer, String> usedColorOwners) {
      if (this.visible && !this.closing) {
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         int dx = this.sx(this.dialogX());
         int dy = this.sy(this.dialogY());
         int dw = 194 * this.pixelScale;
         this.currentDialogHeight = this.dialogHeight(presetColors);
         int dh = this.currentDialogHeight * this.pixelScale;

         for (int index = 0; index < presetColors.size(); index++) {
            KeyCategory.PresetColor presetColor = presetColors.get(index);
            int swatchX = this.swatchX(dx, dw, presetColors, index);
            int swatchY = this.swatchY(dy, presetColors, index);
            if (OverlayRenderHelper.isMouseOver(smx, smy, swatchX, swatchY, 16 * this.pixelScale, 16 * this.pixelScale, 0)) {
               if (!usedColorOwners.containsKey(presetColor.fillColor())) {
                  this.selectedFillColor = presetColor.fillColor();
               }

               return CategoryNameDialog.Result.NONE;
            }
         }

         int btnW = 52 * this.pixelScale;
         int btnH = 12 * this.pixelScale;
         int btnGap = 6 * this.pixelScale;
         int totalW = btnW * 2 + btnGap;
         int btnX = dx + (dw - totalW) / 2;
         int btnY = dy + dh - btnH - 8 * this.pixelScale;
         if (OverlayRenderHelper.isMouseOver(smx, smy, btnX, btnY, btnW, btnH, 0)) {
            return this.canCreate(presetColors, usedColorOwners) ? CategoryNameDialog.Result.CREATE : CategoryNameDialog.Result.NONE;
         } else {
            return OverlayRenderHelper.isMouseOver(smx, smy, btnX + btnW + btnGap, btnY, btnW, btnH, 0)
               ? CategoryNameDialog.Result.CANCEL
               : CategoryNameDialog.Result.NONE;
         }
      } else {
         return CategoryNameDialog.Result.NONE;
      }
   }

   public boolean isPointOverDialog(int mouseX, int mouseY) {
      if (this.visible && !this.closing) {
         int dx = this.sx(this.dialogX());
         int dy = this.sy(this.dialogY());
         int dw = 194 * this.pixelScale;
         int dh = this.currentDialogHeight * this.pixelScale;
         int smx = this.sx(mouseX);
         int smy = this.sy(mouseY);
         return smx >= dx && smx < dx + dw && smy >= dy && smy < dy + dh;
      } else {
         return false;
      }
   }

   public void render(
      GuiGraphicsExtractor guiGraphics,
      KeyboardRenderer renderer,
      Font font,
      int mouseX,
      int mouseY,
      List<KeyCategory.PresetColor> presetColors,
      Map<Integer, String> usedColorOwners
   ) {
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
         int dx = this.sx(this.dialogX());
         int dy = this.sy(this.dialogY());
         int dw = 194 * this.pixelScale;
         this.currentDialogHeight = this.dialogHeight(presetColors);
         int dh = this.currentDialogHeight * this.pixelScale;
         int cr = this.pixelScale;
         int overlayLeft = this.sx(0);
         int overlayTop = this.sy(0);
         int overlayRight = this.sx(this.screenWidth);
         int overlayBottom = this.sy(this.screenHeight);
         int bgAlpha = Math.round(96.0F * fadeAlpha);
         guiGraphics.fill(overlayLeft, overlayTop, overlayRight, overlayBottom, bgAlpha << 24);
         int boxFill = OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F);
         OverlayRenderHelper.blitBlurredBackground(guiGraphics, dx, dy, dw, dh, this.dialogX(), this.dialogY(), 194, this.currentDialogHeight, fadeAlpha);
         OverlayRenderHelper.fillRoundedRect(guiGraphics, dx, dy, dw, dh, cr, OverlayRenderHelper.withAlpha(boxFill, fadeAlpha * KeyVisualStyle.panelAlpha()));
         OverlayRenderHelper.drawRoundedRectOutline(
            guiGraphics, dx, dy, dw, dh, cr, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), fadeAlpha), this.pixelScale
         );
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)((double)dx + (double)dw / 2.0), (float)((double)(dy + 8 * this.pixelScale)));
         guiGraphics.pose().scale((float)((float)this.pixelScale * 0.85F), (float)((float)this.pixelScale * 0.85F));
         guiGraphics.centeredText(
            font,
            AtlasText.text(this.isEditingCategory() ? "ui.categories.edit_title" : "ui.categories.create_title"),
            0,
            0,
            OverlayRenderHelper.withAlpha(KeyVisualStyle.boxText(), fadeAlpha)
         );
         guiGraphics.pose().popMatrix();
         int inputX = dx + 8 * this.pixelScale;
         int inputY = dy + 24 * this.pixelScale;
         int inputW = dw - 16 * this.pixelScale;
         int inputH = 14 * this.pixelScale;
         guiGraphics.fill(
            inputX,
            inputY,
            inputX + inputW,
            inputY + inputH,
            OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.1F), fadeAlpha)
         );
         OverlayRenderHelper.drawRoundedRectOutline(
            guiGraphics, inputX, inputY, inputW, inputH, this.pixelScale, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), fadeAlpha), this.pixelScale
         );
         String text = this.inputText;
         int textColor = KeyVisualStyle.boxText();
         if (text.isEmpty()) {
            text = AtlasText.text("ui.categories.create_hint");
            textColor = KeyVisualStyle.boxSourceText();
         } else if ((Util.getMillis() / 500L & 1L) == 0L && !this.closing) {
            text = text + "_";
         }

         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)((float)(inputX + 4 * this.pixelScale)), (float)((float)(inputY + 3 * this.pixelScale)));
         guiGraphics.pose().scale((float)((float)this.pixelScale * 0.75F), (float)((float)this.pixelScale * 0.75F));
         guiGraphics.text(font, text, 0, 0, OverlayRenderHelper.withAlpha(textColor, fadeAlpha), false);
         guiGraphics.pose().popMatrix();
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)((float)(dx + 8 * this.pixelScale)), (float)((float)(dy + 40 * this.pixelScale)));
         guiGraphics.pose().scale((float)((float)this.pixelScale * 0.6F), (float)((float)this.pixelScale * 0.6F));
         guiGraphics.text(
            font, AtlasText.text("ui.categories.colors_title"), 0, 0, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxSourceText(), fadeAlpha), false
         );
         guiGraphics.pose().popMatrix();
         String hoveredTooltipId = null;
         List<Component> hoveredTooltipLines = null;

         for (int index = 0; index < presetColors.size(); index++) {
            KeyCategory.PresetColor presetColor = presetColors.get(index);
            int swatchX = this.swatchX(dx, dw, presetColors, index);
            int swatchY = this.swatchY(dy, presetColors, index);
            boolean used = usedColorOwners.containsKey(presetColor.fillColor());
            boolean selected = presetColor.fillColor() == this.selectedFillColor;
            boolean hovered = OverlayRenderHelper.isMouseOver(smx, smy, swatchX, swatchY, 16 * this.pixelScale, 16 * this.pixelScale, this.pixelScale);
            guiGraphics.fill(
               swatchX,
               swatchY,
               swatchX + 16 * this.pixelScale,
               swatchY + 16 * this.pixelScale,
               OverlayRenderHelper.withAlpha(presetColor.fillColor(), fadeAlpha)
            );
            int borderColor = selected ? -1 : KeyVisualStyle.boxBorder();
            if (hovered && !selected) {
               borderColor = KeyVisualStyle.boxText();
            }

            OverlayRenderHelper.drawRoundedRectOutline(
               guiGraphics,
               swatchX,
               swatchY,
               16 * this.pixelScale,
               16 * this.pixelScale,
               this.pixelScale,
               OverlayRenderHelper.withAlpha(borderColor, fadeAlpha),
               this.pixelScale
            );
            if (used) {
               guiGraphics.fill(
                  swatchX, swatchY, swatchX + 16 * this.pixelScale, swatchY + 16 * this.pixelScale, OverlayRenderHelper.withAlpha(-16777216, 0.45F * fadeAlpha)
               );
               this.drawDiagonalCross(guiGraphics, swatchX, swatchY, 16 * this.pixelScale, OverlayRenderHelper.withAlpha(borderColor, fadeAlpha));
            }

            if (hovered) {
               hoveredTooltipId = "category.color." + presetColor.id();
               hoveredTooltipLines = used
                  ? List.of(
                     Component.literal(presetColor.name()), AtlasText.translatable("ui.categories.color_used_by", usedColorOwners.get(presetColor.fillColor()))
                  )
                  : List.of(Component.literal(presetColor.name()));
            }
         }

         int btnW = 52 * this.pixelScale;
         int btnH = 12 * this.pixelScale;
         int btnGap = 6 * this.pixelScale;
         int totalBtnW = btnW * 2 + btnGap;
         int btnStartX = dx + (dw - totalBtnW) / 2;
         int btnY = dy + dh - btnH - 8 * this.pixelScale;
         boolean allowHover = !this.closing;
         boolean createHovered = allowHover && OverlayRenderHelper.isMouseOver(smx, smy, btnStartX, btnY, btnW, btnH, this.pixelScale);
         boolean cancelHovered = allowHover && OverlayRenderHelper.isMouseOver(smx, smy, btnStartX + btnW + btnGap, btnY, btnW, btnH, this.pixelScale);
         boolean allowCreate = this.canCreate(presetColors, usedColorOwners);
         renderer.drawHudButton(
            guiGraphics,
            btnStartX,
            btnY,
            btnW,
            btnH,
            AtlasText.text(this.isEditingCategory() ? "ui.button.update" : "ui.button.create"),
            createHovered && allowCreate,
            allowCreate,
            fadeAlpha
         );
         if (!allowCreate) {
            guiGraphics.fill(btnStartX, btnY, btnStartX + btnW, btnY + btnH, OverlayRenderHelper.withAlpha(-16777216, 0.45F * fadeAlpha));
         }

         renderer.drawHudButton(guiGraphics, btnStartX + btnW + btnGap, btnY, btnW, btnH, AtlasText.text("ui.button.cancel"), cancelHovered, false, fadeAlpha);
         this.tooltipState.update(hoveredTooltipId, mouseX, mouseY);
         if (hoveredTooltipId != null && hoveredTooltipLines != null) {
            this.tooltipState
               .renderIfReady(guiGraphics, font, this.screenWidth, this.screenHeight, this.pixelScale, mouseX, mouseY, hoveredTooltipId, hoveredTooltipLines);
         }

         guiGraphics.pose().popMatrix();
      }
   }

   private boolean canCreate(List<KeyCategory.PresetColor> presetColors, Map<Integer, String> usedColorOwners) {
      return !this.inputText.trim().isEmpty()
         && KeyCategory.presetColor(presetColors, this.selectedFillColor) != null
         && !usedColorOwners.containsKey(this.selectedFillColor)
         && !presetColors.isEmpty();
   }

   private void openInternal(String categoryId, String defaultName, int defaultFillColor) {
      this.inputText = defaultName == null ? "" : defaultName;
      this.selectedFillColor = defaultFillColor;
      this.editingCategoryId = categoryId == null ? "" : categoryId;
      this.currentDialogHeight = 118;
      this.visible = true;
      this.closing = false;
      this.openTimeNanos = System.nanoTime();
   }

   private int dialogHeight(List<KeyCategory.PresetColor> presetColors) {
      int rows = this.paletteRows(presetColors);
      int paletteHeight = rows * 16 + Math.max(0, rows - 1) * 6;
      int dynamicHeight = 48 + paletteHeight + 8 + 12 + 8;
      return Math.max(118, dynamicHeight);
   }

   private int paletteRows(List<KeyCategory.PresetColor> presetColors) {
      return Math.max(1, (presetColors.size() + 7 - 1) / 7);
   }

   private int swatchX(int scaledDialogX, int scaledDialogW, List<KeyCategory.PresetColor> presetColors, int index) {
      int row = index / 7;
      int col = index % 7;
      int rowLength = Math.min(7, Math.max(0, presetColors.size() - row * 7));
      int rowWidth = rowLength * 16 + Math.max(0, rowLength - 1) * 6;
      int scaledRowWidth = rowWidth * this.pixelScale;
      int scaledPaletteX = scaledDialogX + (scaledDialogW - scaledRowWidth) / 2;
      return scaledPaletteX + col * 22 * this.pixelScale;
   }

   private int swatchY(int scaledDialogY, List<KeyCategory.PresetColor> presetColors, int index) {
      int row = index / 7;
      return scaledDialogY + 48 * this.pixelScale + row * 22 * this.pixelScale;
   }

   private void drawDiagonalCross(GuiGraphicsExtractor guiGraphics, int x, int y, int size, int color) {
      int step = Math.max(1, this.pixelScale);
      int cells = Math.max(1, size / step);

      for (int cell = 0; cell < cells; cell++) {
         int px = x + cell * step;
         int pyA = y + cell * step;
         int pyB = y + size - step - cell * step;
         guiGraphics.fill(px, pyA, px + step, pyA + step, color);
         guiGraphics.fill(px, pyB, px + step, pyB + step, color);
      }
   }

   private int dialogX() {
      return (this.screenWidth - 194) / 2;
   }

   private int dialogY() {
      return (this.screenHeight - this.currentDialogHeight) / 2;
   }

   private int sx(int x) {
      return OverlayRenderHelper.scaleX(x, this.screenWidth / 2, this.pixelScale);
   }

   private int sy(int y) {
      return OverlayRenderHelper.scaleY(y, this.screenHeight / 2, this.pixelScale);
   }

   public static enum Result {
      NONE,
      CREATE,
      CANCEL;
   }
}
