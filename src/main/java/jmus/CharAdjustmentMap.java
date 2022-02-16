package jmus;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import js.geometry.IRect;

import static js.base.Tools.*;

public final class CharAdjustmentMap {

  public static CharAdjustmentMap forFont(Font font) {
    String key = fontKey(font);
    CharAdjustmentMap adjustmentMap = sMaps.get(key);
    if (adjustmentMap == null) {
      adjustmentMap = new CharAdjustmentMap();
      BufferedImage canvas = TextUtil.constructCanvasImage();
      Graphics2D g = canvas.createGraphics();
      g.setFont(font);
      FontMetrics metr = g.getFontMetrics();

      String str = sAlphabet;
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        IRect bounds = TextUtil.calcCharacterBounds(g, canvas, metr, c);
        adjustmentMap.mCharInfoMap.put(c, bounds);
      }
      sMaps.put(key, adjustmentMap);
    }
    return adjustmentMap;
  }

  private static String fontKey(Font font) {
    return font.getName() + "_" + font.getSize() + "_" + font.getStyle();
  }

  private static Map<String, CharAdjustmentMap> sMaps = hashMap();
  private static String sAlphabet = "ABCDEFG♭♯⁻⁺ᵒ²⁴⁵⁶⁷⁹";

  private Map<Character, IRect> mCharInfoMap = hashMap();
}
