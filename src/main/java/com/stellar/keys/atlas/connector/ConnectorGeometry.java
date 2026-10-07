package com.stellar.keys.atlas.connector;

import com.stellar.keys.atlas.box.BoxPosition;
import com.stellar.keys.atlas.layout.KeyboardKey;

public final class ConnectorGeometry {
   public final int bindingIndex;
   public final KeyboardKey button;
   public final BoxPosition boxPos;
   public final boolean boxAbove;
   public final boolean boxBelow;
   public final boolean boxLeft;
   public final boolean boxRight;
   public final int nudgeX;
   public final int nudgeY;
   public final int[] waypoints;
   public final int totalLineLength;
   public final String routeMode;
   public final String routeDebug;
   public final boolean useDarkColor;

   public ConnectorGeometry(
      int bindingIndex,
      KeyboardKey button,
      BoxPosition boxPos,
      boolean boxAbove,
      boolean boxBelow,
      boolean boxLeft,
      boolean boxRight,
      int nudgeX,
      int nudgeY,
      int[] waypoints,
      int totalLineLength,
      String routeMode,
      String routeDebug
   ) {
      this(bindingIndex, button, boxPos, boxAbove, boxBelow, boxLeft, boxRight, nudgeX, nudgeY, waypoints, totalLineLength, routeMode, routeDebug, false);
   }

   public ConnectorGeometry(
      int bindingIndex,
      KeyboardKey button,
      BoxPosition boxPos,
      boolean boxAbove,
      boolean boxBelow,
      boolean boxLeft,
      boolean boxRight,
      int nudgeX,
      int nudgeY,
      int[] waypoints,
      int totalLineLength,
      String routeMode,
      String routeDebug,
      boolean useDarkColor
   ) {
      this.bindingIndex = bindingIndex;
      this.button = button;
      this.boxPos = boxPos;
      this.boxAbove = boxAbove;
      this.boxBelow = boxBelow;
      this.boxLeft = boxLeft;
      this.boxRight = boxRight;
      this.nudgeX = nudgeX;
      this.nudgeY = nudgeY;
      this.waypoints = waypoints;
      this.totalLineLength = totalLineLength;
      this.routeMode = routeMode;
      this.routeDebug = routeDebug;
      this.useDarkColor = useDarkColor;
   }
}
