package br.uemg.passos.farois;

import android.graphics.*;

/** Arte vetorial e sprites originais, desenhados na resolução lógica de 720 × 405. */
final class Scenery {
  final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  final Path path = new Path();
  private final Shader[] skies = new Shader[Stage.ALL.length];
  static final int INK = 0xff19333b, CREAM = 0xfffff1d1, GOLD = 0xffffcb65, CORAL = 0xffed795f;
  static final Typeface BOLD = Typeface.create("sans-serif-medium", Typeface.NORMAL);
  static final Typeface TITLE = Typeface.create("serif", Typeface.BOLD);
  final String[] hero = {
    "   rrrr   ",
    "  rrrrrr  ",
    "  nffnff  ",
    "  nffff   ",
    "   rrrr   ",
    "  rrtttrr ",
    " fnttttnf ",
    "  nnttn   ",
    "  nn nn   ",
    "  bb bb   "
  };
  final String[] bug = {
    "  a    a  ", "   bbbb   ", "  bbbbbbb ", " bnnbbnnb ", " bwwbbwwb ", "  bbbbbb  ", " nn    nn "
  };

  Scenery() {
    for (int i = 0; i < skies.length; i++)
      skies[i] =
          new LinearGradient(
              0, 0, 0, 320, Stage.ALL[i].sky, Stage.ALL[i].horizon, Shader.TileMode.CLAMP);
  }

  void rect(Canvas c, float x, float y, float w, float h, int color) {
    p.setColor(color);
    c.drawRect(x, y, x + w, y + h, p);
  }

  void box(Canvas c, float x, float y, float w, float h, float r, int color) {
    p.setColor(color);
    c.drawRoundRect(x, y, x + w, y + h, r, r, p);
  }

  void disc(Canvas c, float x, float y, float r, int color) {
    p.setColor(color);
    c.drawCircle(x, y, r, p);
  }

  void tri(Canvas c, float x, float y, float xx, float yy, float xxx, float yyy, int color) {
    path.rewind();
    path.moveTo(x, y);
    path.lineTo(xx, yy);
    path.lineTo(xxx, yyy);
    path.close();
    p.setColor(color);
    c.drawPath(path, p);
  }

  void label(Canvas c, String s, float x, float base, float size, int color, Typeface face) {
    p.setTypeface(face);
    p.setTextSize(size);
    p.setColor(color);
    c.drawText(s, x, base, p);
  }

  void background(Canvas c, int id, float camera, float time, boolean rainy) {
    Stage s = Stage.ALL[id];
    p.setColor(-1);
    p.setShader(skies[id]);
    c.drawRect(0, 0, 720, 405, p);
    p.setShader(null);
    disc(c, 536, 90, 45, 0x33fff2b5);
    disc(c, 536, 90, 33, 0xfffee3a3);
    if (id == 5)
      for (int i = 0; i < 40; i++)
        disc(c, (i * 97) % 720, 12 + (i * 43) % 154, i % 4 == 0 ? 1.5f : 1, 0xbbfff4dd);
    for (int layer = 0; layer < 3; layer++) {
      int span = 204 - layer * 32;
      float offset = (camera * (.09f + .08f * layer)) % span;
      int base = 235 + layer * 36;
      for (int i = -1; i < 8; i++) {
        float x = i * span - offset;
        float peak = base - 73 - (i + 2) % 3 * 12;
        tri(c, x - span * .4f, base, x + span * .45f, peak, x + span * 1.3f, base, s.ridge);
        tri(c, x + span * .45f, peak, x + span * .63f, base, x + span * 1.3f, base, 0x20173040);
      }
      rect(c, 0, base - 1, 720, 405 - base, s.ridge);
      if (layer < 2) rect(c, 0, base - 110, 720, 405 - base + 110, 0x13fff3dd);
    }
    rect(c, 0, 331, 720, 74, 0xff345d6a);
    for (int i = 0; i < 36; i++) {
      float x = (i * 79 - camera * .29f + time * 7) % 754;
      if (x < 0) x += 754;
      rect(c, x - 20, 337 + (i * 17) % 60, 12 + i % 5 * 4, 2, 0x557ed1c2);
    }
    if (rainy)
      for (int i = 0; i < 52; i++) {
        float x = (i * 71 + time * 65) % 740, y = (i * 97 + time * 185) % 410;
        p.setColor(0x779bdbe0);
        c.drawLine(x, y, x - 3, y + 9, p);
      }
  }

  void tower(Canvas c, float x, float y, float size) {
    c.save();
    c.translate(x, y);
    c.scale(size, size);
    tri(c, -55, -51, -8, -47, -8, -56, 0x65ffe395);
    tri(c, 53, -53, 8, -47, 8, -56, 0x65ffe395);
    tri(c, -15, 0, -6, -43, 7, -43, CREAM);
    tri(c, -15, 0, 7, -43, 15, 0, CREAM);
    rect(c, -9, -27, 19, 8, CORAL);
    rect(c, -7, -55, 14, 12, GOLD);
    rect(c, -10, -46, 20, 4, INK);
    tri(c, -12, -55, 0, -66, 12, -55, CORAL);
    rect(c, -3, -11, 6, 11, INK);
    c.restore();
  }

