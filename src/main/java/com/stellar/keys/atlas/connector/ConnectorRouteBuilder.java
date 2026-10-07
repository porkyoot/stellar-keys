package com.stellar.keys.atlas.connector;

import com.stellar.keys.atlas.box.BoxPosition;
import com.stellar.keys.atlas.layout.KeyPlacementOrder;
import com.stellar.keys.atlas.layout.KeyboardKey;
import com.stellar.keys.atlas.layout.KeyboardLayouts;
import com.stellar.keys.atlas.render.OverlayRenderHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;

public final class ConnectorRouteBuilder {
   private final List<KeyboardKey> allKeys;
   private final int pixelScale;
   private final boolean debugMode;
   private final BooleanSupplier cancelled;
   private final Map<Integer, List<KeyboardKey>> keyRowsByY;
   private final Map<Integer, int[]> gapXByRow;
   private final OrthogonalAStarRouter router;

   public ConnectorRouteBuilder(
      List<KeyboardKey> allKeys,
      List<KeyboardKey> obstacleKeys,
      int keyboardMinX,
      int keyboardMaxX,
      int keyboardMinY,
      int keyboardMaxY,
      int pixelScale,
      boolean debugMode,
      int screenWidth,
      int screenHeight
   ) {
      this(allKeys, obstacleKeys, keyboardMinX, keyboardMaxX, keyboardMinY, keyboardMaxY, pixelScale, debugMode, screenWidth, screenHeight, () -> false);
   }

   public ConnectorRouteBuilder(
      List<KeyboardKey> allKeys,
      List<KeyboardKey> obstacleKeys,
      int keyboardMinX,
      int keyboardMaxX,
      int keyboardMinY,
      int keyboardMaxY,
      int pixelScale,
      boolean debugMode,
      int screenWidth,
      int screenHeight,
      BooleanSupplier cancelled
   ) {
      this.allKeys = allKeys;
      this.pixelScale = pixelScale;
      this.debugMode = debugMode;
      this.cancelled = cancelled;
      List<int[]> obstacleRects = new ArrayList<>(obstacleKeys.size());

      for (KeyboardKey key : obstacleKeys) {
         obstacleRects.add(new int[]{this.keyObstacleMinX(key), key.y, this.keyObstacleMaxX(key), key.y + key.height});
      }

      Map<Integer, List<KeyboardKey>> rowMap = new HashMap<>();

      for (KeyboardKey key : allKeys) {
         rowMap.computeIfAbsent(key.y, unused -> new ArrayList<>()).add(key);
      }

      for (List<KeyboardKey> row : rowMap.values()) {
         row.sort((a, b) -> Integer.compare(a.x, b.x));
      }

      this.keyRowsByY = rowMap;
      Map<Integer, int[]> gapMap = new HashMap<>();

      for (Entry<Integer, List<KeyboardKey>> entry : rowMap.entrySet()) {
         List<KeyboardKey> row = entry.getValue();
         if (row.size() >= 2) {
            int[] gaps = new int[row.size() - 1];

            for (int index = 0; index < row.size() - 1; index++) {
               gaps[index] = row.get(index).x + row.get(index).width + 1;
            }

            gapMap.put(entry.getKey(), gaps);
         }
      }

      this.gapXByRow = gapMap;
      this.router = new OrthogonalAStarRouter(
         allKeys, obstacleRects, keyboardMinX, keyboardMaxX, keyboardMinY, keyboardMaxY, pixelScale, screenWidth, screenHeight, cancelled
      );
   }

   private int keyObstacleMinX(KeyboardKey key) {
      if (key.glfwKey == 32) {
         int center = key.x + key.width / 2;
         int halfZone = 20 * this.pixelScale;
         return center - halfZone;
      } else {
         return key.x;
      }
   }

   private int keyObstacleMaxX(KeyboardKey key) {
      if (key.glfwKey == 32) {
         int center = key.x + key.width / 2;
         int halfZone = 20 * this.pixelScale;
         return center + halfZone;
      } else {
         return key.x + key.width;
      }
   }

