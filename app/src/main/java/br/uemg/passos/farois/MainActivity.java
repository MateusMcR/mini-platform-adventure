package br.uemg.passos.farois;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;

public final class MainActivity extends Activity {
  private static final int MIC = 21, GPS = 22, CAMERA = 23;
  FieldStore store;
  SharedPreferences prefs;
  Equipment gear;
  GameView view;
  private LinearLayout panel;
  private boolean fieldKit, foreground;
  private AlertDialog overlay;
  private SoundPool sounds;
  private final android.util.SparseIntArray clips = new android.util.SparseIntArray();

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    immersive();
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    prefs = getSharedPreferences("caderno", MODE_PRIVATE);
    store = new FieldStore(this);
    gear = new Equipment(this);
    AudioAttributes effects =
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
    sounds = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(effects).build();
    int[] events = {Game.COIN, Game.LEAP, Game.ECHO, Game.HIT, Game.FLAG, Game.STOMP};
    int[] files = {R.raw.moeda, R.raw.pulo, R.raw.eco, R.raw.queda, R.raw.farol, R.raw.guarda};
    for (int i = 0; i < events.length; i++) clips.put(events[i], sounds.load(this, files[i], 1));
    if (state != null && state.getBoolean("kit")) kit();
    else camp();
  }

  private void immersive() {
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  }

  @Override
  public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    if (focus) immersive();
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putBoolean("kit", fieldKit);
  }

  boolean allowed(String permission) {
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
  }

  boolean option(String key) {
    return prefs.getBoolean(key, key.equals("som"));
  }

  boolean rain() {
    return prefs.getBoolean("wet", false);
  }

  boolean playing() {
    return foreground && view != null && view.running;
  }

  void note(String message) {
    if (!isDestroyed()) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  int dp(float v) {
    return Math.round(v * getResources().getDisplayMetrics().density);
  }

  void sound(int event) {
    if (sounds == null || !option("som")) return;
    for (int i = 0; i < clips.size(); i++) {
      if ((event & clips.keyAt(i)) != 0) sounds.play(clips.valueAt(i), .55f, .55f, 1, 0, 1);
    }
  }

  private void screen(boolean tools) {
    if (view != null) view.active(false);
    gear.stopPlay();
    if (!tools) gear.stopGps();
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    view = null;
    fieldKit = tools;
    setContentView(R.layout.camp);
    panel = findViewById(R.id.actions);
  }

  private void text(String value, int sp, int color, boolean title) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextColor(color);
    t.setTextSize(sp);
    t.setTypeface(title ? Scenery.TITLE : Scenery.BOLD);
    t.setPadding(0, dp(2), 0, dp(5));
    panel.addView(t);
  }

  private void button(String label, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setTextSize(15);
    b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
    b.setPadding(dp(14), 0, dp(10), 0);
    b.setTextColor(primary ? Scenery.INK : Scenery.CREAM);
    GradientDrawable shape = new GradientDrawable();
    shape.setCornerRadius(dp(9));
    shape.setColor(primary ? Scenery.GOLD : 0xff25454d);
    b.setBackground(
        new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x5574baac), shape, null));
    b.setOnClickListener(v -> action.run());
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
    lp.bottomMargin = dp(5);
    panel.addView(b, lp);
  }

  private void camp() {
    screen(false);
    Game save = store.load();
    text("Acampamento", 25, Scenery.CREAM, true);
    if (save == null)
      text("Acenda os seis faróis antes que acabem suas vidas.", 14, 0xffbed1c9, false);
    else
      text(
          "Fase " + (save.index + 1) + " • " + save.stage.name + " • " + save.score + " pts",
          14,
          Scenery.GOLD,
          false);
    button(
        save == null ? "Iniciar expedição" : "Continuar expedição",
        true,
        () -> start(save == null ? new Game(0, 0, 3) : save));
    button("Caderno de campo", false, this::kit);
    button("Como jogar", false, this::help);
    button("Ranking local", false, () -> message("Ranking da expedição", store.ranking()));
    if (save != null)
      button(
          "Começar de novo",
          false,
          () ->
              new AlertDialog.Builder(this)
                  .setTitle("Recomeçar na primeira ilha?")
                  .setMessage("A campanha salva será substituída. O ranking será preservado.")
                  .setNegativeButton("Voltar", null)
                  .setPositiveButton("Recomeçar", (d, w) -> start(new Game(0, 0, 3)))
                  .show());
  }

  void refreshCamp() {
    if (fieldKit && view == null) kit();
  }

  private File portrait() {
    return new File(getFilesDir(), "retrato.png");
  }

  private void kit() {
    screen(true);
    text("Caderno de campo", 25, Scenery.CREAM, true);
    if (portrait().isFile()) {
      ImageView iv = new ImageView(this);
      iv.setImageBitmap(BitmapFactory.decodeFile(portrait().toString()));
      iv.setContentDescription("Retrato da expedição");
      iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      panel.addView(iv, new LinearLayout.LayoutParams(-1, dp(68)));
    }
    text("Selo da expedição · " + prefs.getString("stamp", "a descobrir"), 14, Scenery.GOLD, false);
    button("Fotografar explorador", false, this::camera);
    button(
        "Obter selo por localização",
        false,
        () -> {
          if (allowed(Manifest.permission.ACCESS_COARSE_LOCATION)) gear.gps();
          else
            requestPermissions(
                new String[] {
                  Manifest.permission.ACCESS_COARSE_LOCATION,
                  Manifest.permission.ACCESS_FINE_LOCATION
                },
                GPS);
        });
    text("Clima de Passos · " + prefs.getString("weather", "a consultar"), 14, 0xffbed1c9, false);
    button("Atualizar clima pela internet", false, gear::weather);
    button(
        "Gesto de pulo · " + (gear.gyro == null ? "sem giroscópio" : onOff("gyro")),
        false,
        () -> {
          if (gear.gyro == null) note("Este aparelho não possui giroscópio.");
          else toggle("gyro");
        });
    button(
        "Eco por som · " + onOff("mic"),
        false,
        () -> {
          if (option("mic") || allowed(Manifest.permission.RECORD_AUDIO)) toggle("mic");
          else requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, MIC);
        });
    button("Efeitos sonoros · " + onOff("som"), false, () -> toggle("som"));
    button("Voltar ao acampamento", true, this::camp);
  }

  private String onOff(String key) {
    return option(key) ? "ligado" : "desligado";
  }

  private void toggle(String key) {
    prefs.edit().putBoolean(key, !option(key)).apply();
    kit();
  }

  private void camera() {
    try {
      startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA);
    } catch (ActivityNotFoundException | SecurityException e) {
      note("Nenhum aplicativo de câmera disponível.");
    }
  }

  @Override
  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    if (req != CAMERA || result != RESULT_OK) return;
    Object photo = data != null && data.getExtras() != null ? data.getExtras().get("data") : null;
    if (!(photo instanceof Bitmap)) {
      note("A câmera não retornou uma miniatura.");
      return;
    }
    try (FileOutputStream out = new FileOutputStream(portrait())) {
      if (!((Bitmap) photo).compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IOException();
      kit();
      note("Retrato salvo no caderno.");
    } catch (IOException e) {
      note("Não foi possível salvar o retrato.");
    }
  }

  @Override
  public void onRequestPermissionsResult(int req, String[] permissions, int[] result) {
    super.onRequestPermissionsResult(req, permissions, result);
    if (req == MIC && allowed(Manifest.permission.RECORD_AUDIO)) {
      prefs.edit().putBoolean("mic", true).apply();
      refreshCamp();
    } else if (req == GPS && allowed(Manifest.permission.ACCESS_COARSE_LOCATION)) gear.gps();
    else note("Permissão não concedida. A campanha continua disponível.");
  }

  private void help() {
    message(
        "Como jogar",
        "Segure ◀ ou ▶ e toque em PULO com outro dedo para atravessar os vãos. Aterrisse sobre"
            + " guardas para derrotá-los. ECO atordoa os guardas próximos por 2 segundos e"
            + " recarrega em 2,6 segundos.\n\n"
            + "Moeda: 25 pts. Guarda: 50 pts. Farol: 150 pts. Você começa com 3 vidas. A bandeira"
            + " marca o retorno após uma queda. Chegue ao farol para avançar.\n\n"
            + "No caderno, ative o giroscópio para pular com um giro breve no plano da tela ou o"
            + " microfone para usar ECO com uma palma. O clima de Passos muda o cenário quando há"
            + " precipitação.\n\n"
            + "Teclado: ← → ou A/D, espaço para pular, E para eco e P para pausar.");
  }

  private void message(String title, String body) {
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(body)
        .setPositiveButton("Voltar", null)
        .show();
  }

  private void start(Game game) {
    fieldKit = false;
    gear.stopGps();
    store.save(game);
    view = new GameView(this, game);
    setContentView(view);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    resumePlay();
  }

  private void resumePlay() {
    if (view != null && foreground) {
      view.active(true);
      view.requestFocus();
      gear.startPlay();
    }
  }

  void pauseGame() {
    if (view == null || view.game.status != Game.RUN || overlay != null) return;
    view.active(false);
    gear.stopPlay();
    store.save(view.game);
    overlay =
        new AlertDialog.Builder(this)
            .setTitle("Expedição pausada")
            .setMessage(
                "Ao voltar ao acampamento, você retoma a fase na última bandeira ou no início da"
                    + " ilha inicial.")
            .setCancelable(false)
            .setPositiveButton(
                "Continuar",
                (d, w) -> {
                  overlay = null;
                  resumePlay();
                })
            .setNegativeButton(
                "Acampamento",
                (d, w) -> {
                  overlay = null;
                  camp();
                })
            .show();
  }

  void result(GameView source) {
    if (source != view || overlay != null || isFinishing() || isDestroyed()) return;
    gear.stopPlay();
    Game g = view.game;
    boolean clear = g.status == Game.CLEAR, more = clear && g.index + 1 < Stage.ALL.length;
    Game next = more ? new Game(g.index + 1, g.score, g.lives) : null;
    if (more) store.save(next);
    else store.finish(g);
    String title =
        more ? "Farol aceso!" : clear ? "Os seis faróis estão acesos!" : "Fim da expedição";
    String body =
        g.stage.name
            + "   •   "
            + g.score
            + " pontos"
            + (more
                ? "\nPróximo destino: " + next.stage.name
                : "\nResultado registrado no ranking local.");
    AlertDialog.Builder b =
        new AlertDialog.Builder(this).setTitle(title).setMessage(body).setCancelable(false);
    if (more)
      b.setPositiveButton(
          "Próxima fase",
          (d, w) -> {
            overlay = null;
            start(next);
          });
    b.setNegativeButton(
        "Acampamento",
        (d, w) -> {
          overlay = null;
          camp();
        });
    overlay = b.show();
  }

  @Override
  public void onBackPressed() {
    if (view != null) pauseGame();
    else if (fieldKit) camp();
    else super.onBackPressed();
  }

  @Override
  protected void onPause() {
    foreground = false;
    if (view != null) {
      view.active(false);
      if (view.game.status == Game.RUN) store.save(view.game);
    }
    gear.stopPlay();
    gear.stopGps();
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    foreground = true;
    if (view != null && overlay == null) pauseGame();
  }

  @Override
  protected void onDestroy() {
    gear.stopPlay();
    gear.stopGps();
    if (sounds != null) sounds.release();
    store.close();
    super.onDestroy();
  }
}
