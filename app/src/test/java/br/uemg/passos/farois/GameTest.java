package br.uemg.passos.farois;

import static org.junit.Assert.*;

import org.junit.Test;

public class GameTest {
  @Test
  public void allSixMapsCanBeCrossedUsingOnlyPlayerInputs() {
    int score = 0, lives = 3;
    for (int level = 0; level < Stage.ALL.length; level++) {
      Game g = new Game(level, score, lives);
      boolean crossingTrap = false;
      for (int tick = 0; tick < 16000 && g.status == Game.RUN; tick++) {
        Stage s = g.stage;
        if (g.grounded) crossingTrap = false;
        if (g.grounded && g.x < g.trapX() && g.trapX() - g.x < 50) {
          g.jump();
          crossingTrap = true;
        }
        if (g.grounded) {
          for (int i = 0; i < s.left.length - 1; i++) {
            float distance = s.left[i] + s.width[i] - g.x;
            if (distance >= 0 && distance < 7) {
              g.jump();
              break;
            }
          }
        }
        for (int i = 0; i < g.enemyX.length; i++) {
          float ahead = g.enemyX[i] - g.x;
          if (!g.dead[i] && ahead >= -5 && ahead < 68 && Math.abs(g.y - g.enemyY[i]) < 85) g.echo();
        }
        g.step(crossingTrap && g.x > g.trapX() + 32 ? 0 : 1);
      }
      assertEquals("Travessia de " + g.stage.name, Game.CLEAR, g.status);
      assertTrue(g.checkpoint);
      assertTrue(g.score >= score + 150);
      score = g.score;
      lives = g.lives;
    }
  }

  @Test
  public void trapConsumesOneLifeAndBirdAndFrogLeaveGround() {
    Game g = new Game(3, 0, 3);
    g.shield = 0;
    g.x = g.trapX();
    g.step(0);
    assertEquals(2, g.lives);
    Game gull = new Game(1, 0, 3);
    assertTrue(gull.enemyY[0] < gull.stage.top[gull.stage.guards[0]]);
    Game frog = new Game(2, 0, 3);
    frog.time = .5f;
    frog.moveGuards(Game.DT);
    assertTrue(frog.enemyY[0] < frog.stage.top[frog.stage.guards[0]]);
  }

  @Test
  public void coinOnlyScoresOnceAcrossRespawns() {
    Game g = new Game(0, 0, 3);
    g.x = g.coinX(0);
    g.step(0);
    assertEquals(25, g.score);
    g.damage();
    g.x = g.coinX(0);
    g.step(0);
    assertEquals(25, g.score);
  }

  @Test
  public void fallingReturnsToFlagAndUsesLastLife() {
    Game g = new Game(2, 100, 2);
    g.x = g.stage.left[g.stage.checkpoint] + 35;
    g.y = g.stage.top[g.stage.checkpoint];
    g.step(0);
    assertTrue(g.checkpoint);
    g.y = 430;
    g.step(0);
    assertEquals(1, g.lives);
    assertEquals(g.stage.left[g.stage.checkpoint] + 28, g.x, .01);
    g.y = 430;
    g.step(0);
    assertEquals(Game.OVER, g.status);
    int score = g.score;
    g.step(1);
    assertEquals(score, g.score);
  }

  @Test
  public void echoHasRangeAndCooldown() {
    Game g = new Game(4, 0, 3);
    g.x = g.enemyX[0];
    g.y = g.stage.top[g.stage.guards[0]];
    g.echo();
    assertTrue(g.stun[0] > 0);
    assertEquals(0, g.stun[g.stun.length - 1], 0);
    g.stun[0] = 0;
    g.echo();
    assertEquals(0, g.stun[0], 0);
  }

  @Test
  public void stompDefeatsGuardAndBounces() {
    Game g = new Game(1, 0, 3);
    g.x = g.enemyX[0];
    g.y = g.enemyY[0] - 20;
    g.coins[g.stage.guards[0]] = true;
    g.grounded = false;
    g.velocityY = 300;
    g.step(0);
    assertTrue(g.dead[0]);
    assertEquals(50, g.score);
    assertTrue(g.velocityY < 0);
  }
}
