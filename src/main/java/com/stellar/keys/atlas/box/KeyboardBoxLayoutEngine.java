package com.stellar.keys.atlas.box;

import com.stellar.keys.atlas.layout.KeyPlacementOrder;
import com.stellar.keys.atlas.layout.KeyboardKey;
import com.stellar.keys.atlas.render.OverlayRenderHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

public final class KeyboardBoxLayoutEngine {
   private static final int SCREEN_MARGIN = 5;
   private static final int LEFT_EDGE_THRESHOLD = 32;
   private static final int RIGHT_EDGE_THRESHOLD = 32;
   private static final int TOP_GAP = 17;
   private static final int TOP_LANE_STEP = 18;
   private static final int TOP_CENTER_KEEP_RANGE = 32;
   private static final int OVERFLOW_X = 42;
   private static final int BOTTOM_GAP = 15;
   private static final int SIDE_GAP = 10;
   private static final int CONFLICT_PAD = 8;
   private static final int MAX_BIAS_WIDTH = 72;
   private static final int MOUSE_GAP = 10;
   private static final int BOX_COLLISION_GAP = 5;
   private static final int DENSE_THRESHOLD = 20;
   private static final int DENSE_BOX_COLLISION_GAP = 5;
   private static final int BOTTOM_SPREAD_MAX_BIAS = 200;
   private static final int SPACE_RIGHT_BIAS_NUMERATOR = 1;
   private static final int SPACE_RIGHT_BIAS_DENOMINATOR = 2;
   private static final int SPACE_REGION_BOTTOM_DROP = 8;
   private static final int LOWER_ROW_SWAY_DROP_THRESHOLD = 48;
   private static final int LOWER_ROW_SWAY_DROP = 12;
   private static final int SEQUENTIAL_LINE_OVERLAP_COST = 1600;
   private static final int SEQUENTIAL_LINE_NEAR_COST = 120;
   private static final int SEQUENTIAL_LINE_NEAR_DISTANCE = 6;
   private final int screenWidth;
   private final int screenHeight;
   private final int pixelScale;

   public KeyboardBoxLayoutEngine(int screenWidth, int screenHeight, int pixelScale) {
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.pixelScale = pixelScale;
   }

