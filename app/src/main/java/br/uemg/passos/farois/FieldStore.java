package br.uemg.passos.farois;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;

/** Uma linha guarda a campanha; o ranking guarda as melhores tentativas concluídas. */
final class FieldStore extends SQLiteOpenHelper {
  FieldStore(Context c) {
    super(c, "expedicao.db", null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE save(id INTEGER PRIMARY KEY, stage INTEGER, points INTEGER, lives INTEGER,"
            + " flag INTEGER, coins INTEGER, dead INTEGER)");
    db.execSQL(
        "CREATE TABLE ranking(id INTEGER PRIMARY KEY, points INTEGER, stage INTEGER, won INTEGER)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

  private static int bits(boolean[] array) {
    int value = 0;
    for (int i = 0; i < array.length; i++) if (array[i]) value |= 1 << i;
    return value;
  }

  private static void unpack(boolean[] array, int mask) {
    for (int i = 0; i < array.length; i++) array[i] = (mask & (1 << i)) != 0;
  }

  void save(Game g) {
    ContentValues v = new ContentValues();
    v.put("id", 1);
    v.put("stage", g.index);
    v.put("points", g.score);
    v.put("lives", g.lives);
    v.put("flag", g.checkpoint ? 1 : 0);
    v.put("coins", bits(g.coins));
    v.put("dead", bits(g.dead));
    getWritableDatabase().insertWithOnConflict("save", null, v, SQLiteDatabase.CONFLICT_REPLACE);
  }

  Game load() {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery("SELECT stage,points,lives,flag,coins,dead FROM save WHERE id=1", null)) {
      if (!c.moveToFirst()) return null;
      Game g = new Game(c.getInt(0), c.getInt(1), c.getInt(2));
      g.checkpoint = c.getInt(3) != 0;
      unpack(g.coins, c.getInt(4));
      unpack(g.dead, c.getInt(5));
      g.respawn();
      return g;
    }
  }

  void finish(Game g) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      db.execSQL(
          "INSERT INTO ranking(points,stage,won) VALUES(?,?,?)",
          new Object[] {g.score, g.index + 1, g.status == Game.CLEAR ? 1 : 0});
      db.delete("save", null, null);
      db.execSQL(
          "DELETE FROM ranking WHERE id NOT IN (SELECT id FROM ranking ORDER BY points DESC, won"
              + " DESC, id DESC LIMIT 10)");
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  int historicalIsland() {
    try (Cursor c = getReadableDatabase().rawQuery("SELECT MAX(stage) FROM ranking", null)) {
      return c.moveToFirst() ? Math.max(0, Math.min(5, c.getInt(0) - 1)) : 0;
    }
  }

  String ranking() {
    StringBuilder b = new StringBuilder();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT points,stage,won FROM ranking ORDER BY points DESC,won DESC,id DESC",
                null)) {
      int place = 1;
      while (c.moveToNext())
        b.append(place++)
            .append("º    ")
            .append(c.getInt(0))
            .append(" pts     ·     ")
            .append(c.getInt(2) == 1 ? "Campanha concluída" : "Fase " + c.getInt(1))
            .append("\n");
    }
    return b.length() == 0
        ? "Sua primeira marca aparece aqui ao concluir uma campanha ou perder a última vida."
        : b.toString().trim();
  }
}
