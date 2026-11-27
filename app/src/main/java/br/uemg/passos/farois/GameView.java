package br.uemg.passos.farois;

import android.graphics.*;
import android.util.SparseIntArray;
import android.view.*;

final class GameView extends View {
  final MainActivity host;
  final Game game;
  final Scenery art;
  final SparseIntArray fingers = new SparseIntArray();
  boolean running, keyLeft, keyRight;
  private long last;
  private float lag, scale = 1, ox, oy, camera, flagTime;

  GameView(MainActivity host, Game game) {
    super(host);
    this.host = host;
    this.game = game;
    art = new Scenery(getResources(), game.index);
    setFocusableInTouchMode(true);
    requestFocus();
    setContentDescription("Trilha dos Faróis. Use as setas, pulo e eco.");
  }

  void active(boolean value) {
    running = value;
    last = 0;
    lag = 0;
    keyLeft = keyRight = false;
    fingers.clear();
    invalidate();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldW, int oldH) {
    scale = Math.min(w / 720f, h / 405f);
    ox = (w - 720 * scale) / 2;
    oy = (h - 405 * scale) / 2;
  }

  int direction() {
    boolean left = keyLeft, right = keyRight;
    for (int i = 0; i < fingers.size(); i++) {
      left |= fingers.valueAt(i) == 1;
      right |= fingers.valueAt(i) == 2;
    }
    return (right ? 1 : 0) - (left ? 1 : 0);
  }

  @Override
  protected void onDraw(Canvas c) {
    long now = System.nanoTime();
    if (running && last != 0) lag += Math.min(.1f, (now - last) / 1e9f);
    last = now;
    int dir = direction();
    while (running && lag >= Game.DT && game.status == Game.RUN) {
      game.step(dir);
      lag -= Game.DT;
    }
    if (game.events != 0) {
      if ((game.events & Game.FLAG) != 0) {
        flagTime = game.time + 2;
        host.store.save(game);
      }
      host.sound(game.events);
      game.events = 0;
    }
    camera = Math.max(0, Math.min(game.x - 190, game.stage.goal + 65 - 720));
    c.drawColor(Scenery.INK);
    c.save();
    c.translate(ox, oy);
    c.scale(scale, scale);
    c.clipRect(0, 0, 720, 405);
    art.world(c, game, camera, host.rain());
    art.rect(c, 12, 10, 166, 44, 0xdf383e34);
    art.rect(c, 12, 10, 3, 44, 0xffd9ad68);
    art.label(
        c, (game.index + 1) + " / 6   " + game.stage.name, 22, 27, 13, Scenery.CREAM, Scenery.BOLD);
    for (int i = 0; i < 3; i++) heart(c, 22 + i * 14, 35, i < game.lives);
    art.label(c, game.score + " pts", 76, 44, 12, Scenery.GOLD, Scenery.BOLD);
    art.rect(c, 12, 55, 166, 2, 0x66515946);
    art.rect(c, 12, 55, 166 * game.x / game.stage.goal, 2, 0xffac823e);
    button(c, 658, 8, 50, 42, "Ⅱ", false);
    if (game.time < 4 || flagTime > game.time) {
      String msg = flagTime > game.time ? "Bandeira ativada" : game.stage.hint;
      art.p.setTextSize(13);
      art.p.setTypeface(Scenery.BOLD);
      float w = art.p.measureText(msg);
      art.box(c, 360 - w / 2 - 10, 62, w + 20, 26, 6, 0xde18353c);
      art.label(c, msg, 360 - w / 2, 80, 13, Scenery.CREAM, Scenery.BOLD);
    }
    button(c, 15, 344, 66, 51, "◀", dir < 0);
    button(c, 89, 344, 66, 51, "▶", dir > 0);
    button(
        c,
        537,
        344,
        73,
        51,
        game.echoCooldown > 0 ? String.valueOf((int) Math.ceil(game.echoCooldown)) : "ECO",
        game.echoCooldown > 0);
    button(c, 621, 337, 84, 58, "PULO", !game.grounded);
    c.restore();
    if (running && game.status != Game.RUN) {
      running = false;
      post(() -> host.result(this));
    } else if (running) postInvalidateOnAnimation();
  }

