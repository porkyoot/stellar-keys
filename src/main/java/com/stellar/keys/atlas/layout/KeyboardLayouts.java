package com.stellar.keys.atlas.layout;

import com.stellar.keys.atlas.AtlasText;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public final class KeyboardLayouts {
   private static final int BASE_KEY_WIDTH = 14;
   private static final int BASE_KEY_HEIGHT = 14;
   private static final int BASE_PADDING = 3;
   private static final int F_KEY_BASE_WIDTH = 16;
   private static final int F_KEY_GROUP_GAP = 15;
   private static final int F_ROW_EXTRA_GAP = 3;
   private static final int ISO_ENTER_LOWER_LEFT_INSET = 3;
   private static final int[] F_KEY_CODES = new int[]{290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301};
   private static final String[] F_KEY_LABELS = new String[]{"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"};
   private static final int ABNT2_BACKSPACE_BASE_WIDTH = 31;
   private static final int ABNT2_ENTER_TOP_BASE_WIDTH = 24;
   private static final int ABNT2_ENTER_BOTTOM_BASE_WIDTH = 21;
   private static final int ISO_BACKSPACE_BASE_WIDTH = 31;
   private static final int ISO_ENTER_TOP_BASE_WIDTH = 24;
   private static final int ISO_ENTER_BOTTOM_BASE_WIDTH = 21;
   private static final int ISO_LEFT_SHIFT_BASE_WIDTH = 18;
   private static final int ISO_SPACE_BASE_WIDTH = 97;
   private static final KeyboardLayouts.KeySpec ISO_TALL_ENTER_KEY = shape(
      spanRows(width(257, "ENTER", 24, false), 2), KeyboardLayouts.KeyShapeSpec.sectioned(17, 0, 0, 3, 0)
   );
   private static final KeyboardLayouts.KeySpec ISO_ENTER_ROW_SPACER = spacer(21);
   private static final List<KeyboardLayouts.KeySpec> ISO_BOTTOM_ROW = row(
      width(341, "CTRL", 21, false),
      width(343, "WIN", 17, false),
      width(342, "ALT", 17, false),
      width(32, "SPACE", 97, false),
      width(346, "ALTGR", 17, false),
      width(347, "WIN", 17, false),
      width(348, "MENU", 17, false),
      fill(345, "CTRL", false)
   );
   private static final List<List<KeyboardLayouts.KeySpec>> US_QWERTY_ROWS = List.of(
      row(
         width(96, "`", 14, false),
         width(49, "1", 14, true),
         width(50, "2", 14, true),
         width(51, "3", 14, true),
         width(52, "4", 14, true),
         width(53, "5", 14, true),
         width(54, "6", 14, true),
         width(55, "7", 14, true),
         width(56, "8", 14, true),
         width(57, "9", 14, true),
         width(48, "0", 14, false),
         width(45, "-", 14, false),
         width(61, "=", 14, false),
         width(259, "BKSP", 28, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "Q", 14, false),
         width(87, "W", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Y", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "[", 14, false),
         width(93, "]", 14, false),
         width(92, "\\", 21, false)
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "A", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, ";", 14, false),
         width(39, "'", 14, false),
         fill(257, "ENTER", false)
      ),
      row(
         width(340, "SHIFT", 31, false),
         width(90, "Z", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, "M", 14, false),
         width(44, ",", 14, false),
         width(46, ".", 14, false),
         width(47, "/", 14, true),
         fill(344, "SHIFT", false)
      ),
      row(
         width(341, "CTRL", 21, false),
         width(342, "ALT", 17, false),
         width(343, "WIN", 17, false),
         width(32, "SPACE", 114, false),
         width(347, "WIN", 17, false),
         width(346, "ALT", 17, false),
         fill(345, "CTRL", false)
      )
   );
   private static final List<List<KeyboardLayouts.KeySpec>> ABNT2_ROWS_TALL_ENTER = List.of(
      row(
         width(96, "'", 14, false),
         width(49, "1", 14, true),
         width(50, "2", 14, true),
         width(51, "3", 14, true),
         width(52, "4", 14, true),
         width(53, "5", 14, true),
         width(54, "6", 14, true),
         width(55, "7", 14, true),
         width(56, "8", 14, true),
         width(57, "9", 14, false),
         width(48, "0", 14, false),
         width(45, "-", 14, false),
         width(61, "=", 14, false),
         width(259, "BKSP", 31, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "Q", 14, false),
         width(87, "W", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Y", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "´", 14, false),
         width(93, "[", 14, false),
         shape(spanRows(width(257, "ENTER", 24, false), 2), KeyboardLayouts.KeyShapeSpec.sectioned(17, 0, 0, 3, 0))
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "A", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, "Ç", 14, false),
         width(39, "~", 14, false),
         width(92, "]", 14, false),
         spacer(21)
      ),
      row(
         width(340, "SHIFT", 18, false),
         width(162, "\\", 14, false),
         width(90, "Z", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, "M", 14, false),
         width(44, ",", 14, false),
         width(46, ".", 14, false),
         width(47, ";", 14, false),
         width(161, "/", 14, true),
         fill(344, "SHIFT", false)
      ),
      row(
         width(341, "CTRL", 21, false),
         width(343, "WIN", 17, false),
         width(342, "ALT", 17, false),
         width(32, "SPACE", 97, false),
         width(346, "ALTGR", 17, false),
         width(347, "WIN", 17, false),
         width(348, "MENU", 17, false),
         fill(345, "CTRL", false)
      )
   );
   private static final List<List<KeyboardLayouts.KeySpec>> UK_QWERTY_ROWS_TALL_ENTER = List.of(
      row(
         width(96, "`", 14, false),
         width(49, "1", 14, true),
         width(50, "2", 14, true),
         width(51, "3", 14, true),
         width(52, "4", 14, true),
         width(53, "5", 14, true),
         width(54, "6", 14, true),
         width(55, "7", 14, true),
         width(56, "8", 14, true),
         width(57, "9", 14, false),
         width(48, "0", 14, false),
         width(45, "-", 14, false),
         width(61, "=", 14, false),
         width(259, "BKSP", 31, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "Q", 14, false),
         width(87, "W", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Y", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "[", 14, false),
         width(93, "]", 14, false),
         ISO_TALL_ENTER_KEY
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "A", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, ";", 14, false),
         width(39, "@", 14, false),
         width(92, "#", 14, false),
         ISO_ENTER_ROW_SPACER
      ),
      row(
         width(340, "SHIFT", 18, false),
         width(162, "\\", 14, false),
         width(90, "Z", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, "M", 14, false),
         width(44, ",", 14, false),
         width(46, ".", 14, false),
         width(47, "/", 14, true),
         fill(344, "SHIFT", false)
      ),
      ISO_BOTTOM_ROW
   );
   private static final List<List<KeyboardLayouts.KeySpec>> FRENCH_AZERTY_ROWS_TALL_ENTER = List.of(
      row(
         width(96, "²", 14, false),
         width(49, "&", 14, true),
         width(50, "é", 14, true),
         width(51, "\"", 14, true),
         width(52, "'", 14, true),
         width(53, "(", 14, true),
         width(54, "-", 14, true),
         width(55, "è", 14, true),
         width(56, "_", 14, true),
         width(57, "ç", 14, false),
         width(48, "à", 14, false),
         width(45, ")", 14, false),
         width(61, "=", 14, false),
         width(259, "BKSP", 31, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "A", 14, false),
         width(87, "Z", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Y", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "^", 14, false),
         width(93, "$", 14, false),
         ISO_TALL_ENTER_KEY
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "Q", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, "M", 14, false),
         width(39, "Ù", 14, false),
         width(92, "*", 14, false),
         ISO_ENTER_ROW_SPACER
      ),
      row(
         width(340, "SHIFT", 18, false),
         width(162, "<", 14, false),
         width(90, "W", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, ",", 14, false),
         width(44, ";", 14, false),
         width(46, ":", 14, false),
         width(47, "!", 14, true),
         fill(344, "SHIFT", false)
      ),
      ISO_BOTTOM_ROW
   );
   private static final List<List<KeyboardLayouts.KeySpec>> BELGIAN_AZERTY_ROWS_TALL_ENTER = List.of(
      row(
         width(96, "²", 14, false),
         width(49, "&", 14, true),
         width(50, "é", 14, true),
         width(51, "\"", 14, true),
         width(52, "'", 14, true),
         width(53, "(", 14, true),
         width(54, "§", 14, true),
         width(55, "è", 14, true),
         width(56, "!", 14, true),
         width(57, "ç", 14, false),
         width(48, "à", 14, false),
         width(45, ")", 14, false),
         width(61, "-", 14, false),
         width(259, "BKSP", 31, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "A", 14, false),
         width(87, "Z", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Y", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "^", 14, false),
         width(93, "$", 14, false),
         ISO_TALL_ENTER_KEY
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "Q", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, "M", 14, false),
         width(39, "ù", 14, false),
         width(92, "µ", 14, false),
         ISO_ENTER_ROW_SPACER
      ),
      row(
         width(340, "SHIFT", 18, false),
         width(162, "<", 14, false),
         width(90, "W", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, ",", 14, false),
         width(44, ";", 14, false),
         width(46, ":", 14, false),
         width(47, "=", 14, true),
         fill(344, "SHIFT", false)
      ),
      ISO_BOTTOM_ROW
   );
   private static final List<List<KeyboardLayouts.KeySpec>> GERMAN_QWERTZ_ROWS_TALL_ENTER = List.of(
      row(
         width(96, "^", 14, false),
         width(49, "1", 14, true),
         width(50, "2", 14, true),
         width(51, "3", 14, true),
         width(52, "4", 14, true),
         width(53, "5", 14, true),
         width(54, "6", 14, true),
         width(55, "7", 14, true),
         width(56, "8", 14, true),
         width(57, "9", 14, false),
         width(48, "0", 14, false),
         width(45, "ß", 14, false),
         width(61, "´", 14, false),
         width(259, "BKSP", 31, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "Q", 14, false),
         width(87, "W", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Z", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "Ü", 14, false),
         width(93, "+", 14, false),
         ISO_TALL_ENTER_KEY
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "A", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, "Ö", 14, false),
         width(39, "Ä", 14, false),
         width(92, "#", 14, false),
         ISO_ENTER_ROW_SPACER
      ),
      row(
         width(340, "SHIFT", 18, false),
         width(162, "<", 14, false),
         width(90, "Y", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, "M", 14, false),
         width(44, ",", 14, false),
         width(46, ".", 14, false),
         width(47, "-", 14, true),
         fill(344, "SHIFT", false)
      ),
      ISO_BOTTOM_ROW
   );
   private static final List<List<KeyboardLayouts.KeySpec>> SWISS_QWERTZ_ROWS_TALL_ENTER = List.of(
      row(
         width(96, "§", 14, false),
         width(49, "1", 14, true),
         width(50, "2", 14, true),
         width(51, "3", 14, true),
         width(52, "4", 14, true),
         width(53, "5", 14, true),
         width(54, "6", 14, true),
         width(55, "7", 14, true),
         width(56, "8", 14, true),
         width(57, "9", 14, false),
         width(48, "0", 14, false),
         width(45, "'", 14, false),
         width(61, "^", 14, false),
         width(259, "BKSP", 31, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "Q", 14, false),
         width(87, "W", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Z", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "Ü", 14, false),
         width(93, "¨", 14, false),
         ISO_TALL_ENTER_KEY
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "A", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, "Ö", 14, false),
         width(39, "Ä", 14, false),
         width(92, "$", 14, false),
         ISO_ENTER_ROW_SPACER
      ),
      row(
         width(340, "SHIFT", 18, false),
         width(162, "<", 14, false),
         width(90, "Y", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, "M", 14, false),
         width(44, ",", 14, false),
         width(46, ".", 14, false),
         width(47, "-", 14, true),
         fill(344, "SHIFT", false)
      ),
      ISO_BOTTOM_ROW
   );
   private static final List<List<KeyboardLayouts.KeySpec>> SPANISH_ROWS_TALL_ENTER = List.of(
      row(
         width(96, "º", 14, false),
         width(49, "1", 14, true),
         width(50, "2", 14, true),
         width(51, "3", 14, true),
         width(52, "4", 14, true),
         width(53, "5", 14, true),
         width(54, "6", 14, true),
         width(55, "7", 14, true),
         width(56, "8", 14, true),
         width(57, "9", 14, false),
         width(48, "0", 14, false),
         width(45, "'", 14, false),
         width(61, "¡", 14, false),
         width(259, "BKSP", 31, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "Q", 14, false),
         width(87, "W", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Y", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "´", 14, false),
         width(93, "+", 14, false),
         ISO_TALL_ENTER_KEY
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "A", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, "Ñ", 14, false),
         width(39, "´", 14, false),
         width(92, "Ç", 14, false),
         ISO_ENTER_ROW_SPACER
      ),
      row(
         width(340, "SHIFT", 18, false),
         width(162, "<", 14, false),
         width(90, "Z", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, "M", 14, false),
         width(44, ",", 14, false),
         width(46, ".", 14, false),
         width(47, "-", 14, true),
         fill(344, "SHIFT", false)
      ),
      ISO_BOTTOM_ROW
   );
   private static final List<List<KeyboardLayouts.KeySpec>> ITALIAN_ROWS_TALL_ENTER = List.of(
      row(
         width(96, "\\", 14, false),
         width(49, "1", 14, true),
         width(50, "2", 14, true),
         width(51, "3", 14, true),
         width(52, "4", 14, true),
         width(53, "5", 14, true),
         width(54, "6", 14, true),
         width(55, "7", 14, true),
         width(56, "8", 14, true),
         width(57, "9", 14, false),
         width(48, "0", 14, false),
         width(45, "'", 14, false),
         width(61, "ì", 14, false),
         width(259, "BKSP", 31, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "Q", 14, false),
         width(87, "W", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Y", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "è", 14, false),
         width(93, "+", 14, false),
         ISO_TALL_ENTER_KEY
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "A", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, "ò", 14, false),
         width(39, "à", 14, false),
         width(92, "ù", 14, false),
         ISO_ENTER_ROW_SPACER
      ),
      row(
         width(340, "SHIFT", 18, false),
         width(162, "<", 14, false),
         width(90, "Z", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, "M", 14, false),
         width(44, ",", 14, false),
         width(46, ".", 14, false),
         width(47, "-", 14, true),
         fill(344, "SHIFT", false)
      ),
      ISO_BOTTOM_ROW
   );
   private static final List<List<KeyboardLayouts.KeySpec>> NORDIC_ISO_ROWS_TALL_ENTER = List.of(
      row(
         width(96, "§", 14, false),
         width(49, "1", 14, true),
         width(50, "2", 14, true),
         width(51, "3", 14, true),
         width(52, "4", 14, true),
         width(53, "5", 14, true),
         width(54, "6", 14, true),
         width(55, "7", 14, true),
         width(56, "8", 14, true),
         width(57, "9", 14, false),
         width(48, "0", 14, false),
         width(45, "+", 14, false),
         width(61, "¨", 14, false),
         width(259, "BKSP", 31, false)
      ),
      row(
         width(258, "TAB", 21, false),
         width(81, "Q", 14, false),
         width(87, "W", 14, true),
         width(69, "E", 14, false),
         width(82, "R", 14, false),
         width(84, "T", 14, true),
         width(89, "Y", 14, false),
         width(85, "U", 14, false),
         width(73, "I", 14, false),
         width(79, "O", 14, false),
         width(80, "P", 14, false),
         width(91, "Å", 14, false),
         width(93, "^", 14, false),
         ISO_TALL_ENTER_KEY
      ),
      row(
         width(280, "CAPS", 24, false),
         width(65, "A", 14, true),
         width(83, "S", 14, true),
         width(68, "D", 14, true),
         width(70, "F", 14, false),
         width(71, "G", 14, false),
         width(72, "H", 14, false),
         width(74, "J", 14, false),
         width(75, "K", 14, false),
         width(76, "L", 14, false),
         width(59, "Ö", 14, false),
         width(39, "Ä", 14, false),
         width(92, "'", 14, false),
         ISO_ENTER_ROW_SPACER
      ),
      row(
         width(340, "SHIFT", 18, false),
         width(162, "<", 14, false),
         width(90, "Z", 14, false),
         width(88, "X", 14, false),
         width(67, "C", 14, false),
         width(86, "V", 14, false),
         width(66, "B", 14, false),
         width(78, "N", 14, false),
         width(77, "M", 14, false),
         width(44, ",", 14, false),
         width(46, ".", 14, false),
         width(47, "-", 14, true),
         fill(344, "SHIFT", false)
      ),
      ISO_BOTTOM_ROW
   );
   public static final int MOUSE_SCROLL_UP = -200;
   public static final int MOUSE_SCROLL_DOWN = -201;

   private KeyboardLayouts() {
   }

   public static List<String> defaultHiddenBindingLabels() {
      return defaultHiddenBindingLabels(KeyboardLayouts.MainLayoutPreset.US_QWERTY);
   }

   public static List<String> defaultHiddenBindingLabels(KeyboardLayouts.MainLayoutPreset preset) {
      List<String> labels = new ArrayList<>();

      for (List<KeyboardLayouts.KeySpec> row : preset.rows()) {
         for (KeyboardLayouts.KeySpec spec : row) {
            if (!spec.spacer && spec.isDefault && !labels.contains(spec.label)) {
               labels.add(spec.label);
            }
         }
      }

      return List.copyOf(labels);
   }

   public static String canonicalHiddenBindingLabel(int glfwKey) {
      String label = keyLabelForGlfwKey(US_QWERTY_ROWS, glfwKey);
      return label != null && !label.isBlank() ? label : "";
   }

   private static String keyLabelForGlfwKey(List<List<KeyboardLayouts.KeySpec>> rows, int glfwKey) {
      for (List<KeyboardLayouts.KeySpec> row : rows) {
         for (KeyboardLayouts.KeySpec spec : row) {
            if (!spec.spacer && spec.glfwKey == glfwKey) {
               return spec.label;
            }
         }
      }

      return null;
   }

   public static KeyboardLayout buildCenteredUsQwerty(int screenWidth, int screenHeight, int scale, boolean showFKeys) {
      return buildCenteredMainLayout(screenWidth, screenHeight, scale, showFKeys, KeyboardLayouts.MainLayoutPreset.US_QWERTY);
   }

   public static KeyboardLayout buildCenteredMainLayout(
      int screenWidth, int screenHeight, int scale, boolean showFKeys, KeyboardLayouts.MainLayoutPreset preset
   ) {
      List<List<KeyboardLayouts.KeySpec>> rows = preset.rows();
      int screenCenterX = screenWidth / 2;
      int screenCenterY = screenHeight / 2;
      int keyHeight = 14 * scale;
      int gap = scaledPadding(scale);
      int rowStep = keyHeight + gap;
      int totalWidth = computeReferenceRowWidth(scale, gap, rows);
      int baseTotalWidth = computeReferenceRowWidth(1, 3, rows);
      int baseCenteredHeight = rows.size() * 14 + (rows.size() - 1) * 3 + 17;
      if (showFKeys) {
         baseCenteredHeight += 20;
      }

      int baseStartX = (screenWidth - baseTotalWidth) / 2;
      int baseStartY = (screenHeight - baseCenteredHeight) / 2;
      int startX = screenCenterX + (baseStartX - screenCenterX) * scale;
      int fRowGap = 6 * scale;
      int layoutTopY = screenCenterY + (baseStartY - screenCenterY) * scale;
      int startY = showFKeys ? layoutTopY + keyHeight + fRowGap : layoutTopY;
      List<KeyboardKey> keys = new ArrayList<>();
      if (showFKeys) {
         buildFKeyRow(keys, startX, layoutTopY, scale, gap);
      }

      int currentY = startY;

      for (List<KeyboardLayouts.KeySpec> row : rows) {
         int currentX = startX;

         for (int i = 0; i < row.size(); i++) {
            KeyboardLayouts.KeySpec spec = row.get(i);
            int width = spec.fillRemaining ? totalWidth - (currentX - startX) : spec.baseWidth * scale;
            int height = scaledHeight(spec, keyHeight, gap);
            KeyboardKeyShape shape = spec.shapeSpec.scale(scale);
            if (!spec.spacer) {
               keys.add(new KeyboardKey(currentX, currentY, width, height, spec.glfwKey, spec.label, spec.isDefault, false, shape));
            }

            currentX += width;
            if (i < row.size() - 1) {
               currentX += gap;
            }
         }

         currentY += rowStep;
      }

      return createLayout(keys, 3 * scale);
   }

   public static KeyboardLayout buildAuxiliaryKeysLayout(int screenWidth, int screenHeight, int scale) {
      int keyW = 14 * scale;
      int keyH = 14 * scale;
      int gap = scaledPadding(scale);
      int centerX = screenWidth / 2;
      int centerY = screenHeight / 2;
      List<KeyboardKey> keys = new ArrayList<>();
      int cols = 7;
      int rows = 5;
      int extraGap = gap * 2;
      int numpadStartCol = 3;
      int mouseGridW = 62;
      int mouseGridH = 82;
      int mouseW = mouseGridW * scale;
      int mouseH = mouseGridH * scale;
      int baseGap = 3;
      int baseKeyW = 14;
      int baseExtraGap = baseGap * 2;
      int baseKbWidth = cols * baseKeyW + (cols - 1) * baseGap + baseExtraGap;
      int baseKbHeight = rows * 14 + (rows - 1) * baseGap;
      int baseTotalH = Math.max(baseKbHeight, mouseGridH);
      int baseMouseRightMargin = baseGap * 12;
      int baseKbOffsetFromCenter = baseGap * 6;
      int baseKbStartX = Math.max(baseGap * 3, centerX - baseKbWidth - baseKbOffsetFromCenter);
      int baseMouseX = Math.max(baseKbStartX + baseKbWidth + baseGap * 8, screenWidth - baseMouseRightMargin - mouseGridW);
      int baseStartY = centerY - baseTotalH / 2;
      int baseKbStartY = baseStartY + (baseTotalH - baseKbHeight) / 2;
      int baseMouseY = baseStartY + (baseTotalH - mouseGridH) / 2;
      int kbStartX = centerX + (baseKbStartX - centerX) * scale;
      int mouseX = centerX + (baseMouseX - centerX) * scale;
      int kbStartY = centerY + (baseKbStartY - centerY) * scale;
      int mouseY = centerY + (baseMouseY - centerY) * scale;
      IntFunction<Integer> cellX = col -> {
         int base = kbStartX + col * (keyW + gap);
         return col >= numpadStartCol ? base + extraGap : base;
      };
      IntFunction<Integer> cellY = row -> kbStartY + row * (keyH + gap);
      keys.add(new KeyboardKey(cellX.apply(0), cellY.apply(0), keyW, keyH, 260, "INS", false));
      keys.add(new KeyboardKey(cellX.apply(1), cellY.apply(0), keyW, keyH, 268, "HOME", false));
      keys.add(new KeyboardKey(cellX.apply(2), cellY.apply(0), keyW, keyH, 266, "PGUP", false));
      keys.add(new KeyboardKey(cellX.apply(3), cellY.apply(0), keyW, keyH, 282, "NUM", false));
      keys.add(new KeyboardKey(cellX.apply(4), cellY.apply(0), keyW, keyH, 331, "/", false));
      keys.add(new KeyboardKey(cellX.apply(5), cellY.apply(0), keyW, keyH, 332, "*", false));
      keys.add(new KeyboardKey(cellX.apply(6), cellY.apply(0), keyW, keyH, 333, "-", false));
      keys.add(new KeyboardKey(cellX.apply(0), cellY.apply(1), keyW, keyH, 261, "DEL", false));
      keys.add(new KeyboardKey(cellX.apply(1), cellY.apply(1), keyW, keyH, 269, "END", false));
      keys.add(new KeyboardKey(cellX.apply(2), cellY.apply(1), keyW, keyH, 267, "PGDN", false));
      keys.add(new KeyboardKey(cellX.apply(3), cellY.apply(1), keyW, keyH, 327, "7", false));
      keys.add(new KeyboardKey(cellX.apply(4), cellY.apply(1), keyW, keyH, 328, "8", false));
      keys.add(new KeyboardKey(cellX.apply(5), cellY.apply(1), keyW, keyH, 329, "9", false));
      keys.add(new KeyboardKey(cellX.apply(6), cellY.apply(1), keyW, keyH * 2 + gap, 334, "+", false));
      keys.add(new KeyboardKey(cellX.apply(3), cellY.apply(2), keyW, keyH, 324, "4", false));
      keys.add(new KeyboardKey(cellX.apply(4), cellY.apply(2), keyW, keyH, 325, "5", false));
      keys.add(new KeyboardKey(cellX.apply(5), cellY.apply(2), keyW, keyH, 326, "6", false));
      keys.add(new KeyboardKey(cellX.apply(1), cellY.apply(3), keyW, keyH, 265, "^", false));
      keys.add(new KeyboardKey(cellX.apply(3), cellY.apply(3), keyW, keyH, 321, "1", false));
      keys.add(new KeyboardKey(cellX.apply(4), cellY.apply(3), keyW, keyH, 322, "2", false));
      keys.add(new KeyboardKey(cellX.apply(5), cellY.apply(3), keyW, keyH, 323, "3", false));
      keys.add(new KeyboardKey(cellX.apply(6), cellY.apply(3), keyW, keyH * 2 + gap, 335, "Enter", false));
      keys.add(new KeyboardKey(cellX.apply(0), cellY.apply(4), keyW, keyH, 263, "<", false));
      keys.add(new KeyboardKey(cellX.apply(1), cellY.apply(4), keyW, keyH, 264, "v", false));
      keys.add(new KeyboardKey(cellX.apply(2), cellY.apply(4), keyW, keyH, 262, ">", false));
      int zeroX = cellX.apply(3);
      keys.add(new KeyboardKey(zeroX, cellY.apply(4), keyW * 2 + gap, keyH, 320, "0", false));
      keys.add(new KeyboardKey(cellX.apply(5), cellY.apply(4), keyW, keyH, 330, ".", false));
      int mbo = -100;
      keys.add(new KeyboardKey(mouseX + 7 * scale, mouseY + 32 * scale, 4 * scale, 10 * scale, mbo + 4, "MB5", false, true));
      keys.add(new KeyboardKey(mouseX + 7 * scale, mouseY + 43 * scale, 4 * scale, 10 * scale, mbo + 3, "MB4", false, true));
      keys.add(new KeyboardKey(mouseX + 12 * scale, mouseY + 1 * scale, 16 * scale, 16 * scale, -200, "S^", false, true));
      keys.add(new KeyboardKey(mouseX + 29 * scale, mouseY + 18 * scale, 4 * scale, 13 * scale, mbo + 2, "MMB", false, true));
      keys.add(new KeyboardKey(mouseX + 12 * scale, mouseY + 32 * scale, 16 * scale, 13 * scale, -201, "Sv", false, true));
      keys.add(new KeyboardKey(mouseX + 12 * scale, mouseY + 1 * scale, 16 * scale, 44 * scale, mbo + 0, "LMB", false, true));
      keys.add(new KeyboardKey(mouseX + 34 * scale, mouseY + 1 * scale, 16 * scale, 44 * scale, mbo + 1, "RMB", false, true));
      int[] mouseDevice = new int[]{mouseX, mouseY, mouseW, mouseH};
      return createLayout(keys, 3 * scale, mouseDevice);
   }

   public static int scaledPadding(int scale) {
      return 3 * scale;
   }

   private static void buildFKeyRow(List<KeyboardKey> keys, int startX, int y, int scale, int innerGap) {
      int fKeyW = 16 * scale;
      int fKeyH = 14 * scale;
      int groupGap = 15 * scale;
      int currentX = startX;

      for (int i = 0; i < F_KEY_CODES.length; i++) {
         if (i == 4 || i == 8) {
            currentX += groupGap;
         } else if (i > 0) {
            currentX += innerGap;
         }

         keys.add(new KeyboardKey(currentX, y, fKeyW, fKeyH, F_KEY_CODES[i], F_KEY_LABELS[i], false));
         currentX += fKeyW;
      }
   }

   private static KeyboardLayout createLayout(List<KeyboardKey> keys, int housingPad) {
      return createLayout(keys, housingPad, null);
   }

   private static KeyboardLayout createLayout(List<KeyboardKey> keys, int housingPad, int[] mouseDeviceBounds) {
      if (keys.isEmpty()) {
         return new KeyboardLayout(List.of(), 0, 0, 0, 0, 0, 0, 0, 0);
      } else {
         int minX = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int minY = Integer.MAX_VALUE;
         int maxY = Integer.MIN_VALUE;

         for (KeyboardKey key : keys) {
            if (!key.isMouseKey) {
               minX = Math.min(minX, key.x);
               maxX = Math.max(maxX, key.x + key.width);
               minY = Math.min(minY, key.y);
               maxY = Math.max(maxY, key.y + key.height);
            }
         }

         if (minX == Integer.MAX_VALUE) {
            return new KeyboardLayout(keys, 0, 0, 0, 0, 0, 0, 0, 0, mouseDeviceBounds);
         } else {
            int housingX = minX - housingPad;
            int housingY = minY - housingPad;
            int housingWidth = maxX - minX + housingPad * 2;
            int housingHeight = maxY - minY + housingPad * 2;
            return new KeyboardLayout(keys, minX, maxX, minY, maxY, housingX, housingY, housingWidth, housingHeight, mouseDeviceBounds);
         }
      }
   }

   private static int computeReferenceRowWidth(int scale, int gap, List<List<KeyboardLayouts.KeySpec>> rows) {
      int widest = 0;

      for (List<KeyboardLayouts.KeySpec> row : rows) {
         int width = 0;

         for (int i = 0; i < row.size(); i++) {
            KeyboardLayouts.KeySpec spec = row.get(i);
            if (!spec.fillRemaining) {
               width += spec.baseWidth * scale;
            }

            if (i < row.size() - 1) {
               width += gap;
            }
         }

         widest = Math.max(widest, width);
      }

      return widest;
   }

   private static List<KeyboardLayouts.KeySpec> row(KeyboardLayouts.KeySpec... keys) {
      return List.of(keys);
   }

   private static int scaledHeight(KeyboardLayouts.KeySpec spec, int keyHeight, int gap) {
      return keyHeight * spec.rowSpan + gap * (spec.rowSpan - 1);
   }

   private static KeyboardLayouts.KeySpec width(int glfwKey, String label, int baseWidth, boolean isDefault) {
      return new KeyboardLayouts.KeySpec(glfwKey, label, baseWidth, false, isDefault);
   }

   private static KeyboardLayouts.KeySpec fill(int glfwKey, String label, boolean isDefault) {
      return new KeyboardLayouts.KeySpec(glfwKey, label, 0, true, isDefault, false);
   }

   private static KeyboardLayouts.KeySpec spacer(int baseWidth) {
      return new KeyboardLayouts.KeySpec(-1, "", baseWidth, false, false, true);
   }

   private static KeyboardLayouts.KeySpec shape(KeyboardLayouts.KeySpec spec, KeyboardLayouts.KeyShapeSpec shapeSpec) {
      return spec.withShape(shapeSpec);
   }

   private static KeyboardLayouts.KeySpec spanRows(KeyboardLayouts.KeySpec spec, int rowSpan) {
      return spec.withRowSpan(rowSpan);
   }

   private static final class KeyShapeSpec {
      private static final KeyboardLayouts.KeyShapeSpec RECTANGLE = new KeyboardLayouts.KeyShapeSpec(0, 0, 0, 0, 0);
      final int splitBaseY;
      final int topInsetLeft;
      final int topInsetRight;
      final int bottomInsetLeft;
      final int bottomInsetRight;

      private KeyShapeSpec(int splitBaseY, int topInsetLeft, int topInsetRight, int bottomInsetLeft, int bottomInsetRight) {
         this.splitBaseY = Math.max(0, splitBaseY);
         this.topInsetLeft = Math.max(0, topInsetLeft);
         this.topInsetRight = Math.max(0, topInsetRight);
         this.bottomInsetLeft = Math.max(0, bottomInsetLeft);
         this.bottomInsetRight = Math.max(0, bottomInsetRight);
      }

      static KeyboardLayouts.KeyShapeSpec rectangle() {
         return RECTANGLE;
      }

      static KeyboardLayouts.KeyShapeSpec sectioned(int splitBaseY, int topInsetLeft, int topInsetRight, int bottomInsetLeft, int bottomInsetRight) {
         return splitBaseY <= 0 && topInsetLeft == 0 && topInsetRight == 0 && bottomInsetLeft == 0 && bottomInsetRight == 0
            ? RECTANGLE
            : new KeyboardLayouts.KeyShapeSpec(splitBaseY, topInsetLeft, topInsetRight, bottomInsetLeft, bottomInsetRight);
      }

      KeyboardKeyShape scale(int scale) {
         return this == RECTANGLE
            ? KeyboardKeyShape.rectangle()
            : KeyboardKeyShape.sectioned(
               this.splitBaseY * scale, this.topInsetLeft * scale, this.topInsetRight * scale, this.bottomInsetLeft * scale, this.bottomInsetRight * scale
            );
      }
   }

   private static final class KeySpec {
      final int glfwKey;
      final String label;
      final int baseWidth;
      final boolean fillRemaining;
      final boolean isDefault;
      final boolean spacer;
      final int rowSpan;
      final KeyboardLayouts.KeyShapeSpec shapeSpec;

      KeySpec(int glfwKey, String label, int baseWidth, boolean fillRemaining, boolean isDefault) {
         this(glfwKey, label, baseWidth, fillRemaining, isDefault, false, 1, KeyboardLayouts.KeyShapeSpec.rectangle());
      }

      KeySpec(int glfwKey, String label, int baseWidth, boolean fillRemaining, boolean isDefault, boolean spacer) {
         this(glfwKey, label, baseWidth, fillRemaining, isDefault, spacer, 1, KeyboardLayouts.KeyShapeSpec.rectangle());
      }

      KeySpec(
         int glfwKey,
         String label,
         int baseWidth,
         boolean fillRemaining,
         boolean isDefault,
         boolean spacer,
         int rowSpan,
         KeyboardLayouts.KeyShapeSpec shapeSpec
      ) {
         this.glfwKey = glfwKey;
         this.label = label;
         this.baseWidth = baseWidth;
         this.fillRemaining = fillRemaining;
         this.isDefault = isDefault;
         this.spacer = spacer;
         this.rowSpan = Math.max(1, rowSpan);
         this.shapeSpec = shapeSpec == null ? KeyboardLayouts.KeyShapeSpec.rectangle() : shapeSpec;
      }

      KeyboardLayouts.KeySpec withRowSpan(int rowSpan) {
         return new KeyboardLayouts.KeySpec(this.glfwKey, this.label, this.baseWidth, this.fillRemaining, this.isDefault, this.spacer, rowSpan, this.shapeSpec);
      }

      KeyboardLayouts.KeySpec withShape(KeyboardLayouts.KeyShapeSpec shapeSpec) {
         return new KeyboardLayouts.KeySpec(this.glfwKey, this.label, this.baseWidth, this.fillRemaining, this.isDefault, this.spacer, this.rowSpan, shapeSpec);
      }
   }

   public static enum MainLayoutPreset {
      US_QWERTY("ui.layout.us_qwerty", "US", KeyboardLayouts.US_QWERTY_ROWS, true),
      UK_QWERTY("ui.layout.uk_qwerty", "UK", KeyboardLayouts.UK_QWERTY_ROWS_TALL_ENTER, true),
      ABNT2("ui.layout.abnt2", "BR", KeyboardLayouts.ABNT2_ROWS_TALL_ENTER, true),
      FRENCH_AZERTY("ui.layout.french_azerty", "FR", KeyboardLayouts.FRENCH_AZERTY_ROWS_TALL_ENTER, true),
      BELGIAN_AZERTY("ui.layout.belgian_azerty", "BE", KeyboardLayouts.BELGIAN_AZERTY_ROWS_TALL_ENTER, true),
      GERMAN_QWERTZ("ui.layout.german_qwertz", "DE", KeyboardLayouts.GERMAN_QWERTZ_ROWS_TALL_ENTER, true),
      SWISS_QWERTZ("ui.layout.swiss_qwertz", "CH", KeyboardLayouts.SWISS_QWERTZ_ROWS_TALL_ENTER, true),
      SPANISH("ui.layout.spanish", "ES", KeyboardLayouts.SPANISH_ROWS_TALL_ENTER, true),
      ITALIAN("ui.layout.italian", "IT", KeyboardLayouts.ITALIAN_ROWS_TALL_ENTER, true),
      NORDIC_ISO("ui.layout.nordic_iso", "NORD", KeyboardLayouts.NORDIC_ISO_ROWS_TALL_ENTER, true),
      AZERTY("ui.layout.french_azerty", "FR", KeyboardLayouts.FRENCH_AZERTY_ROWS_TALL_ENTER, false),
      QWERTZ("ui.layout.german_qwertz", "DE", KeyboardLayouts.GERMAN_QWERTZ_ROWS_TALL_ENTER, false);

      private static final List<KeyboardLayouts.MainLayoutPreset> SELECTABLE = List.of(
         US_QWERTY, UK_QWERTY, ABNT2, FRENCH_AZERTY, BELGIAN_AZERTY, GERMAN_QWERTZ, SWISS_QWERTZ, SPANISH, ITALIAN, NORDIC_ISO
      );
      private final String displayNameKey;
      private final String buttonLabel;
      private final List<List<KeyboardLayouts.KeySpec>> rows;
      private final boolean selectable;

      private MainLayoutPreset(String displayNameKey, String buttonLabel, List<List<KeyboardLayouts.KeySpec>> rows, boolean selectable) {
         this.displayNameKey = displayNameKey;
         this.buttonLabel = buttonLabel;
         this.rows = rows;
         this.selectable = selectable;
      }

      public String displayName() {
         return AtlasText.text(this.displayNameKey);
      }

      public String buttonLabel() {
         return this.buttonLabel;
      }

      public boolean selectable() {
         return this.selectable;
      }

      public static List<KeyboardLayouts.MainLayoutPreset> selectableValues() {
         return SELECTABLE;
      }

      public KeyboardLayouts.MainLayoutPreset next() {
         List<KeyboardLayouts.MainLayoutPreset> presets = selectableValues();
         KeyboardLayouts.MainLayoutPreset current = this.selectableEquivalent();
         int index = presets.indexOf(current);
         int nextIndex = index < 0 ? 0 : (index + 1) % presets.size();
         return presets.get(nextIndex);
      }

      public KeyboardLayouts.MainLayoutPreset selectableEquivalent() {
         switch (this) {
            case AZERTY:
               return FRENCH_AZERTY;
            case QWERTZ:
               return GERMAN_QWERTZ;
            default:
               return this;
         }
      }

      private List<List<KeyboardLayouts.KeySpec>> rows() {
         return this.rows;
      }
   }
}
