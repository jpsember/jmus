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

  public static final boolean DEBUG = false && alert("DEBUG in effect");

  // The list of characters that we will construct adjustments for
  //
  private static String sAlphabet = "ABCDEFG♭♯⁻⁺ᵒ²⁴⁵⁶⁷⁹";

  public static CharAdjustmentMap forFont(Font font) {
    String key = fontKey(font);

    // See if we have it in our memory cache
    CharAdjustmentMap adjustmentMap = sMemoryCache.get(key);
    if (adjustmentMap != null)
      return adjustmentMap;

    // See if it is in a cache
    File cacheFile = new File(cacheDir(), key + ".json");
    if (cacheFile.exists()) {
      files().log("reading from cache:", cacheFile);
      JSMap json = JSMap.from(cacheFile);
      if (sAlphabet.equals(json.opt("alphabet", ""))) {
        adjustmentMap = new CharAdjustmentMap();
        adjustmentMap.parse(json);
      } else
        files().log("cached map had wrong version");
    }
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
      files().writeString(cacheFile, adjustmentMap.toString());
    }
    sMemoryCache.put(key, adjustmentMap);
    return adjustmentMap;
  }

  /**
   * Get unique string identifying this font
   */
  private static String fontKey(Font font) {
    return font.getName() + "_" + font.getSize() + "_" + font.getStyle();
  }

  // In-memory cache of CharAdjustmentMaps
  //
  private static Map<String, CharAdjustmentMap> sMemoryCache = hashMap();

  private static Files files() {
    if (sFiles == null) {
      sFiles = new Files();
      if (DEBUG)
        sFiles.alertVerbose();
    }
    return sFiles;
  }

  /**
   * Get the directory (creating if necessary) to use as a disk cache for
   * CharAdjustmentMaps
   */
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

  private static File sCacheDir;
  private static Files sFiles;

  public JSMap toJson() {
    JSMap m = map();
    m.put("alphabet", sAlphabet);
    JSMap entries = map();
    m.put("entries", entries);
    for (Entry<Character, IRect> ent : mCharInfoMap.entrySet()) {
      entries.put(ent.getKey().toString(), ent.getValue());
    }
    return m;
  }

  @Override
  public String toString() {
    return toJson().toString();
  }

  public IRect getRect(char c) {
    return mCharInfoMap.get(c);
  }

  private void parse(JSMap m) {
    JSMap entries = m.getMap("entries");
    for (String k : entries.keySet()) {
      IRect r = IRect.DEFAULT_INSTANCE.parse(entries.getList(k));
      mCharInfoMap.put(k.charAt(0), r);
    }
  }

  private Map<Character, IRect> mCharInfoMap = hashMap();

}