   public List<ConnectorGeometry> build(List<KeyboardKey> keysWithBindings, Map<KeyboardKey, BoxPosition> boxPositions) {
      if (keysWithBindings.isEmpty()) {
         return List.of();
      } else {
         List<KeyboardKey> orderedKeys = this.orderConnectorKeysCenterOut(keysWithBindings);
         Map<KeyboardKey, Integer> bindingIndexByKey = new HashMap<>();

         for (int index = 0; index < orderedKeys.size(); index++) {
            bindingIndexByKey.put(orderedKeys.get(index), index);
         }

         List<int[]> drawnSegments = new ArrayList<>();
         List<BoxPosition> allBoxes = new ArrayList<>(boxPositions.values());
         Map<BoxPosition, int[]> committedBoxRects = new HashMap<>();
         Map<KeyboardKey, ConnectorGeometry> geometryByKey = new HashMap<>();

         for (KeyboardKey button : orderedKeys) {
            if (this.cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
               break;
            }

            BoxPosition boxPos = boxPositions.get(button);
            if (boxPos != null) {
               List<BoxPosition> otherBoxes = new ArrayList<>(allBoxes.size() - 1);

               for (BoxPosition candidate : allBoxes) {
                  if (candidate != boxPos) {
                     otherBoxes.add(candidate);
                  }
               }

               int bindingIndex = bindingIndexByKey.getOrDefault(button, geometryByKey.size());
               ConnectorGeometry connectorGeometry = this.buildForKey(button, boxPos, bindingIndex, drawnSegments, committedBoxRects, otherBoxes);
               if (connectorGeometry != null) {
                  geometryByKey.put(button, connectorGeometry);
                  this.addRouteSegments(connectorGeometry.waypoints, drawnSegments);
               }
            }
         }

         List<ConnectorGeometry> geometry = new ArrayList<>(geometryByKey.size());

         for (KeyboardKey key : orderedKeys) {
            ConnectorGeometry connectorGeometry = geometryByKey.get(key);
            if (connectorGeometry != null) {
               geometry.add(connectorGeometry);
            }
         }

         return this.finalizeGeometry(geometry);
      }
   }

   public ConnectorGeometry buildForKey(
      KeyboardKey button, BoxPosition boxPos, int bindingIndex, List<int[]> drawnSegments, Map<BoxPosition, int[]> committedBoxRects
   ) {
      return this.buildForKey(button, boxPos, bindingIndex, drawnSegments, committedBoxRects, this.committedOtherBoxes(committedBoxRects, boxPos));
   }

   public void addRouteSegments(int[] waypoints, List<int[]> drawnSegments) {
      for (int index = 0; index + 3 < waypoints.length; index += 2) {
         drawnSegments.add(new int[]{waypoints[index], waypoints[index + 1], waypoints[index + 2], waypoints[index + 3]});
      }
   }

   public List<ConnectorGeometry> finalizeGeometry(List<ConnectorGeometry> geometry) {
      List<ConnectorGeometry> coloredGeometry = this.assignAlternatingColors(geometry);
      coloredGeometry.sort((a, b) -> Integer.compare(a.bindingIndex, b.bindingIndex));
      return List.copyOf(coloredGeometry);
   }

   private int[] findNonOverlappingNudge(
      BoxPosition boxPos, boolean boxAbove, boolean boxBelow, boolean boxLeft, boolean boxRight, Map<BoxPosition, int[]> committedBoxRects
   ) {
      int maxNudge = !boxAbove && !boxBelow ? 4 * this.pixelScale : 6 * this.pixelScale;
      int step = Math.max(1, this.pixelScale);

      for (int delta = 0; delta <= maxNudge; delta += step) {
         int[] attempts = delta == 0 ? new int[]{0} : new int[]{delta, -delta};

         for (int signedDelta : attempts) {
            int candidateX = !boxAbove && !boxBelow ? 0 : signedDelta;
            int candidateY = !boxLeft && !boxRight ? 0 : signedDelta;
            if (!this.nudgeOverlapsOtherBox(boxPos.boxX + candidateX, boxPos.boxY + candidateY, boxPos.boxWidth, boxPos.boxHeight, boxPos, committedBoxRects)) {
               return new int[]{candidateX, candidateY};
            }
         }
      }

      return new int[]{0, 0};
   }