   public Map<KeyboardKey, BoxPosition> calculateBoxPositions(
      List<KeyboardKey> keysWithBindings,
      List<KeyboardKey> allKeys,
      int keyboardCenterX,
      int kbMinX,
      int kbMaxX,
      int kbMinY,
      int kbMaxY,
      Function<Integer, BoxTextLayout> layoutByKey,
      int[] mouseDeviceBounds
   ) {
      List<KeyboardKey> kbKeys = new ArrayList<>();
      List<KeyboardKey> mouseKeysAll = new ArrayList<>();

      for (KeyboardKey key : keysWithBindings) {
         if (key.isMouseKey) {
            mouseKeysAll.add(key);
         } else {
            kbKeys.add(key);
         }
      }

      List<KeyboardKey> physicalKeyboardKeys = new ArrayList<>();

      for (KeyboardKey keyx : allKeys) {
         if (!keyx.isMouseKey) {
            physicalKeyboardKeys.add(keyx);
         }
      }

      Map<KeyboardKey, BoxPosition> positions = new LinkedHashMap<>();
      Map<Integer, int[]> globalPlacedBoxes = new LinkedHashMap<>();
      boolean dense = keysWithBindings.size() > 20;
      int screenCenterX = this.screenWidth / 2;
      int screenCenterY = this.screenHeight / 2;
      int iScreenMinX = OverlayRenderHelper.scaleX(0, screenCenterX, this.pixelScale);
      int iScreenMaxX = OverlayRenderHelper.scaleX(this.screenWidth, screenCenterX, this.pixelScale);
      int iScreenMinY = OverlayRenderHelper.scaleY(0, screenCenterY, this.pixelScale);
      int iScreenMaxY = OverlayRenderHelper.scaleY(this.screenHeight, screenCenterY, this.pixelScale);
      int iMargin = 5 * this.pixelScale;
      int clampMinX = iScreenMinX + iMargin;
      int clampMaxX = iScreenMaxX - iMargin;
      int clampMinY = iScreenMinY + iMargin;
      int clampMaxY = iScreenMaxY - iMargin;
      Map<Integer, List<KeyboardKey>> keysByRow = new LinkedHashMap<>();

      for (KeyboardKey keyxx : kbKeys) {
         keysByRow.computeIfAbsent(keyxx.y, ignored -> new ArrayList<>()).add(keyxx);
      }

      KeyboardKey spaceKey = null;

      for (KeyboardKey keyxx : physicalKeyboardKeys) {
         if (keyxx.glfwKey == 32) {
            spaceKey = keyxx;
            break;
         }
      }

      BoxDimensions spaceDims = spaceKey == null ? null : layoutByKey.apply(32).dimensions;
      Map<KeyboardKey, Integer> placementPriority = KeyPlacementOrder.buildCenterColumnSweepPriority(physicalKeyboardKeys, kbKeys);
      int leftEdgeThreshold = 32 * this.pixelScale;
      int rightEdgeThreshold = 32 * this.pixelScale;
      Set<KeyboardKey> forceLeftKeys = new HashSet<>();
      Set<KeyboardKey> forceRightKeys = new HashSet<>();
      Map<KeyboardKey, Integer> spillRightPreferredY = new LinkedHashMap<>();
      Map<Integer, List<KeyboardKey>> physicalRows = new LinkedHashMap<>();

      for (KeyboardKey keyxxx : physicalKeyboardKeys) {
         physicalRows.computeIfAbsent(keyxxx.y, ignored -> new ArrayList<>()).add(keyxxx);
      }

      int mainKeyboardMinY = kbMinY;
      List<Integer> physicalRowYs = new ArrayList<>(physicalRows.keySet());
      physicalRowYs.sort(Integer::compareTo);
      if (physicalRowYs.size() > 1) {
         int topRowY = physicalRowYs.get(0);
         List<KeyboardKey> topRow = physicalRows.get(topRowY);
         if (this.isFunctionRow(topRow)) {
            mainKeyboardMinY = physicalRowYs.get(1);
         }
      }

      Map<Integer, List<KeyboardKey>> physicalRowEndPair = new LinkedHashMap<>();

      for (Entry<Integer, List<KeyboardKey>> entry : physicalRows.entrySet()) {
         List<KeyboardKey> row = entry.getValue();
         if (row.size() >= 2) {
            row.sort((a, b) -> Integer.compare(a.x, b.x));
            List<KeyboardKey> pair = new ArrayList<>();
            pair.add(row.get(row.size() - 2));
            pair.add(row.get(row.size() - 1));
            physicalRowEndPair.put(entry.getKey(), pair);
         } else if (row.size() == 1) {
            List<KeyboardKey> pair = new ArrayList<>();
            pair.add(row.get(0));
            physicalRowEndPair.put(entry.getKey(), pair);
         }
      }

      Set<KeyboardKey> assignedKeySet = new HashSet<>(kbKeys);

      for (List<KeyboardKey> rowKeys : keysByRow.values()) {
         if (!rowKeys.isEmpty()) {
            KeyboardKey leftmost = rowKeys.stream().min((a, b) -> Integer.compare(a.x, b.x)).orElse(null);
            KeyboardKey rightmost = rowKeys.stream().max((a, b) -> Integer.compare(a.x + a.width, b.x + b.width)).orElse(null);
            if (leftmost != null && leftmost.x - kbMinX < leftEdgeThreshold) {
               forceLeftKeys.add(leftmost);
            }

            if (rightmost != null && kbMaxX - (rightmost.x + rightmost.width) < rightEdgeThreshold) {
               forceRightKeys.add(rightmost);
            }
         }
      }

      for (List<KeyboardKey> pair : physicalRowEndPair.values()) {
         KeyboardKey candidate = null;

         for (int i = pair.size() - 1; i >= 0; i--) {
            if (assignedKeySet.contains(pair.get(i))) {
               candidate = pair.get(i);
               break;
            }
         }

         if (candidate != null) {
            forceRightKeys.add(candidate);
         }
      }

      for (int rowY : physicalRowYs) {
         KeyboardKey primaryRight = this.findRightEdgeAssignedKey(physicalRowEndPair.get(rowY), assignedKeySet);
         if (primaryRight != null && !this.hasAssignedRightEdgeBelow(rowY, physicalRowYs, physicalRowEndPair, assignedKeySet)) {
            KeyboardKey secondaryRight = this.findNearestAssignedLeftNeighbor(physicalRows.get(rowY), primaryRight, assignedKeySet);
            if (secondaryRight != null) {
               forceRightKeys.add(secondaryRight);
               int lowerRowY = this.findNextPhysicalRowY(rowY, physicalRowYs);
               if (lowerRowY >= 0) {
                  spillRightPreferredY.put(secondaryRight, lowerRowY);
               }
            }
         }
      }

      List<KeyboardKey> topKeys = new ArrayList<>();
      List<KeyboardKey> bottomKeys = new ArrayList<>();
      List<KeyboardKey> leftKeys = new ArrayList<>();
      List<KeyboardKey> rightKeys = new ArrayList<>();

      for (KeyboardKey keyxxx : kbKeys) {
         if (keyxxx.glfwKey == 32) {
            bottomKeys.add(keyxxx);
         } else if (forceLeftKeys.contains(keyxxx)) {
            leftKeys.add(keyxxx);
         } else if (forceRightKeys.contains(keyxxx)) {
            rightKeys.add(keyxxx);
         } else {
            int keyCenterX = keyxxx.x + keyxxx.width / 2;
            int keyCenterY = keyxxx.y + keyxxx.height / 2;
            int distToTop = keyCenterY - kbMinY;
            int distToBottom = kbMaxY - keyCenterY;
            int distToLeft = keyCenterX - kbMinX;
            int distToRight = kbMaxX - keyCenterX;
            if (keyCenterX > keyboardCenterX) {
               int rightBias = (keyCenterX - keyboardCenterX) / 4;
               distToBottom = Math.max(0, distToBottom - rightBias);
            }

            KeyboardBoxLayoutEngine.BoxSide side = this.classifySide(distToTop, distToBottom, distToLeft, distToRight);
            if (mainKeyboardMinY > kbMinY && !this.isFunctionKey(keyxxx) && side == KeyboardBoxLayoutEngine.BoxSide.BOTTOM) {
               int mainKeyboardDistToTop = keyCenterY - mainKeyboardMinY;
               KeyboardBoxLayoutEngine.BoxSide mainKeyboardSide = this.classifySide(mainKeyboardDistToTop, distToBottom, distToLeft, distToRight);
               if (mainKeyboardSide != KeyboardBoxLayoutEngine.BoxSide.BOTTOM) {
                  side = mainKeyboardSide;
               }
            }

            if (side == KeyboardBoxLayoutEngine.BoxSide.TOP) {
               topKeys.add(keyxxx);
            } else if (side == KeyboardBoxLayoutEngine.BoxSide.BOTTOM) {
               bottomKeys.add(keyxxx);
            } else if (side == KeyboardBoxLayoutEngine.BoxSide.LEFT) {
               leftKeys.add(keyxxx);
            } else {
               rightKeys.add(keyxxx);
            }
         }
      }

      int topGap = 17 * this.pixelScale;
      int topLaneStep = 18 * this.pixelScale;
      if (dense) {
         topLaneStep = topLaneStep * 3 / 2;
      }

      int keyboardWidth = kbMaxX - kbMinX;
      int overflowX = 42 * this.pixelScale;
      int firstKeyRowY = kbKeys.stream().mapToInt(keyxxxx -> keyxxxx.y).min().orElse(Integer.MAX_VALUE);
      KeyboardKey firstTopRowLeftmostKey = topKeys.stream().filter(keyxxxx -> keyxxxx.y == firstKeyRowY).min((a, b) -> Integer.compare(a.x, b.x)).orElse(null);
      Map<Integer, int[]> topBoxPositions = new LinkedHashMap<>();
      int bottomBoxY = kbMaxY + 15 * this.pixelScale;
      Map<Integer, int[]> bottomBoxPositions = new LinkedHashMap<>();
      int sideGap = 10 * this.pixelScale;
      Map<Integer, int[]> leftBoxPositions = new LinkedHashMap<>();
      Map<Integer, int[]> rightBoxPositions = new LinkedHashMap<>();
      Set<KeyboardKey> topKeySet = new HashSet<>(topKeys);
      Set<KeyboardKey> bottomKeySet = new HashSet<>(bottomKeys);
      Set<KeyboardKey> leftKeySet = new HashSet<>(leftKeys);
      List<KeyboardKey> orderedKeyboardKeys = new ArrayList<>(kbKeys);
      orderedKeyboardKeys.sort((a, b) -> KeyPlacementOrder.compareKeyboardKeys(a, b, placementPriority));

      for (KeyboardKey keyxxxx : orderedKeyboardKeys) {
         if (topKeySet.contains(keyxxxx)) {
            BoxDimensions dims = layoutByKey.apply(keyxxxx.glfwKey).dimensions;
            int topBoxY = kbMinY - dims.height - topGap;
            boolean firstKeyboardRow = keyxxxx.y == firstKeyRowY;
            boolean preferLeftLimit = keyxxxx == firstTopRowLeftmostKey;
            if (firstKeyboardRow) {
               topBoxY -= topLaneStep;
            }

            int proportionalX = this.computeProportionalX(keyxxxx, kbMinX, keyboardWidth, dims.width, overflowX, kbMaxX);
            if (preferLeftLimit) {
               proportionalX -= 10 * this.pixelScale;
            }

            proportionalX = Math.max(kbMinX - overflowX, Math.min(proportionalX, kbMaxX - dims.width));
            int halfWidth = keyboardWidth / 2;
            int keyCenterXx = keyxxxx.x + keyxxxx.width / 2;
            int keyXOnKeyboard = keyxxxx.x + keyxxxx.width / 2 - kbMinX;
            boolean preferRaisedLane = keyXOnKeyboard < halfWidth
               && this.hasConflictingTopBoxOnSameLane(topBoxPositions, proportionalX, topBoxY, dims.width, 8 * this.pixelScale);
            boolean preferCenteredLane = Math.abs(keyCenterXx - keyboardCenterX) <= 32 * this.pixelScale;
            int[] bestPos = this.findBestPositionTopBorder(
               proportionalX,
               topBoxY,
               dims.width,
               dims.height,
               topBoxPositions,
               preferRaisedLane,
               preferLeftLimit,
               preferCenteredLane,
               clampMinX,
               clampMaxX,
               clampMinY,
               clampMaxY
            );
            bestPos = this.resolveGlobalCollision(bestPos, dims.width, dims.height, globalPlacedBoxes, dense, clampMinX, clampMaxX, clampMinY, clampMaxY);
            bestPos = this.lowerTopBoxIntoOpenSpace(bestPos, topBoxY, dims.width, dims.height, globalPlacedBoxes, clampMinY, clampMaxY);
            topBoxPositions.put(keyxxxx.hashCode(), new int[]{bestPos[0], bestPos[1], dims.width, dims.height});
            globalPlacedBoxes.put(System.identityHashCode(keyxxxx), new int[]{bestPos[0], bestPos[1], dims.width, dims.height});
            positions.put(keyxxxx, new BoxPosition(bestPos[0], bestPos[1], bestPos[0] + dims.width / 2, bestPos[1] + dims.height, dims.width, dims.height));
         } else if (!bottomKeySet.contains(keyxxxx)) {
            if (leftKeySet.contains(keyxxxx)) {
               BoxTextLayout layout = layoutByKey.apply(keyxxxx.glfwKey);
               BoxDimensions dimsx = layout.dimensions;
               boolean centerAligned = this.isCenteredSideBox(layout);
               int preferredY = this.preferredSideBoxY(keyxxxx, dimsx, centerAligned);
               int[] bestPos = this.findBestPositionSideBorder(
                  kbMinX - dimsx.width - sideGap, preferredY, dimsx.width, dimsx.height, leftBoxPositions, true, clampMinX, clampMaxX, clampMinY, clampMaxY
               );
               bestPos = this.resolveGlobalCollision(bestPos, dimsx.width, dimsx.height, globalPlacedBoxes, dense, clampMinX, clampMaxX, clampMinY, clampMaxY);
               leftBoxPositions.put(keyxxxx.hashCode(), new int[]{bestPos[0], bestPos[1], dimsx.width, dimsx.height});
               globalPlacedBoxes.put(System.identityHashCode(keyxxxx), new int[]{bestPos[0], bestPos[1], dimsx.width, dimsx.height});
               int lineEndY = this.sideLineEndY(keyxxxx, bestPos[1], dimsx.height, centerAligned);
               positions.put(keyxxxx, new BoxPosition(bestPos[0], bestPos[1], bestPos[0] + dimsx.width, lineEndY, dimsx.width, dimsx.height));
            } else {
               BoxTextLayout layout = layoutByKey.apply(keyxxxx.glfwKey);
               BoxDimensions dimsx = layout.dimensions;
               boolean centerAligned = this.isCenteredSideBox(layout);
               int preferredY = this.preferredSideBoxY(keyxxxx, dimsx, centerAligned);
               Integer spillPreferredRowY = spillRightPreferredY.get(keyxxxx);
               if (spillPreferredRowY != null) {
                  preferredY = centerAligned ? spillPreferredRowY + (keyxxxx.height - dimsx.height) / 2 : spillPreferredRowY;
               }

               int[] bestPos = this.findBestPositionSideBorder(
                  kbMaxX + sideGap, preferredY, dimsx.width, dimsx.height, rightBoxPositions, false, clampMinX, clampMaxX, clampMinY, clampMaxY
               );
               bestPos = this.resolveGlobalCollision(bestPos, dimsx.width, dimsx.height, globalPlacedBoxes, dense, clampMinX, clampMaxX, clampMinY, clampMaxY);
               rightBoxPositions.put(keyxxxx.hashCode(), new int[]{bestPos[0], bestPos[1], dimsx.width, dimsx.height});
               globalPlacedBoxes.put(System.identityHashCode(keyxxxx), new int[]{bestPos[0], bestPos[1], dimsx.width, dimsx.height});
               int lineEndY = this.sideLineEndY(keyxxxx, bestPos[1], dimsx.height, centerAligned);
               positions.put(keyxxxx, new BoxPosition(bestPos[0], bestPos[1], bestPos[0], lineEndY, dimsx.width, dimsx.height));
            }
         } else {
            BoxDimensions dimsx = layoutByKey.apply(keyxxxx.glfwKey).dimensions;
            int proportionalX = this.computeProportionalX(keyxxxx, kbMinX, keyboardWidth, dimsx.width, overflowX, kbMaxX);
            proportionalX = this.applyBottomRightBias(keyxxxx, proportionalX, kbMinX, keyboardWidth);
            proportionalX = Math.max(clampMinX, Math.min(proportionalX, clampMaxX - dimsx.width));
            int preferredBottomY = bottomBoxY + this.bottomBoxVerticalBias(keyxxxx, proportionalX, dimsx, spaceKey, spaceDims, physicalRowYs);
            int[] bestPos;
            if (keyxxxx.glfwKey == 32) {
               bestPos = null;
               int bestCost = Integer.MAX_VALUE;
               int spaceLeft = keyxxxx.x;
               int spaceRight = keyxxxx.x + keyxxxx.width - dimsx.width;
               int preferredSpaceX = spaceLeft + (spaceRight - spaceLeft) * 1 / 2;

               for (int tryX : this.buildCenteredSearchXs(preferredSpaceX, spaceLeft, spaceRight, 6)) {
                  int[] candidate = this.findBestPositionBottomBorder(
                     tryX, bottomBoxY, dimsx.width, dimsx.height, bottomBoxPositions, clampMinX, clampMaxX, clampMinY, clampMaxY
                  );
                  int cost = Math.abs(candidate[0] - preferredSpaceX) + 2 * Math.abs(candidate[1] - bottomBoxY);
                  if (cost < bestCost) {
                     bestCost = cost;
                     bestPos = candidate;
                  }
               }

               if (bestPos == null) {
                  bestPos = new int[]{proportionalX, bottomBoxY};
               }
            } else {
               bestPos = this.findBestPositionBottomBorder(
                  proportionalX, preferredBottomY, dimsx.width, dimsx.height, bottomBoxPositions, clampMinX, clampMaxX, clampMinY, clampMaxY
               );
            }

            bestPos = this.resolveGlobalCollision(bestPos, dimsx.width, dimsx.height, globalPlacedBoxes, dense, clampMinX, clampMaxX, clampMinY, clampMaxY);
            bottomBoxPositions.put(keyxxxx.hashCode(), new int[]{bestPos[0], bestPos[1], dimsx.width, dimsx.height});
            globalPlacedBoxes.put(System.identityHashCode(keyxxxx), new int[]{bestPos[0], bestPos[1], dimsx.width, dimsx.height});
            positions.put(keyxxxx, new BoxPosition(bestPos[0], bestPos[1], bestPos[0] + dimsx.width / 2, bestPos[1] - 1, dimsx.width, dimsx.height));
         }
      }

      if (!mouseKeysAll.isEmpty()) {
         int mbo = -100;
         int mMinX = Integer.MAX_VALUE;
         int mMaxX = Integer.MIN_VALUE;
         int mMinY = Integer.MAX_VALUE;
         int mMaxY = Integer.MIN_VALUE;
         if (mouseDeviceBounds != null) {
            mMinX = mouseDeviceBounds[0];
            mMinY = mouseDeviceBounds[1];
            mMaxX = mouseDeviceBounds[0] + mouseDeviceBounds[2];
            mMaxY = mouseDeviceBounds[1] + mouseDeviceBounds[3];
         } else {
            for (KeyboardKey mk : mouseKeysAll) {
               mMinX = Math.min(mMinX, mk.x);
               mMaxX = Math.max(mMaxX, mk.x + mk.width);
               mMinY = Math.min(mMinY, mk.y);
               mMaxY = Math.max(mMaxY, mk.y + mk.height);
            }
         }

         int mouseGap = 10 * this.pixelScale;
         Map<Integer, int[]> mouseTopBoxes = new LinkedHashMap<>();
         Map<Integer, int[]> mouseBottomBoxes = new LinkedHashMap<>();
         Map<Integer, int[]> mouseLeftBoxes = new LinkedHashMap<>();
         List<KeyboardKey> sortedMouseKeys = new ArrayList<>(mouseKeysAll);
         sortedMouseKeys.sort((a, b) -> {
            int order = KeyPlacementOrder.mouseKeySortOrder(a.glfwKey) - KeyPlacementOrder.mouseKeySortOrder(b.glfwKey);
            return order != 0 ? order : Integer.compare(a.glfwKey, b.glfwKey);
         });

         for (KeyboardKey mk : sortedMouseKeys) {
            BoxDimensions dimsx = layoutByKey.apply(mk.glfwKey).dimensions;
            int gk = mk.glfwKey;
            if (gk == mbo + 0 || gk == mbo + 1) {
               int topY = mMinY - dimsx.height - mouseGap;
               int preferredX;
               if (gk == mbo + 1) {
                  preferredX = mMaxX - dimsx.width + 6 * this.pixelScale;
               } else {
                  int collisionGap = 5 * this.pixelScale;
                  if (!mouseTopBoxes.isEmpty()) {
                     int[] rmbBox = mouseTopBoxes.values().iterator().next();
                     preferredX = rmbBox[0] - dimsx.width - collisionGap;
                  } else {
                     preferredX = mMinX;
                  }
               }

               int[] bestPos = this.findBestPositionTopBorder(
                  preferredX, topY, dimsx.width, dimsx.height, mouseTopBoxes, false, gk == mbo + 0, false, clampMinX, clampMaxX, clampMinY, clampMaxY
               );
               mouseTopBoxes.put(mk.hashCode(), new int[]{bestPos[0], bestPos[1], dimsx.width, dimsx.height});
               positions.put(mk, new BoxPosition(bestPos[0], bestPos[1], bestPos[0] + dimsx.width / 2, bestPos[1] + dimsx.height, dimsx.width, dimsx.height));
            } else if (gk != mbo + 3 && gk != mbo + 4) {
               int keyCX = mk.x + mk.width / 2;
               int preferredX = keyCX - dimsx.width / 2;
               preferredX = Math.max(mMinX, Math.min(preferredX, mMaxX - dimsx.width));
               int botY = mMaxY + mouseGap;
               int[] bestPos = this.findBestPositionBottomBorder(
                  preferredX, botY, dimsx.width, dimsx.height, mouseBottomBoxes, clampMinX, clampMaxX, clampMinY, clampMaxY
               );
               mouseBottomBoxes.put(mk.hashCode(), new int[]{bestPos[0], bestPos[1], dimsx.width, dimsx.height});
               positions.put(mk, new BoxPosition(bestPos[0], bestPos[1], bestPos[0] + dimsx.width / 2, bestPos[1] - 1, dimsx.width, dimsx.height));
            } else {
               BoxTextLayout boxLayout = layoutByKey.apply(mk.glfwKey);
               boolean centerAligned = this.isCenteredSideBox(boxLayout);
               int preferredY = this.preferredSideBoxY(mk, dimsx, centerAligned);
               int nudge = 4 * this.pixelScale;
               if (gk == mbo + 4) {
                  preferredY -= nudge;
               } else {
                  preferredY += nudge;
               }

               int[] bestPos = this.findBestPositionSideBorder(
                  mMinX - dimsx.width - mouseGap, preferredY, dimsx.width, dimsx.height, mouseLeftBoxes, true, clampMinX, clampMaxX, clampMinY, clampMaxY
               );
               mouseLeftBoxes.put(mk.hashCode(), new int[]{bestPos[0], bestPos[1], dimsx.width, dimsx.height});
               int keyCenterYx = mk.y + mk.height / 2;
               positions.put(mk, new BoxPosition(bestPos[0], bestPos[1], bestPos[0] + dimsx.width, keyCenterYx, dimsx.width, dimsx.height));
            }
         }
      }

      return positions;
   }

