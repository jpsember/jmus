package jmus;

import static js.base.Tools.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.erichseifert.vectorgraphics2d.Document;
import de.erichseifert.vectorgraphics2d.Processor;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import de.erichseifert.vectorgraphics2d.eps.EPSProcessor;
import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.pdf.PDFProcessor;
import de.erichseifert.vectorgraphics2d.util.PageSize;

import js.base.BaseObject;
import js.file.Files;
import js.geometry.IRect;
import js.graphics.ImgUtil;

import static jmus.Util.*;

/**
 * For plotting a song into a PDF file
 */
public final class PagePlotter extends BaseObject {

  public PagePlotter() {
    loadTools();
    mGraphics = new VectorGraphics2D();
  }

  public Graphics2D graphics() {
    return mGraphics;
  }

  public void generateOutputFile(File outputFile) {
    CommandSequence commands = ((VectorGraphics2D) mGraphics).getCommands();

    Processor processor;

    switch (Files.getExtension(outputFile)) {
    default:
      throw notSupported("can't handle extension:", outputFile);
    case "pdf":
      processor = new PDFProcessor();
      break;
    case "eps":
      processor = new EPSProcessor();
      break;
    }

    /**
     * <pre>
     * From the documentation:
     * 
     *  public static final PageSize A3 = new PageSize(297.0, 420.0);
        public static final PageSize A4 = new PageSize(210.0, 297.0);
        public static final PageSize A5 = new PageSize(148.0, 210.0);
        
        ...we can specify a page size ourselves instead of guessing at things.
     * 
     * </pre>
     */
    PageSize pageSize = new PageSize(850, 1100);

    Document doc = processor.getDocument(commands, pageSize);

    try {
      doc.writeTo(new FileOutputStream(outputFile));
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  private Graphics2D mGraphics;

  public void experiment() {

    BufferedImage img = ImgUtil.build(PAGE_SIZE, ImgUtil.PREFERRED_IMAGE_TYPE_COLOR);

    if (alert("rendering to normal graphics")) {
      mGraphics = img.createGraphics();
      mGraphics.setColor(Color.white);
      fill(0, 0, PAGE_SIZE.x, PAGE_SIZE.y);
    }

    Graphics2D g = graphics();
    OUR_PAINT.apply(g);

    g.drawString("Gm   Hello", PAGE_SIZE.x * .6f, 100);

    if (false)
      cross(PAGE_CONTENT.midX(), PAGE_CONTENT.midY());

    {
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

    if (true) {
      graphics().drawString("\u05E9\u05DC\u05D5\u05DD \u05E2\u05D5\u05DC\u05DD", 600, 600);
    } else {
      String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
      int y = PAGE_CONTENT.y + 20;
      boolean fs = true;
      for (String s : fonts) {
        if (y >= PAGE_CONTENT.endY())
          break;

        Font f = new Font(s, Font.PLAIN, 18);

        if (fs) {
          f = new Font(Font.MONOSPACED, Font.PLAIN, 11);
          fs = false;
        }

        graphics().setFont(f);
        FontMetrics m = g.getFontMetrics();
        graphics().drawString(s + "(E♭ A♭ F♯)", PAGE_CONTENT.x + 20, y + m.getAscent());

        char c[] = { '♭' };
        graphics().drawChars(c, 0, 1, PAGE_CONTENT.x + 180, y + m.getAscent());

        y += m.getHeight();
      }
    }

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

    ImgUtil.writeImage(Files.S, img, new File("_SKIP_experiment.png"));
    halt();
    generateOutputFile(new File("_SKIP_experiment.pdf"));
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

}
