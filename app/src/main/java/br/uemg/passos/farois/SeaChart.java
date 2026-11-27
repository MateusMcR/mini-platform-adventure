package br.uemg.passos.farois;

import android.graphics.*;
import android.os.SystemClock;
import android.view.*;

/** Cada nó pertence à mesma carta ilustrada. Toque ou setas selecionam o porto. */
final class SeaChart extends View {
  private static final int[] X = {46, 115, 81, 183, 223, 304}, Y = {157, 115, 55, 53, 132, 79};
  private final Bitmap chart, traveler;
  private final Paint p = new Paint();
  private final RectF bounds = new RectF(), dest = new RectF();
  private final Rect sprite = new Rect(0, 0, 18, 22);
  private final int open;
  private final MainActivity host;
  int selected;
  private float fromX, fromY, scale = 1, ox, oy;
  private long moveAt;

  SeaChart(MainActivity host, int open, int selected) {
    super(host);
    this.host = host;
    this.open = open;
    this.selected = selected;
    fromX = X[selected];
    fromY = Y[selected];
    chart = BitmapFactory.decodeResource(getResources(), R.drawable.arquipelago);
    traveler = BitmapFactory.decodeResource(getResources(), R.drawable.viajante);
    setFocusableInTouchMode(true);
    setContentDescription("Carta das seis ilhas. Use as setas para selecionar um destino.");
  }

  private float fraction() {
    return Math.min(1, (SystemClock.uptimeMillis() - moveAt) / 320f);
  }

  void select(int n) {
    if (n < 0 || n >= X.length) return;
    float t = fraction();
    fromX += (X[selected] - fromX) * t;
    fromY += (Y[selected] - fromY) * t;
    selected = n;
    moveAt = SystemClock.uptimeMillis();
    host.chartInfo();
    invalidate();
  }

  @Override
  protected void onDraw(Canvas c) {
    c.drawColor(0xff738f89);
    scale = Math.min(getWidth() / 360f, getHeight() / 216f);
    ox = (getWidth() - 360 * scale) / 2;
    oy = (getHeight() - 216 * scale) / 2;
    c.save();
    c.translate(ox, oy);
    c.scale(scale, scale);
    bounds.set(0, 0, 360, 216);
    p.setColor(-1);
    p.setFilterBitmap(false);
    c.drawBitmap(chart, null, bounds, p);
    for (int i = 1; i < X.length; i++) {
      int dx = X[i] - X[i - 1], dy = Y[i] - Y[i - 1];
      int dots = (int) Math.hypot(dx, dy) / 5;
      for (int j = 1; j < dots; j++) {
        float t = j / (float) dots;
        p.setColor(i <= open ? 0xfff2dc9e : 0xff647d75);
        float x = X[i - 1] + dx * t, y = Y[i - 1] + dy * t - (float) Math.sin(t * Math.PI) * 6;
        c.drawRect(x, y, x + 2, y + 2, p);
      }
    }
    for (int i = 0; i < X.length; i++) {
      p.setColor(0xff424d41);
      c.drawCircle(X[i], Y[i], 6, p);
      p.setColor(i < open ? 0xffeac877 : i == open ? 0xfff4ddaa : 0xff8c9485);
      c.drawCircle(X[i], Y[i], 4, p);
      if (i > open) {
        p.setColor(0xff50594e);
        c.drawRect(X[i] - 2, Y[i] - 1, X[i] + 2, Y[i] + 2, p);
      }
    }
    float t = fraction();
    float x = fromX + (X[selected] - fromX) * t, y = fromY + (Y[selected] - fromY) * t;
    dest.set(x - 7, y - 19, x + 7, y - 2);
    p.setColor(-1);
    c.drawBitmap(traveler, sprite, dest, p);
    c.restore();
    if (t < 1) postInvalidateOnAnimation();
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (e.getAction() == MotionEvent.ACTION_UP) {
      float x = (e.getX() - ox) / scale, y = (e.getY() - oy) / scale;
      for (int i = 0; i < X.length; i++)
        if (Math.hypot(x - X[i], y - Y[i]) < 24) {
          select(i);
          break;
        }
      performClick();
    }
    return true;
  }

  @Override
  public boolean performClick() {
    super.performClick();
    return true;
  }

  @Override
  public boolean onKeyDown(int key, KeyEvent e) {
    if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
      select(selected - 1);
      return true;
    }
    if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
      select(selected + 1);
      return true;
    }
    if (key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_DPAD_CENTER) {
      host.enterIsland();
      return true;
    }
    return super.onKeyDown(key, e);
  }
}
