package com.stellar.keys.atlas.box;

import com.stellar.keys.atlas.KeyVisualStyle;
import com.stellar.keys.atlas.KeybindAtlasClientConfig;
import com.stellar.keys.atlas.layout.KeyAssignmentInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.Font;

public final class BoxTextLayoutFactory {
   public static final float ASSIGNMENT_TEXT_SCALE = 0.58F;
   public static final float ASSIGNMENT_TEXT_MIN_SCALE = 0.38F;
   public static final float SOURCE_TEXT_SCALE = 0.42F;
   public static final float SOURCE_TEXT_MIN_SCALE = 0.3F;
   public static final int SOURCE_SEPARATOR_HEIGHT = 4;
   public static final int TEXT_X_PADDING = 3;
   public static final int TEXT_Y_PADDING = 2;
   public static final int ROW_GAP = 1;
   public static final int MAX_BOX_WIDTH = 97;
   public static final int MAX_HOVER_BOX_WIDTH = 119;
   public static final int EDIT_PEN_EXTRA = 14;
   public static final int EMPTY_BOX_WIDTH = 22;
   public static final int EMPTY_BOX_HEIGHT = 12;
   public static final int MODIFIER_TEXT_COLOR = -8880504;
   private static final int DENSE_VISIBLE_BOX_THRESHOLD = 20;
   private static final int FULL_DENSE_VISIBLE_BOX_THRESHOLD = 30;
   private static final float DENSE_ASSIGNMENT_TEXT_SCALE = 0.52F;
   private static final float DENSE_ASSIGNMENT_TEXT_MIN_SCALE = 0.34F;
   private static final float DENSE_SOURCE_TEXT_SCALE = 0.38F;
   private static final float DENSE_SOURCE_TEXT_MIN_SCALE = 0.28F;
   private static final int DENSE_TEXT_Y_PADDING = 1;
   private static final int DENSE_MAX_BOX_WIDTH = 88;
   private final Font font;
   private final int pixelScale;
   private final BoxTextLayout emptyLayout;
   private final Map<String, Integer> fontWidthCache = new HashMap<>();

   public BoxTextLayoutFactory(Font font, int pixelScale) {
      this.font = font;
      this.pixelScale = pixelScale;
      this.emptyLayout = this.createEmptyLayout();
   }

   private int cachedFontWidth(String text) {
      Integer cached = this.fontWidthCache.get(text);
      if (cached != null) {
         return cached;
      } else {
         int width = this.font.width(text);
         this.fontWidthCache.put(text, width);
         return width;
      }
   }

   public BoxTextLayout create(List<KeyAssignmentInfo> assignments, boolean hovered) {
      return this.create(assignments, hovered, false, 0);
   }

   public BoxTextLayout create(List<KeyAssignmentInfo> assignments, boolean hovered, boolean editMode) {
      return this.create(assignments, hovered, editMode, 0);
   }

