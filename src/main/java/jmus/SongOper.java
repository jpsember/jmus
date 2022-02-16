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

import static jmus.MusUtil.*;

import jmus.gen.MusicKey;
import jmus.gen.SongConfig;
import jmus.gen.Song;
import js.app.AppOper;
import js.file.Files;

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
  public SongConfig defaultArgs() {
    return SongConfig.DEFAULT_INSTANCE;
  }

  @Override
  public void perform() {
    mConfig = config();

    if (DEV) {
      if (false) {
        TextUtil.experiment();
        return;
      }

      generateQuiz();
      //if (true) return;
      mConfig = mConfig.toBuilder().input(new File("samples/wish.txt")) //
      //    .style(1) //
          .build();
      generateSong();
      return;
    }

    if (Files.empty(mConfig.input())) {
      generateQuiz();
    } else {
      generateSong();
    }
  }

  private void generateQuiz() {
    new QuizGenerator(mConfig).generate();
  }

  private void generateSong() {
    mSourceFile = mConfig.input();
    if (Files.empty(mSourceFile))
      setError("Please specify a source file");

    Song song = new SongParser(mSourceFile).parse();

    //pr("parsed:",INDENT,song);

    MusicKey key = null;
    if (nonEmpty(mConfig.scale()))
      key = musicKey(mConfig.scale());

    PagePlotter p = new PagePlotter();
    p.setKey(key);
    p.plotSong(song, style(mConfig.style()));

    File outFile = mConfig.output();
    if (Files.empty(outFile))
      outFile = mSourceFile;
    outFile = Files.setExtension(outFile, "png");
    p.generateOutputFile(outFile);
  }

  private SongConfig mConfig;
  private File mSourceFile;

}
