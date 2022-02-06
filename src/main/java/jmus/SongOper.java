/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package jmus;

import static js.base.Tools.*;

import java.io.File;
import java.util.Random;

import static jmus.MusUtil.*;

import jmus.gen.Chord;
import jmus.gen.MainConfig;
import jmus.gen.Scale;
import jmus.gen.Song;
import js.app.AppOper;
import js.file.Files;
import js.geometry.IPoint;

public class SongOper extends AppOper {

  @Override
  public String userCommand() {
    return "main";
  }

  @Override
  public String getHelpDescription() {
    return "main operation";
  }

  @Override
  public MainConfig defaultArgs() {
    return MainConfig.DEFAULT_INSTANCE;
  }

  @Override
  public void perform() {
    mConfig = config();

    if (alert("experiment")) {
      experiment();
      return;
    }

    mSourceFile = mConfig.input();
    if (Files.empty(mSourceFile)) {
      setError("Please specify a source file");
    }

    Song song = new SongParser(mSourceFile).parse();

    Scale scale = null;
    if (nonEmpty(mConfig.scale()))
      scale = scale(mConfig.scale());

    PagePlotter p = new PagePlotter();
    p.render(song, scale, mConfig.style());
    File outFile = mConfig.output();
    if (Files.empty(outFile))
      outFile = mSourceFile;
    outFile = Files.setExtension(outFile, "png");
    p.generateOutputFile(outFile);
    pr("...done");
  }

  private MainConfig mConfig;
  private File mSourceFile;

  private void experiment() {
    PagePlotter p = new PagePlotter();

    Style style = style(1);

    Random r = new Random(1965);
    int x = PAGE_CONTENT.x;
    int y = PAGE_CONTENT.y;

    for (int i = 0; i < 60; i++) {

      Chord.Builder c;

      if (r.nextInt(4) == 2)
        c = chord(1 + r.nextInt(7), 1 + r.nextInt(7));
      else
        c = chord(1 + r.nextInt(7));

      Scale scale = scale("b-flat");

      x += p.plotChord(c, scale, style, new IPoint(x, y));
      if (x + 20 > PAGE_CONTENT.endX()) {
        x = PAGE_CONTENT.x;
        y += style.chordHeight;
      }
    }
    File outFile = new File("samples/experiment.png");
    p.generateOutputFile(outFile);
    pr("...done");
  }
}
