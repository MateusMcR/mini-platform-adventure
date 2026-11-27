package br.uemg.passos.farois;

import android.graphics.*;
import android.os.SystemClock;
import android.view.*;

/** Sequência ilustrada em três planos. O relógio só corre com a janela visível. */
final class Cinematic extends View {
  static final float SHOT=6f;
  final boolean ending;
  private final MainActivity host;
  private final Bitmap port, night, woods;
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF frame=new RectF();
  private long last;
  private float elapsed;
  private boolean active, finished;
  private static final String[][] INTRO={
    {"A última vigília","Naquela noite, nenhum dos seis faróis respondeu ao Aurora."},
    {"Três pancadas","Lia ouviu na concha o sinal de sua mãe. O barco ainda estava no mar."},
    {"Antes da maré","Ela pegou a lanterna e partiu. Cada farol aceso abriria um trecho do canal."}
  };
  private static final String[][] OUTRO={
    {"O sexto facho","A luz varreu a neblina. Do outro lado, uma vela mudou de rumo."},
    {"O Aurora voltou","Lia bateu três vezes no cais. Do convés, sua mãe respondeu."},
    {"Trilha dos Faróis","Mateus Carvalho Rodrigues da Silva  •  Sistemas de Informação, UEMG Passos"}
  };
  Cinematic(MainActivity host,boolean ending,float seconds){
    super(host);this.host=host;this.ending=ending;elapsed=seconds;
    port=BitmapFactory.decodeResource(getResources(),R.drawable.porto);
    night=BitmapFactory.decodeResource(getResources(),R.drawable.horizonte_5);
    woods=BitmapFactory.decodeResource(getResources(),R.drawable.horizonte_2);
    setFocusableInTouchMode(true);
    setContentDescription("Cena animada. Toque para avançar. Toque no canto superior direito para pular.");
  }
  float position(){return elapsed;}
  void active(boolean enabled){active=enabled;last=0;invalidate();}
  void finish(){
    if(finished)return;
    finished=true;post(()->host.afterFilm(this));
  }
  private void label(Canvas c,String text,float x,float y,float size,int color,Typeface face){
    paint.setColor(color);paint.setTypeface(face);paint.setTextSize(size);c.drawText(text,x,y,paint);
  }
  private void block(Canvas c,float x,float y,float w,float h,int color){
    paint.setColor(color);c.drawRect(x,y,x+w,y+h,paint);
  }
  @Override protected void onDraw(Canvas c){
    long now=SystemClock.uptimeMillis();
    if(active && last!=0)elapsed+=Math.min(.12f,(now-last)/1000f);
    last=now;if(elapsed>=SHOT*3){finish();return;}
    int scene=(int)(elapsed/SHOT);
    float t=(elapsed-scene*SHOT)/SHOT;
    c.save();c.scale(getWidth()/720f,getHeight()/405f);
    Bitmap bg=ending?(scene==0?night:port):(scene==0?night:scene==1?port:woods);
    paint.setColor(-1);paint.setFilterBitmap(false);
    frame.set(-18*t,-5,749-18*t,330);c.drawBitmap(bg,null,frame,paint);
    if(scene!=2 || !ending){
      float x=(ending?140+160*t:300-78*t)+scene*16;
      float y=220+2*(float)Math.sin(t*12);
      block(c,x,y,46,6,0xff514937);block(c,x+6,y+6,33,4,0xff614d38);
      block(c,x+20,y-34,2,34,0xff564c3c);
      Path sail=new Path();sail.moveTo(x+19,y-32);sail.lineTo(x+19,y-2);sail.lineTo(x+2,y-2);sail.close();
      paint.setColor(Scenery.CREAM);c.drawPath(sail,paint);
      if(ending || scene==1){paint.setColor(Scenery.GOLD);c.drawCircle(x+36,y-3,2+2*(float)Math.abs(Math.sin(t*15)),paint);}
    }
    block(c,0,293,720,112,Scenery.INK);
    String[] shot=(ending?OUTRO:INTRO)[scene];
    label(c,shot[0],23,324,23,Scenery.GOLD,Scenery.TITLE);
    label(c,shot[1],23,351,15,Scenery.CREAM,Scenery.BOLD);
    label(c,ending&&scene==2?"Programação III  •  Ilha bônus liberada":"Toque para avançar",23,382,12,Scenery.CREAM,Scenery.BOLD);
    block(c,615,9,95,32,0xdd34392f);
    label(c,ending?"Ver carta  ›":"Pular  ›",628,30,14,Scenery.CREAM,Scenery.BOLD);
    if(t<.09f)block(c,0,0,720,405,((int)(150*(1-t/.09f)))<<24);
    c.restore();if(active && !finished)postInvalidateOnAnimation();
  }
  @Override public boolean onTouchEvent(MotionEvent e){
    if(e.getAction()==MotionEvent.ACTION_UP){
      if(e.getX()>getWidth()*.83 && e.getY()<getHeight()*.17)finish();else advance();
      performClick();
    }
    return true;
  }
  private void advance(){elapsed=((int)(elapsed/SHOT)+1)*SHOT;invalidate();if(elapsed>=SHOT*3)finish();}
  @Override public boolean performClick(){super.performClick();return true;}
  @Override public boolean onKeyDown(int key,KeyEvent e){
    if(key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_SPACE){if(e.getRepeatCount()==0)advance();return true;}
    return super.onKeyDown(key,e);
  }
}
