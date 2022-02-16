package jmus;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import js.file.Files;
import js.geometry.IRect;
import js.json.JSMap;

import static js.base.Tools.*;

public final class CharAdjustmentMap {

  public static CharAdjustmentMap forFont(Font font) {
    String key = fontKey(font);
    CharAdjustmentMap adjustmentMap = sMaps.get(key);
    if (adjustmentMap == null) {

      // See if it is in a cache
      File cacheFile = new File(cacheDir(), key + ".json");
      if (cacheFile.exists()) {
        files().log("reading from cache:", cacheFile);
        adjustmentMap = new CharAdjustmentMap();
        adjustmentMap.parse(JSMap.from(cacheFile));
      } else {

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
        files().writeString(cacheFile, adjustmentMap.toString());
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

  private static File cacheDir() {
    if (sCacheDir == null) {
      File dir = new File(Files.homeDirectory(), "Library/Caches");
      if (dir.exists())
        dir = new File(dir, "_charAdjustmentMapCache_");
      else
        dir = Files.getDesktopFile("_charAdjustmentMapCache_");
      sCacheDir = files().mkdirs(dir);
    }
    return sCacheDir;
  }

  private void parse(JSMap m) {
    for (String k : m.keySet()) {
      IRect r = IRect.DEFAULT_INSTANCE.parse(m.getList(k));
      mCharInfoMap.put(k.charAt(0), r);
    }
  }

  public JSMap toJson() {
    JSMap m = map();
    for (Entry<Character, IRect> ent : mCharInfoMap.entrySet()) {
      m.put(ent.getKey().toString(), ent.getValue());
    }
    return m;
  }

  @Override
  public String toString() {
    return toJson().toString();
  }

  private static Files files() {
    if (sFiles == null) {
      sFiles = new Files();
      sFiles.alertVerbose();
    }
    return sFiles;
  }

  private Map<Character, IRect> mCharInfoMap = hashMap();
  private static File sCacheDir;
  private static Files sFiles;
}
