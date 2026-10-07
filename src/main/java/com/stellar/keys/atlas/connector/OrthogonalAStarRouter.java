package com.stellar.keys.atlas.connector;

import com.stellar.keys.atlas.box.BoxPosition;
import com.stellar.keys.atlas.layout.KeyboardKey;
import com.stellar.keys.atlas.render.OverlayRenderHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class OrthogonalAStarRouter {
   private static final int DIR_UP = 0;
   private static final int DIR_RIGHT = 1;
   private static final int DIR_DOWN = 2;
   private static final int DIR_LEFT = 3;
   private static final int DIR_NONE = 4;
   private static final int[] DX = new int[]{0, 1, 0, -1};
   private static final int[] DY = new int[]{-1, 0, 1, 0};
   private static final int BASE_MOVE_COST = 10;
   private static final int TURN_COST = 18;
   private static final int INSIDE_KEYBOARD_TURN_COST = 8;
   private static final int INSIDE_HORIZONTAL_COST = 6;
   private static final int INSIDE_VERTICAL_COST = 6;
   private static final int PARALLEL_LINE_SOFT_COST = 10;
   private static final int LINE_CROSS_COST = 1400;
   private static final int DIRECTION_PREFERENCE_COST = 8;
   private static final int START_GAP_PREFERENCE_COST = 10;
   private static final int OBSTACLE_SIDE_SOFT_LANES = 6;
   private static final int OBSTACLE_SIDE_SOFT_VERTICAL_MARGIN = 1;
   private static final int[] OBSTACLE_SIDE_SOFT_COSTS = new int[]{28, 20, 14, 10, 7, 4};
   private static final int BOX_ENTRY_GOAL_SPAN_STEPS = 1;
   private static final int MAX_EXPANDED_STATES = 120000;
   private static final int SOFT_PENALTY_CAP = 48;
   private static final int BOX_MARGIN = 1;
   private static final int BOX_TOP_EXTRA_MARGIN = 1;
   private static final int BOX_LEFT_EXTRA_MARGIN = 1;
   private final List<KeyboardKey> allKeys;
   private final List<int[]> obstacleRects;
   private final int keyboardMinX;
   private final int keyboardMaxX;
   private final int keyboardMinY;
   private final int keyboardMaxY;
   private final int pixelScale;
   private final int screenWidth;
   private final int screenHeight;
   private final BooleanSupplier cancelled;

   OrthogonalAStarRouter(
      List<KeyboardKey> allKeys,
      List<int[]> obstacleRects,
      int keyboardMinX,
      int keyboardMaxX,
      int keyboardMinY,
      int keyboardMaxY,
      int pixelScale,
      int screenWidth,
      int screenHeight,
      BooleanSupplier cancelled
   ) {
      this.allKeys = allKeys;
      this.obstacleRects = obstacleRects;
      this.keyboardMinX = keyboardMinX;
      this.keyboardMaxX = keyboardMaxX;
      this.keyboardMinY = keyboardMinY;
      this.keyboardMaxY = keyboardMaxY;
      this.pixelScale = pixelScale;
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.cancelled = cancelled;
   }

   OrthogonalAStarRouter.RouteResult route(
      KeyboardKey sourceKey,
      BoxPosition boxPos,
      boolean boxAbove,
      boolean boxBelow,
      boolean boxLeft,
      boolean boxRight,
      int nudgeX,
      int nudgeY,
      List<BoxPosition> otherBoxes,
      List<int[]> drawnSegments,
      int preferredGapStartX,
      boolean debugMode
   ) {
      if (this.pixelScale > 1) {
         return this.routeInLogicalSpace(
            sourceKey, boxPos, boxAbove, boxBelow, boxLeft, boxRight, nudgeX, nudgeY, otherBoxes, drawnSegments, preferredGapStartX, debugMode
         );
      } else {
         int step = Math.max(1, this.pixelScale);
         List<OrthogonalAStarRouter.GridPoint> startAnchors = this.buildStartAnchors(
            sourceKey, boxPos, boxAbove, boxBelow, boxLeft, boxRight, step, preferredGapStartX
         );
         int goalMinX = boxPos.boxX + nudgeX;
         int goalMinY = boxPos.boxY + nudgeY;
         int goalMaxX = goalMinX + boxPos.boxWidth;
         int goalMaxY = goalMinY + boxPos.boxHeight;
         int sourceCenterX = sourceKey.x + sourceKey.width / 2;
         int goalCenterX = boxPos.lineEndX + nudgeX;
         int discouragedHorizontalDirection = 4;
         if (goalCenterX < sourceCenterX - step) {
            discouragedHorizontalDirection = 3;
         } else if (goalCenterX > sourceCenterX + step) {
            discouragedHorizontalDirection = 1;
         }

         int preferredStartX = -1;
         if (boxAbove || boxBelow) {
            preferredStartX = preferredGapStartX >= 0 ? preferredGapStartX : clamp(boxPos.lineEndX + nudgeX, sourceKey.x, sourceKey.x + sourceKey.width);
         }

         OrthogonalAStarRouter.RouteGrid grid = this.buildGrid(
            sourceKey,
            boxPos,
            nudgeX,
            nudgeY,
            otherBoxes,
            drawnSegments,
            step,
            goalMinX,
            goalMinY,
            goalMaxX,
            goalMaxY,
            discouragedHorizontalDirection,
            boxAbove,
            boxBelow,
            boxLeft,
            boxRight,
            goalCenterX,
            boxPos.lineEndY + nudgeY,
            true
         );
         OrthogonalAStarRouter.PathCandidate strictBest = this.findBestPath(
            grid,
            startAnchors,
            boxAbove,
            boxBelow,
            boxLeft,
            boxRight,
            false,
            discouragedHorizontalDirection,
            preferredStartX,
            goalMinX,
            goalMinY,
            goalMaxX,
            goalMaxY
         );
         boolean fallbackUsed = false;
         boolean fullBorderGoalFallback = false;
         OrthogonalAStarRouter.PathCandidate best = strictBest;
         if (strictBest == null) {
            fallbackUsed = true;
            best = this.findBestPath(
               grid,
               startAnchors,
               boxAbove,
               boxBelow,
               boxLeft,
               boxRight,
               true,
               discouragedHorizontalDirection,
               preferredStartX,
               goalMinX,
               goalMinY,
               goalMaxX,
               goalMaxY
            );
         }

         if (best == null) {
            fullBorderGoalFallback = true;
            grid = this.buildGrid(
               sourceKey,
               boxPos,
               nudgeX,
               nudgeY,
               otherBoxes,
               drawnSegments,
               step,
               goalMinX,
               goalMinY,
               goalMaxX,
               goalMaxY,
               discouragedHorizontalDirection,
               boxAbove,
               boxBelow,
               boxLeft,
               boxRight,
               goalCenterX,
               boxPos.lineEndY + nudgeY,
               false
            );
            fallbackUsed = false;
            best = this.findBestPath(
               grid,
               startAnchors,
               boxAbove,
               boxBelow,
               boxLeft,
               boxRight,
               false,
               discouragedHorizontalDirection,
               preferredStartX,
               goalMinX,
               goalMinY,
               goalMaxX,
               goalMaxY
            );
            if (best == null) {
               fallbackUsed = true;
               best = this.findBestPath(
                  grid,
                  startAnchors,
                  boxAbove,
                  boxBelow,
                  boxLeft,
                  boxRight,
                  true,
                  discouragedHorizontalDirection,
                  preferredStartX,
                  goalMinX,
                  goalMinY,
                  goalMaxX,
                  goalMaxY
               );
            }
         }

         if (best == null) {
            OrthogonalAStarRouter.GridPoint start = startAnchors.isEmpty()
               ? new OrthogonalAStarRouter.GridPoint(sourceKey.x + sourceKey.width / 2, sourceKey.y + sourceKey.height / 2)
               : startAnchors.get(0);
            OrthogonalAStarRouter.GridPoint end = new OrthogonalAStarRouter.GridPoint(boxPos.lineEndX + nudgeX, boxPos.lineEndY + nudgeY);
            int[] directFallback = this.buildDirectFallback(start, end, boxAbove || boxBelow);
            String routeDebug = debugMode
               ? "algo=orthogonal-a-star step=" + step + " mode=direct-fallback start=" + pointString(start.x, start.y) + " end=" + pointString(end.x, end.y)
               : "";
            return new OrthogonalAStarRouter.RouteResult(directFallback, "astar-direct-fallback", routeDebug);
         } else {
            int[] finalWaypoints = this.trimTerminalAttachment(best.waypoints, boxPos, nudgeX, nudgeY, boxAbove, boxBelow, boxLeft, boxRight, step);
            String routeMode = fallbackUsed ? "astar-fallback-line-cross" : "astar-strict";
            String routeDebug = "";
            if (debugMode) {
               routeDebug = this.buildDebugString(step, startAnchors.size(), best, fallbackUsed, grid, finalWaypoints)
                  + " goal="
                  + (fullBorderGoalFallback ? "full-border" : "preferred-entry");
            }

            return new OrthogonalAStarRouter.RouteResult(finalWaypoints, routeMode, routeDebug);
         }
      }
   }

   private OrthogonalAStarRouter.RouteResult routeInLogicalSpace(
      KeyboardKey sourceKey,
      BoxPosition boxPos,
      boolean boxAbove,
      boolean boxBelow,
      boolean boxLeft,
      boolean boxRight,
      int nudgeX,
      int nudgeY,
      List<BoxPosition> otherBoxes,
      List<int[]> drawnSegments,
      int preferredGapStartX,
      boolean debugMode
   ) {
      OrthogonalAStarRouter logicalRouter = new OrthogonalAStarRouter(
         this.normalizeKeys(this.allKeys),
         this.normalizeRects(this.obstacleRects),
         this.unscaleX(this.keyboardMinX),
         this.unscaleX(this.keyboardMaxX),
         this.unscaleY(this.keyboardMinY),
         this.unscaleY(this.keyboardMaxY),
         1,
         this.screenWidth,
         this.screenHeight,
         this.cancelled
      );
      OrthogonalAStarRouter.RouteResult logicalRoute = logicalRouter.route(
         this.normalizeKey(sourceKey),
         this.normalizeBoxPosition(boxPos),
         boxAbove,
         boxBelow,
         boxLeft,
         boxRight,
         this.normalizeDelta(nudgeX),
         this.normalizeDelta(nudgeY),
         this.normalizeBoxes(otherBoxes),
         this.normalizeSegments(drawnSegments),
         preferredGapStartX >= 0 ? this.unscaleX(preferredGapStartX) : -1,
         debugMode
      );
      String routeDebug = debugMode ? "algo=orthogonal-a-star logical-step=1 scale=" + this.pixelScale : "";
      return new OrthogonalAStarRouter.RouteResult(this.scaleWaypoints(logicalRoute.waypoints), logicalRoute.routeMode, routeDebug);
   }

   private String buildDebugString(
      int step,
      int startAnchorCount,
      OrthogonalAStarRouter.PathCandidate candidate,
      boolean fallbackUsed,
      OrthogonalAStarRouter.RouteGrid grid,
      int[] displayWaypoints
   ) {
      StringBuilder debug = new StringBuilder()
         .append("algo=orthogonal-a-star")
         .append(" step=")
         .append(step)
         .append(" mode=")
         .append(fallbackUsed ? "line-cross-fallback" : "strict")
         .append(" anchors=")
         .append(startAnchorCount)
         .append("x*")
         .append(" start=")
         .append(pointString(candidate.start.x, candidate.start.y))
         .append(" end=")
         .append(pointString(displayWaypoints[displayWaypoints.length - 2], displayWaypoints[displayWaypoints.length - 1]))
         .append(" explored=")
         .append(candidate.expandedStates)
         .append(" turns=")
         .append(Math.max(0, displayWaypoints.length / 2 - 2))
         .append(" lineCross=")
         .append(candidate.lineCrossings)
         .append(" softLine=")
         .append(candidate.softLinePenalty)
         .append(" cost=")
         .append(candidate.totalCost)
         .append(" pathLen=")
         .append(OverlayRenderHelper.polylineLength(displayWaypoints))
         .append(" bounds=")
         .append(grid.originX)
         .append(',')
         .append(grid.originY)
         .append(' ')
         .append(grid.maxX())
         .append('x')
         .append(grid.maxY());
      int displayEndX = displayWaypoints[displayWaypoints.length - 2];
      int displayEndY = displayWaypoints[displayWaypoints.length - 1];
      if (candidate.end.x != displayEndX || candidate.end.y != displayEndY) {
         debug.append(" searchEnd=").append(pointString(candidate.end.x, candidate.end.y));
      }

      return debug.toString();
   }

   private int horizontalBoxGap(KeyboardKey sourceKey, BoxPosition boxPos, boolean boxLeft, boolean boxRight) {
      if (boxLeft) {
         return Math.max(0, sourceKey.x - (boxPos.boxX + boxPos.boxWidth));
      } else {
         return boxRight ? Math.max(0, boxPos.boxX - (sourceKey.x + sourceKey.width)) : 0;
      }
   }

   private int verticalBoxGap(KeyboardKey sourceKey, BoxPosition boxPos, boolean boxAbove, boolean boxBelow) {
      if (boxAbove) {
         return Math.max(0, sourceKey.y - (boxPos.boxY + boxPos.boxHeight));
      } else {
         return boxBelow ? Math.max(0, boxPos.boxY - (sourceKey.y + sourceKey.height)) : 0;
      }
   }

   private List<KeyboardKey> normalizeKeys(List<KeyboardKey> keys) {
      List<KeyboardKey> normalized = new ArrayList<>(keys.size());

      for (KeyboardKey key : keys) {
         normalized.add(this.normalizeKey(key));
      }

      return normalized;
   }

   private KeyboardKey normalizeKey(KeyboardKey key) {
      int x = this.unscaleX(key.x);
      int y = this.unscaleY(key.y);
      int width = Math.max(1, this.unscaleX(key.x + key.width) - x);
      int height = Math.max(1, this.unscaleY(key.y + key.height) - y);
      return new KeyboardKey(x, y, width, height, key.glfwKey, key.label, key.isDefault, key.isMouseKey);
   }

   private List<int[]> normalizeRects(List<int[]> rects) {
      List<int[]> normalized = new ArrayList<>(rects.size());

      for (int[] rect : rects) {
         normalized.add(new int[]{this.unscaleX(rect[0]), this.unscaleY(rect[1]), this.unscaleX(rect[2]), this.unscaleY(rect[3])});
      }

      return normalized;
   }

   private BoxPosition normalizeBoxPosition(BoxPosition boxPos) {
      int boxX = this.unscaleX(boxPos.boxX);
      int boxY = this.unscaleY(boxPos.boxY);
      int boxWidth = Math.max(1, this.unscaleX(boxPos.boxX + boxPos.boxWidth) - boxX);
      int boxHeight = Math.max(1, this.unscaleY(boxPos.boxY + boxPos.boxHeight) - boxY);
      return new BoxPosition(boxX, boxY, this.unscaleX(boxPos.lineEndX), this.unscaleY(boxPos.lineEndY), boxWidth, boxHeight);
   }

   private List<BoxPosition> normalizeBoxes(List<BoxPosition> boxes) {
      List<BoxPosition> normalized = new ArrayList<>(boxes.size());

      for (BoxPosition box : boxes) {
         normalized.add(this.normalizeBoxPosition(box));
      }

      return normalized;
   }

   private List<int[]> normalizeSegments(List<int[]> segments) {
      List<int[]> normalized = new ArrayList<>(segments.size());

      for (int[] segment : segments) {
         normalized.add(new int[]{this.unscaleX(segment[0]), this.unscaleY(segment[1]), this.unscaleX(segment[2]), this.unscaleY(segment[3])});
      }

      return normalized;
   }

   private int[] scaleWaypoints(int[] logicalWaypoints) {
      int[] scaled = new int[logicalWaypoints.length];

      for (int index = 0; index + 1 < logicalWaypoints.length; index += 2) {
         scaled[index] = OverlayRenderHelper.scaleX(logicalWaypoints[index], this.screenWidth / 2, this.pixelScale);
         scaled[index + 1] = OverlayRenderHelper.scaleY(logicalWaypoints[index + 1], this.screenHeight / 2, this.pixelScale);
      }

      return scaled;
   }

   private int unscaleX(int x) {
      return this.screenWidth / 2 + Math.round((float)(x - this.screenWidth / 2) / (float)this.pixelScale);
   }

   private int unscaleY(int y) {
      return this.screenHeight / 2 + Math.round((float)(y - this.screenHeight / 2) / (float)this.pixelScale);
   }

   private int normalizeDelta(int delta) {
      return Math.round((float)delta / (float)this.pixelScale);
   }

   private List<OrthogonalAStarRouter.GridPoint> buildStartAnchors(
      KeyboardKey sourceKey, BoxPosition boxPos, boolean boxAbove, boolean boxBelow, boolean boxLeft, boolean boxRight, int step, int preferredGapStartX
   ) {
      if ((boxAbove || boxBelow) && (boxLeft || boxRight)) {
         int horizontalGap = this.horizontalBoxGap(sourceKey, boxPos, boxLeft, boxRight);
         int verticalGap = this.verticalBoxGap(sourceKey, boxPos, boxAbove, boxBelow);
         if (horizontalGap > verticalGap) {
            int x = boxLeft ? sourceKey.x : sourceKey.x + sourceKey.width;
            int preferredY = clamp(boxPos.lineEndY, sourceKey.y, sourceKey.y + sourceKey.height);
            return this.buildAxisAnchors(preferredY, sourceKey.y, sourceKey.y + sourceKey.height, x, false, step, -1);
         }
      }

      if (!boxAbove && !boxBelow) {
         int x = boxLeft ? sourceKey.x : sourceKey.x + sourceKey.width;
         int preferredY = clamp(boxPos.lineEndY, sourceKey.y, sourceKey.y + sourceKey.height);
         return this.buildAxisAnchors(preferredY, sourceKey.y, sourceKey.y + sourceKey.height, x, false, step, -1);
      } else {
         int y = boxAbove ? sourceKey.y : sourceKey.y + sourceKey.height;
         int preferredX = clamp(boxPos.lineEndX, sourceKey.x, sourceKey.x + sourceKey.width);
         return this.buildAxisAnchors(preferredX, sourceKey.x, sourceKey.x + sourceKey.width, y, true, step, preferredGapStartX);
      }
   }

   private List<OrthogonalAStarRouter.GridPoint> buildAxisAnchors(
      int preferred, int min, int max, int fixed, boolean horizontalEdge, int step, int extraPreferred
   ) {
      Set<Long> unique = new LinkedHashSet<>();
      if (extraPreferred >= min && extraPreferred <= max) {
         this.addAnchor(unique, extraPreferred, fixed, horizontalEdge, step);
      }

      this.addAnchor(unique, preferred, fixed, horizontalEdge, step);
      this.addAnchor(unique, (min + max) / 2, fixed, horizontalEdge, step);
      this.addAnchor(unique, min, fixed, horizontalEdge, step);
      this.addAnchor(unique, max, fixed, horizontalEdge, step);
      if (max - min >= step * 4) {
         this.addAnchor(unique, min + (max - min) / 3, fixed, horizontalEdge, step);
         this.addAnchor(unique, max - (max - min) / 3, fixed, horizontalEdge, step);
      }

      List<OrthogonalAStarRouter.GridPoint> anchors = new ArrayList<>(unique.size());

      for (long packed : unique) {
         anchors.add(unpackPoint(packed));
      }

      return anchors;
   }

   private void addAnchor(Set<Long> unique, int varying, int fixed, boolean horizontalEdge, int step) {
      int alignedVarying = alignNearest(varying, step);
      int alignedFixed = alignNearest(fixed, step);
      int x = horizontalEdge ? alignedVarying : alignedFixed;
      int y = horizontalEdge ? alignedFixed : alignedVarying;
      unique.add(packPoint(x, y));
   }

   private OrthogonalAStarRouter.RouteGrid buildGrid(
      KeyboardKey sourceKey,
      BoxPosition ownBox,
      int nudgeX,
      int nudgeY,
      List<BoxPosition> otherBoxes,
      List<int[]> drawnSegments,
      int step,
      int goalMinX,
      int goalMinY,
      int goalMaxX,
      int goalMaxY,
      int discouragedHorizontalDirection,
      boolean boxAbove,
      boolean boxBelow,
      boolean boxLeft,
      boolean boxRight,
      int goalAnchorX,
      int goalAnchorY,
      boolean preferNarrowGoalCells
   ) {
      int margin = Math.max(6 * step, 12);
      int minX = this.keyboardMinX;
      int maxX = this.keyboardMaxX;
      int minY = this.keyboardMinY;
      int maxY = this.keyboardMaxY;

      for (KeyboardKey key : this.allKeys) {
         minX = Math.min(minX, key.x);
         maxX = Math.max(maxX, key.x + key.width);
         minY = Math.min(minY, key.y);
         maxY = Math.max(maxY, key.y + key.height);
      }

      for (BoxPosition box : otherBoxes) {
         minX = Math.min(minX, box.boxX);
         maxX = Math.max(maxX, box.boxX + box.boxWidth);
         minY = Math.min(minY, box.boxY);
         maxY = Math.max(maxY, box.boxY + box.boxHeight);
      }

      minX = Math.min(minX, ownBox.boxX + nudgeX);
      maxX = Math.max(maxX, ownBox.boxX + nudgeX + ownBox.boxWidth);
      minY = Math.min(minY, ownBox.boxY + nudgeY);
      maxY = Math.max(maxY, ownBox.boxY + nudgeY + ownBox.boxHeight);
      int originX = alignDown(minX - margin, step);
      int originY = alignDown(minY - margin, step);
      int endX = alignUp(maxX + margin, step);
      int endY = alignUp(maxY + margin, step);
      OrthogonalAStarRouter.RouteGrid grid = new OrthogonalAStarRouter.RouteGrid(originX, originY, endX, endY, step);

      for (int[] rect : this.obstacleRects) {
         this.markRect(grid.baseBlocked, grid, rect[0], rect[1], rect[2], rect[3]);
         this.markObstacleSidePenalty(grid, rect[0], rect[1], rect[2], rect[3], discouragedHorizontalDirection);
      }

      this.markRect(grid.baseBlocked, grid, goalMinX, goalMinY, goalMaxX, goalMaxY);
      int boxMargin = 1 * step;

      for (BoxPosition box : otherBoxes) {
         int blockedMinX = box.boxX - boxMargin - 1;
         int blockedMinY = box.boxY - boxMargin - 1;
         int blockedMaxX = box.boxX + box.boxWidth + boxMargin;
         int blockedMaxY = box.boxY + box.boxHeight + boxMargin;
         this.markRect(grid.baseBlocked, grid, blockedMinX, blockedMinY, blockedMaxX, blockedMaxY);
      }

      this.markBoxBorderGoals(
         grid, goalMinX, goalMinY, goalMaxX, goalMaxY, boxAbove, boxBelow, boxLeft, boxRight, goalAnchorX, goalAnchorY, preferNarrowGoalCells
      );

      for (int[] segment : drawnSegments) {
         this.markSegment(grid, segment[0], segment[1], segment[2], segment[3]);
      }

      return grid;
   }

   private void markObstacleSidePenalty(OrthogonalAStarRouter.RouteGrid grid, int minX, int minY, int maxX, int maxY, int discouragedHorizontalDirection) {
      if (discouragedHorizontalDirection == 3 || discouragedHorizontalDirection == 1) {
         int startY = alignDown(minY - grid.step * 1, grid.step);
         int endY = alignUp(maxY + grid.step * 1, grid.step);

         for (int lane = 1; lane <= 6; lane++) {
            int x = discouragedHorizontalDirection == 3 ? minX - grid.step * lane : maxX + grid.step * lane;
            int amount = OBSTACLE_SIDE_SOFT_COSTS[Math.min(lane - 1, OBSTACLE_SIDE_SOFT_COSTS.length - 1)];

            for (int y = startY; y <= endY; y += grid.step) {
               this.addSoftPenalty(grid, x, y, amount);
            }
         }
      }
   }

   private void markBoxBorderGoals(
      OrthogonalAStarRouter.RouteGrid grid,
      int minX,
      int minY,
      int maxX,
      int maxY,
      boolean boxAbove,
      boolean boxBelow,
      boolean boxLeft,
      boolean boxRight,
      int goalAnchorX,
      int goalAnchorY,
      boolean preferNarrowGoalCells
   ) {
      int step = grid.step;
      int startX = alignDown(minX, step);
      int endX = alignUp(maxX, step);
      int startY = alignDown(minY, step);
      int endY = alignUp(maxY, step);
      if (preferNarrowGoalCells) {
         if (boxAbove || boxBelow) {
            int goalY = boxAbove ? endY : startY;
            this.markPreferredGoalCells(grid, goalY, clamp(goalAnchorX, startX, endX), startX, endX, true);
            return;
         }

         if (boxLeft || boxRight) {
            int goalX = boxLeft ? endX : startX;
            this.markPreferredGoalCells(grid, goalX, clamp(goalAnchorY, startY, endY), startY, endY, false);
            return;
         }
      }

      for (int x = startX; x <= endX; x += step) {
         this.markGoalCell(grid, x, startY);
         this.markGoalCell(grid, x, endY);
      }

      for (int y = startY + step; y < endY; y += step) {
         this.markGoalCell(grid, startX, y);
         this.markGoalCell(grid, endX, y);
      }
   }

   private void markPreferredGoalCells(OrthogonalAStarRouter.RouteGrid grid, int fixed, int preferred, int min, int max, boolean horizontalEdge) {
      int step = grid.step;
      int alignedPreferred = alignNearest(preferred, step);

      for (int deltaSteps = 0; deltaSteps <= 1; deltaSteps++) {
         int delta = deltaSteps * step;
         if (deltaSteps == 0) {
            this.markPreferredGoalCell(grid, fixed, alignedPreferred, horizontalEdge, min, max);
         } else {
            this.markPreferredGoalCell(grid, fixed, alignedPreferred - delta, horizontalEdge, min, max);
            this.markPreferredGoalCell(grid, fixed, alignedPreferred + delta, horizontalEdge, min, max);
         }
      }
   }

   private void markPreferredGoalCell(OrthogonalAStarRouter.RouteGrid grid, int fixed, int varying, boolean horizontalEdge, int min, int max) {
      if (varying >= min && varying <= max) {
         if (horizontalEdge) {
            this.markGoalCell(grid, varying, fixed);
         } else {
            this.markGoalCell(grid, fixed, varying);
         }
      }
   }

   private void markGoalCell(OrthogonalAStarRouter.RouteGrid grid, int x, int y) {
      int index = grid.indexOf(x, y);
      if (index >= 0) {
         grid.isGoalCell[index] = true;
      }
   }

   private OrthogonalAStarRouter.PathCandidate findBestPath(
      OrthogonalAStarRouter.RouteGrid grid,
      List<OrthogonalAStarRouter.GridPoint> startAnchors,
      boolean boxAbove,
      boolean boxBelow,
      boolean boxLeft,
      boolean boxRight,
      boolean allowLineCrossings,
      int discouragedHorizontalDirection,
      int preferredStartX,
      int goalMinX,
      int goalMinY,
      int goalMaxX,
      int goalMaxY
   ) {
      OrthogonalAStarRouter.PathCandidate best = null;

      for (OrthogonalAStarRouter.GridPoint start : startAnchors) {
         if (this.cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            break;
         }

         OrthogonalAStarRouter.PathCandidate candidate = this.runSearch(
            grid,
            start,
            boxAbove || boxBelow,
            boxLeft || boxRight,
            allowLineCrossings,
            discouragedHorizontalDirection,
            preferredStartX,
            goalMinX,
            goalMinY,
            goalMaxX,
            goalMaxY
         );
         if (candidate != null && (best == null || candidate.isBetterThan(best))) {
            best = candidate;
            if (candidate.lineCrossings == 0 && candidate.turns == 0 && !allowLineCrossings) {
               break;
            }
         }
      }

      return best;
   }

   private OrthogonalAStarRouter.PathCandidate runSearch(
      OrthogonalAStarRouter.RouteGrid grid,
      OrthogonalAStarRouter.GridPoint start,
      boolean preferVerticalInsideKeyboard,
      boolean preferHorizontalInsideKeyboard,
      boolean allowLineCrossings,
      int discouragedHorizontalDirection,
      int preferredStartX,
      int goalMinX,
      int goalMinY,
      int goalMaxX,
      int goalMaxY
   ) {
      int startCell = grid.indexOf(start.x, start.y);
      if (startCell < 0) {
         return null;
      } else if (!allowLineCrossings && grid.lineOccupied[startCell]) {
         return null;
      } else {
         int stateCount = grid.cellCount * 5;
         int[] bestCost = new int[stateCount];
         int[] bestCrossings = new int[stateCount];
         int[] parentState = new int[stateCount];
         Arrays.fill(bestCost, Integer.MAX_VALUE);
         Arrays.fill(bestCrossings, Integer.MAX_VALUE);
         Arrays.fill(parentState, -1);
         PriorityQueue<OrthogonalAStarRouter.OpenState> open = new PriorityQueue<>((a, b) -> {
            if (a.estimatedTotalCost != b.estimatedTotalCost) {
               return Integer.compare(a.estimatedTotalCost, b.estimatedTotalCost);
            } else if (a.lineCrossings != b.lineCrossings) {
               return Integer.compare(a.lineCrossings, b.lineCrossings);
            } else {
               return a.turns != b.turns ? Integer.compare(a.turns, b.turns) : Integer.compare(a.tieBreaker, b.tieBreaker);
            }
         });
         int startH = this.heuristicToBox(start.x, start.y, goalMinX, goalMinY, goalMaxX, goalMaxY);
         int startCost = preferredStartX >= 0 ? Math.abs(start.x - preferredStartX) * 10 : 0;
         int startState = encodeState(startCell, 4, grid.cellCount);
         bestCost[startState] = startCost;
         bestCrossings[startState] = 0;
         open.add(new OrthogonalAStarRouter.OpenState(startCell, 4, startCost, startCost + startH, 0, 0, 0));
         int expansions = 0;
         int tieBreaker = 1;
         int bestEndState = -1;

         while (!open.isEmpty()) {
            if (this.cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
               return null;
            }

            OrthogonalAStarRouter.OpenState current = open.poll();
            int currentState = encodeState(current.cell, current.direction, grid.cellCount);
            if (current.totalCost == bestCost[currentState] && current.lineCrossings == bestCrossings[currentState]) {
               if (grid.isGoalCell[current.cell]) {
                  bestEndState = currentState;
                  break;
               }

               if (++expansions > 120000) {
                  return null;
               }

               int currentX = grid.cellX(current.cell);
               int currentY = grid.cellY(current.cell);

               for (int direction = 0; direction < 4; direction++) {
                  int nextX = currentX + DX[direction] * grid.step;
                  int nextY = currentY + DY[direction] * grid.step;
                  int nextCell = grid.indexOf(nextX, nextY);
                  if (nextCell >= 0 && (!grid.baseBlocked[nextCell] || grid.isGoalCell[nextCell])) {
                     int lineCrossings = current.lineCrossings;
                     if (grid.lineOccupied[nextCell] && !grid.isGoalCell[nextCell]) {
                        if (!allowLineCrossings) {
                           continue;
                        }

                        lineCrossings++;
                     }

                     int moveCost = 10 + grid.softPenalty[nextCell];
                     if (current.direction != 4 && current.direction != direction) {
                        moveCost += 18;
                        if (this.isInsideKeyboard(currentX, currentY)) {
                           moveCost += 8;
                        }
                     }

                     if (preferVerticalInsideKeyboard && this.isInsideKeyboard(nextX, nextY) && (direction == 3 || direction == 1)) {
                        moveCost += 6;
                     }

                     if (preferHorizontalInsideKeyboard && this.isInsideKeyboard(nextX, nextY) && (direction == 0 || direction == 2)) {
                        moveCost += 6;
                     }

                     if (this.isInsideKeyboard(nextX, nextY) && direction == discouragedHorizontalDirection) {
                        moveCost += 8;
                     }

                     if (lineCrossings > current.lineCrossings) {
                        moveCost += 1400;
                     }

                     int nextCost = current.totalCost + moveCost;
                     int nextState = encodeState(nextCell, direction, grid.cellCount);
                     if (nextCost <= bestCost[nextState] && (nextCost != bestCost[nextState] || lineCrossings < bestCrossings[nextState])) {
                        bestCost[nextState] = nextCost;
                        bestCrossings[nextState] = lineCrossings;
                        parentState[nextState] = currentState;
                        int turns = current.turns + (current.direction != 4 && current.direction != direction ? 1 : 0);
                        int estimate = nextCost + this.heuristicToBox(nextX, nextY, goalMinX, goalMinY, goalMaxX, goalMaxY);
                        open.add(new OrthogonalAStarRouter.OpenState(nextCell, direction, nextCost, estimate, lineCrossings, turns, tieBreaker++));
                     }
                  }
               }
            }
         }

         if (bestEndState < 0) {
            return null;
         } else {
            int[] waypoints = this.reconstructWaypoints(grid, parentState, bestEndState);
            int totalCost = bestCost[bestEndState];
            int softPenalty = this.computeSoftLinePenalty(waypoints, grid);
            int turns = Math.max(0, waypoints.length / 2 - 2);
            int endCell = decodeCell(bestEndState, grid.cellCount);
            OrthogonalAStarRouter.GridPoint end = new OrthogonalAStarRouter.GridPoint(grid.cellX(endCell), grid.cellY(endCell));
            return new OrthogonalAStarRouter.PathCandidate(start, end, waypoints, totalCost, bestCrossings[bestEndState], turns, softPenalty, expansions);
         }
      }
   }

   private int[] reconstructWaypoints(OrthogonalAStarRouter.RouteGrid grid, int[] parentState, int endState) {
      List<OrthogonalAStarRouter.GridPoint> reversed = new ArrayList<>();
      int state = endState;

      while (state >= 0) {
         int cell = decodeCell(state, grid.cellCount);
         reversed.add(new OrthogonalAStarRouter.GridPoint(grid.cellX(cell), grid.cellY(cell)));
         state = parentState[state];
      }

      List<Integer> points = new ArrayList<>();

      for (int index = reversed.size() - 1; index >= 0; index--) {
         OrthogonalAStarRouter.GridPoint point = reversed.get(index);
         this.appendPoint(points, point.x, point.y);
      }

      int[] waypoints = new int[points.size()];

      for (int index = 0; index < points.size(); index++) {
         waypoints[index] = points.get(index);
      }

      return waypoints;
   }

   private int computeSoftLinePenalty(int[] waypoints, OrthogonalAStarRouter.RouteGrid grid) {
      int penalty = 0;

      for (int index = 0; index + 1 < waypoints.length; index += 2) {
         int cell = grid.indexOf(waypoints[index], waypoints[index + 1]);
         if (cell >= 0) {
            penalty += grid.softPenalty[cell];
         }
      }

      return penalty;
   }

   private int[] trimTerminalAttachment(
      int[] waypoints, BoxPosition boxPos, int nudgeX, int nudgeY, boolean boxAbove, boolean boxBelow, boolean boxLeft, boolean boxRight, int step
   ) {
      if (waypoints.length < 6) {
         return waypoints;
      } else {
         int boxMinX = boxPos.boxX + nudgeX;
         int boxMaxX = boxMinX + boxPos.boxWidth;
         int boxMinY = boxPos.boxY + nudgeY;
         int boxMaxY = boxMinY + boxPos.boxHeight;
         int prevX = waypoints[waypoints.length - 6];
         int prevY = waypoints[waypoints.length - 5];
         int approachX = waypoints[waypoints.length - 4];
         int approachY = waypoints[waypoints.length - 3];
         int goalX = waypoints[waypoints.length - 2];
         int goalY = waypoints[waypoints.length - 1];
         if ((boxAbove || boxBelow) && approachY == goalY + (boxAbove ? step : -step) && prevY == approachY) {
            int clippedX = this.clipHorizontalApproach(prevX, approachX, boxMinX, boxMaxX);
            if (clippedX != Integer.MIN_VALUE && clippedX != goalX) {
               int[] trimmed = (int[])waypoints.clone();
               trimmed[trimmed.length - 4] = clippedX;
               trimmed[trimmed.length - 2] = clippedX;
               return this.compressCollinearWaypoints(trimmed);
            }
         }

         if ((boxLeft || boxRight) && approachX == goalX + (boxLeft ? step : -step) && prevX == approachX) {
            int clippedY = this.clipVerticalApproach(prevY, approachY, boxMinY, boxMaxY);
            if (clippedY != Integer.MIN_VALUE && clippedY != goalY) {
               int[] trimmed = (int[])waypoints.clone();
               trimmed[trimmed.length - 3] = clippedY;
               trimmed[trimmed.length - 1] = clippedY;
               return this.compressCollinearWaypoints(trimmed);
            }
         }

         return waypoints;
      }
   }

   private int clipHorizontalApproach(int startX, int endX, int minX, int maxX) {
      if (startX == endX) {
         return startX >= minX && startX <= maxX ? startX : Integer.MIN_VALUE;
      } else if (startX < endX) {
         int entry = Math.max(startX, minX);
         return entry <= endX && entry <= maxX ? entry : Integer.MIN_VALUE;
      } else {
         int entry = Math.min(startX, maxX);
         return entry >= endX && entry >= minX ? entry : Integer.MIN_VALUE;
      }
   }

   private int clipVerticalApproach(int startY, int endY, int minY, int maxY) {
      if (startY == endY) {
         return startY >= minY && startY <= maxY ? startY : Integer.MIN_VALUE;
      } else if (startY < endY) {
         int entry = Math.max(startY, minY);
         return entry <= endY && entry <= maxY ? entry : Integer.MIN_VALUE;
      } else {
         int entry = Math.min(startY, maxY);
         return entry >= endY && entry >= minY ? entry : Integer.MIN_VALUE;
      }
   }

   private int[] compressCollinearWaypoints(int[] waypoints) {
      List<Integer> compressed = new ArrayList<>();

      for (int index = 0; index + 1 < waypoints.length; index += 2) {
         this.appendPoint(compressed, waypoints[index], waypoints[index + 1]);
      }

      int[] result = new int[compressed.size()];

      for (int index = 0; index < compressed.size(); index++) {
         result[index] = compressed.get(index);
      }

      return result;
   }

   private int[] buildDirectFallback(OrthogonalAStarRouter.GridPoint start, OrthogonalAStarRouter.GridPoint end, boolean preferVertical) {
      if (start.x == end.x || start.y == end.y) {
         return new int[]{start.x, start.y, end.x, end.y};
      } else {
         return preferVertical ? new int[]{start.x, start.y, start.x, end.y, end.x, end.y} : new int[]{start.x, start.y, end.x, start.y, end.x, end.y};
      }
   }

   private void markRect(boolean[] target, OrthogonalAStarRouter.RouteGrid grid, int minX, int minY, int maxX, int maxY) {
      int startX = alignDown(minX, grid.step);
      int endX = alignUp(maxX, grid.step);
      int startY = alignDown(minY, grid.step);
      int endY = alignUp(maxY, grid.step);

      for (int y = startY; y <= endY; y += grid.step) {
         for (int x = startX; x <= endX; x += grid.step) {
            int index = grid.indexOf(x, y);
            if (index >= 0) {
               target[index] = true;
            }
         }
      }
   }

   private void markSegment(OrthogonalAStarRouter.RouteGrid grid, int x1, int y1, int x2, int y2) {
      if (x1 == x2) {
         int minY = Math.min(y1, y2);
         int maxY = Math.max(y1, y2);

         for (int y = alignDown(minY, grid.step); y <= alignUp(maxY, grid.step); y += grid.step) {
            this.markLineCell(grid, x1, y);
            this.addSoftPenalty(grid, x1 - grid.step, y, 10);
            this.addSoftPenalty(grid, x1 + grid.step, y, 10);
         }
      } else {
         int minX = Math.min(x1, x2);
         int maxX = Math.max(x1, x2);

         for (int x = alignDown(minX, grid.step); x <= alignUp(maxX, grid.step); x += grid.step) {
            this.markLineCell(grid, x, y1);
            this.addSoftPenalty(grid, x, y1 - grid.step, 10);
            this.addSoftPenalty(grid, x, y1 + grid.step, 10);
         }
      }
   }

   private void markLineCell(OrthogonalAStarRouter.RouteGrid grid, int x, int y) {
      int index = grid.indexOf(x, y);
      if (index >= 0) {
         grid.lineOccupied[index] = true;
      }
   }

   private void addSoftPenalty(OrthogonalAStarRouter.RouteGrid grid, int x, int y, int amount) {
      int index = grid.indexOf(x, y);
      if (index >= 0 && !grid.lineOccupied[index]) {
         grid.softPenalty[index] = Math.min(48, grid.softPenalty[index] + amount);
      }
   }

   private boolean isInsideKeyboard(int x, int y) {
      return x >= this.keyboardMinX && x <= this.keyboardMaxX && y >= this.keyboardMinY && y <= this.keyboardMaxY;
   }

   private int heuristicToBox(int px, int py, int boxMinX, int boxMinY, int boxMaxX, int boxMaxY) {
      int dx = Math.max(0, Math.max(boxMinX - px, px - boxMaxX));
      int dy = Math.max(0, Math.max(boxMinY - py, py - boxMaxY));
      return (dx + dy) * 10;
   }

   private void appendPoint(List<Integer> points, int x, int y) {
      int size = points.size();
      if (size >= 2) {
         int lastX = points.get(size - 2);
         int lastY = points.get(size - 1);
         if (lastX == x && lastY == y) {
            return;
         }
      }

      if (size >= 4) {
         int prevX = points.get(size - 4);
         int prevY = points.get(size - 3);
         int lastX = points.get(size - 2);
         int lastY = points.get(size - 1);
         if (prevX == lastX && lastX == x || prevY == lastY && lastY == y) {
            points.set(size - 2, x);
            points.set(size - 1, y);
            return;
         }
      }

      points.add(x);
      points.add(y);
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private static int alignDown(int value, int step) {
      return Math.floorDiv(value, step) * step;
   }

   private static int alignUp(int value, int step) {
      return Math.floorDiv(value + step - 1, step) * step;
   }

   private static int alignNearest(int value, int step) {
      return Math.round((float)value / (float)step) * step;
   }

   private static int encodeState(int cell, int direction, int cellCount) {
      return direction * cellCount + cell;
   }

   private static int decodeCell(int state, int cellCount) {
      return state % cellCount;
   }

   private static long packPoint(int x, int y) {
      return (long)x << 32 ^ (long)y & 4294967295L;
   }

   private static OrthogonalAStarRouter.GridPoint unpackPoint(long packed) {
      return new OrthogonalAStarRouter.GridPoint((int)(packed >> 32), (int)packed);
   }

   private static String pointString(int x, int y) {
      return "(" + x + "," + y + ")";
   }

   private static final class GridPoint {
      final int x;
      final int y;

      GridPoint(int x, int y) {
         this.x = x;
         this.y = y;
      }
   }

   private static final class OpenState {
      final int cell;
      final int direction;
      final int totalCost;
      final int estimatedTotalCost;
      final int lineCrossings;
      final int turns;
      final int tieBreaker;

      OpenState(int cell, int direction, int totalCost, int estimatedTotalCost, int lineCrossings, int turns, int tieBreaker) {
         this.cell = cell;
         this.direction = direction;
         this.totalCost = totalCost;
         this.estimatedTotalCost = estimatedTotalCost;
         this.lineCrossings = lineCrossings;
         this.turns = turns;
         this.tieBreaker = tieBreaker;
      }
   }

   private static final class PathCandidate {
      final OrthogonalAStarRouter.GridPoint start;
      final OrthogonalAStarRouter.GridPoint end;
      final int[] waypoints;
      final int totalCost;
      final int lineCrossings;
      final int turns;
      final int softLinePenalty;
      final int expandedStates;

      PathCandidate(
         OrthogonalAStarRouter.GridPoint start,
         OrthogonalAStarRouter.GridPoint end,
         int[] waypoints,
         int totalCost,
         int lineCrossings,
         int turns,
         int softLinePenalty,
         int expandedStates
      ) {
         this.start = start;
         this.end = end;
         this.waypoints = waypoints;
         this.totalCost = totalCost;
         this.lineCrossings = lineCrossings;
         this.turns = turns;
         this.softLinePenalty = softLinePenalty;
         this.expandedStates = expandedStates;
      }

      boolean isBetterThan(OrthogonalAStarRouter.PathCandidate other) {
         if (this.lineCrossings != other.lineCrossings) {
            return this.lineCrossings < other.lineCrossings;
         } else if (this.totalCost != other.totalCost) {
            return this.totalCost < other.totalCost;
         } else {
            return this.turns != other.turns
               ? this.turns < other.turns
               : OverlayRenderHelper.polylineLength(this.waypoints) < OverlayRenderHelper.polylineLength(other.waypoints);
         }
      }
   }

   private static final class RouteGrid {
      final int originX;
      final int originY;
      final int endX;
      final int endY;
      final int step;
      final int width;
      final int height;
      final int cellCount;
      final boolean[] baseBlocked;
      final boolean[] lineOccupied;
      final boolean[] isGoalCell;
      final int[] softPenalty;

      RouteGrid(int originX, int originY, int endX, int endY, int step) {
         this.originX = originX;
         this.originY = originY;
         this.endX = endX;
         this.endY = endY;
         this.step = step;
         this.width = (endX - originX) / step + 1;
         this.height = (endY - originY) / step + 1;
         this.cellCount = this.width * this.height;
         this.baseBlocked = new boolean[this.cellCount];
         this.lineOccupied = new boolean[this.cellCount];
         this.isGoalCell = new boolean[this.cellCount];
         this.softPenalty = new int[this.cellCount];
      }

      int indexOf(int x, int y) {
         if (x >= this.originX && x <= this.endX && y >= this.originY && y <= this.endY) {
            int gridX = (x - this.originX) / this.step;
            int gridY = (y - this.originY) / this.step;
            return gridX >= 0 && gridX < this.width && gridY >= 0 && gridY < this.height ? gridY * this.width + gridX : -1;
         } else {
            return -1;
         }
      }

      int cellX(int cell) {
         return this.originX + cell % this.width * this.step;
      }

      int cellY(int cell) {
         return this.originY + cell / this.width * this.step;
      }

      int maxX() {
         return this.endX - this.originX;
      }

      int maxY() {
         return this.endY - this.originY;
      }
   }

   static final class RouteResult {
      final int[] waypoints;
      final String routeMode;
      final String routeDebug;

      RouteResult(int[] waypoints, String routeMode, String routeDebug) {
         this.waypoints = waypoints;
         this.routeMode = routeMode;
         this.routeDebug = routeDebug;
      }
   }
}
