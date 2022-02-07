package jmus;

import static jmus.MusUtil.*;
import static js.base.Tools.*;

import java.util.List;

import js.graphics.Paint;

public final class Style {
  final Paint paintChord;
  final Paint paintChordSmall;
  final Paint paintBarFrame;
  final int meanChordWidthPixels;
  final int chordHeight;
  final int dashHeight;
  final int barPadY;
  final int barPadX;
  final int chordPadX;
  final int spacingBetweenSections;
  final Paint paintTitle;
  final Paint paintSubtitle;
  final Paint paintText;
  final Paint paintSmallText;
  final int dashOffset;

  Style(Paint chord, Paint chordSmall, Paint barFrame, int meanChordWidth, int chordHt, int dashHt, int barpx,
      int barpy, int chordpx, int spaceBetwSect, Paint title, Paint subtitle, Paint text, Paint smallText,
      int dashOff) {
    paintChord = chord.build();
    paintChordSmall = chordSmall.build();
    paintBarFrame = barFrame.build();
    meanChordWidthPixels = meanChordWidth;
    chordHeight = chordHt;
    dashHeight = dashHt;
    barPadX = barpx;
    barPadY = barpy;
    chordPadX = chordpx;
    spacingBetweenSections = spaceBetwSect;
    paintTitle = title.build();
    paintSubtitle = subtitle.build();
    paintText = text.build();
    paintSmallText = smallText.build();
    dashOffset = dashOff;
  }

  private static final Paint ptChord = PAINT_NORMAL.toBuilder().font(FONT_PLAIN, 1.8f).build();
  private static final Paint ptChordSmall = ptChord.toBuilder().font(FONT_PLAIN, 1f).build();
  private static final Paint ptFrame = PAINT_NORMAL.toBuilder().color(128, 128, 128).width(3).build();

  private static final Paint ptTitle = PAINT_NORMAL.toBuilder().font(FONT_BOLD, 1.5f).build();
  private static final Paint ptSubtitle = ptTitle.toBuilder().font(FONT_BOLD, 1.0f).build();
  private static final Paint ptText = PAINT_NORMAL.toBuilder().font(FONT_PLAIN, 0.7f).build();
  private static final Paint ptSmallText = ptText.toBuilder().font(FONT_PLAIN, 0.6f).build();

  public static List<Style> styles() {
    if (sStyles == null) {
      sStyles = arrayList();

      sStyles.add(new Style( //
          ptChord.toBuilder().font(FONT_PLAIN, 1.2f), //
          ptChordSmall.toBuilder().font(FONT_PLAIN, 0.8f), //
          ptFrame.toBuilder().width(2).build(), 24, 32, 2, 10, 7, 9, 20, //
          ptTitle, ptSubtitle, ptText, ptSmallText, 14));

      sStyles.add(new Style(ptChord, ptChordSmall, ptFrame, 35, 48, 3, 15, 10, 12, 34, ptTitle, ptSubtitle,
          ptText, ptSmallText, 10));
    }
    return sStyles;
  }

  private static List<Style> sStyles;

}