  private void heart(Canvas c, int x, int y, boolean full) {
    int color = full ? 0xffe59a72 : 0xff767b6a;
    art.rect(c, x + 1, y, 3, 2, color);
    art.rect(c, x + 6, y, 3, 2, color);
    art.rect(c, x, y + 2, 10, 3, color);
    art.rect(c, x + 2, y + 5, 6, 2, color);
    art.rect(c, x + 4, y + 7, 2, 2, color);
  }

  private void button(Canvas c, int x, int y, int w, int h, String text, boolean on) {
    boolean jump = x == 621;
    art.box(c, x, y + 2, w, h, 5, 0xa5262b27);
    art.box(c, x, y, w, h - 2, 5, 0xbbc3b995);
    art.box(c, x + 1, y + 1, w - 2, h - 4, 4, on ? 0xe98b6649 : jump ? 0xe388533c : 0xcb3e483c);
    art.p.setTextSize(16);
    art.p.setTypeface(Scenery.BOLD);
    art.label(
        c,
        text,
        x + (w - art.p.measureText(text)) / 2,
        y + h / 2f + 6,
        16,
        Scenery.CREAM,
        Scenery.BOLD);
  }

  private int zone(float rawX, float rawY) {
    float x = (rawX - ox) / scale, y = (rawY - oy) / scale;
    if (x < 0 || x > 720 || y < 0 || y > 405) return 0;
    if (y < 57 && x >= 644) return 5;
    if (y < 330) return 0;
    if (x < 85) return 1;
    if (x < 166) return 2;
    if (x >= 615) return 3;
    if (x >= 525) return 4;
    return 0;
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (!running) return true;
    int a = e.getActionMasked(), i = e.getActionIndex(), id = e.getPointerId(i);
    if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
      int z = zone(e.getX(i), e.getY(i));
      fingers.put(id, z);
      if (z == 3) game.jump();
      else if (z == 4) game.echo();
      else if (z == 5) host.pauseGame();
    } else if (a == MotionEvent.ACTION_MOVE) {
      for (int k = 0; k < e.getPointerCount(); k++) {
        int finger = e.getPointerId(k), old = fingers.get(finger);
        if (old <= 2) fingers.put(finger, zoneMovement(e.getX(k), e.getY(k)));
      }
    } else if (a == MotionEvent.ACTION_CANCEL) fingers.clear();
    else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_POINTER_UP) fingers.delete(id);
    return true;
  }

  private int zoneMovement(float x, float y) {
    int z = zone(x, y);
    return z == 1 || z == 2 ? z : 0;
  }

  @Override
  public boolean onKeyDown(int key, KeyEvent e) {
    if (!running) return super.onKeyDown(key, e);
    switch (key) {
      case KeyEvent.KEYCODE_DPAD_LEFT:
      case KeyEvent.KEYCODE_A:
        keyLeft = true;
        return true;
      case KeyEvent.KEYCODE_DPAD_RIGHT:
      case KeyEvent.KEYCODE_D:
        keyRight = true;
        return true;
      case KeyEvent.KEYCODE_SPACE:
      case KeyEvent.KEYCODE_DPAD_UP:
        if (e.getRepeatCount() == 0) game.jump();
        return true;
      case KeyEvent.KEYCODE_E:
        if (e.getRepeatCount() == 0) game.echo();
        return true;
      case KeyEvent.KEYCODE_P:
        host.pauseGame();
        return true;
      default:
        return super.onKeyDown(key, e);
    }
  }

  @Override
  public boolean onKeyUp(int key, KeyEvent e) {
    switch (key) {
      case KeyEvent.KEYCODE_DPAD_LEFT:
      case KeyEvent.KEYCODE_A:
        keyLeft = false;
        return true;
      case KeyEvent.KEYCODE_DPAD_RIGHT:
      case KeyEvent.KEYCODE_D:
        keyRight = false;
        return true;
      default:
        return super.onKeyUp(key, e);
    }
  }
}
