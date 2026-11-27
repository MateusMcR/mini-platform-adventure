package br.uemg.passos.farois;

import android.graphics.*;

/** Cenários e sprites em pixel art, desenhados na resolução lógica de 720 × 405. */
final class Scenery {
  final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  final Path path = new Path();
  static final int INK = 0xff34392f, CREAM = 0xfff3e7cd, GOLD = 0xfff2c475;
  static final Typeface BOLD = Typeface.create("sans-serif-condensed", Typeface.BOLD);
  static final Typeface TITLE = Typeface.create("serif", Typeface.BOLD);
  private static final int[] SKY = {
    R.drawable.horizonte_0,
    R.drawable.horizonte_1,
    R.drawable.horizonte_2,
    R.drawable.horizonte_3,
    R.drawable.horizonte_4,
    R.drawable.horizonte_5
  };
  private static final int[] GROUND = {
    R.drawable.chao_0,
    R.drawable.chao_1,
    R.drawable.chao_2,
    R.drawable.chao_3,
    R.drawable.chao_4,
    R.drawable.chao_5
  };
  private final Bitmap sky, soil, landmarks, tower, hero;
  private final Paint pixel = new Paint();
  private final Rect source = new Rect();
  private final RectF dest = new RectF();

  Scenery(android.content.res.Resources res, int stage) {
    sky = BitmapFactory.decodeResource(res, SKY[Math.min(5, stage)]);
    soil = BitmapFactory.decodeResource(res, GROUND[Math.min(5, stage)]);
    landmarks = BitmapFactory.decodeResource(res, R.drawable.marcos);
    tower = BitmapFactory.decodeResource(res, R.drawable.torre);
    hero = BitmapFactory.decodeResource(res, R.drawable.viajante);
    pixel.setFilterBitmap(false);
  }

