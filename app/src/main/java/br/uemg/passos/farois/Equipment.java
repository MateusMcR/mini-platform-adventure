package br.uemg.passos.farois;

import android.annotation.SuppressLint;
import android.content.*;
import android.hardware.*;
import android.location.*;
import android.media.*;
import android.os.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** O microfone mede a amplitude em memória. A consulta meteorológica sempre usa Passos. */
final class Equipment implements SensorEventListener {
  final MainActivity host;
  final SensorManager sensors;
  final Sensor gyro;
  final LocationManager locations;
  final Handler main = new Handler(Looper.getMainLooper());
  private LocationListener listener;
  private final Runnable timeout;
  private volatile int audioSession;
  private long lastGesture;
  private boolean fetching;

  Equipment(MainActivity host) {
    this.host = host;
    timeout =
        () -> {
          stopGps();
          host.note("Sem posição disponível. Tente novamente em local aberto.");
        };
    sensors = (SensorManager) host.getSystemService(Context.SENSOR_SERVICE);
    gyro = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    locations = (LocationManager) host.getSystemService(Context.LOCATION_SERVICE);
  }

  void startPlay() {
    stopPlay();
    if (gyro != null && host.option("gyro"))
      sensors.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
    if (host.option("mic") && host.allowed(android.Manifest.permission.RECORD_AUDIO)) {
      int session = audioSession;
      new Thread(() -> listen(session), "amplitude").start();
    }
  }

  void stopPlay() {
    sensors.unregisterListener(this);
    audioSession++;
  }

  boolean valid(int session) {
    return session == audioSession;
  }

  @SuppressLint("MissingPermission")
  void listen(int session) {
    AudioRecord input = null;
    try {
      int minimum =
          AudioRecord.getMinBufferSize(
              16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
      if (minimum <= 0) throw new IllegalStateException();
      input =
          new AudioRecord(
              MediaRecorder.AudioSource.MIC,
              16000,
              AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
              Math.max(2048, minimum));
      if (!valid(session)) return;
      input.startRecording();
      short[] samples = new short[512];
      long lastEcho = 0;
      while (valid(session)) {
        int n = input.read(samples, 0, samples.length);
        if (n <= 0) throw new IllegalStateException();
        long energy = 0;
        for (int i = 0; i < n; i++) energy += (long) samples[i] * samples[i];
        long now = SystemClock.elapsedRealtime();
        if (energy / n > 2400L * 2400 && now - lastEcho > 900) {
          lastEcho = now;
          main.post(
              () -> {
                if (valid(session) && host.playing()) host.view.game.echo();
              });
        }
      }
    } catch (RuntimeException e) {
      main.post(
          () -> {
            if (valid(session)) host.note("Microfone indisponível. Use o botão ECO.");
          });
    } finally {
      if (input != null) {
        try {
          if (input.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) input.stop();
        } finally {
          input.release();
        }
      }
    }
  }

  @Override
  public void onSensorChanged(SensorEvent e) {
    long now = SystemClock.elapsedRealtime();
    if (Math.abs(e.values[2]) > 2.5f && now - lastGesture > 550 && host.playing()) {
      host.view.game.jump();
      lastGesture = now;
    }
  }

  @Override
  public void onAccuracyChanged(Sensor s, int accuracy) {}

  @SuppressLint("MissingPermission")
  void gps() {
    stopGps();
    boolean fine = host.allowed(android.Manifest.permission.ACCESS_FINE_LOCATION);
    String provider =
        fine && locations.isProviderEnabled(LocationManager.GPS_PROVIDER)
            ? LocationManager.GPS_PROVIDER
            : LocationManager.NETWORK_PROVIDER;
    if (!locations.isProviderEnabled(provider)) {
      host.note("Ative a localização do aparelho para obter o selo.");
      return;
    }
    listener =
        new LocationListener() {
          public void onLocationChanged(Location fix) {
            String region =
                (fix.getLatitude() >= 0 ? "Norte" : "Sul")
                    + " / "
                    + (fix.getLongitude() >= 0 ? "Leste" : "Oeste");
            float[] distance = new float[1];
            Location.distanceBetween(
                fix.getLatitude(), fix.getLongitude(), -20.72, -46.61, distance);
            region +=
                String.format(
                    Locale.forLanguageTag("pt-BR"), " · %.0f km de Passos", distance[0] / 1000f);
            host.prefs.edit().putString("stamp", region).apply();
            stopGps();
            host.refreshCamp();
            host.note("Selo registrado: " + region);
          }

          public void onProviderDisabled(String p) {
            stopGps();
            host.note("Localização desativada.");
          }

          public void onProviderEnabled(String p) {}

          public void onStatusChanged(String p, int s, Bundle b) {}
        };
    try {
      locations.requestLocationUpdates(provider, 0, 0, listener, Looper.getMainLooper());
      main.postDelayed(timeout, 25000);
      host.note("Buscando o selo de localização…");
    } catch (IllegalArgumentException | SecurityException e) {
      stopGps();
      host.note("Localização indisponível.");
    }
  }

  void stopGps() {
    main.removeCallbacks(timeout);
    if (listener != null) {
      locations.removeUpdates(listener);
      listener = null;
    }
  }

  void weather() {
    if (fetching) return;
    fetching = true;
    host.note("Consultando o tempo em Passos…");
    new Thread(
            () -> {
              HttpsURLConnection conn = null;
              try {
                URL url =
                    new URL(
                        "https://api.open-meteo.com/v1/forecast?latitude=-20.72&longitude=-46.61&current=weather_code,temperature_2m");
                conn = (HttpsURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                StringBuilder json = new StringBuilder();
                try (BufferedReader r =
                    new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = r.readLine()) != null && json.length() < 20000) json.append(line);
                }
                JSONObject cur = new JSONObject(json.toString()).getJSONObject("current");
                int code = cur.getInt("weather_code");
                boolean wet = code >= 51;
                String info =
                    String.format(
                        Locale.forLanguageTag("pt-BR"),
                        "%.0f °C · %s · %s",
                        cur.getDouble("temperature_2m"),
                        wet ? "precipitação" : "tempo seco",
                        cur.getString("time").replace("T", " ") + " UTC");
                main.post(
                    () -> {
                      if (host.isDestroyed()) return;
                      fetching = false;
                      host.prefs.edit().putString("weather", info).putBoolean("wet", wet).apply();
                      host.refreshCamp();
                      host.note("Clima de Passos atualizado.");
                    });
              } catch (Exception e) {
                main.post(
                    () -> {
                      if (host.isDestroyed()) return;
                      fetching = false;
                      host.note("Sem atualização. O último cenário continua disponível.");
                    });
              } finally {
                if (conn != null) conn.disconnect();
              }
            },
            "clima-passos")
        .start();
  }
}
