package br.uemg.passos.farois;

import static org.junit.Assert.*;

import android.graphics.*;
import android.view.View;
import android.widget.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AndroidFlowTest {
  private ActivityController<MainActivity> controller;
  private MainActivity activity;

  @Before
  public void open() {
    controller = Robolectric.buildActivity(MainActivity.class).setup();
    activity = controller.get();
  }

  @After
  public void close() {
    controller.pause().stop().destroy();
  }

  private Button button(String name) {
    LinearLayout group = activity.findViewById(R.id.actions);
    for (int i = 0; i < group.getChildCount(); i++)
      if (group.getChildAt(i) instanceof Button) {
        Button b = (Button) group.getChildAt(i);
        if (name.contentEquals(b.getText())) return b;
      }
    throw new AssertionError("Botão ausente: " + name);
  }

  @Test
  public void campSettingsResumeAndPause() {
    button("Caderno de campo").performClick();
    button("Efeitos sonoros · ligado").performClick();
    assertFalse(activity.option("som"));
    button("Voltar ao acampamento").performClick();
    button("Iniciar expedição").performClick();
    assertTrue(activity.playing());
    activity.view.game.score = 125;
    controller.pause().resume();
    assertFalse(activity.playing());
    assertEquals(125, activity.store.load().score);
  }

  @Test
  public void progressAndRankingAreStored() {
    FieldStore db = activity.store;
    Game g = new Game(3, 325, 2);
    g.checkpoint = true;
    g.coins[1] = true;
    g.dead[0] = true;
    db.save(g);
    Game copy = db.load();
    assertTrue(copy.coins[1] && copy.dead[0] && copy.checkpoint);
    assertEquals(g.score, copy.score);
    g.status = Game.OVER;
    db.finish(g);
    assertNull(db.load());
    assertTrue(db.ranking().contains("325 pts"));
  }

  @Test
  public void cameraResultAppearsInFieldJournal() {
    button("Caderno de campo").performClick();
    button("Fotografar explorador").performClick();
    android.content.Intent capture =
        org.robolectric.Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
    assertEquals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE, capture.getAction());
    android.content.Intent result = new android.content.Intent();
    result.putExtra("data", Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888));
    activity.onActivityResult(23, android.app.Activity.RESULT_OK, result);
    assertTrue(new File(activity.getFilesDir(), "retrato.png").length() > 0);
    LinearLayout group = activity.findViewById(R.id.actions);
    assertTrue(group.getChildAt(1) instanceof ImageView);
  }

  @Test
  public void deniedMicrophoneKeepsTouchControlsPlayable() {
    button("Caderno de campo").performClick();
    button("Eco por som · desligado").performClick();
    activity.onRequestPermissionsResult(
        21,
        new String[] {android.Manifest.permission.RECORD_AUDIO},
        new int[] {android.content.pm.PackageManager.PERMISSION_DENIED});
    assertFalse(activity.option("mic"));
    button("Voltar ao acampamento").performClick();
    button("Iniciar expedição").performClick();
    GameView view = activity.view;
    view.layout(0, 0, 720, 405);
    android.view.MotionEvent touch =
        android.view.MotionEvent.obtain(0, 0, android.view.MotionEvent.ACTION_DOWN, 570, 365, 0);
    view.onTouchEvent(touch);
    touch.recycle();
    assertTrue(view.game.echoCooldown > 0);
  }

  @Test
  public void renderScenes() throws Exception {
    CoverView cover = new CoverView(activity, null);
    draw(cover, 640, 540, "capa");
    button("Iniciar expedição").performClick();
    GameView v = activity.view;
    v.active(false);
    v.game.shield = 0;
    v.game.time = 5;
    v.game.x = 248;
    draw(v, 1080, 608, "enseada");
  }

  private void draw(View v, int w, int h, String name) throws IOException {
    v.layout(0, 0, w, h);
    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    v.draw(new Canvas(bmp));
    assertNotEquals(bmp.getPixel(20, 20), bmp.getPixel(w / 2, h * 3 / 4));
    File dir = new File("build/previews");
    if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Diretório de prévia");
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      assertTrue(bmp.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
  }
}