   public BoxPosition adjustSequentialBoxPosition(KeyboardKey key, BoxPosition template, Map<BoxPosition, int[]> committedBoxRects, List<int[]> drawnSegments) {
      if (template == null) {
         return template;
      } else {
         int screenCenterX = this.screenWidth / 2;
         int screenCenterY = this.screenHeight / 2;
         int iScreenMinX = OverlayRenderHelper.scaleX(0, screenCenterX, this.pixelScale);
         int iScreenMaxX = OverlayRenderHelper.scaleX(this.screenWidth, screenCenterX, this.pixelScale);
         int iScreenMinY = OverlayRenderHelper.scaleY(0, screenCenterY, this.pixelScale);
         int iScreenMaxY = OverlayRenderHelper.scaleY(this.screenHeight, screenCenterY, this.pixelScale);
         int iMargin = 5 * this.pixelScale;
         int clampMinX = iScreenMinX + iMargin;
         int clampMaxX = iScreenMaxX - iMargin;
         int clampMinY = iScreenMinY + iMargin;
         int clampMaxY = iScreenMaxY - iMargin;
         Map<Integer, int[]> placedBoxes = new LinkedHashMap<>();
         int placedIndex = 0;

         for (int[] rect : committedBoxRects.values()) {
            placedBoxes.put(placedIndex++, rect);
         }

         KeyboardBoxLayoutEngine.SequentialBoxOrientation orientation = this.inferSequentialOrientation(template);
         int[] preferredPos = this.findSequentialPreferredPosition(key, template, orientation, placedBoxes, clampMinX, clampMaxX, clampMinY, clampMaxY);
         LinkedHashSet<Long> candidates = new LinkedHashSet<>();
         this.addSequentialCandidate(candidates, template.boxX, template.boxY);
         this.addSequentialCandidate(candidates, preferredPos[0], preferredPos[1]);
         this.collectSequentialCandidates(candidates, template, preferredPos, orientation, placedBoxes);
         int[] bestPos = null;
         int bestScore = Integer.MAX_VALUE;

         for (long packedCandidate : candidates) {
            int candidateX = this.unpackSequentialX(packedCandidate);
            int candidateY = this.unpackSequentialY(packedCandidate);
            if (candidateX >= clampMinX
               && candidateX + template.boxWidth <= clampMaxX
               && candidateY >= clampMinY
               && candidateY + template.boxHeight <= clampMaxY
               && !this.hasBoxCollision(candidateX, candidateY, template.boxWidth, template.boxHeight, placedBoxes)
               && this.isSequentialOrientationValid(key, candidateX, candidateY, template, orientation)) {
               int score = this.scoreSequentialCandidate(candidateX, candidateY, template, drawnSegments);
               if (bestPos == null || score < bestScore) {
                  bestPos = new int[]{candidateX, candidateY};
                  bestScore = score;
               }
            }
         }

         if (bestPos == null) {
            bestPos = this.resolveGlobalCollision(
               preferredPos, template.boxWidth, template.boxHeight, placedBoxes, committedBoxRects.size() > 20, clampMinX, clampMaxX, clampMinY, clampMaxY
            );
            if (!this.isSequentialOrientationValid(key, bestPos[0], bestPos[1], template, orientation)) {
               bestPos = this.fallbackSequentialPosition(key, template, orientation, clampMinX, clampMaxX, clampMinY, clampMaxY);
            }
         }

         return this.shiftBoxPosition(template, bestPos[0], bestPos[1]);
      }
   }

