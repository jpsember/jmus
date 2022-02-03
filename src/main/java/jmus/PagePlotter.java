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

    if (false)
      renderFonts(0);

    PAINT_LIGHTER.apply(g);

    tx().text("Gm Hello");
    tx().text("E♭ A♭ F♯");
    tx().heightScale(0.5f).text("~dash");
    tx().text("Hippopotamus");

    renderText(new IPoint(600, 200));

    generateOutputFile(new File("_SKIP_experiment.png"));
  }

  private void renderText(IPoint center) {
    Graphics2D g = graphics();
    FontMetrics f = g.getFontMetrics();

    int y = 0;
    int maxWidth = 0;
    for (TextEntry.Builder tx : mTextEntries) {
      tx.yOffset(y);

      if (tx.text().startsWith("~")) {
      } else
        tx.renderWidth(f.stringWidth(tx.text()));

      maxWidth = Math.max(maxWidth, tx.renderWidth());
      // apparently this cast is not required, as += performs type coercion (which is strange)
      // https://stackoverflow.com/questions/8272635
      y += (int) (f.getHeight() * tx.heightScale());
    }
    int heightTotal = y - f.getLeading();

    int py = center.y - heightTotal / 2;
    int x0 = center.x - maxWidth / 2;
    int x1 = center.x + maxWidth / 2;

    for (TextEntry.Builder tx : mTextEntries) {

      if (tx.text().startsWith("~")) {
        switch (tx.text().substring(1)) {
        default:
          throw notSupported("unknown text:", tx);
        case "dash": {
          int y0 = (int) (py + tx.yOffset() - f.getAscent() + (f.getAscent() * tx.heightScale() * 0.5f));
          graphics().drawLine(x0, y0, x1, y0);
        }
          break;
        }
      } else
        graphics().drawString(tx.text(), center.x - tx.renderWidth() / 2, py + tx.yOffset());
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
