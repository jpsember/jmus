package jmus;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import js.file.Files;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.graphics.ImgUtil;
import js.json.JSMap;

import static js.base.Tools.*;
import static jmus.MusUtil.*;

public final class TextUtil {

  private static final int CANVAS_PAD_PIXELS = 8;
  private static final IPoint CANVAS_SIZE = new IPoint(50 + 2 * CANVAS_PAD_PIXELS,
      80 + 2 * CANVAS_PAD_PIXELS);

  public static BufferedImage constructCanvasImage() {
    return ImgUtil.build(CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);
  }

  public static IRect calcCharacterBounds(Graphics2D graphics, BufferedImage image, FontMetrics fontMetrics,
      char c) {

    graphics.setBackground(Color.white);
    graphics.setColor(Color.black);

    graphics.clearRect(0, 0, CANVAS_SIZE.x, CANVAS_SIZE.y);

    int plotOriginX = CANVAS_PAD_PIXELS;
    int plotOriginY = CANVAS_PAD_PIXELS + fontMetrics.getAscent();
    char[] charArray = { c };
    graphics.drawChars(charArray, 0, 1, plotOriginX, plotOriginY);

    int xMin = CANVAS_SIZE.x;
    int xMax = 0;
    int yMin = CANVAS_SIZE.y;
    int yMax = 0;

    int[] imagePixels = ImgUtil.rgbPixels(image);
    //halt("image size:",ImgUtil.size(image),"pix len:",pix.length);
    int yStart = 0;
    int yEnd = plotOriginY + CANVAS_PAD_PIXELS;
    int xStart = 0;
    int xEnd = plotOriginX + fontMetrics.charWidth(c) + CANVAS_PAD_PIXELS;
    if (xEnd > CANVAS_SIZE.x || yEnd > CANVAS_SIZE.y)
      throw badArg("char bounds maybe extends outside of buffer");

    int bgColor = imagePixels[0];
    int scanIndex = yStart * CANVAS_SIZE.x;

    for (int scanY = yStart; scanY < yEnd; scanY++, scanIndex += CANVAS_SIZE.x) {
      for (int scanX = xStart; scanX < xEnd; scanX++) {
        if (imagePixels[scanIndex + scanX] == bgColor)
          continue;
        if (scanX < xMin)
          xMin = scanX;
        if (scanX > xMax)
          xMax = scanX;
        if (scanY < yMin)
          yMin = scanY;
        if (scanY > yMax)
          yMax = scanY;
      }
    }
    IRect bounds = new IRect(xMin - plotOriginX, yMin - plotOriginY, xMax - xMin, yMax - yMin);

    // Render the predicted and actual bounds of the character,
    // and then the character itself so it is always in the foreground
    //
    graphics.setColor(Color.red);
    int logW = fontMetrics.charWidth(c);
    graphics.drawRect(CANVAS_PAD_PIXELS, CANVAS_PAD_PIXELS, logW, fontMetrics.getAscent());

    graphics.setColor(Color.blue);
    graphics.drawRect(bounds.x + CANVAS_PAD_PIXELS, bounds.y + CANVAS_PAD_PIXELS + fontMetrics.getAscent(),
        bounds.width, bounds.height);

    graphics.setColor(Color.black);
    graphics.drawChars(charArray, 0, 1, plotOriginX, plotOriginY);
    return bounds;
  }

  public static void experiment2() {
    Font font = style(0).paintChord().font();
    BufferedImage canvas = constructCanvasImage();
    Graphics2D g = canvas.createGraphics();
    g.setFont(font);
    FontMetrics metr = g.getFontMetrics();

    JSMap m = map();
    String str = "ABCDEFG♭♯⁻⁺ᵒ²⁴⁵⁶⁷⁹";
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      IRect bounds = calcCharacterBounds(g, canvas, metr, c);
      m.put(Character.toString(c), bounds);

      ImgUtil.writeImage(Files.S, canvas, Files.getDesktopFile("char_" + str.substring(i, i + 1) + ".png"));
    }
    pr(m);
  }

  public static void experiment() {
    Font font = style(0).paintChord().font();
    CharAdjustmentMap cm = CharAdjustmentMap.forFont(font);
    pr(cm.toJson());
  }
}
