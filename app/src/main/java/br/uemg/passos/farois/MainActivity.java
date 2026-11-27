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
  SeaChart chart;
  private boolean replay;
  private MediaPlayer music;
  private static final int[] THEMES = {
    R.raw.ilha0, R.raw.ilha1, R.raw.ilha2, R.raw.ilha3, R.raw.ilha4, R.raw.ilha5
  };
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
    else if (state != null && state.containsKey("island")) openChart(state.getInt("island"));
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
    if (chart != null) b.putInt("island", chart.selected);
  }

  boolean allowed(String permission) {
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
  }

  boolean option(String key) {
    return prefs.getBoolean(key, key.equals("som") || key.equals("music"));
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
    chart = null;
    releaseMusic();
    if (view != null) view.active(false);
    gear.stopPlay();
    if (!tools) gear.stopGps();
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    view = null;
    fieldKit = tools;
    setContentView(R.layout.camp);
    panel = findViewById(R.id.actions);
    if (tools) {
      ScrollView sheet = (ScrollView) panel.getParent();
      LinearLayout.LayoutParams size = (LinearLayout.LayoutParams) sheet.getLayoutParams();
      size.weight = .64f;
      sheet.setLayoutParams(size);
    }
  }

  private void text(String value, int sp, int color, boolean title) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextColor(color == Scenery.GOLD ? 0xff794b31 : 0xff363d32);
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
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    b.setStateListAnimator(null);
    b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
    b.setPadding(dp(14), 0, dp(10), 0);
    b.setTextColor(primary ? 0xfffff2d8 : 0xff373d33);
    GradientDrawable shape = new GradientDrawable();
    shape.setCornerRadius(dp(2));
    shape.setColor(primary ? 0xff854b36 : fieldKit ? 0xffe8dabc : 0xfff0e5ca);
    if (primary || fieldKit) shape.setStroke(dp(1), primary ? 0xff69402f : 0xffc0af8c);
    if (!fieldKit) {
      int icon =
          primary
              ? R.drawable.ic_play
              : label.equals("Como jogar")
                  ? R.drawable.ic_game
                  : label.equals("Ranking local")
                      ? R.drawable.ic_flag
                      : label.equals("Caderno de campo") ? R.drawable.ic_book : 0;
      b.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
      b.setCompoundDrawablePadding(dp(9));
    }
    b.setBackground(
        new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x5574baac), shape, null));
    b.setOnClickListener(v -> action.run());
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
    lp.bottomMargin = dp(5);
    panel.addView(b, lp);
    if (!fieldKit && !primary) {
      View rule = new View(this);
      rule.setBackgroundColor(0xffcfbea0);
      panel.addView(rule, new LinearLayout.LayoutParams(-1, dp(1)));
    }
  }

  private void camp() {
    screen(false);
    Game save = store.load();
    TextView prefix = new TextView(this);
    prefix.setText("Trilha dos");
    prefix.setTextColor(0xff454834);
    prefix.setTextSize(25);
    prefix.setTypeface(Typeface.create("serif", Typeface.ITALIC));
    prefix.setIncludeFontPadding(false);
    panel.addView(prefix);
    ImageView logo = new ImageView(this);
    logo.setContentDescription("Faróis");
    android.graphics.drawable.BitmapDrawable lettering =
        (android.graphics.drawable.BitmapDrawable) getDrawable(R.drawable.titulo_farois);
    lettering.setFilterBitmap(false);
    logo.setImageDrawable(lettering);
    logo.setScaleType(ImageView.ScaleType.FIT_START);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(49));
    lp.bottomMargin = dp(12);
    panel.addView(logo, lp);
    if (save != null)
      text(
          "Fase " + (save.index + 1) + " • " + save.stage.name + " • " + save.score + " pts",
          14,
          Scenery.GOLD,
          false);
    button(save == null ? "Iniciar expedição" : "Continuar expedição", true, this::embark);
    button("Caderno de campo", false, this::kit);
    button("Como jogar", false, this::help);
    button("Diário de Lia", false, () -> message("A concha do Aurora", Voyage.PROLOGUE));
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
                  .setPositiveButton(
                      "Recomeçar",
                      (d, w) -> {
                        store.save(new Game(0, 0, 3));
                        openChart(0);
                      })
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
    button("Trilha das ilhas · " + onOff("music"), false, () -> toggle("music"));
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
            + " recarrega em 1,8 segundo.\n\n"
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

  private void embark() {
    Game save = store.load();
    if (save == null) {
      save = new Game(0, 0, 3);
      store.save(save);
    }
    openChart(save.index);
    if (!prefs.getBoolean("prologue", false)) {
      prefs.edit().putBoolean("prologue", true).apply();
      message("A concha do Aurora", Voyage.PROLOGUE);
    }
  }

  private int unlocked(Game save) {
    return Math.max(
        store.historicalIsland(),
        Math.max(save == null ? 0 : save.index, prefs.getInt("openIsland", 0)));
  }

  private void openChart(int selected) {
    screen(false);
    Game save = store.load();
    int open = unlocked(save);
    LinearLayout layout = new LinearLayout(this);
    layout.setBackgroundColor(0xfff0e5ca);
    chart = new SeaChart(this, open, Math.max(0, Math.min(5, selected)));
    layout.addView(chart, new LinearLayout.LayoutParams(0, -1, 1.8f));
    ScrollView sheet = new ScrollView(this);
    sheet.setFillViewport(true);
    panel = new LinearLayout(this);
    panel.setId(R.id.actions);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setGravity(Gravity.CENTER_VERTICAL);
    panel.setPadding(dp(12), dp(8), dp(12), dp(8));
    sheet.addView(panel);
    layout.addView(sheet, new LinearLayout.LayoutParams(0, -1, 1));
    setContentView(layout);
    chartInfo();
    chart.requestFocus();
  }

  void chartInfo() {
    panel.removeAllViews();
    Game save = store.load();
    int n = chart.selected, open = unlocked(save);
    text("Carta do arquipélago", 19, Scenery.INK, true);
    text((n + 1) + " / 6   " + Stage.ALL[n].name, 16, Scenery.GOLD, false);
    if (n <= open) {
      text(Voyage.CHAPTER[n], 14, Scenery.INK, false);
      boolean current = save != null && n == save.index;
      button(current ? "Entrar na fase" : "Revisitar ilha", true, this::enterIsland);
      if (!current) text("Visita livre · 3 vidas", 12, Scenery.GOLD, false);
    } else
      text(
          "Acenda o farol de " + Stage.ALL[n - 1].name + " para liberar a rota.",
          14,
          Scenery.INK,
          false);
    LinearLayout nav = new LinearLayout(this);
    for (int d : new int[] {-1, 1}) {
      Button b = new Button(this);
      b.setText(d < 0 ? "◀" : "▶");
      b.setContentDescription(d < 0 ? "Ilha anterior" : "Ilha seguinte");
      b.setEnabled(n + d >= 0 && n + d < 6);
      b.setOnClickListener(v -> chart.select(n + d));
      nav.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
    }
    panel.addView(nav);
    button("Acampamento", false, this::camp);
  }

  void enterIsland() {
    if (chart == null) return;
    int n = chart.selected;
    Game save = store.load();
    if (n > unlocked(save)) return;
    replay = save == null || n != save.index;
    start(replay ? new Game(n, 0, 3) : save);
  }

  void saveCampaign(Game g) {
    if (!replay) store.save(g);
  }

  private void releaseMusic() {
    if (music != null) {
      music.release();
      music = null;
    }
  }

  private void pauseMusic() {
    if (music != null && music.isPlaying()) music.pause();
  }

  private void playMusic() {
    if (!option("music")) return;
    if (music == null) {
      music = MediaPlayer.create(this, THEMES[view.game.index]);
      if (music != null) {
        music.setVolume(.37f, .37f);
        music.setLooping(true);
      }
    }
    if (music != null) music.start();
  }

  private void start(Game game) {
    chart = null;
    releaseMusic();
    fieldKit = false;
    gear.stopGps();
    saveCampaign(game);
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
      playMusic();
    }
  }

  void pauseGame() {
    if (view == null || view.game.status != Game.RUN || overlay != null) return;
    view.active(false);
    pauseMusic();
    gear.stopPlay();
    saveCampaign(view.game);
    overlay =
        new AlertDialog.Builder(this)
            .setTitle("Expedição pausada")
            .setMessage(
                replay
                    ? "Ao voltar à carta, você encerra a visita livre."
                    : "Ao voltar à carta, você poderá retomar a fase na bandeira ou no início.")
            .setCancelable(false)
            .setPositiveButton(
                "Continuar",
                (d, w) -> {
                  overlay = null;
                  resumePlay();
                })
            .setNegativeButton(
                "Carta das ilhas",
                (d, w) -> {
                  overlay = null;
                  openChart(view.game.index);
                })
            .show();
  }

  void result(GameView source) {
    if (source != view || overlay != null || isFinishing() || isDestroyed()) return;
    gear.stopPlay();
    releaseMusic();
    Game g = view.game;
    if (replay) {
      overlay =
          new AlertDialog.Builder(this)
              .setTitle(g.status == Game.CLEAR ? "Ilha revisitada" : "Fim da visita")
              .setMessage(g.score + " pontos nesta visita.")
              .setCancelable(false)
              .setPositiveButton(
                  "Voltar à carta",
                  (d, w) -> {
                    overlay = null;
                    openChart(g.index);
                  })
              .show();
      return;
    }
    boolean clear = g.status == Game.CLEAR, more = clear && g.index + 1 < Stage.ALL.length;
    Game next = more ? new Game(g.index + 1, g.score, g.lives) : null;
    if (clear)
      prefs.edit().putInt("openIsland", Math.max(unlocked(g), Math.min(5, g.index + 1))).apply();
    if (more) store.save(next);
    else store.finish(g);
    String title =
        more ? "Farol aceso!" : clear ? "Os seis faróis estão acesos!" : "Fim da expedição";
    String body =
        (clear && !more ? Voyage.END + "\n\n" : "")
            + g.stage.name
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
          "Ver rota na carta",
          (d, w) -> {
            overlay = null;
            openChart(next.index);
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
    else if (fieldKit || chart != null) camp();
    else super.onBackPressed();
  }

  @Override
  protected void onPause() {
    foreground = false;
    pauseMusic();
    if (view != null) {
      view.active(false);
      if (view.game.status == Game.RUN) saveCampaign(view.game);
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
    releaseMusic();
    if (sounds != null) sounds.release();
    store.close();
    super.onDestroy();
  }
}
