package jmus;

import static js.base.Tools.*;

import java.awt.Graphics2D;
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
    CommandSequence commands = mGraphics.getCommands();

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

  private VectorGraphics2D mGraphics;

  public void experiment() {

    Graphics2D g = graphics();
    OUR_PAINT.apply(g);

    g.drawString("Gm   Hello", PAGE_SIZE.x * .6f, 100);

    cross(PAGE_CONTENT.midX(), PAGE_CONTENT.midY());
    if (false) {
      int w = PAGE_FULL.width;
      int h = PAGE_FULL.height;
      cross(0, 0);
      cross(w, h);

      int m = 50;
      for (int x = 0; x <= w; x += m) {
        for (int y = 0; y <= h; y += m) {
          cross(x, y);
        }
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
    fill(PAGE_CONTENT.midX() - radius / 2, PAGE_CONTENT.midY() - radius / 2, radius, radius);
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
