package br.uemg.passos.farois;

/**
 * Modelo determinístico. Coordenadas representam o centro dos pés. Atualizado em passos de 1/120 s.
 */
final class Game {
  static final float DT = 1f / 120, SPEED = 225, JUMP = 442, GRAVITY = 1060;
  static final int RUN = 0, CLEAR = 1, OVER = 2;
  static final int COIN = 1, HIT = 2, FLAG = 4, STOMP = 8, ECHO = 16, LEAP = 32;
  final Stage stage;
  final int index;
  final float[] enemyX, enemyY, stun;
  final boolean[] dead, coins;
  float x, y, velocityY, time, grace, jumpBuffer, shield, echoCooldown;
  int score, lives, status, events, facing = 1;
  boolean grounded, checkpoint, climbing;
  int bossLife=3;
  float bossTime;
  boolean struck;
  boolean swimming(){return x>stage.waterStart()-7 && x<stage.waterEnd()+7 && y>=300;}
  boolean atTree(){return Math.abs(x-stage.trunk())<23 && y>=stage.canopy()-1 && y<=stage.top[stage.grove]+2;}
  int arena(){return stage.left[stage.grove+1];}
  float bossX(){return arena()+395;}
  boolean fighting(){return index==Stage.BONUS && bossLife>0 && x>=arena();}
  float cycle(){return bossTime%4.5f;}
  boolean vulnerable(){return cycle()>2.3f && cycle()<4.1f;}
  float waveX(){return bossX()-(cycle()-1.0f)*(190+(3-bossLife)*32);}


  Game(int index, int score, int lives) {
    this.index = index;
    this.score = score;
    this.lives = lives;
    stage = Stage.ALL[index];
    enemyX = new float[stage.guards.length];
    enemyY = new float[enemyX.length];
    stun = new float[enemyX.length];
    dead = new boolean[enemyX.length];
    coins = new boolean[stage.left.length];
    respawn();
    shield = 0;
    moveGuards(0);
  }

  void respawn() {
    int island = checkpoint ? stage.checkpoint : 0;
    x = stage.left[island] + 28;
    y = stage.top[island];
    climbing=false;
    velocityY = jumpBuffer = 0;
    grounded = true;
    grace = .09f;
    shield = 1.35f;
  }

  void jump() {
    if(status!=RUN)return;
    if(swimming()){velocityY=-360;grounded=false;events|=LEAP;return;}
    if(atTree() && y>stage.canopy()+2){climbing=true;grounded=false;return;}
    jumpBuffer=.12f;
  }

  void echo() {
    if (status != RUN || echoCooldown > 0) return;
    echoCooldown = 1.8f;
    events |= ECHO;
    if(fighting() && vulnerable() && !struck && Math.abs(x-bossX())<145){
      bossLife--;struck=true;score+=100;
    }
    for (int i = 0; i < enemyX.length; i++)
      if (Math.abs(x - enemyX[i]) <= 150 && Math.abs(y - enemyY[i]) < 90) stun[i] = 2;
  }

  float coinX(int i) {
    return stage.left[i] + stage.width[i] * .49f;
  }

  float coinY(int i) {
    if(i==stage.grove)return stage.canopy()-20;
    return stage.top[i] - (i % 3 == 1 ? 73 : 28);
  }

  static final int CRAB = 0, GULL = 1, FROG = 2;

  int species(int i) {
    return index == 0 ? CRAB : index == 2 ? FROG : (index + i) % 3;
  }

  int trapIsland() {
    return 0;
  }

  float trapX() {
    return stage.left[trapIsland()] + stage.width[trapIsland()] * .48f;
  }

  float trapHeight() {
    if (index == 3 || index == 4) return 5 + 26 * Math.max(0, (float) Math.sin(time * 2.4));
    return index == 2 ? 17 : 12;
  }

  void moveGuards(float dt) {
    for (int i = 0; i < enemyX.length; i++) {
      int island = stage.guards[i];
      if (stun[i] > 0) {
        stun[i] -= dt;
        continue;
      }
      int kind = species(i);
      float phase = time * (1.1f + index * .17f) + i;
      enemyY[i] = stage.top[island];
      if (kind == GULL) enemyY[i] -= 21 + 12 * Math.sin(phase * 1.7);
      if (kind == FROG) enemyY[i] -= 34 * Math.max(0, Math.sin(phase * 2));
      float lane = stage.width[island] * .26f;
      enemyX[i] =
          stage.left[island]
              + stage.width[island] * .58f
              + lane * (float) Math.sin(time * (1.1f + index * .17f) + i);
    }
  }