   public BoxTextLayout create(List<KeyAssignmentInfo> assignments, boolean hovered, boolean editMode, int visibleBindingBoxCount) {
      if (assignments != null && !assignments.isEmpty()) {
         BoxTextLayoutFactory.LayoutTuning tuning = this.resolveLayoutTuning(hovered, visibleBindingBoxCount);
         List<BoxTextLayoutFactory.RowSpec> specs = hovered ? this.buildHoveredSpecs(assignments, tuning) : this.buildCompactSpecs(assignments, tuning);
         if (specs.isEmpty()) {
            return this.emptyLayout;
         } else {
            int horizontalPadding = 6 * this.pixelScale;
            int verticalPadding = tuning.verticalPadding * this.pixelScale;
            int rowGap = tuning.rowGap * this.pixelScale;
            int extraWidth = editMode && hovered ? 14 * this.pixelScale : 0;
            int maxBoxWidth = tuning.maxBoxWidth * this.pixelScale + extraWidth;
            int measuredWidth = 2 * this.pixelScale;
            List<Float> rowScales = new ArrayList<>(specs.size());
            List<Integer> rowHeights = new ArrayList<>(specs.size());

            for (BoxTextLayoutFactory.RowSpec spec : specs) {
               if (spec.kind == BoxTextRow.Kind.SEPARATOR) {
                  rowScales.add(1.0F);
                  rowHeights.add(spec.fixedHeight * this.pixelScale);
               } else {
                  int rowWidth = this.cachedFontWidth(spec.text);
                  float rowScale = this.scaleForWidth(rowWidth, spec.baseScale, spec.minScale, maxBoxWidth - horizontalPadding);
                  rowScales.add(rowScale);
                  rowHeights.add(Math.max(1, Math.round(9.0F * rowScale)));
                  measuredWidth = Math.max(measuredWidth, Math.round((float)rowWidth * rowScale) + horizontalPadding);
               }
            }

            int boxWidth = Math.min(measuredWidth + extraWidth, maxBoxWidth);
            int totalTextHeight = 0;

            for (int i = 0; i < rowHeights.size(); i++) {
               totalTextHeight += rowHeights.get(i);
               if (i + 1 < rowHeights.size()) {
                  totalTextHeight += rowGap;
               }
            }

            int boxHeight = totalTextHeight + verticalPadding * 2;
            List<BoxTextRow> rows = new ArrayList<>(specs.size());
            int yOffset = verticalPadding;

            for (int ix = 0; ix < specs.size(); ix++) {
               BoxTextLayoutFactory.RowSpec specx = specs.get(ix);
               if (specx.kind == BoxTextRow.Kind.SEPARATOR) {
                  rows.add(BoxTextRow.separator(specx.color, yOffset, rowHeights.get(ix)));
               } else if (specx.kind == BoxTextRow.Kind.RIGHT_TEXT) {
                  rows.add(BoxTextRow.rightText(specx.text, rowScales.get(ix), specx.color, yOffset, rowHeights.get(ix)));
               } else {
                  rows.add(BoxTextRow.text(specx.text, rowScales.get(ix), specx.color, yOffset, rowHeights.get(ix)));
               }

               yOffset += rowHeights.get(ix);
               if (ix + 1 < specs.size()) {
                  yOffset += rowGap;
               }
            }

            return new BoxTextLayout(rows, new BoxDimensions(boxWidth, boxHeight));
         }
      } else {
         return this.emptyLayout;
      }
   }

   public BoxTextLayout emptyLayout() {
      return this.emptyLayout;
   }

   private BoxTextLayout createEmptyLayout() {
      return new BoxTextLayout(List.of(), new BoxDimensions(22 * this.pixelScale, 12 * this.pixelScale));
   }

   private BoxTextLayoutFactory.LayoutTuning resolveLayoutTuning(boolean hovered, int visibleBindingBoxCount) {
      if (hovered) {
         return new BoxTextLayoutFactory.LayoutTuning(0.58F, 0.38F, 0.42F, 0.3F, 2, 1, 119);
      } else {
         float denseProgress = this.denseProgress(visibleBindingBoxCount);
         return new BoxTextLayoutFactory.LayoutTuning(
            lerp(0.58F, 0.52F, denseProgress),
            lerp(0.38F, 0.34F, denseProgress),
            lerp(0.42F, 0.38F, denseProgress),
            lerp(0.3F, 0.28F, denseProgress),
            lerpInt(2, 1, denseProgress),
            1,
            lerpInt(97, 88, denseProgress)
         );
      }
   }

   private float denseProgress(int visibleBindingBoxCount) {
      if (visibleBindingBoxCount <= 20) {
         return 0.0F;
      } else {
         float range = 10.0F;
         return range <= 0.0F ? 1.0F : clamp01((float)(visibleBindingBoxCount - 20) / range);
      }
   }

   private float scaleForWidth(int textWidth, float baseScale, float minScale, int availableWidth) {
      float scaledBase = baseScale * (float)this.pixelScale;
      float scaledMin = minScale * (float)this.pixelScale;
      return textWidth > 0 && availableWidth > 0 ? Math.max(scaledMin, Math.min(scaledBase, (float)availableWidth / (float)textWidth)) : scaledBase;
   }

   private List<BoxTextLayoutFactory.RowSpec> buildCompactSpecs(List<KeyAssignmentInfo> assignments, BoxTextLayoutFactory.LayoutTuning tuning) {
      List<BoxTextLayoutFactory.RowSpec> specs = new ArrayList<>(assignments.size());
      int maxShown = Math.max(1, KeybindAtlasClientConfig.maxUnhoveredAssignments());
      int totalValid = 0;

      for (KeyAssignmentInfo a : assignments) {
         if (a.label != null && !a.label.isEmpty()) {
            totalValid++;
         }
      }

      int added = 0;

      for (KeyAssignmentInfo assignment : assignments) {
         if (assignment.label != null && !assignment.label.isEmpty()) {
            if (added >= maxShown) {
               int remaining = totalValid - maxShown;
               specs.add(BoxTextLayoutFactory.RowSpec.rightText("+" + remaining, tuning.sourceScale, tuning.sourceMinScale, KeyVisualStyle.boxSourceText()));
               break;
            }

            specs.add(BoxTextLayoutFactory.RowSpec.text(assignment.label, tuning.assignmentScale, tuning.assignmentMinScale, KeyVisualStyle.boxText()));
            added++;
         }
      }

      return specs;
   }