   private int preferredSideBoxY(KeyboardKey key, BoxDimensions dims, boolean centerAligned) {
      return centerAligned ? key.y + (key.height - dims.height) / 2 : key.y;
   }

   private int applyBottomRightBias(KeyboardKey key, int preferredX, int kbMinX, int keyboardWidth) {
      int keyCenterX = key.x + key.width / 2;
      int halfWidth = Math.max(1, keyboardWidth / 2);
      int keyXOnKeyboard = keyCenterX - kbMinX;
      int distFromCenter = Math.abs(keyXOnKeyboard - halfWidth);
      boolean rightHalf = keyXOnKeyboard >= halfWidth;
      int bias;
      if (rightHalf) {
         int maxBias = 200 * this.pixelScale;
         double t = (double)distFromCenter / (double)halfWidth;
         bias = (int)(Math.pow(t, 1.5) * (double)maxBias);
      } else {
         int maxBias = 40 * this.pixelScale;
         double t = (double)distFromCenter / (double)halfWidth;
         bias = (int)(Math.pow(t, 1.5) * (double)maxBias);
      }

      return rightHalf ? preferredX + bias : preferredX - bias;
   }

   private int bottomBoxVerticalBias(
      KeyboardKey key, int preferredX, BoxDimensions dims, KeyboardKey spaceKey, BoxDimensions spaceDims, List<Integer> physicalRowYs
   ) {
      if (spaceKey != null && spaceDims != null && key.glfwKey != 32) {
         if (this.isRowAboveSpaceRow(key, spaceKey, physicalRowYs)) {
            int keyCenterX = key.x + key.width / 2;
            int preferredCenterX = preferredX + dims.width / 2;
            int swayThreshold = 48 * this.pixelScale;
            if (Math.abs(preferredCenterX - keyCenterX) >= swayThreshold) {
               return 12 * this.pixelScale;
            }
         }

         int keyCenterX = key.x + key.width / 2;
         int spaceCenterX = spaceKey.x + spaceKey.width / 2;
         if (keyCenterX > spaceCenterX) {
            return 0;
         } else {
            int preferredSpaceX = spaceKey.x + (spaceKey.width - spaceDims.width) * 1 / 2;
            int preferredSpaceRight = preferredSpaceX + spaceDims.width;
            int preferredRight = preferredX + dims.width;
            boolean overlapsPreferredSpaceRegion = preferredX < preferredSpaceRight && preferredRight > preferredSpaceX;
            return !overlapsPreferredSpaceRegion ? 0 : 8 * this.pixelScale;
         }
      } else {
         return 0;
      }
   }