  void damage() {
    if (--lives == 0) status = OVER;
    else respawn();
    events |= HIT;
  }

  void step(int direction) {
    if (status != RUN) return;
    time += DT;
    echoCooldown = Math.max(0, echoCooldown - DT);
    shield = Math.max(0, shield - DT);
    moveGuards(DT);
    if (direction != 0) facing = direction;
    if (grounded) grace = .09f;
    else grace -= DT;
    if (jumpBuffer > 0 && grace > 0) {
      velocityY = -JUMP;
      grace = 0;
      grounded = false;
      jumpBuffer = 0;
      events |= LEAP;
    }
    jumpBuffer -= DT;
    float oldY = y;
    x = Math.max(10, Math.min(stage.goal + 8, x + direction * SPEED * DT));
    if(climbing){
      if(Math.abs(x-stage.trunk())>24)climbing=false;
      else{
        y=Math.max(stage.canopy(),y-105*DT);
        velocityY=0;
        if(y<=stage.canopy()){climbing=false;grounded=true;grace=.09f;}
        return;
      }
    }
    boolean water=swimming();
    velocityY += (water?420:GRAVITY) * DT;
    if(water)velocityY=Math.min(velocityY,65);
    y += velocityY * DT;
    grounded = false;
    for (int i = 0; i < stage.left.length; i++) {
      if (x + 8 <= stage.left[i] || x - 8 >= stage.left[i] + stage.width[i]) continue;
      if (velocityY >= 0 && oldY <= stage.top[i] + .01f && y >= stage.top[i]) {
        y = stage.top[i];
        velocityY = 0;
        grounded = true;
        if (i == stage.checkpoint && !checkpoint) {
          checkpoint = true;
          events |= FLAG;
        }
      }
      if (!coins[i] && Math.abs(x - coinX(i)) < 19 && Math.abs(y - 17 - coinY(i)) < 24) {
        coins[i] = true;
        score += 25;
        events |= COIN;
      }
    }
    if(swimming() && y>371){y=371;velocityY=0;}
    // As margens são sólidas por baixo da linha d'água. Uma braçada permite sair.
    if(y>stage.top[stage.grove]+3 && x>=stage.waterEnd()-8 && x<stage.waterEnd()+10)x=stage.waterEnd()-8;
    if(y>stage.top[stage.channel]+3 && x<=stage.waterStart()+8 && x>stage.waterStart()-10)x=stage.waterStart()+8;
    if(Math.abs(x-stage.trunk())<=60 && velocityY>=0 && oldY<=stage.canopy()+.01f && y>=stage.canopy()){
      y=stage.canopy();velocityY=0;grounded=true;
    }
    if(fighting()){
      float previous=cycle();bossTime+=DT;
      if(cycle()<previous)struck=false;
      boolean wave=cycle()>=1 && cycle()<3.3f && Math.abs(x-waveX())<14;
      if(shield==0 && ((wave && y>stage.top[stage.grove+1]-20) || Math.abs(x-bossX())<31)){
        damage();return;
      }
    }
    float ground = stage.top[trapIsland()];
    if (shield == 0 && Math.abs(x - trapX()) < 17 && y > ground - trapHeight() && y - 32 < ground) {
      damage();
      return;
    }
    if (y > 410) {
      damage();
      return;
    }
    for (int i = 0; i < enemyX.length; i++) {
      float floor = enemyY[i];
      if (dead[i]
          || stun[i] > 0
          || Math.abs(x - enemyX[i]) >= 18
          || y < floor - 19
          || y - 32 > floor) continue;
      if (velocityY > 0 && oldY <= floor - 16) {
        dead[i] = true;
        score += 50;
        velocityY = -JUMP * .68f;
        events |= STOMP;
      } else if (shield == 0) {
        damage();
        return;
      }
    }
    if (x >= stage.goal && grounded && (index!=Stage.BONUS || bossLife==0)) {
      score += 150;
      status = CLEAR;
    }
  }
}
