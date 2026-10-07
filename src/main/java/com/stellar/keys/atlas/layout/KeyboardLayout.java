package com.stellar.keys.atlas.layout;

import java.util.List;

public final class KeyboardLayout {
   public final List<KeyboardKey> keys;
   public final int minX;
   public final int maxX;
   public final int minY;
   public final int maxY;
   public final int housingX;
   public final int housingY;
   public final int housingWidth;
   public final int housingHeight;
   public final int[] mouseDeviceBounds;

   public KeyboardLayout(List<KeyboardKey> keys, int minX, int maxX, int minY, int maxY, int housingX, int housingY, int housingWidth, int housingHeight) {
      this(keys, minX, maxX, minY, maxY, housingX, housingY, housingWidth, housingHeight, null);
   }

   public KeyboardLayout(
      List<KeyboardKey> keys, int minX, int maxX, int minY, int maxY, int housingX, int housingY, int housingWidth, int housingHeight, int[] mouseDeviceBounds
   ) {
      this.keys = List.copyOf(keys);
      this.minX = minX;
      this.maxX = maxX;
      this.minY = minY;
      this.maxY = maxY;
      this.housingX = housingX;
      this.housingY = housingY;
      this.housingWidth = housingWidth;
      this.housingHeight = housingHeight;
      this.mouseDeviceBounds = mouseDeviceBounds;
   }
}