   private boolean isRowAboveSpaceRow(KeyboardKey key, KeyboardKey spaceKey, List<Integer> physicalRowYs) {
      int rowAboveSpace = Integer.MIN_VALUE;

      for (int rowY : physicalRowYs) {
         if (rowY >= spaceKey.y) {
            break;
         }

         rowAboveSpace = rowY;
      }

      return rowAboveSpace != Integer.MIN_VALUE && key.y == rowAboveSpace;
   }

   private List<Integer> buildCenteredSearchXs(int preferredX, int minX, int maxX, int step) {
      List<Integer> positions = new ArrayList<>();
      if (minX > maxX) {
         return positions;
      } else {
         int clampedPreferred = Math.max(minX, Math.min(preferredX, maxX));
         positions.add(clampedPreferred);
         int offset = step;

         while (true) {
            boolean added = false;
            int right = clampedPreferred + offset;
            if (right <= maxX) {
               positions.add(right);
               added = true;
            }

            int left = clampedPreferred - offset;
            if (left >= minX) {
               positions.add(left);
               added = true;
            }

            if (!added) {
               return positions;
            }

            offset += step;
         }
      }
   }

   private boolean isCenteredSideBox(BoxTextLayout layout) {
      return layout.rows.size() <= 1;
   }

   private int sideLineEndY(KeyboardKey key, int boxY, int boxHeight, boolean centerAligned) {
      int boxCenterY = boxY + boxHeight / 2;
      if (!centerAligned) {
         return boxCenterY;
      } else {
         int keyCenterY = key.y + key.height / 2;
         return Math.abs(boxCenterY - keyCenterY) <= 1 ? keyCenterY : boxCenterY;
      }
   }

   private int[] resolveGlobalCollision(
      int[] pos,
      int boxWidth,
      int boxHeight,
      Map<Integer, int[]> globalPlacedBoxes,
      boolean dense,
      int screenMinX,
      int screenMaxX,
      int screenMinY,
      int screenMaxY
   ) {
      if (!this.hasBoxCollision(pos[0], pos[1], boxWidth, boxHeight, globalPlacedBoxes)) {
         return pos;
      } else {
         int gap = (dense ? 5 : 5) * this.pixelScale;
         List<int[]> candidates = new ArrayList<>();

         for (int[] existing : globalPlacedBoxes.values()) {
            int ey = existing[1];
            int eh = existing[3];
            int ex = existing[0];
            int ew = existing[2];
            candidates.add(new int[]{pos[0], ey + eh + gap});
            candidates.add(new int[]{pos[0], ey - boxHeight - gap});
            candidates.add(new int[]{ex + ew + gap, pos[1]});
            candidates.add(new int[]{ex - boxWidth - gap, pos[1]});
            candidates.add(new int[]{ex + ew + gap, ey + eh + gap});
            candidates.add(new int[]{ex - boxWidth - gap, ey + eh + gap});
            candidates.add(new int[]{ex + ew + gap, ey - boxHeight - gap});
            candidates.add(new int[]{ex - boxWidth - gap, ey - boxHeight - gap});
         }

         candidates.sort((a, b) -> Integer.compare(Math.abs(a[0] - pos[0]) + Math.abs(a[1] - pos[1]), Math.abs(b[0] - pos[0]) + Math.abs(b[1] - pos[1])));

         for (int[] candidate : candidates) {
            int x = candidate[0];
            int y = candidate[1];
            if (x >= screenMinX
               && x + boxWidth <= screenMaxX
               && y >= screenMinY
               && y + boxHeight <= screenMaxY
               && !this.hasBoxCollision(x, y, boxWidth, boxHeight, globalPlacedBoxes)) {
               return candidate;
            }
         }

         return pos;
      }
   }

   private int[] lowerTopBoxIntoOpenSpace(int[] pos, int targetY, int boxWidth, int boxHeight, Map<Integer, int[]> placedBoxes, int screenMinY, int screenMaxY) {
      if (pos[1] >= targetY) {
         return pos;
      } else {
         int bestY = pos[1];
         int step = Math.max(1, this.pixelScale);

         for (int candidateY = pos[1] + step; candidateY <= targetY; candidateY += step) {
            if (candidateY >= screenMinY && candidateY + boxHeight <= screenMaxY && !this.hasBoxCollision(pos[0], candidateY, boxWidth, boxHeight, placedBoxes)
               )
             {
               bestY = candidateY;
            }
         }

         return bestY == pos[1] ? pos : new int[]{pos[0], bestY};
      }
   }

   private boolean hasBoxCollision(int x, int y, int boxWidth, int boxHeight, Map<Integer, int[]> placedBoxes) {
      int gap = 5 * this.pixelScale;

      for (int[] existing : placedBoxes.values()) {
         int dx = Math.abs(x + boxWidth / 2 - (existing[0] + existing[2] / 2));
         int dy = Math.abs(y + boxHeight / 2 - (existing[1] + existing[3] / 2));
         if (dx < (boxWidth + existing[2]) / 2 + gap && dy < (boxHeight + existing[3]) / 2 + gap) {
            return true;
         }
      }

      return false;
   }

   private int computeProportionalX(KeyboardKey key, int kbMinX, int keyboardWidth, int boxWidth, int overflowX, int kbMaxX) {
      int keyCenterX = key.x + key.width / 2;
      float keyFraction = keyboardWidth > 0 ? (float)(keyCenterX - kbMinX) / (float)keyboardWidth : 0.5F;
      int totalWidth = keyboardWidth + 2 * overflowX;
      int proportionalX = kbMinX - overflowX + (int)(keyFraction * (float)totalWidth) - boxWidth / 2;
      int halfWidth = keyboardWidth / 2;
      int keyXOnKeyboard = keyCenterX - kbMinX;
      return proportionalX - this.extraLeftBias(keyXOnKeyboard, halfWidth, boxWidth);
   }

   private int extraLeftBias(int keyXOnKeyboard, int halfWidth, int boxWidth) {
      if (keyXOnKeyboard >= halfWidth) {
         return 0;
      } else {
         int leftWeight = halfWidth - keyXOnKeyboard;
         int scaledBoxWidth = Math.min(boxWidth, 72 * this.pixelScale);
         int linearBias = leftWeight * (24 * this.pixelScale + scaledBoxWidth / 3) / Math.max(1, halfWidth);
         int curvedBias = leftWeight * leftWeight * 10 * this.pixelScale / Math.max(1, halfWidth * halfWidth);
         return linearBias + curvedBias;
      }
   }

