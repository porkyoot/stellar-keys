package com.stellar.keys.atlas.layout;

public final class KeyboardKeyShape {
   private static final KeyboardKeyShape RECTANGLE = new KeyboardKeyShape(0, 0, 0, 0, 0);
   public final int splitY;
   public final int topInsetLeft;
   public final int topInsetRight;
   public final int bottomInsetLeft;
   public final int bottomInsetRight;

   private KeyboardKeyShape(int splitY, int topInsetLeft, int topInsetRight, int bottomInsetLeft, int bottomInsetRight) {
      this.splitY = Math.max(0, splitY);
      this.topInsetLeft = Math.max(0, topInsetLeft);
      this.topInsetRight = Math.max(0, topInsetRight);
      this.bottomInsetLeft = Math.max(0, bottomInsetLeft);
      this.bottomInsetRight = Math.max(0, bottomInsetRight);
   }

   public static KeyboardKeyShape rectangle() {
      return RECTANGLE;
   }

   public static KeyboardKeyShape sectioned(int splitY, int topInsetLeft, int topInsetRight, int bottomInsetLeft, int bottomInsetRight) {
      return splitY <= 0 && topInsetLeft == 0 && topInsetRight == 0 && bottomInsetLeft == 0 && bottomInsetRight == 0
         ? RECTANGLE
         : new KeyboardKeyShape(splitY, topInsetLeft, topInsetRight, bottomInsetLeft, bottomInsetRight);
   }

   public boolean isRectangle() {
      return this == RECTANGLE
         || this.splitY == 0 && this.topInsetLeft == 0 && this.topInsetRight == 0 && this.bottomInsetLeft == 0 && this.bottomInsetRight == 0;
   }

   public int effectiveSplitY(int height) {
      return this.isRectangle() ? height : Math.max(0, Math.min(this.splitY, height));
   }

   public int leftInsetAt(int localY, int height) {
      return this.useTopSection(localY, height) ? this.topInsetLeft : this.bottomInsetLeft;
   }

   public int rightInsetAt(int localY, int height) {
      return this.useTopSection(localY, height) ? this.topInsetRight : this.bottomInsetRight;
   }

   public boolean containsLocal(int localX, int localY, int width, int height, int borderThickness) {
      if (localY >= -borderThickness && localY < height + borderThickness) {
         int sampleY = Math.max(0, Math.min(height - 1, localY));
         int left = this.leftInsetAt(sampleY, height) - borderThickness;
         int right = width - this.rightInsetAt(sampleY, height) + borderThickness;
         return localX >= left && localX < right;
      } else {
         return false;
      }
   }

   private boolean useTopSection(int localY, int height) {
      return localY < this.effectiveSplitY(height);
   }

   @Override
   public boolean equals(Object other) {
      if (this == other) {
         return true;
      } else {
         return !(other instanceof KeyboardKeyShape that)
            ? false
            : this.splitY == that.splitY
               && this.topInsetLeft == that.topInsetLeft
               && this.topInsetRight == that.topInsetRight
               && this.bottomInsetLeft == that.bottomInsetLeft
               && this.bottomInsetRight == that.bottomInsetRight;
      }
   }

   @Override
   public int hashCode() {
      int result = this.splitY;
      result = 31 * result + this.topInsetLeft;
      result = 31 * result + this.topInsetRight;
      result = 31 * result + this.bottomInsetLeft;
      return 31 * result + this.bottomInsetRight;
   }
}
