package jmus;

import static js.base.Tools.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import jmus.gen.TextEntry;
import js.base.BaseObject;
import js.file.Files;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.Matrix;
import js.graphics.ImgUtil;

import static jmus.Util.*;

/**
 * For plotting a song into a png
 */
public final class PagePlotter extends BaseObject {

  public PagePlotter() {
    loadTools();
    BufferedImage img = mImage = ImgUtil.build(PAGE_SIZE.scaledBy(DOTS_PER_INCH),
        ImgUtil.PREFERRED_IMAGE_TYPE_COLOR);
    Graphics2D g = mGraphics = img.createGraphics();
    g.setColor(Color.white);
    g.fillRect(0, 0, img.getWidth(), img.getHeight());
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setTransform(Matrix.getScale(DOTS_PER_INCH).toAffineTransform());
    PAINT_NORMAL.apply(g);
  }

  public Graphics2D graphics() {
    return mGraphics;
  }

  public BufferedImage image() {
    return mImage;
  }

  public void generateOutputFile(File outputFile) {
    ImgUtil.writeImage(Files.S, mImage, outputFile);
  }

  public void experiment() {

    Graphics2D g = graphics();
    PAINT_NORMAL.apply(g);

    // g.drawString("Gm   Hello", PAGE_SIZE.x * .6f, 100);

    if (false)
      cross(PAGE_CONTENT.midX(), PAGE_CONTENT.midY());

    if (false) {
      String[] strs = { "Gm Hello", "E♭ A♭ F♯", "Hippopotamus" };

      FontMetrics f = g.getFontMetrics();

      int lc = strs.length;
      int h = f.getHeight() * lc - f.getLeading();

      int w = 0;
      for (String s : strs) {
        w = Math.max(w, f.stringWidth(s));
      }
      IRect b = new IRect(PAGE_CONTENT.midX() - w / 2, PAGE_CONTENT.midY() - h / 2, w, h);
      rect(b);

      int py = b.y + f.getAscent();
      for (String s : strs) {
        graphics().drawString(s, b.x, py);
        py += f.getHeight();
      }
    }

    if (false)
      renderFonts(0);

    PAINT_LIGHTER.apply(g);
    if (false) {
      rect(PAGE_FULL);
      rect(PAGE_FULL.withInset(PAGE_MARGIN / 2));
    }

    int radius = 30;
    int pad = 25;
    if (false) {
      fill(PAGE_MARGIN + pad, PAGE_MARGIN + pad, radius, radius);
      fill(PAGE_SIZE.x - PAGE_MARGIN - radius - pad, PAGE_SIZE.y - PAGE_MARGIN - radius - pad, radius,
          radius);
    }
    if (false)
      fill(PAGE_CONTENT.midX() - radius / 2, PAGE_CONTENT.midY() - radius / 2, radius, radius);

    tx().text("Gm Hello");
    tx().text("E♭ A♭ F♯");
    tx().heightScale(0.3f).text("―");
    tx().text("Hippopotamus");

    renderText(new IPoint(600, 200));

    generateOutputFile(new File("_SKIP_experiment.png"));
  }

  private void renderText(IPoint center) {
    Graphics2D g = graphics();
    FontMetrics f = g.getFontMetrics();

    //    int lc = mTextEntries.size();
    //    int h = f.getHeight() * lc - f.getLeading();
    //    
    int y = 0;
    int maxWidth = 0;
    for (TextEntry.Builder tx : mTextEntries) {
      tx.yOffset(y);
      tx.renderWidth(f.stringWidth(tx.text()));
      maxWidth = Math.max(maxWidth, tx.renderWidth());
      // apparently this cast is not required, as += performs type coercion (which is strange)
      // https://stackoverflow.com/questions/8272635
      y += (int) (f.getHeight() * tx.heightScale());
    }
    int heightTotal = y - f.getLeading();

    int py = center.y - heightTotal / 2;
    int px = center.x - maxWidth / 2;
    for (TextEntry.Builder tx : mTextEntries) {
      graphics().drawString(tx.text(), px, py + tx.yOffset());
    }
    mTextEntries.clear();
  }

  // fontOffset 0, 86, 165
  //
  private void renderFonts(int fontOffset) {
    String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    int column = -1;
    int y = -1;
    int x = -1;

    while (true) {
      if (fontOffset >= fonts.length)
        break;
      String s = fonts[fontOffset];
      fontOffset++;
      if (y < 0) {
        column++;
        if (column >= 2)
          break;
        y = PAGE_CONTENT.y + 20;
        x = (int) (PAGE_CONTENT.x + 10 + column * PIXELS_PER_INCH * 4f);
      }

      Font f = new Font(s, Font.BOLD, 18);
      graphics().setFont(f);
      FontMetrics m = graphics().getFontMetrics();
      graphics().drawString(s + "(E♭ A♭ F♯)", x + 20, y + m.getAscent());
      y += m.getHeight();
      if (y + m.getAscent() >= PAGE_CONTENT.endY())
        y = -1;
    }
    pr("font offset:", fontOffset);
  }

  private void rect(IRect r) {
    graphics().drawRect(r.x, r.y, r.width, r.height);
  }

  private void fill(int x, int y, int w, int h) {
    graphics().fillRect(x, y, w, h);
  }

  private void cross(int x, int y) {
    Graphics2D g = graphics();
    int r = 4;
    g.drawLine(x - r, y, x + r, y);
    g.drawLine(x, y - r, x, y + 4);
  }

  public TextEntry.Builder tx() {
    TextEntry.Builder b = TextEntry.newBuilder();
    mTextEntries.add(b);
    return b;
  }

  private BufferedImage mImage;
  private Graphics2D mGraphics;
  private List<TextEntry.Builder> mTextEntries = arrayList();

}