   private int[] findBestPositionTopBorder(
      int preferredX,
      int preferredY,
      int boxWidth,
      int boxHeight,
      Map<Integer, int[]> placedBoxes,
      boolean preferRaisedLane,
      boolean preferLeftShift,
      boolean preferCenteredLane,
      int screenMinX,
      int screenMaxX,
      int screenMinY,
      int screenMaxY
   ) {
      int gap = 8 * this.pixelScale;
      int laneStep = 18 * this.pixelScale;
      int shiftStep = Math.max(3 * this.pixelScale, 4);
      int maxShiftSteps = 18;
      int[] earlyLaneOrder = preferRaisedLane ? new int[]{1, 2, 0, 3} : new int[]{0, 1, 2, 3};
      if (preferCenteredLane) {
         for (int laneIndex : earlyLaneOrder) {
            int laneY = preferredY - laneIndex * laneStep;
            if (laneY >= screenMinY && laneY + boxHeight <= screenMaxY) {
               int clampedX = Math.max(screenMinX, Math.min(preferredX, screenMaxX - boxWidth));
               if (!this.hasBoxCollision(clampedX, laneY, boxWidth, boxHeight, placedBoxes)) {
                  return new int[]{clampedX, laneY};
               }
            }
         }
      }

      for (int laneIndexx : earlyLaneOrder) {
         int laneY = preferredY - laneIndexx * laneStep;
         if (laneY >= screenMinY && laneY + boxHeight <= screenMaxY) {
            for (int step = 0; step <= maxShiftSteps; step++) {
               int[] laneCandidates = step == 0
                  ? new int[]{preferredX}
                  : (
                     preferLeftShift
                        ? new int[]{preferredX - step * shiftStep, preferredX + step * shiftStep}
                        : new int[]{preferredX + step * shiftStep, preferredX - step * shiftStep}
                  );

               for (int candidateX : laneCandidates) {
                  int clampedX = Math.max(screenMinX, Math.min(candidateX, screenMaxX - boxWidth));
                  if (!this.hasBoxCollision(clampedX, laneY, boxWidth, boxHeight, placedBoxes)) {
                     return new int[]{clampedX, laneY};
                  }
               }
            }
         }
      }

      List<int[]> candidates = new ArrayList<>();
      List<Integer> laneYs = new ArrayList<>();

      for (int lane = 0; lane <= 5; lane++) {
         laneYs.add(preferredY - lane * laneStep);
      }

      for (int laneY : laneYs) {
         candidates.add(new int[]{preferredX, laneY});
      }

      for (int[] existing : placedBoxes.values()) {
         int ex = existing[0];
         int ey = existing[1];
         int ew = existing[2];
         candidates.add(new int[]{ex + ew + gap, ey});
         candidates.add(new int[]{ex - boxWidth - gap, ey});

         for (int laneY : laneYs) {
            candidates.add(new int[]{ex + ew + gap, laneY});
            candidates.add(new int[]{ex - boxWidth - gap, laneY});
         }
      }

      candidates.sort(
         (a, b) -> Integer.compare(2 * Math.abs(a[0] - preferredX) + Math.abs(a[1] - preferredY), 2 * Math.abs(b[0] - preferredX) + Math.abs(b[1] - preferredY))
      );

      for (int[] candidate : candidates) {
         int x = candidate[0];
         int y = candidate[1];
         if (y <= preferredY
            && x >= screenMinX
            && x + boxWidth <= screenMaxX
            && y >= screenMinY
            && y + boxHeight <= screenMaxY
            && !this.hasBoxCollision(x, y, boxWidth, boxHeight, placedBoxes)) {
            return candidate;
         }
      }

      return new int[]{Math.max(screenMinX, Math.min(preferredX, screenMaxX - boxWidth)), Math.max(screenMinY, Math.min(preferredY, screenMaxY - boxHeight))};
   }

   private boolean hasConflictingTopBoxOnSameLane(Map<Integer, int[]> placedBoxes, int preferredX, int laneY, int boxWidth, int gap) {
      int preferredRight = preferredX + boxWidth;

      for (int[] box : placedBoxes.values()) {
         if (box[1] == laneY) {
            int existingLeft = box[0];
            int existingRight = box[0] + box[2];
            if (existingLeft < preferredX && existingRight + gap > preferredX) {
               return true;
            }

            if (existingLeft <= preferredRight + gap && existingRight >= preferredX - gap) {
               return true;
            }
         }
      }

      return false;
   }

   private int[] findBestPositionBottomBorder(
      int preferredX,
      int preferredY,
      int boxWidth,
      int boxHeight,
      Map<Integer, int[]> placedBoxes,
      int screenMinX,
      int screenMaxX,
      int screenMinY,
      int screenMaxY
   ) {
      int gap = 5 * this.pixelScale;
      List<int[]> candidates = new ArrayList<>();
      int shiftStep = Math.max(3 * this.pixelScale, 4);
      int maxShiftSteps = 10;

      for (int lane = 0; lane <= 5; lane++) {
         int laneY = preferredY + lane * (boxHeight + gap);
         candidates.add(new int[]{preferredX, laneY});

         for (int step = 1; step <= maxShiftSteps; step++) {
            candidates.add(new int[]{preferredX - step * shiftStep, laneY});
            candidates.add(new int[]{preferredX + step * shiftStep, laneY});
         }
      }

      for (int[] existing : placedBoxes.values()) {
         int ex = existing[0];
         int ew = existing[2];
         int ey = existing[1];
         int eh = existing[3];
         candidates.add(new int[]{ex + ew + gap, preferredY});
         candidates.add(new int[]{ex - boxWidth - gap, preferredY});
         candidates.add(new int[]{ex + ew + gap, preferredY + boxHeight + gap});
         candidates.add(new int[]{ex - boxWidth - gap, preferredY + boxHeight + gap});
         candidates.add(new int[]{preferredX, ey + eh + gap});
      }

      candidates.sort(
         (a, b) -> Integer.compare(Math.abs(a[0] - preferredX) + 2 * Math.abs(a[1] - preferredY), Math.abs(b[0] - preferredX) + 2 * Math.abs(b[1] - preferredY))
      );

      for (int[] candidate : candidates) {
         int x = candidate[0];
         int y = candidate[1];
         if (y >= preferredY
            && x >= screenMinX
            && x + boxWidth <= screenMaxX
            && y >= screenMinY
            && y + boxHeight <= screenMaxY
            && !this.hasBoxCollision(x, y, boxWidth, boxHeight, placedBoxes)) {
            return candidate;
         }
      }

      return new int[]{preferredX, preferredY};
   }

   private int[] findBestPositionSideBorder(
      int preferredX,
      int preferredY,
      int boxWidth,
      int boxHeight,
      Map<Integer, int[]> placedBoxes,
      boolean isLeft,
      int screenMinX,
      int screenMaxX,
      int screenMinY,
      int screenMaxY
   ) {
      int gap = 5 * this.pixelScale;
      List<int[]> candidates = new ArrayList<>();
      int verticalStep = Math.max(3 * this.pixelScale, 4);
      int maxVerticalSteps = 8;
      int maxColumns = 2;

      for (int column = 0; column <= maxColumns; column++) {
         int columnX = isLeft ? preferredX - column * (boxWidth + gap) : preferredX + column * (boxWidth + gap);
         candidates.add(new int[]{columnX, preferredY});

         for (int step = 1; step <= maxVerticalSteps; step++) {
            int delta = step * verticalStep;
            candidates.add(new int[]{columnX, preferredY - delta});
            candidates.add(new int[]{columnX, preferredY + delta});
         }
      }

      for (int[] existing : placedBoxes.values()) {
         int ey = existing[1];
         int eh = existing[3];

         for (int column = 0; column <= maxColumns; column++) {
            int columnX = isLeft ? preferredX - column * (boxWidth + gap) : preferredX + column * (boxWidth + gap);
            candidates.add(new int[]{columnX, ey + eh + gap});
            candidates.add(new int[]{columnX, ey - boxHeight - gap});
         }
      }

      candidates.sort(
         (a, b) -> Integer.compare(2 * Math.abs(a[1] - preferredY) + Math.abs(a[0] - preferredX), 2 * Math.abs(b[1] - preferredY) + Math.abs(b[0] - preferredX))
      );

      for (int[] candidate : candidates) {
         int x = candidate[0];
         int y = candidate[1];
         if (x >= screenMinX
            && x + boxWidth <= screenMaxX
            && y >= screenMinY
            && y + boxHeight <= screenMaxY
            && !this.hasBoxCollision(x, y, boxWidth, boxHeight, placedBoxes)) {
            return candidate;
         }
      }

      return new int[]{preferredX, preferredY};
   }

