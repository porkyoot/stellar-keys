package com.stellar.keys.atlas.box;

public final class BoxTextRow {
   public final BoxTextRow.Kind kind;
   public final String text;
   public final float scale;
   public final int color;
   public final int yOffset;
   public final int height;

   private BoxTextRow(BoxTextRow.Kind kind, String text, float scale, int color, int yOffset, int height) {
      this.kind = kind;
      this.text = text;
      this.scale = scale;
      this.color = color;
      this.yOffset = yOffset;
      this.height = height;
   }

   public static BoxTextRow text(String text, float scale, int color, int yOffset, int height) {
      return new BoxTextRow(BoxTextRow.Kind.TEXT, text, scale, color, yOffset, height);
   }

   public static BoxTextRow rightText(String text, float scale, int color, int yOffset, int height) {
      return new BoxTextRow(BoxTextRow.Kind.RIGHT_TEXT, text, scale, color, yOffset, height);
   }

   public static BoxTextRow separator(int color, int yOffset, int height) {
      return new BoxTextRow(BoxTextRow.Kind.SEPARATOR, "", 1.0F, color, yOffset, height);
   }

   public static enum Kind {
      TEXT,
      RIGHT_TEXT,
      SEPARATOR;
   }
}
