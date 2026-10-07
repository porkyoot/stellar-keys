package com.stellar.keys.atlas.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KeyPlacementOrder {
   private KeyPlacementOrder() {
   }

   public static Map<KeyboardKey, Integer> buildCenterColumnSweepPriority(Collection<KeyboardKey> physicalKeys, Collection<KeyboardKey> visibleKeys) {
      Map<Integer, List<KeyboardKey>> physicalRows = buildKeyboardRows(physicalKeys);
      if (physicalRows.isEmpty()) {
         return new LinkedHashMap<>();
      } else {
         Set<KeyboardKey> visibleKeyboardKeys = new HashSet<>();

         for (KeyboardKey key : visibleKeys) {
            if (!key.isMouseKey) {
               visibleKeyboardKeys.add(key);
            }
         }

         if (visibleKeyboardKeys.isEmpty()) {
            return new LinkedHashMap<>();
         } else {
            Map<Integer, Integer> rowPriority = buildCenterOutRowPriority(physicalKeys);
            List<Integer> orderedRows = new ArrayList<>(physicalRows.keySet());
            orderedRows.sort((a, b) -> Integer.compare(rowPriority.getOrDefault(a, Integer.MAX_VALUE), rowPriority.getOrDefault(b, Integer.MAX_VALUE)));
            int step2 = estimateColumnStep2(physicalRows);
            int mergeThreshold2 = Math.max(4, step2 / 3);
            Map<Integer, Integer> rowOffset2ByY = new LinkedHashMap<>();
            Map<KeyboardKey, Integer> normalizedCenter2ByKey = new LinkedHashMap<>();
            List<Integer> referenceCenters2 = new ArrayList<>();

            for (int rowY : orderedRows) {
               List<KeyboardKey> rowKeys = physicalRows.get(rowY);
               int rowOffset2 = referenceCenters2.isEmpty() ? 0 : findBestRowOffset2(rowKeys, referenceCenters2, mergeThreshold2);
               rowOffset2ByY.put(rowY, rowOffset2);

               for (KeyboardKey keyx : rowKeys) {
                  int normalizedCenter2 = center2X(keyx) + rowOffset2;
                  normalizedCenter2ByKey.put(keyx, normalizedCenter2);
                  referenceCenters2.add(normalizedCenter2);
               }
            }

            List<Integer> columnCenters2 = collapseCenters(new ArrayList<>(referenceCenters2), mergeThreshold2);
            if (columnCenters2.isEmpty()) {
               return new LinkedHashMap<>();
            } else {
               int anchorRowY = orderedRows.get(0);
               List<KeyboardKey> anchorRow = new ArrayList<>(physicalRows.get(anchorRowY));
               anchorRow.sort((a, b) -> Integer.compare(a.x, b.x));
               KeyboardKey anchorKey = anchorRow.get(Math.max(0, (anchorRow.size() - 1) / 2));
               int anchorCenter2 = normalizedCenter2ByKey.getOrDefault(anchorKey, center2X(anchorKey));
               int centerColumnIndex = findNearestColumnIndex(columnCenters2, anchorCenter2);
               Map<Integer, List<KeyboardKey>> visibleKeysByColumn = new LinkedHashMap<>();

               for (KeyboardKey keyx : visibleKeyboardKeys) {
                  int normalizedCenter2 = normalizedCenter2ByKey.getOrDefault(keyx, center2X(keyx));
                  int columnIndex = findNearestColumnIndex(columnCenters2, normalizedCenter2);
                  visibleKeysByColumn.computeIfAbsent(columnIndex, ignored -> new ArrayList<>()).add(keyx);
               }

               Map<KeyboardKey, Integer> priority = new LinkedHashMap<>();
               int order = 0;

               for (int columnIndex : buildCenterOutColumnIndexes(columnCenters2.size(), centerColumnIndex)) {
                  List<KeyboardKey> columnKeys = visibleKeysByColumn.get(columnIndex);
                  if (columnKeys != null && !columnKeys.isEmpty()) {
                     int columnCenter2 = columnCenters2.get(columnIndex);
                     columnKeys.sort((a, b) -> {
                        int rowA = rowPriority.getOrDefault(a.y, Integer.MAX_VALUE);
                        int rowB = rowPriority.getOrDefault(b.y, Integer.MAX_VALUE);
                        if (rowA != rowB) {
                           return Integer.compare(rowA, rowB);
                        } else {
                           int distanceA = Math.abs(normalizedCenter2ByKey.getOrDefault(a, center2X(a)) - columnCenter2);
                           int distanceB = Math.abs(normalizedCenter2ByKey.getOrDefault(b, center2X(b)) - columnCenter2);
                           if (distanceA != distanceB) {
                              return Integer.compare(distanceA, distanceB);
                           } else {
                              return a.y != b.y ? Integer.compare(a.y, b.y) : Integer.compare(a.x, b.x);
                           }
                        }
                     });

                     for (KeyboardKey keyx : columnKeys) {
                        if (!priority.containsKey(keyx)) {
                           priority.put(keyx, order++);
                        }
                     }
                  }
               }

               return priority;
            }
         }
      }
   }

   public static Map<Integer, Integer> buildCenterOutRowPriority(Collection<KeyboardKey> keys) {
      List<Integer> rows = new ArrayList<>();

      for (KeyboardKey key : keys) {
         if (!key.isMouseKey && !rows.contains(key.y)) {
            rows.add(key.y);
         }
      }

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
            if (upper >= 0) {
               priority.put(rows.get(upper), rank++);
            }

            int lower = centerIndex + offset;
            if (lower < rows.size()) {
               priority.put(rows.get(lower), rank++);
            }
         }

         return priority;
      }
   }

   public static Map<KeyboardKey, Integer> buildGlobalEdgePriority(Collection<KeyboardKey> visibleKeys) {
      Map<KeyboardKey, Integer> priority = new LinkedHashMap<>();
      KeyboardKey leftmost = null;
      KeyboardKey rightmost = null;

      for (KeyboardKey key : visibleKeys) {
         if (!key.isMouseKey) {
            if (leftmost == null || key.x < leftmost.x || key.x == leftmost.x && key.y < leftmost.y) {
               leftmost = key;
            }

            int keyRight = key.x + key.width;
            int bestRight = rightmost == null ? Integer.MIN_VALUE : rightmost.x + rightmost.width;
            if (rightmost == null || keyRight > bestRight || keyRight == bestRight && key.y < rightmost.y) {
               rightmost = key;
            }
         }
      }

      int order = 0;
      if (leftmost != null) {
         priority.put(leftmost, order++);
      }

      if (rightmost != null && rightmost != leftmost) {
         priority.put(rightmost, order);
      }

      return priority;
   }

   public static Map<KeyboardKey, Integer> buildRowCenterOutPriority(Collection<KeyboardKey> physicalKeys, Collection<KeyboardKey> visibleKeys) {
      Map<Integer, List<KeyboardKey>> keysByRow = new LinkedHashMap<>();

      for (KeyboardKey key : physicalKeys) {
         if (!key.isMouseKey) {
            keysByRow.computeIfAbsent(key.y, ignored -> new ArrayList<>()).add(key);
         }
      }

      Set<KeyboardKey> visibleSet = new HashSet<>();

      for (KeyboardKey keyx : visibleKeys) {
         if (!keyx.isMouseKey) {
            visibleSet.add(keyx);
         }
      }

      Map<Integer, Integer> rowPriority = buildCenterOutRowPriority(physicalKeys);
      Map<KeyboardKey, Integer> priority = new LinkedHashMap<>();
      Map<Integer, List<KeyboardKey>> sweepByRow = new LinkedHashMap<>();

      for (List<KeyboardKey> rowKeys : keysByRow.values()) {
         List<KeyboardKey> rowByX = new ArrayList<>(rowKeys);
         rowByX.sort((a, b) -> Integer.compare(a.x, b.x));
         List<KeyboardKey> sweepOrder = new ArrayList<>();
         int startIndex = Math.max(0, (rowByX.size() - 1) / 2);

         for (int offset = 0; offset < rowByX.size(); offset++) {
            int rightIndex = startIndex + offset;
            if (rightIndex < rowByX.size()) {
               sweepOrder.add(rowByX.get(rightIndex));
            }

            if (offset != 0) {
               int leftIndex = startIndex - offset;
               if (leftIndex >= 0) {
                  sweepOrder.add(rowByX.get(leftIndex));
               }
            }
         }

         sweepByRow.put(rowKeys.get(0).y, sweepOrder);
      }

      List<Integer> orderedRows = new ArrayList<>(sweepByRow.keySet());
      orderedRows.sort((a, b) -> Integer.compare(rowPriority.getOrDefault(a, Integer.MAX_VALUE), rowPriority.getOrDefault(b, Integer.MAX_VALUE)));
      int order = 0;
      int maxSlots = 0;

      for (List<KeyboardKey> row : sweepByRow.values()) {
         maxSlots = Math.max(maxSlots, row.size());
      }

      for (int slot = 0; slot < maxSlots; slot++) {
         for (int rowY : orderedRows) {
            List<KeyboardKey> row = sweepByRow.get(rowY);
            if (slot < row.size()) {
               KeyboardKey keyxx = row.get(slot);
               if (visibleSet.contains(keyxx)) {
                  priority.put(keyxx, order++);
               }
            }
         }
      }

      return priority;
   }

   public static int compareKeyboardKeys(KeyboardKey a, KeyboardKey b, Map<KeyboardKey, Integer> placementPriority) {
      int pa = placementPriority.getOrDefault(a, Integer.MAX_VALUE);
      int pb = placementPriority.getOrDefault(b, Integer.MAX_VALUE);
      if (pa != pb) {
         return Integer.compare(pa, pb);
      } else {
         return a.y != b.y ? Integer.compare(a.y, b.y) : Integer.compare(a.x, b.x);
      }
   }

   public static int compareKeyboardKeys(
      KeyboardKey a, KeyboardKey b, Map<KeyboardKey, Integer> edgePriority, Map<Integer, Integer> rowPriority, Map<KeyboardKey, Integer> rowCenterOutPriority
   ) {
      int ea = edgePriority.getOrDefault(a, Integer.MAX_VALUE);
      int eb = edgePriority.getOrDefault(b, Integer.MAX_VALUE);
      if (ea != eb) {
         return Integer.compare(ea, eb);
      } else {
         int pa = rowPriority.getOrDefault(a.y, Integer.MAX_VALUE);
         int pb = rowPriority.getOrDefault(b.y, Integer.MAX_VALUE);
         if (pa != pb) {
            return Integer.compare(pa, pb);
         } else {
            int oa = rowCenterOutPriority.getOrDefault(a, Integer.MAX_VALUE);
            int ob = rowCenterOutPriority.getOrDefault(b, Integer.MAX_VALUE);
            if (oa != ob) {
               return Integer.compare(oa, ob);
            } else {
               return a.y != b.y ? Integer.compare(a.y, b.y) : Integer.compare(a.x, b.x);
            }
         }
      }
   }

   public static int mouseKeySortOrder(int glfwKey) {
      int mbo = -100;
      if (glfwKey == mbo + 1) {
         return 0;
      } else if (glfwKey == mbo + 0) {
         return 1;
      } else if (glfwKey == mbo + 4) {
         return 2;
      } else {
         return glfwKey == mbo + 3 ? 3 : 4;
      }
   }

   private static Map<Integer, List<KeyboardKey>> buildKeyboardRows(Collection<KeyboardKey> keys) {
      Map<Integer, List<KeyboardKey>> rows = new LinkedHashMap<>();

      for (KeyboardKey key : keys) {
         if (!key.isMouseKey) {
            rows.computeIfAbsent(key.y, ignored -> new ArrayList<>()).add(key);
         }
      }

      for (List<KeyboardKey> row : rows.values()) {
         row.sort((a, b) -> Integer.compare(a.x, b.x));
      }

      return rows;
   }

   private static int estimateColumnStep2(Map<Integer, List<KeyboardKey>> rows) {
      List<Integer> steps2 = new ArrayList<>();

      for (List<KeyboardKey> row : rows.values()) {
         for (int index = 1; index < row.size(); index++) {
            int step2 = center2X(row.get(index)) - center2X(row.get(index - 1));
            if (step2 > 0) {
               steps2.add(step2);
            }
         }
      }

      if (steps2.isEmpty()) {
         return 34;
      } else {
         steps2.sort(Integer::compareTo);
         return steps2.get(steps2.size() / 2);
      }
   }

   private static int findBestRowOffset2(List<KeyboardKey> rowKeys, List<Integer> referenceCenters2, int mergeThreshold2) {
      Set<Integer> candidateOffsets2 = new HashSet<>();
      candidateOffsets2.add(0);

      for (KeyboardKey key : rowKeys) {
         int keyCenter2 = center2X(key);

         for (int referenceCenter2 : referenceCenters2) {
            candidateOffsets2.add(referenceCenter2 - keyCenter2);
         }
      }

      int bestOffset2 = 0;
      int bestMatches = Integer.MIN_VALUE;
      int bestDistance = Integer.MAX_VALUE;
      int bestMagnitude = Integer.MAX_VALUE;

      for (int candidateOffset2 : candidateOffsets2) {
         int matches = 0;
         int totalDistance = 0;

         for (KeyboardKey key : rowKeys) {
            int distance = nearestDistance(referenceCenters2, center2X(key) + candidateOffset2);
            totalDistance += distance;
            if (distance <= mergeThreshold2) {
               matches++;
            }
         }

         int magnitude = Math.abs(candidateOffset2);
         if (matches > bestMatches
            || matches == bestMatches && totalDistance < bestDistance
            || matches == bestMatches && totalDistance == bestDistance && magnitude < bestMagnitude) {
            bestOffset2 = candidateOffset2;
            bestMatches = matches;
            bestDistance = totalDistance;
            bestMagnitude = magnitude;
         }
      }

      return bestOffset2;
   }

   private static int nearestDistance(List<Integer> referenceCenters2, int targetCenter2) {
      int bestDistance = Integer.MAX_VALUE;

      for (int referenceCenter2 : referenceCenters2) {
         bestDistance = Math.min(bestDistance, Math.abs(referenceCenter2 - targetCenter2));
      }

      return bestDistance;
   }

   private static List<Integer> collapseCenters(List<Integer> centers2, int mergeThreshold2) {
      if (centers2.isEmpty()) {
         return List.of();
      } else {
         centers2.sort(Integer::compareTo);
         List<Integer> collapsed = new ArrayList<>();
         int clusterSum = centers2.get(0);
         int clusterCount = 1;

         for (int index = 1; index < centers2.size(); index++) {
            int center2 = centers2.get(index);
            int clusterAverage = clusterSum / clusterCount;
            if (Math.abs(center2 - clusterAverage) <= mergeThreshold2) {
               clusterSum += center2;
               clusterCount++;
            } else {
               collapsed.add(clusterAverage);
               clusterSum = center2;
               clusterCount = 1;
            }
         }

         collapsed.add(clusterSum / clusterCount);
         return collapsed;
      }
   }

   private static List<Integer> buildCenterOutColumnIndexes(int columnCount, int centerColumnIndex) {
      List<Integer> order = new ArrayList<>();
      if (columnCount <= 0) {
         return order;
      } else {
         order.add(centerColumnIndex);

         for (int offset = 1; order.size() < columnCount; offset++) {
            int left = centerColumnIndex - offset;
            if (left >= 0) {
               order.add(left);
            }

            int right = centerColumnIndex + offset;
            if (right < columnCount) {
               order.add(right);
            }
         }

         return order;
      }
   }

   private static int findNearestColumnIndex(List<Integer> columnCenters2, int targetCenter2) {
      int bestIndex = 0;
      int bestDistance = Math.abs(columnCenters2.get(0) - targetCenter2);

      for (int index = 1; index < columnCenters2.size(); index++) {
         int distance = Math.abs(columnCenters2.get(index) - targetCenter2);
         if (distance < bestDistance) {
            bestIndex = index;
            bestDistance = distance;
         }
      }

      return bestIndex;
   }

   private static int center2X(KeyboardKey key) {
      return key.x * 2 + key.width;
   }
}