   private int[] findSequentialPreferredPosition(
      KeyboardKey key,
      BoxPosition template,
      KeyboardBoxLayoutEngine.SequentialBoxOrientation orientation,
      Map<Integer, int[]> placedBoxes,
      int screenMinX,
      int screenMaxX,
      int screenMinY,
      int screenMaxY
   ) {
      switch (orientation) {
         case TOP:
            boolean preferRaisedLane = template.boxY < key.y - template.boxHeight - 17 * this.pixelScale;
            boolean preferLeftShift = template.boxX < key.x;
            boolean preferCenteredLane = Math.abs(template.lineEndX - (key.x + key.width / 2)) <= 32 * this.pixelScale;
            return this.findBestPositionTopBorder(
               template.boxX,
               template.boxY,
               template.boxWidth,
               template.boxHeight,
               placedBoxes,
               preferRaisedLane,
               preferLeftShift,
               preferCenteredLane,
               screenMinX,
               screenMaxX,
               screenMinY,
               screenMaxY
            );
         case BOTTOM:
            return this.findBestPositionBottomBorder(
               template.boxX, template.boxY, template.boxWidth, template.boxHeight, placedBoxes, screenMinX, screenMaxX, screenMinY, screenMaxY
            );
         case LEFT:
            return this.findBestPositionSideBorder(
               template.boxX, template.boxY, template.boxWidth, template.boxHeight, placedBoxes, true, screenMinX, screenMaxX, screenMinY, screenMaxY
            );
         case RIGHT:
         default:
            return this.findBestPositionSideBorder(
               template.boxX, template.boxY, template.boxWidth, template.boxHeight, placedBoxes, false, screenMinX, screenMaxX, screenMinY, screenMaxY
            );
      }
   }

   private void collectSequentialCandidates(
      LinkedHashSet<Long> candidates,
      BoxPosition template,
      int[] preferredPos,
      KeyboardBoxLayoutEngine.SequentialBoxOrientation orientation,
      Map<Integer, int[]> placedBoxes
   ) {
      int shiftStep = Math.max(3 * this.pixelScale, 4);
      int gap = 5 * this.pixelScale;
      switch (orientation) {
         case TOP:
            this.addTopSequentialCandidates(candidates, template, preferredPos, shiftStep);
            break;
         case BOTTOM:
            this.addBottomSequentialCandidates(candidates, template, preferredPos, shiftStep, gap);
            break;
         case LEFT:
            this.addSideSequentialCandidates(candidates, template, preferredPos, shiftStep, gap, true);
            break;
         case RIGHT:
            this.addSideSequentialCandidates(candidates, template, preferredPos, shiftStep, gap, false);
      }

      for (int[] rect : placedBoxes.values()) {
         int ex = rect[0];
         int ey = rect[1];
         int ew = rect[2];
         int eh = rect[3];
         switch (orientation) {
            case TOP:
               this.addSequentialCandidate(candidates, ex + ew + gap, ey);
               this.addSequentialCandidate(candidates, ex - template.boxWidth - gap, ey);
               this.addSequentialCandidate(candidates, ex, ey - template.boxHeight - gap);
               break;
            case BOTTOM:
               this.addSequentialCandidate(candidates, ex + ew + gap, ey + eh + gap);
               this.addSequentialCandidate(candidates, ex - template.boxWidth - gap, ey + eh + gap);
               this.addSequentialCandidate(candidates, ex, ey + eh + gap);
               break;
            case LEFT:
               this.addSequentialCandidate(candidates, ex - template.boxWidth - gap, ey);
               this.addSequentialCandidate(candidates, ex - template.boxWidth - gap, ey + eh + gap);
               this.addSequentialCandidate(candidates, ex - template.boxWidth - gap, ey - template.boxHeight - gap);
               break;
            case RIGHT:
               this.addSequentialCandidate(candidates, ex + ew + gap, ey);
               this.addSequentialCandidate(candidates, ex + ew + gap, ey + eh + gap);
               this.addSequentialCandidate(candidates, ex + ew + gap, ey - template.boxHeight - gap);
         }
      }
   }

   private void addTopSequentialCandidates(LinkedHashSet<Long> candidates, BoxPosition template, int[] preferredPos, int shiftStep) {
      int laneStep = 18 * this.pixelScale;

      for (int lane = 0; lane <= 5; lane++) {
         int y = template.boxY - lane * laneStep;
         this.addHorizontalSequentialCandidates(candidates, template.boxX, preferredPos[0], y, shiftStep, 6);
      }
   }

   private void addBottomSequentialCandidates(LinkedHashSet<Long> candidates, BoxPosition template, int[] preferredPos, int shiftStep, int gap) {
      int laneHeight = template.boxHeight + gap;

      for (int lane = 0; lane <= 5; lane++) {
         int y = template.boxY + lane * laneHeight;
         this.addHorizontalSequentialCandidates(candidates, template.boxX, preferredPos[0], y, shiftStep, 6);
      }
   }

   private void addSideSequentialCandidates(LinkedHashSet<Long> candidates, BoxPosition template, int[] preferredPos, int verticalStep, int gap, boolean isLeft) {
      int direction = isLeft ? -1 : 1;

      for (int column = 0; column <= 2; column++) {
         int templateX = template.boxX + direction * column * (template.boxWidth + gap);
         int preferredX = preferredPos[0] + direction * column * (template.boxWidth + gap);
         this.addVerticalSequentialCandidates(candidates, templateX, preferredX, template.boxY, preferredPos[1], verticalStep, 8);
      }
   }

   private void addHorizontalSequentialCandidates(LinkedHashSet<Long> candidates, int templateX, int preferredX, int y, int shiftStep, int maxShiftSteps) {
      this.addSequentialCandidate(candidates, templateX, y);
      this.addSequentialCandidate(candidates, preferredX, y);

      for (int step = 1; step <= maxShiftSteps; step++) {
         int delta = step * shiftStep;
         this.addSequentialCandidate(candidates, templateX - delta, y);
         this.addSequentialCandidate(candidates, templateX + delta, y);
         this.addSequentialCandidate(candidates, preferredX - delta, y);
         this.addSequentialCandidate(candidates, preferredX + delta, y);
      }
   }

   private void addVerticalSequentialCandidates(
      LinkedHashSet<Long> candidates, int templateX, int preferredX, int templateY, int preferredY, int verticalStep, int maxVerticalSteps
   ) {
      this.addSequentialCandidate(candidates, templateX, templateY);
      this.addSequentialCandidate(candidates, preferredX, preferredY);

      for (int step = 1; step <= maxVerticalSteps; step++) {
         int delta = step * verticalStep;
         this.addSequentialCandidate(candidates, templateX, templateY - delta);
         this.addSequentialCandidate(candidates, templateX, templateY + delta);
         this.addSequentialCandidate(candidates, preferredX, preferredY - delta);
         this.addSequentialCandidate(candidates, preferredX, preferredY + delta);
      }
   }

   private int scoreSequentialCandidate(int candidateX, int candidateY, BoxPosition template, List<int[]> drawnSegments) {
      int score = Math.abs(candidateX - template.boxX) + Math.abs(candidateY - template.boxY);
      return score + this.sequentialLinePenalty(candidateX, candidateY, template.boxWidth, template.boxHeight, drawnSegments);
   }

   private int sequentialLinePenalty(int boxX, int boxY, int boxWidth, int boxHeight, List<int[]> drawnSegments) {
      int penalty = 0;
      int nearDistance = Math.max(6 * this.pixelScale, 5 * this.pixelScale);

      for (int[] segment : drawnSegments) {
         if (this.segmentIntersectsBox(segment[0], segment[1], segment[2], segment[3], boxX, boxY, boxWidth, boxHeight, 0)) {
            penalty += 1600 * this.pixelScale;
         } else {
            int distance = this.orthogonalSegmentDistanceToBox(segment[0], segment[1], segment[2], segment[3], boxX, boxY, boxWidth, boxHeight);
            if (distance <= nearDistance) {
               penalty += (nearDistance - distance + 1) * 120;
            }
         }
      }

      return penalty;
   }

   private int orthogonalSegmentDistanceToBox(int sx1, int sy1, int sx2, int sy2, int boxX, int boxY, int boxWidth, int boxHeight) {
      if (sy1 == sy2) {
         int segmentMinX = Math.min(sx1, sx2);
         int segmentMaxX = Math.max(sx1, sx2);
         return this.intervalDistance(segmentMinX, segmentMaxX, boxX, boxX + boxWidth) + this.axisDistance(sy1, boxY, boxY + boxHeight);
      } else {
         int segmentMinY = Math.min(sy1, sy2);
         int segmentMaxY = Math.max(sy1, sy2);
         return this.axisDistance(sx1, boxX, boxX + boxWidth) + this.intervalDistance(segmentMinY, segmentMaxY, boxY, boxY + boxHeight);
      }
   }

