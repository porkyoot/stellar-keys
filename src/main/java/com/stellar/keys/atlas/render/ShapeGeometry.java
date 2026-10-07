package com.stellar.keys.atlas.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ShapeGeometry {
   private static final Map<ShapeGeometry.PanelShapeKey, List<ShapeGeometry.FillOp>> PANEL_SHAPES = new HashMap<>();
   private static final Map<ShapeGeometry.ShadedKeyShapeKey, List<ShapeGeometry.FillOp>> SHADED_KEY_SHAPES = new HashMap<>();

   private ShapeGeometry() {
   }

   static List<ShapeGeometry.FillOp> shadedKeyShape(int width, int height, int corner, int scale) {
      return SHADED_KEY_SHAPES.computeIfAbsent(new ShapeGeometry.ShadedKeyShapeKey(width, height, corner, scale), ShapeGeometry::buildShadedKeyShape);
   }

   static List<ShapeGeometry.FillOp> panelShape(int width, int height, int corner, int scale) {
      return PANEL_SHAPES.computeIfAbsent(new ShapeGeometry.PanelShapeKey(width, height, corner, scale), ShapeGeometry::buildPanelShape);
   }

   static int fillColor(ShapeGeometry.FillRole role, int baseColor, int topColor, int wallColor, int borderColor, int bottomBorderColor) {
      switch (role) {
         case FACE:
         default:
            return baseColor;
         case TOP:
            return topColor;
         case WALL:
            return wallColor;
         case BORDER:
            return borderColor;
         case BOTTOM_BORDER:
            return bottomBorderColor;
      }
   }

   private static List<ShapeGeometry.FillOp> buildPanelShape(ShapeGeometry.PanelShapeKey key) {
      List<ShapeGeometry.FillOp> ops = new ArrayList<>();
      appendRect(ops, 0, 0, key.width, key.height, ShapeGeometry.FillRole.BORDER);
      int topThickness = Math.max(1, key.scale);
      int sideThickness = Math.max(1, key.scale - 1);
      int bottomThickness = Math.max(1, key.scale);
      int faceBottom = key.height - bottomThickness;
      ShapeGeometry.RowOpMerger bodyMerger = new ShapeGeometry.RowOpMerger();

      for (int row = 0; row < key.height; row++) {
         List<ShapeGeometry.RowSegment> rowSegments = new ArrayList<>(3);
         int inset = Math.max(0, key.corner - Math.min(row, key.height - 1 - row));
         int rightX = key.width - inset;
         if (row >= faceBottom) {
            appendRowSegment(rowSegments, inset, rightX, ShapeGeometry.FillRole.WALL);
         } else {
            int leftWallEnd = inset + sideThickness;
            int rightWallStart = rightX - sideThickness;
            ShapeGeometry.FillRole faceRole = row < topThickness ? ShapeGeometry.FillRole.TOP : ShapeGeometry.FillRole.FACE;
            if (leftWallEnd < rightWallStart) {
               appendRowSegment(rowSegments, inset, leftWallEnd, ShapeGeometry.FillRole.WALL);
               appendRowSegment(rowSegments, leftWallEnd, rightWallStart, faceRole);
               appendRowSegment(rowSegments, rightWallStart, rightX, ShapeGeometry.FillRole.WALL);
            } else {
               appendRowSegment(rowSegments, inset, rightX, faceRole);
            }
         }

         bodyMerger.addRow(ops, row, rowSegments);
      }

      bodyMerger.flush(ops);
      applyBaseBottomCornerProgression(ops, key.width, key.height, Math.max(1, key.scale - 1));
      appendRect(ops, key.corner, -key.scale, key.width - key.corner, 0, ShapeGeometry.FillRole.BORDER);
      appendRect(ops, key.corner, key.height, key.width - key.corner, key.height + key.scale, ShapeGeometry.FillRole.BOTTOM_BORDER);
      appendRect(ops, -key.scale, key.corner, 0, key.height - key.corner, ShapeGeometry.FillRole.BORDER);
      appendRect(ops, key.width, key.corner, key.width + key.scale, key.height - key.corner, ShapeGeometry.FillRole.BORDER);

      for (int ci = 1; ci <= key.corner; ci++) {
         int trim = cornerTrimStep(ci, key.scale);
         appendRect(ops, key.corner - ci, -key.scale + trim, key.corner - ci + 1, 0, ShapeGeometry.FillRole.BORDER);
         appendRect(ops, -key.scale + trim, key.corner - ci, 0, key.corner - ci + 1, ShapeGeometry.FillRole.BORDER);
         appendRect(ops, key.width - key.corner + ci - 1, -key.scale + trim, key.width - key.corner + ci, 0, ShapeGeometry.FillRole.BORDER);
         appendRect(ops, key.width, key.corner - ci, key.width + key.scale - trim, key.corner - ci + 1, ShapeGeometry.FillRole.BORDER);
         appendRect(ops, key.corner - ci, key.height, key.corner - ci + 1, key.height + key.scale - trim, ShapeGeometry.FillRole.BOTTOM_BORDER);
         appendRect(ops, -key.scale + trim, key.height - key.corner + ci - 1, 0, key.height - key.corner + ci, ShapeGeometry.FillRole.BOTTOM_BORDER);
         appendRect(
            ops, key.width - key.corner + ci - 1, key.height, key.width - key.corner + ci, key.height + key.scale - trim, ShapeGeometry.FillRole.BOTTOM_BORDER
         );
         appendRect(
            ops, key.width, key.height - key.corner + ci - 1, key.width + key.scale - trim, key.height - key.corner + ci, ShapeGeometry.FillRole.BOTTOM_BORDER
         );
      }

      applyFinalBottomCornerOverrides(ops, key.width, key.height, Math.max(1, key.scale - 1));
      return List.copyOf(ops);
   }

   private static List<ShapeGeometry.FillOp> buildShadedKeyShape(ShapeGeometry.ShadedKeyShapeKey key) {
      List<ShapeGeometry.FillOp> ops = new ArrayList<>();
      appendRect(ops, 0, 0, key.width, key.height, ShapeGeometry.FillRole.BORDER);
      int sideThickness = Math.max(1, key.scale);
      int bottomThickness = Math.max(2, key.scale + 1);
      int faceBottom = key.height - bottomThickness;
      ShapeGeometry.RowOpMerger bodyMerger = new ShapeGeometry.RowOpMerger();

      for (int row = 0; row < key.height; row++) {
         List<ShapeGeometry.RowSegment> rowSegments = new ArrayList<>(3);
         int inset = Math.max(0, key.corner - Math.min(row, key.height - 1 - row));
         int rightX = key.width - inset;
         if (row >= faceBottom) {
            appendRowSegment(rowSegments, inset, rightX, ShapeGeometry.FillRole.WALL);
         } else {
            int rowsUntilBottom = faceBottom - row;
            int innerCut = rowsUntilBottom <= sideThickness ? sideThickness - rowsUntilBottom + 1 : 0;
            int leftWallEnd = inset + sideThickness + innerCut;
            int rightWallStart = rightX - sideThickness - innerCut;
            ShapeGeometry.FillRole faceRole = row < key.scale ? ShapeGeometry.FillRole.TOP : ShapeGeometry.FillRole.FACE;
            if (leftWallEnd < rightWallStart) {
               appendRowSegment(rowSegments, inset, leftWallEnd, ShapeGeometry.FillRole.WALL);
               appendRowSegment(rowSegments, leftWallEnd, rightWallStart, faceRole);
               appendRowSegment(rowSegments, rightWallStart, rightX, ShapeGeometry.FillRole.WALL);
            } else {
               appendRowSegment(rowSegments, inset, rightX, faceRole);
            }
         }

         bodyMerger.addRow(ops, row, rowSegments);
      }

      bodyMerger.flush(ops);
      applyBaseBottomCornerProgression(ops, key.width, key.height, key.scale);
      appendRect(ops, key.corner, -key.scale, key.width - key.corner, 0, ShapeGeometry.FillRole.BORDER);
      appendRect(ops, key.corner, key.height, key.width - key.corner, key.height + key.scale, ShapeGeometry.FillRole.BOTTOM_BORDER);
      appendRect(ops, -key.scale, key.corner, 0, key.height - key.corner, ShapeGeometry.FillRole.BORDER);
      appendRect(ops, key.width, key.corner, key.width + key.scale, key.height - key.corner, ShapeGeometry.FillRole.BORDER);

      for (int ci = 1; ci <= key.corner; ci++) {
         int trim = cornerTrimStep(ci, key.scale);
         appendRect(ops, key.corner - ci, -key.scale + trim, key.corner - ci + 1, 0, ShapeGeometry.FillRole.BORDER);
         appendRect(ops, -key.scale + trim, key.corner - ci, 0, key.corner - ci + 1, ShapeGeometry.FillRole.BORDER);
         appendRect(ops, key.width - key.corner + ci - 1, -key.scale + trim, key.width - key.corner + ci, 0, ShapeGeometry.FillRole.BORDER);
         appendRect(ops, key.width, key.corner - ci, key.width + key.scale - trim, key.corner - ci + 1, ShapeGeometry.FillRole.BORDER);
         appendRect(ops, key.corner - ci, key.height, key.corner - ci + 1, key.height + key.scale - trim, ShapeGeometry.FillRole.BOTTOM_BORDER);
         appendRect(ops, -key.scale + trim, key.height - key.corner + ci - 1, 0, key.height - key.corner + ci, ShapeGeometry.FillRole.BOTTOM_BORDER);
         appendRect(
            ops, key.width - key.corner + ci - 1, key.height, key.width - key.corner + ci, key.height + key.scale - trim, ShapeGeometry.FillRole.BOTTOM_BORDER
         );
         appendRect(
            ops, key.width, key.height - key.corner + ci - 1, key.width + key.scale - trim, key.height - key.corner + ci, ShapeGeometry.FillRole.BOTTOM_BORDER
         );
      }

      applyFinalBottomCornerOverrides(ops, key.width, key.height, key.scale);
      return List.copyOf(ops);
   }

   private static void appendRowSegment(List<ShapeGeometry.RowSegment> rowSegments, int x1, int x2, ShapeGeometry.FillRole role) {
      if (x1 < x2) {
         rowSegments.add(new ShapeGeometry.RowSegment(x1, x2, role));
      }
   }

   private static void appendRect(List<ShapeGeometry.FillOp> ops, int x1, int y1, int x2, int y2, ShapeGeometry.FillRole role) {
      if (x1 < x2 && y1 < y2) {
         ops.add(new ShapeGeometry.FillOp(x1, y1, x2, y2, role));
      }
   }

   private static void applyBaseBottomCornerProgression(List<ShapeGeometry.FillOp> ops, int width, int height, int scale) {
      for (int step = 0; step < scale; step++) {
         if (step > 0) {
            int rowY = height - scale + step;
            appendRect(ops, 0, rowY, step, rowY + 1, ShapeGeometry.FillRole.BOTTOM_BORDER);
            appendRect(ops, width - step, rowY, width, rowY + 1, ShapeGeometry.FillRole.BOTTOM_BORDER);
         }
      }

      if (scale > 1) {
         for (int stepx = 0; stepx < scale; stepx++) {
            int rowY = height - scale + stepx;
            appendRect(ops, stepx, rowY, stepx + 1, rowY + 1, ShapeGeometry.FillRole.WALL);
            appendRect(ops, width - stepx - 1, rowY, width - stepx, rowY + 1, ShapeGeometry.FillRole.WALL);
         }
      }
   }

   private static void applyFinalBottomCornerOverrides(List<ShapeGeometry.FillOp> ops, int width, int height, int scale) {
      if (scale > 1) {
         for (int step = 0; step < scale; step++) {
            int rowY = height - scale + step;
            appendRect(ops, 0, rowY, 1, rowY + 1, ShapeGeometry.FillRole.BOTTOM_BORDER);
            appendRect(ops, width - 1, rowY, width, rowY + 1, ShapeGeometry.FillRole.BOTTOM_BORDER);
            if (step > 0) {
               appendRect(ops, step, rowY, step + 1, rowY + 1, ShapeGeometry.FillRole.BOTTOM_BORDER);
               appendRect(ops, width - step - 1, rowY, width - step, rowY + 1, ShapeGeometry.FillRole.BOTTOM_BORDER);
            }
         }

         for (int stepx = 0; stepx < scale; stepx++) {
            int rowY = height - scale + 1 + stepx;
            appendRect(ops, stepx, rowY, stepx + 1, rowY + 1, ShapeGeometry.FillRole.BOTTOM_BORDER);
            appendRect(ops, width - stepx - 1, rowY, width - stepx, rowY + 1, ShapeGeometry.FillRole.BOTTOM_BORDER);
         }
      }
   }

   private static int cornerTrimStep(int cornerIndex, int scale) {
      if (scale <= 1) {
         return 0;
      } else {
         return scale == 2 ? cornerIndex - 1 : Math.min(scale - 1, cornerIndex - 1);
      }
   }

   static final class FillOp {
      final int x1;
      final int y1;
      final int x2;
      final int y2;
      final ShapeGeometry.FillRole role;

      FillOp(int x1, int y1, int x2, int y2, ShapeGeometry.FillRole role) {
         this.x1 = x1;
         this.y1 = y1;
         this.x2 = x2;
         this.y2 = y2;
         this.role = role;
      }
   }

   static enum FillRole {
      FACE,
      TOP,
      WALL,
      BORDER,
      BOTTOM_BORDER;
   }

   private static final class MutableFillOp {
      final int x1;
      final int x2;
      final ShapeGeometry.FillRole role;
      final int y1;
      int y2;

      MutableFillOp(int x1, int y1, int x2, int y2, ShapeGeometry.FillRole role) {
         this.x1 = x1;
         this.x2 = x2;
         this.role = role;
         this.y1 = y1;
         this.y2 = y2;
      }

      ShapeGeometry.FillOp freeze() {
         return new ShapeGeometry.FillOp(this.x1, this.y1, this.x2, this.y2, this.role);
      }
   }

   private static final class PanelShapeKey {
      final int width;
      final int height;
      final int corner;
      final int scale;

      PanelShapeKey(int width, int height, int corner, int scale) {
         this.width = width;
         this.height = height;
         this.corner = corner;
         this.scale = scale;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof ShapeGeometry.PanelShapeKey that)
               ? false
               : this.width == that.width && this.height == that.height && this.corner == that.corner && this.scale == that.scale;
         }
      }

      @Override
      public int hashCode() {
         int result = this.width;
         result = 31 * result + this.height;
         result = 31 * result + this.corner;
         return 31 * result + this.scale;
      }
   }

   private static final class RowOpMerger {
      private List<ShapeGeometry.MutableFillOp> active = List.of();

      void addRow(List<ShapeGeometry.FillOp> output, int row, List<ShapeGeometry.RowSegment> rowSegments) {
         List<ShapeGeometry.MutableFillOp> next = new ArrayList<>(rowSegments.size());
         int max = Math.max(this.active.size(), rowSegments.size());

         for (int i = 0; i < max; i++) {
            ShapeGeometry.MutableFillOp existing = i < this.active.size() ? this.active.get(i) : null;
            ShapeGeometry.RowSegment current = i < rowSegments.size() ? rowSegments.get(i) : null;
            if (existing != null
               && current != null
               && existing.x1 == current.x1
               && existing.x2 == current.x2
               && existing.role == current.role
               && existing.y2 == row) {
               existing.y2 = row + 1;
               next.add(existing);
            } else {
               if (existing != null) {
                  output.add(existing.freeze());
               }

               if (current != null) {
                  next.add(new ShapeGeometry.MutableFillOp(current.x1, row, current.x2, row + 1, current.role));
               }
            }
         }

         this.active = next;
      }

      void flush(List<ShapeGeometry.FillOp> output) {
         for (ShapeGeometry.MutableFillOp rect : this.active) {
            output.add(rect.freeze());
         }

         this.active = List.of();
      }
   }

   private static final class RowSegment {
      final int x1;
      final int x2;
      final ShapeGeometry.FillRole role;

      RowSegment(int x1, int x2, ShapeGeometry.FillRole role) {
         this.x1 = x1;
         this.x2 = x2;
         this.role = role;
      }
   }

   private static final class ShadedKeyShapeKey {
      final int width;
      final int height;
      final int corner;
      final int scale;

      ShadedKeyShapeKey(int width, int height, int corner, int scale) {
         this.width = width;
         this.height = height;
         this.corner = corner;
         this.scale = scale;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof ShapeGeometry.ShadedKeyShapeKey that)
               ? false
               : this.width == that.width && this.height == that.height && this.corner == that.corner && this.scale == that.scale;
         }
      }

      @Override
      public int hashCode() {
         int result = this.width;
         result = 31 * result + this.height;
         result = 31 * result + this.corner;
         return 31 * result + this.scale;
      }
   }
}
