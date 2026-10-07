package com.stellar.keys.atlas.layout;

public final class KeyboardKey {
   public final int x;
   public final int y;
   public final int width;
   public final int height;
   public final int glfwKey;
   public final String label;
   public final boolean isDefault;
   public final boolean isMouseKey;
   public final KeyboardKeyShape shape;

   public KeyboardKey(int x, int y, int width, int height, int glfwKey, String label, boolean isDefault) {
      this(x, y, width, height, glfwKey, label, isDefault, false, KeyboardKeyShape.rectangle());
   }

   public KeyboardKey(int x, int y, int width, int height, int glfwKey, String label, boolean isDefault, boolean isMouseKey) {
      this(x, y, width, height, glfwKey, label, isDefault, isMouseKey, KeyboardKeyShape.rectangle());
   }

   public KeyboardKey(int x, int y, int width, int height, int glfwKey, String label, boolean isDefault, boolean isMouseKey, KeyboardKeyShape shape) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.glfwKey = glfwKey;
      this.label = label;
      this.isDefault = isDefault;
      this.isMouseKey = isMouseKey;
      this.shape = shape == null ? KeyboardKeyShape.rectangle() : shape;
   }

   public boolean isHovered(int mouseX, int mouseY, int borderThickness) {
      return this.shape.containsLocal(mouseX - this.x, mouseY - this.y, this.width, this.height, borderThickness);
   }
}