   private boolean segmentIntersectsBox(int sx1, int sy1, int sx2, int sy2, int boxX, int boxY, int boxWidth, int boxHeight, int margin) {
      if (sy1 == sy2) {
         int segmentMinX = Math.min(sx1, sx2);
         int segmentMaxX = Math.max(sx1, sx2);
         return sy1 >= boxY - margin && sy1 <= boxY + boxHeight + margin && segmentMaxX >= boxX - margin && segmentMinX <= boxX + boxWidth + margin;
      } else {
         int segmentMinY = Math.min(sy1, sy2);
         int segmentMaxY = Math.max(sy1, sy2);
         return sx1 >= boxX - margin && sx1 <= boxX + boxWidth + margin && segmentMaxY >= boxY - margin && segmentMinY <= boxY + boxHeight + margin;
      }
   }

   private int intervalDistance(int minA, int maxA, int minB, int maxB) {
      if (maxA < minB) {
         return minB - maxA;
      } else {
         return maxB < minA ? minA - maxB : 0;
      }
   }

   private int axisDistance(int value, int min, int max) {
      if (value < min) {
         return min - value;
      } else {
         return value > max ? value - max : 0;
      }
   }

   private BoxPosition shiftBoxPosition(BoxPosition template, int boxX, int boxY) {
      int lineOffsetX = template.lineEndX - template.boxX;
      int lineOffsetY = template.lineEndY - template.boxY;
      return new BoxPosition(boxX, boxY, boxX + lineOffsetX, boxY + lineOffsetY, template.boxWidth, template.boxHeight);
   }

   private boolean isSequentialOrientationValid(
      KeyboardKey key, int boxX, int boxY, BoxPosition template, KeyboardBoxLayoutEngine.SequentialBoxOrientation orientation
   ) {
      switch (orientation) {
         case TOP:
            return boxY + template.boxHeight <= key.y && this.keepsTemplateHorizontalSide(boxX, template);
         case BOTTOM:
            return boxY >= key.y + key.height && this.keepsTemplateHorizontalSide(boxX, template);
         case LEFT:
            return boxX + template.boxWidth <= key.x;
         case RIGHT:
         default:
            return boxX >= key.x + key.width;
      }
   }

   private boolean keepsTemplateHorizontalSide(int candidateBoxX, BoxPosition template) {
      int screenCenterX = this.screenWidth / 2;
      int deadZone = Math.max(6 * this.pixelScale, 4);
      int templateCenterX = template.boxX + template.boxWidth / 2;
      int candidateCenterX = candidateBoxX + template.boxWidth / 2;
      if (templateCenterX < screenCenterX - deadZone) {
         return candidateCenterX <= screenCenterX + deadZone;
      } else {
         return templateCenterX > screenCenterX + deadZone ? candidateCenterX >= screenCenterX - deadZone : true;
      }
   }

   private int[] fallbackSequentialPosition(
      KeyboardKey key,
      BoxPosition template,
      KeyboardBoxLayoutEngine.SequentialBoxOrientation orientation,
      int clampMinX,
      int clampMaxX,
      int clampMinY,
      int clampMaxY
   ) {
      int boxX = template.boxX;
      int boxY = template.boxY;
      switch (orientation) {
         case TOP:
            boxY = key.y - template.boxHeight;
            break;
         case BOTTOM:
            boxY = key.y + key.height;
            break;
         case LEFT:
            boxX = key.x - template.boxWidth;
            break;
         case RIGHT:
            boxX = key.x + key.width;
      }

      boxX = Math.max(clampMinX, Math.min(boxX, clampMaxX - template.boxWidth));
      boxY = Math.max(clampMinY, Math.min(boxY, clampMaxY - template.boxHeight));
      return new int[]{boxX, boxY};
   }

   private KeyboardBoxLayoutEngine.SequentialBoxOrientation inferSequentialOrientation(BoxPosition template) {
      int lineOffsetX = template.lineEndX - template.boxX;
      int lineOffsetY = template.lineEndY - template.boxY;
      if (lineOffsetY >= template.boxHeight) {
         return KeyboardBoxLayoutEngine.SequentialBoxOrientation.TOP;
      } else if (lineOffsetY <= 0) {
         return KeyboardBoxLayoutEngine.SequentialBoxOrientation.BOTTOM;
      } else {
         return lineOffsetX >= template.boxWidth
            ? KeyboardBoxLayoutEngine.SequentialBoxOrientation.LEFT
            : KeyboardBoxLayoutEngine.SequentialBoxOrientation.RIGHT;
      }
   }

   private void addSequentialCandidate(Set<Long> candidates, int x, int y) {
      candidates.add(this.packSequentialCandidate(x, y));
   }

   private long packSequentialCandidate(int x, int y) {
      return (long)x << 32 ^ (long)y & 4294967295L;
   }

   private int unpackSequentialX(long packedCandidate) {
      return (int)(packedCandidate >> 32);
   }

   private int unpackSequentialY(long packedCandidate) {
      return (int)packedCandidate;
   }

   private boolean isFunctionRow(List<KeyboardKey> row) {
      if (row != null && !row.isEmpty()) {
         for (KeyboardKey key : row) {
            if (!this.isFunctionKey(key)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private KeyboardKey findRightEdgeAssignedKey(List<KeyboardKey> pair, Set<KeyboardKey> assignedKeySet) {
      if (pair != null && !pair.isEmpty()) {
         for (int index = pair.size() - 1; index >= 0; index--) {
            KeyboardKey key = pair.get(index);
            if (assignedKeySet.contains(key)) {
               return key;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean hasAssignedRightEdgeBelow(
      int rowY, List<Integer> orderedRowYs, Map<Integer, List<KeyboardKey>> physicalRowEndPair, Set<KeyboardKey> assignedKeySet
   ) {
      for (int candidateRowY : orderedRowYs) {
         if (candidateRowY > rowY && this.findRightEdgeAssignedKey(physicalRowEndPair.get(candidateRowY), assignedKeySet) != null) {
            return true;
         }
      }

      return false;
   }

   private KeyboardKey findNearestAssignedLeftNeighbor(List<KeyboardKey> row, KeyboardKey anchor, Set<KeyboardKey> assignedKeySet) {
      if (row != null && !row.isEmpty() && anchor != null) {
         for (int index = row.size() - 1; index >= 0; index--) {
            KeyboardKey candidate = row.get(index);
            if (candidate != anchor && candidate.x + candidate.width <= anchor.x && assignedKeySet.contains(candidate)) {
               return candidate;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private int findNextPhysicalRowY(int rowY, List<Integer> orderedRowYs) {
      for (int candidateRowY : orderedRowYs) {
         if (candidateRowY > rowY) {
            return candidateRowY;
         }
      }

      return -1;
   }

   private boolean isFunctionKey(KeyboardKey key) {
      return key.glfwKey >= 290 && key.glfwKey <= 301;
   }

   private KeyboardBoxLayoutEngine.BoxSide classifySide(int distToTop, int distToBottom, int distToLeft, int distToRight) {
      int minDist = Math.min(Math.min(distToTop, distToBottom), Math.min(distToLeft, distToRight));
      if (minDist == distToTop) {
         return KeyboardBoxLayoutEngine.BoxSide.TOP;
      } else if (minDist == distToBottom) {
         return KeyboardBoxLayoutEngine.BoxSide.BOTTOM;
      } else {
         return minDist == distToLeft ? KeyboardBoxLayoutEngine.BoxSide.LEFT : KeyboardBoxLayoutEngine.BoxSide.RIGHT;
      }
   }

   private static enum BoxSide {
      TOP,
      BOTTOM,
      LEFT,
      RIGHT;
   }

   private static enum SequentialBoxOrientation {
      TOP,
      BOTTOM,
      LEFT,
      RIGHT;
   }
}
