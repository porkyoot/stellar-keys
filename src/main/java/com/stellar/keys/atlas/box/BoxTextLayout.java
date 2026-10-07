package com.stellar.keys.atlas.box;

import java.util.List;

public final class BoxTextLayout {
   public final List<BoxTextRow> rows;
   public final BoxDimensions dimensions;

   public BoxTextLayout(List<BoxTextRow> rows, BoxDimensions dimensions) {
      this.rows = List.copyOf(rows);
      this.dimensions = dimensions;
   }
}