   private List<BoxTextLayoutFactory.RowSpec> buildHoveredSpecs(List<KeyAssignmentInfo> assignments, BoxTextLayoutFactory.LayoutTuning tuning) {
      Map<String, List<KeyAssignmentInfo>> groups = new LinkedHashMap<>();

      for (KeyAssignmentInfo assignment : assignments) {
         if (assignment.label != null && !assignment.label.isEmpty()) {
            String source = assignment.source == null ? "" : assignment.source;
            groups.computeIfAbsent(source, ignored -> new ArrayList<>()).add(assignment);
         }
      }

      List<BoxTextLayoutFactory.RowSpec> specs = new ArrayList<>(assignments.size() * 2);
      boolean addSeparatorBeforeGroup = false;

      for (Entry<String, List<KeyAssignmentInfo>> entry : groups.entrySet()) {
         if (addSeparatorBeforeGroup) {
            specs.add(BoxTextLayoutFactory.RowSpec.separator(KeyVisualStyle.boxSourceText(), 4));
         }

         String source = entry.getKey();
         if (!source.isEmpty()) {
            specs.add(BoxTextLayoutFactory.RowSpec.text(source, tuning.sourceScale, tuning.sourceMinScale, KeyVisualStyle.boxSourceText()));
         }

         for (KeyAssignmentInfo assignmentx : entry.getValue()) {
            if (assignmentx.modifierPrefix != null && !assignmentx.modifierPrefix.isEmpty()) {
               specs.add(BoxTextLayoutFactory.RowSpec.text("[" + assignmentx.modifierPrefix + "]", tuning.sourceScale, tuning.sourceMinScale, -8880504));
            }

            specs.add(BoxTextLayoutFactory.RowSpec.text(assignmentx.label, tuning.assignmentScale, tuning.assignmentMinScale, KeyVisualStyle.boxText()));
         }

         addSeparatorBeforeGroup = true;
      }

      return specs;
   }

   private static float lerp(float start, float end, float t) {
      return start + (end - start) * clamp01(t);
   }

   private static int lerpInt(int start, int end, float t) {
      return Math.round(lerp((float)start, (float)end, t));
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static final class LayoutTuning {
      final float assignmentScale;
      final float assignmentMinScale;
      final float sourceScale;
      final float sourceMinScale;
      final int verticalPadding;
      final int rowGap;
      final int maxBoxWidth;

      private LayoutTuning(
         float assignmentScale, float assignmentMinScale, float sourceScale, float sourceMinScale, int verticalPadding, int rowGap, int maxBoxWidth
      ) {
         this.assignmentScale = assignmentScale;
         this.assignmentMinScale = assignmentMinScale;
         this.sourceScale = sourceScale;
         this.sourceMinScale = sourceMinScale;
         this.verticalPadding = verticalPadding;
         this.rowGap = rowGap;
         this.maxBoxWidth = maxBoxWidth;
      }
   }

   private static final class RowSpec {
      final BoxTextRow.Kind kind;
      final String text;
      final float baseScale;
      final float minScale;
      final int color;
      final int fixedHeight;

      private RowSpec(BoxTextRow.Kind kind, String text, float baseScale, float minScale, int color, int fixedHeight) {
         this.kind = kind;
         this.text = text;
         this.baseScale = baseScale;
         this.minScale = minScale;
         this.color = color;
         this.fixedHeight = fixedHeight;
      }

      static BoxTextLayoutFactory.RowSpec text(String text, float baseScale, float minScale, int color) {
         return new BoxTextLayoutFactory.RowSpec(BoxTextRow.Kind.TEXT, text, baseScale, minScale, color, 0);
      }

      static BoxTextLayoutFactory.RowSpec rightText(String text, float baseScale, float minScale, int color) {
         return new BoxTextLayoutFactory.RowSpec(BoxTextRow.Kind.RIGHT_TEXT, text, baseScale, minScale, color, 0);
      }

      static BoxTextLayoutFactory.RowSpec separator(int color, int fixedHeight) {
         return new BoxTextLayoutFactory.RowSpec(BoxTextRow.Kind.SEPARATOR, "", 1.0F, 1.0F, color, fixedHeight);
      }
   }
}