   private List<ConnectorGeometry> assignAlternatingColors(List<ConnectorGeometry> geometry) {
      int proximityThreshold = Math.max(3, this.pixelScale + 2);
      boolean[] dark = new boolean[geometry.size()];

      for (int index = 0; index < geometry.size(); index++) {
         boolean needsDark = false;
         int[] waypoints = geometry.get(index).waypoints;

         for (int previous = 0; previous < index; previous++) {
            if (!dark[previous] && this.segmentsAreClose(waypoints, geometry.get(previous).waypoints, proximityThreshold)) {
               needsDark = true;
               break;
            }
         }

         dark[index] = needsDark;
      }

      List<ConnectorGeometry> result = new ArrayList<>(geometry.size());

      for (int index = 0; index < geometry.size(); index++) {
         ConnectorGeometry connectorGeometry = geometry.get(index);
         result.add(
            new ConnectorGeometry(
               connectorGeometry.bindingIndex,
               connectorGeometry.button,
               connectorGeometry.boxPos,
               connectorGeometry.boxAbove,
               connectorGeometry.boxBelow,
               connectorGeometry.boxLeft,
               connectorGeometry.boxRight,
               connectorGeometry.nudgeX,
               connectorGeometry.nudgeY,
               connectorGeometry.waypoints,
               connectorGeometry.totalLineLength,
               connectorGeometry.routeMode,
               connectorGeometry.routeDebug,
               dark[index]
            )
         );
      }

      return result;
   }