  void image(
      Canvas c, Bitmap img, int sx, int sy, int sw, int sh, float x, float y, float w, float h) {
    source.set(sx, sy, sx + sw, sy + sh);
    dest.set(x, y, x + w, y + h);
    c.drawBitmap(img, source, dest, pixel);
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

  private void background(Canvas c, Game g, float camera, boolean rainy) {
    float pan = Math.min(240, camera * .09f);
    image(c, sky, 0, 0, sky.getWidth(), sky.getHeight(), -pan, 0, 960, 408);
    // Pequenas cristas deslizam no sentido oposto ao movimento da câmera.
    for (int i = 0; i < 28; i++) {
      float x = (i * 83 + g.time * 5 - camera * .23f) % 740;
      if (x < 0) x += 740;
      rect(c, x - 12, 279 + (i * 17) % 98, 5 + i % 5, 1, 0x667f9281);
    }
    if (rainy)
      for (int i = 0; i < 56; i++) {
        float x = (i * 71 + g.time * 65) % 740, y = (i * 97 + g.time * 185) % 410;
        p.setColor(0x99c0c8ba);
        c.drawLine(x, y, x - 3, y + 8, p);
      }
  }

  private void island(Canvas c, Stage s, int i) {
    float left = s.left[i], top = s.top[i], width = s.width[i];
    c.save();
    c.clipRect(left, top, left + width, 405);
    for (int off = 0; off < width; off += 144)
      image(c, soil, 0, 0, 72, 86, left + off, top, 144, 172);
    c.restore();
  }

  private void crab(Canvas c, float x, float y, float time, boolean stunned) {
    c.save();
    c.translate(Math.round(x), Math.round(y));
    int shell = stunned ? 0xff818f79 : 0xffac5a40, light = stunned ? 0xffc3c3a7 : 0xffd18c60;
    int foot = (int) (time * 6) % 2;
    for (int side = -1; side <= 1; side += 2) {
      c.save();
      c.scale(side, 1);
      rect(c, 6, -5, 6, 2, INK);
      rect(c, 9, -3 + foot, 4, 2, INK);
      rect(c, 7, -11, 6, 3, shell);
      rect(c, 11, -14, 3, 4, light);
      rect(c, 4, -15, 2, 6, INK);
      rect(c, 4, -15, 2, 2, CREAM);
      c.restore();
    }
    rect(c, -8, -10, 16, 7, INK);
    rect(c, -7, -11, 14, 6, shell);
    rect(c, -5, -12, 10, 2, light);
    c.restore();
  }

  private void creature(Canvas c, Game g, int i) {
    float x = g.enemyX[i], y = g.enemyY[i];
    boolean dizzy = g.stun[i] > 0;
    if (g.species(i) == Game.CRAB) {
      crab(c, x, y, g.time + i, dizzy);
      return;
    }
    c.save();
    c.translate(Math.round(x), Math.round(y));
    if (g.species(i) == Game.GULL) {
      int plumage = dizzy ? 0xffa7b5a8 : 0xffece3cb;
      rect(c, -8, -11, 16, 7, plumage);
      rect(c, 4, -15, 7, 7, plumage);
      rect(c, 10, -12, 5, 2, 0xffd39a49);
      rect(c, 8, -14, 2, 2, INK);
      int wing = (int) (g.time * 5) % 2 == 0 ? -16 : -8;
      tri(c, -6, -9, -14, wing, 2, -8, 0xff717c79);
      rect(c, -3, -4, 7, 2, 0xff927446);
    } else {
      int skin = dizzy ? 0xffa4b5a2 : 0xff71915b;
      rect(c, -9, -8, 16, 7, skin);
      rect(c, 2, -12, 9, 8, skin);
      rect(c, 6, -13, 3, 3, CREAM);
      rect(c, 7, -13, 1, 2, INK);
      rect(c, -11, -4, 7, 4, 0xff566746);
      rect(c, 5, -2, 9, 2, 0xff566746);
    }
    c.restore();
  }

  private void obstacle(Canvas c, Game g) {
    float x = g.trapX(), y = g.stage.top[g.trapIsland()], h = g.trapHeight();
    if (g.index == 3 || g.index == 4) {
      int spray = g.index == 4 ? 0xffdd8e51 : 0xffa7d2ce;
      rect(c, x - 12, y - 3, 24, 3, 0xff695e51);
      rect(c, x - 5, y - h, 9, h, spray);
      rect(c, x - 9, y - h * .68f, 4, h * .68f, spray);
      rect(c, x + 6, y - h * .75f, 3, h * .75f, spray);
      rect(c, x - 2, y - h - 4, 3, 2, spray);
    } else {
      int color = g.index == 2 ? 0xff485a40 : 0xff655545;
      for (int j = -1; j <= 1; j++)
        tri(c, x + j * 7 - 4, y, x + j * 7 + 1, y - h + (j == 0 ? 0 : 4), x + j * 7 + 4, y, color);
      rect(c, x - 13, y - 2, 26, 2, 0xff917557);
    }
  }

  private void tidalGarden(Canvas c,Game g){
    Stage s=g.stage;
    float x=s.waterStart(),w=s.waterEnd()-x;
    rect(c,x,323,w,82,0xed477c79);
    for(int i=0;i<w;i+=16){
      float surface=322+2*(float)Math.sin(g.time*2+i*.12);
      rect(c,x+i,surface,16,3,0xffacd3bd);
      rect(c,x+i+4,surface+17,5,1,0xff739d92);
      if(i%32==0)rect(c,x+i+6,368-(i%5)*3,2,3,0xff97c2b1);
    }
    int tree=s.trunk(),top=s.canopy();
    rect(c,tree-8,top+5,16,102,0xff6d5841);
    for(int i=12;i<102;i+=15)rect(c,tree-12,top+i,22,3,0xffb09669);
    for(int i=-2;i<=2;i++){
      float h=top-9+Math.abs(i)*3;
      box(c,tree+i*20-18,h-15,37,25,7,0xff506e51);
      box(c,tree+i*20-15,h-17,29,9,5,0xff8fa172);
    }
    rect(c,tree-60,top,120,5,0xff655a42);
    rect(c,tree-59,top,118,2,0xffc1b277);
  }

  private void leviathan(Canvas c,Game g){
    float x=g.bossX(),y=g.stage.top[g.stage.grove+1];
    int shell=g.vulnerable()?0xff72998c:0xff505e67;
    for(int i=-2;i<=2;i++){
      float twitch=(float)Math.sin(g.bossTime*4+i)*4;
      box(c,x+i*14-7,y-14,14,12+twitch,4,0xff485460);
    }
    box(c,x-35,y-49,70,39,11,shell);
    for(int i=-2;i<=2;i++)tri(c,x+i*13-7,y-45,x+i*13,y-61,x+i*13+6,y-44,0xff938c73);
    disc(c,x-19,y-34,5,Scenery.CREAM);disc(c,x-21,y-34,2,INK);
    disc(c,x+4,y-36,g.vulnerable()?9:5,g.vulnerable()?GOLD:0xffaa9a82);
    if(!g.fighting())return;
    if(g.cycle()<1)rect(c,x-35,y-4,35*g.cycle(),3,GOLD);
    if(g.cycle()>=1 && g.cycle()<3.3f){
      float wave=g.waveX();tri(c,wave-13,y,wave+4,y-24,wave+14,y,0xff63a4a2);
      rect(c,wave-1,y-23,7,3,CREAM);
    }
    for(int i=0;i<3;i++)rect(c,x-23+16*i,y-72,13,4,i<g.bossLife?GOLD:INK);
  }

  void world(Canvas c, Game g, float camera, boolean rainy) {
    Stage s = g.stage;
    background(c, g, camera, rainy);
    c.save();
    c.translate(-camera, 0);
    for (int i = 0; i < s.left.length; i++) {
      if (s.left[i] > camera + 740 || s.left[i] + s.width[i] < camera - 10) continue;
      if (i != s.checkpoint && i < s.left.length - 1)
        image(
            c,
            landmarks,
            Math.min(g.index,5) * 48,
            (i % 3) * 40,
            48,
            40,
            s.left[i] + s.width[i] * .74f - 36,
            s.top[i] - 60,
            72,
            60);
      island(c, s, i);
      if (!g.coins[i]) {
        float x = g.coinX(i), y = g.coinY(i) + 2 * (float) Math.sin(g.time * 4 + i);
        float half = 4 + 2 * Math.abs((float) Math.cos(g.time * 3 + i));
        box(c, x - half - 1, y - 8, half * 2 + 2, 16, 4, 0xff614b31);
        box(c, x - half, y - 7, half * 2, 14, 3, 0xffdbad56);
        rect(c, x - 1, y - 5, 2, 9, 0xfffae3a1);
      }
    }
    tidalGarden(c,g);
    if(g.index==Stage.BONUS && g.bossLife>0)leviathan(c,g);
    obstacle(c, g);
    int flag = s.checkpoint;
    float fx = s.left[flag] + 28, fy = s.top[flag];
    rect(c, fx, fy - 39, 3, 39, CREAM);
    tri(c, fx + 3, fy - 39, fx + 24, fy - 33, fx + 3, fy - 26, g.checkpoint ? GOLD : 0xff71cace);
    image(c, tower, 0, 0, 44, 76, s.goal - 23, s.top[s.top.length - 1] - 82, 46, 82);
    for (int i = 0; i < g.enemyX.length; i++) if (!g.dead[i]) creature(c, g, i);
    if (g.echoCooldown > 1.38f) {
      float radius = (1.8f - g.echoCooldown) * 350;
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(2.5f);
      disc(c, g.x, g.y - 17, radius, 0xaaebffca);
      p.setStyle(Paint.Style.FILL);
    }
    if (g.shield < .02f || ((int) (g.shield * 10)) % 2 == 0) {
      int frame = g.grounded ? (int) (g.x / 10) % 4 : 2;
      c.save();
      c.translate(Math.round(g.x), Math.round(g.y));
      c.scale(g.facing, 1);
      image(c, hero, frame * 18, 0, 18, 22, -14, -35, 29, 35);
      c.restore();
    }
    c.restore();
  }
}
