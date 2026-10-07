package com.stellar.keys.atlas.box;

public final class BoxPosition {
   public final int boxX;
   public final int boxY;
   public final int lineEndX;
   public final int lineEndY;
   public final int boxWidth;
   public final int boxHeight;

   public BoxPosition(int boxX, int boxY, int lineEndX, int lineEndY, int boxWidth, int boxHeight) {
      this.boxX = boxX;
      this.boxY = boxY;
      this.lineEndX = lineEndX;
      this.lineEndY = lineEndY;
      this.boxWidth = boxWidth;
      this.boxHeight = boxHeight;
   }
}