   private boolean segmentsAreClose(int[] waypointsA, int[] waypointsB, int threshold) {
      for (int aIndex = 0; aIndex + 3 < waypointsA.length; aIndex += 2) {
         int ax1 = waypointsA[aIndex];
         int ay1 = waypointsA[aIndex + 1];
         int ax2 = waypointsA[aIndex + 2];
         int ay2 = waypointsA[aIndex + 3];

         for (int bIndex = 0; bIndex + 3 < waypointsB.length; bIndex += 2) {
            int bx1 = waypointsB[bIndex];
            int by1 = waypointsB[bIndex + 1];
            int bx2 = waypointsB[bIndex + 2];
            int by2 = waypointsB[bIndex + 3];
            if (this.orthoSegmentsClose(ax1, ay1, ax2, ay2, bx1, by1, bx2, by2, threshold)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean orthoSegmentsClose(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2, int threshold) {
      if (ay1 == ay2 && by1 == by2) {
         int distance = Math.abs(ay1 - by1);
         if (distance <= threshold && this.overlapLength(ax1, ax2, bx1, bx2) > 0) {
            return true;
         }
      }

      if (ax1 == ax2 && bx1 == bx2) {
         int distance = Math.abs(ax1 - bx1);
         if (distance <= threshold && this.overlapLength(ay1, ay2, by1, by2) > 0) {
            return true;
         }
      }

      if (ax1 == ax2 && by1 == by2) {
         int verticalMinY = Math.min(ay1, ay2);
         int verticalMaxY = Math.max(ay1, ay2);
         int horizontalMinX = Math.min(bx1, bx2);
         int horizontalMaxX = Math.max(bx1, bx2);
         if (ax1 >= horizontalMinX - threshold && ax1 <= horizontalMaxX + threshold && by1 >= verticalMinY - threshold && by1 <= verticalMaxY + threshold) {
            return true;
         }
      }

      if (ay1 == ay2 && bx1 == bx2) {
         int horizontalMinX = Math.min(ax1, ax2);
         int horizontalMaxX = Math.max(ax1, ax2);
         int verticalMinY = Math.min(by1, by2);
         int verticalMaxY = Math.max(by1, by2);
         if (bx1 >= horizontalMinX - threshold && bx1 <= horizontalMaxX + threshold && ay1 >= verticalMinY - threshold && ay1 <= verticalMaxY + threshold) {
            return true;
         }
      }

      return false;
   }

   public List<KeyboardKey> orderConnectorKeysCenterOut(List<KeyboardKey> keysWithBindings) {
      List<KeyboardKey> keyboardKeys = new ArrayList<>();
      List<KeyboardKey> mouseKeys = new ArrayList<>();

      for (KeyboardKey key : keysWithBindings) {
         if (key.isMouseKey) {
            mouseKeys.add(key);
         } else {
            keyboardKeys.add(key);
         }
      }

      Map<KeyboardKey, Integer> placementPriority = KeyPlacementOrder.buildCenterColumnSweepPriority(this.allKeys, keyboardKeys);
      List<KeyboardKey> ordered = new ArrayList<>(keyboardKeys);
      ordered.sort((a, b) -> KeyPlacementOrder.compareKeyboardKeys(a, b, placementPriority));
      this.promoteKeyAheadOf(ordered, 77, 74);
      this.promoteKeyAheadOf(ordered, 44, 75);
      this.promoteKeyAheadOf(ordered, 46, 76);
      ordered.addAll(this.orderMouseKeys(mouseKeys));
      return ordered;
   }

   private void promoteKeyAheadOf(List<KeyboardKey> ordered, int keyToMoveGlfw, int keyToPrecedeGlfw) {
      int moveIndex = this.findKeyIndex(ordered, keyToMoveGlfw);
      int precedeIndex = this.findKeyIndex(ordered, keyToPrecedeGlfw);
      if (moveIndex >= 0 && precedeIndex >= 0 && moveIndex >= precedeIndex) {
         KeyboardKey keyToMove = ordered.remove(moveIndex);
         ordered.add(precedeIndex, keyToMove);
      }
   }

   private int findKeyIndex(List<KeyboardKey> ordered, int glfwKey) {
      for (int index = 0; index < ordered.size(); index++) {
         if (ordered.get(index).glfwKey == glfwKey) {
            return index;
         }
      }

      return -1;
   }

   private ConnectorGeometry buildForKey(
      KeyboardKey button,
      BoxPosition boxPos,
      int bindingIndex,
      List<int[]> drawnSegments,
      Map<BoxPosition, int[]> committedBoxRects,
      List<BoxPosition> otherBoxes
   ) {
      if (!this.cancelled.getAsBoolean() && !Thread.currentThread().isInterrupted() && boxPos != null) {
         boolean boxLeft = boxPos.boxX + boxPos.boxWidth < button.x;
         boolean boxRight = boxPos.boxX > button.x + button.width;
         boolean boxAbove = boxPos.boxY + boxPos.boxHeight < button.y;
         boolean boxBelow = boxPos.boxY > button.y + button.height;
         int[] nudge = this.findNonOverlappingNudge(boxPos, boxAbove, boxBelow, boxLeft, boxRight, committedBoxRects);
         int nudgeX = nudge[0];
         int nudgeY = nudge[1];
         committedBoxRects.put(boxPos, new int[]{boxPos.boxX + nudgeX, boxPos.boxY + nudgeY, boxPos.boxWidth, boxPos.boxHeight});
         int preferredGapStartX = this.findPreferredAdjacentRowGapStartX(button, boxAbove, boxBelow, boxLeft, boxRight, boxPos.lineEndX + nudgeX);
         OrthogonalAStarRouter.RouteResult route = this.router
            .route(button, boxPos, boxAbove, boxBelow, boxLeft, boxRight, nudgeX, nudgeY, otherBoxes, drawnSegments, preferredGapStartX, this.debugMode);
         return new ConnectorGeometry(
            bindingIndex,
            button,
            boxPos,
            boxAbove,
            boxBelow,
            boxLeft,
            boxRight,
            nudgeX,
            nudgeY,
            route.waypoints,
            OverlayRenderHelper.polylineLength(route.waypoints),
            route.routeMode,
            route.routeDebug
         );
      } else {
         return null;
      }
   }

   private List<BoxPosition> committedOtherBoxes(Map<BoxPosition, int[]> committedBoxRects, BoxPosition self) {
      List<BoxPosition> otherBoxes = new ArrayList<>(committedBoxRects.size());

      for (Entry<BoxPosition, int[]> entry : committedBoxRects.entrySet()) {
         if (entry.getKey() != self) {
            BoxPosition original = entry.getKey();
            int[] rect = entry.getValue();
            int lineOffsetX = original.lineEndX - original.boxX;
            int lineOffsetY = original.lineEndY - original.boxY;
            otherBoxes.add(new BoxPosition(rect[0], rect[1], rect[0] + lineOffsetX, rect[1] + lineOffsetY, rect[2], rect[3]));
         }
      }

      return otherBoxes;
   }

   private List<KeyboardKey> orderMouseKeys(List<KeyboardKey> mouseKeys) {
      List<KeyboardKey> ordered = new ArrayList<>(mouseKeys);
      ordered.sort((a, b) -> {
         int order = KeyPlacementOrder.mouseKeySortOrder(a.glfwKey) - KeyPlacementOrder.mouseKeySortOrder(b.glfwKey);
         return order != 0 ? order : Integer.compare(a.glfwKey, b.glfwKey);
      });
      return ordered;
   }

   private boolean nudgeOverlapsOtherBox(int x, int y, int width, int height, BoxPosition self, Map<BoxPosition, int[]> committedBoxRects) {
      for (Entry<BoxPosition, int[]> entry : committedBoxRects.entrySet()) {
         if (entry.getKey() != self) {
            int[] other = entry.getValue();
            if (x < other[0] + other[2] && x + width > other[0] && y < other[1] + other[3] && y + height > other[1]) {
               return true;
            }
         }
      }

      return false;
   }

   private int findGapInAdjacentRow(int targetX, KeyboardKey key, boolean above) {
      int adjacentY = this.findAdjacentRowY(key, above);
      if (adjacentY < 0) {
         return -1;
      } else {
         int[] gaps = this.gapXByRow.get(adjacentY);
         if (gaps == null) {
            return -1;
         } else {
            int best = -1;
            int bestDistance = Integer.MAX_VALUE;

            for (int gapX : gaps) {
               if (gapX >= key.x && gapX <= key.x + key.width) {
                  int distance = Math.abs(gapX - targetX);
                  if (distance < bestDistance) {
                     bestDistance = distance;
                     best = gapX;
                  }
               }
            }

            return best;
         }
      }
   }

   private int findPreferredAdjacentRowGapStartX(KeyboardKey key, boolean boxAbove, boolean boxBelow, boolean boxLeft, boolean boxRight, int targetX) {
      if (!boxAbove && !boxBelow) {
         return -1;
      } else {
         if (boxLeft) {
            int rightmostGap = this.findExtremeGapInAdjacentRow(key, boxAbove, true);
            if (rightmostGap >= 0) {
               return rightmostGap;
            }
         } else if (boxRight) {
            int leftmostGap = this.findExtremeGapInAdjacentRow(key, boxAbove, false);
            if (leftmostGap >= 0) {
               return leftmostGap;
            }
         }

         return this.findGapInAdjacentRow(targetX, key, boxAbove);
      }
   }

   private int findExtremeGapInAdjacentRow(KeyboardKey key, boolean above, boolean preferRightmost) {
      int adjacentY = this.findAdjacentRowY(key, above);
      if (adjacentY < 0) {
         return -1;
      } else {
         int[] gaps = this.gapXByRow.get(adjacentY);
         if (gaps != null && gaps.length != 0) {
            int best = -1;

            for (int gapX : gaps) {
               if (gapX >= key.x && gapX <= key.x + key.width && (best < 0 || preferRightmost && gapX > best || !preferRightmost && gapX < best)) {
                  best = gapX;
               }
            }

            return best;
         } else {
            return -1;
         }
      }
   }

   private int findAdjacentRowY(KeyboardKey key, boolean above) {
      int padding = KeyboardLayouts.scaledPadding(this.pixelScale);
      int adjacentY = -1;
      int bestDistance = Integer.MAX_VALUE;

      for (int rowY : this.keyRowsByY.keySet()) {
         int distance = above ? Math.abs(rowY + this.keyRowsByY.get(rowY).get(0).height + padding - key.y) : Math.abs(rowY - (key.y + key.height + padding));
         if (distance <= 1 && distance < bestDistance) {
            bestDistance = distance;
            adjacentY = rowY;
         }
      }

      return adjacentY;
   }

   private int overlapLength(int a1, int a2, int b1, int b2) {
      int low = Math.max(Math.min(a1, a2), Math.min(b1, b2));
      int high = Math.min(Math.max(a1, a2), Math.max(b1, b2));
      return high - low;
   }
}