  void landmark(Canvas c, int biome, float x, float y) {
    switch (biome) {
      case 0:
      case 2:
        rect(c, x - 2, y - 37, 5, 37, 0xff84694e);
        if (biome == 0) {
          tri(c, x, y - 40, x - 25, y - 26, x - 14, y - 40, 0xff498f78);
          tri(c, x, y - 40, x + 27, y - 29, x + 13, y - 43, 0xff518f70);
          tri(c, x, y - 40, x - 9, y - 53, x - 18, y - 47, 0xff68a578);
        } else {
          tri(c, x - 17, y - 15, x, y - 52, x + 17, y - 15, 0xff486e68);
          tri(c, x - 13, y - 30, x, y - 60, x + 13, y - 30, 0xff638975);
        }
        break;
      case 1:
      case 4:
        tri(c, x - 16, y, x - 3, y - 20, x + 12, y, biome == 1 ? 0xffac7762 : 0xff7c5257);
        tri(c, x - 3, y - 20, x + 3, y - 9, x + 12, y, 0xffa8786b);
        break;
      default:
        tri(c, x - 9, y, x - 12, y - 26, x + 3, y, 0xff9da9bd);
        tri(c, x - 4, y, x + 7, y - 35, x + 12, y, biome == 3 ? 0xffb5d8cd : 0xffad96d2);
        tri(c, x + 3, y, x + 7, y - 35, x + 8, y, 0xffe6e1d4);
    }
  }

  void island(Canvas c, Stage s, int i) {
    float x = s.left[i], y = s.top[i], w = s.width[i];
    rect(c, x, y, w, 405 - y, 0xff384b52);
    rect(c, x, y + 6, w, 10, 0xff63716c);
    for (int k = 0; k < w - 12; k += 24) {
      float h = 10 + (i * 7 + k) % 20;
      tri(c, x + k, y + 13, x + k + 10, y + 13 + h, x + k + 20, y + 13, 0xff516062);
      if (k % 48 == 0) {
        rect(c, x + k + 5, y - 4, 2, 4, s.grass);
        rect(c, x + k + 8, y - 6, 2, 6, s.grass);
      }
    }
    rect(c, x, y, w, 6, s.grass);
  }

  void sprite(
      Canvas c, String[] rows, float x, float feet, float scale, int facing, boolean stunned) {
    c.save();
    c.translate(x, feet);
    c.scale(scale * facing, scale);
    for (int row = 0; row < rows.length; row++) {
      String line = rows[row];
      for (int col = 0; col < line.length(); col++) {
        char pixel = line.charAt(col);
        int color;
        switch (pixel) {
          case 'n':
            color = INK;
            break;
          case 'r':
            color = CORAL;
            break;
          case 'f':
            color = 0xfff4c498;
            break;
          case 't':
            color = 0xff52a99b;
            break;
          case 'b':
            color = stunned ? 0xff85beb5 : 0xff734f63;
            break;
          case 'a':
            color = GOLD;
            break;
          case 'w':
            color = CREAM;
            break;
          default:
            continue;
        }
        rect(c, col - 5, row - rows.length, 1.04f, 1.04f, color);
      }
    }
    c.restore();
  }

  void world(Canvas c, Game g, float camera, boolean rainy) {
    Stage s = g.stage;
    background(c, g.index, camera, g.time, rainy);
    c.save();
    c.translate(-camera, 0);
    for (int i = 0; i < s.left.length; i++) {
      if (s.left[i] > camera + 740 || s.left[i] + s.width[i] < camera - 10) continue;
      if (i != s.checkpoint && i < s.left.length - 1)
        landmark(c, g.index, s.left[i] + s.width[i] * .74f, s.top[i]);
      island(c, s, i);
      if (!g.coins[i]) {
        float x = g.coinX(i), y = g.coinY(i) + 2 * (float) Math.sin(g.time * 4 + i);
        disc(c, x, y, 9, 0x40ffdc7e);
        disc(c, x, y, 6.4f, 0xffc67b43);
        disc(c, x, y - 1, 5.5f, GOLD);
        rect(c, x - 1, y - 4, 2, 6, CREAM);
      }
    }
    int flag = s.checkpoint;
    float fx = s.left[flag] + 28, fy = s.top[flag];
    rect(c, fx, fy - 39, 3, 39, CREAM);
    tri(c, fx + 3, fy - 39, fx + 24, fy - 33, fx + 3, fy - 26, g.checkpoint ? GOLD : 0xff71cace);
    tower(c, s.goal, s.top[s.top.length - 1], 1.2f);
    for (int i = 0; i < g.enemyX.length; i++)
      if (!g.dead[i]) sprite(c, bug, g.enemyX[i], s.top[s.guards[i]], 2.8f, 1, g.stun[i] > 0);
    if (g.echoCooldown > 2.18f) {
      float radius = (2.6f - g.echoCooldown) * 350;
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(2.5f);
      disc(c, g.x, g.y - 17, radius, 0xaaebffca);
      p.setStyle(Paint.Style.FILL);
    }
    if (g.shield < .02f || ((int) (g.shield * 10)) % 2 == 0) {
      float stride = g.grounded ? Math.abs((float) Math.sin(g.x / 12)) * 1.8f : 0;
      sprite(c, hero, g.x, g.y - stride, 3.2f, g.facing, false);
    }
    c.restore();
  }
}
