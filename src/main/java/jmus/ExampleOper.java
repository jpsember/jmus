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

import js.parsing.DFA;
import js.parsing.Scanner;
import js.parsing.Token;
import jmus.gen.MainConfig;
import js.app.AppOper;
import js.file.Files;

public class ExampleOper extends AppOper {

  /* private */ static final int T_WS = 0, T_CR = 1, T_STRING = 2, T_CHORD = 3;

  @Override
  public String userCommand() {
    loadTools();
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
    Scanner s = new Scanner(dfa(), Files.readString(new File("_SKIP_sample.txt")));
    while (s.hasNext()) {
      Token t = s.read();
      if (t.id(T_CR))
        continue;
      pr(t);
    }
  }

  public DFA dfa() {
    if (mDFA == null)
      mDFA = new DFA(Files.readString(this.getClass(), "tokens.dfa"));
    return mDFA;
  }

  private DFA mDFA;

}
