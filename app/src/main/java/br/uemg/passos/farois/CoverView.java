package br.uemg.passos.farois;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public final class CoverView extends View {
  final Scenery art = new Scenery();

  public CoverView(Context c, AttributeSet attrs) {
    super(c, attrs);
  }

  @Override
  protected void onDraw(Canvas c) {
    float w = getWidth(), h = getHeight();
    c.save();
    c.scale(w / 480, h / 405);
    art.background(c, 5, 128, 0, false);
    art.tri(c, -40, 405, 262, 235, 514, 405, 0xff344a53);
    art.tri(c, 162, 405, 370, 202, 540, 405, 0xff23414a);
    art.tower(c, 369, 210, 1.55f);
    art.rect(c, 0, 0, 480, 405, 0x20223143);
    art.label(c, "MINI PLATFORM ADVENTURE", 26, 46, 13, Scenery.GOLD, Scenery.BOLD);
    art.label(c, "Trilha dos", 23, 103, 43, Scenery.CREAM, Scenery.TITLE);
    art.label(c, "Faróis", 23, 157, 65, Scenery.CREAM, Scenery.TITLE);
    art.sprite(c, art.hero, 79, 351, 4, 1, false);
    art.tri(c, -20, 333, 163, 373, 0, 414, 0xff17363e);
    art.rect(c, 0, 374, 480, 32, 0xff17363e);
    art.label(c, "SEIS ILHAS     •     UMA EXPEDIÇÃO", 26, 394, 11, Scenery.CREAM, Scenery.BOLD);
    c.restore();
  }
}
