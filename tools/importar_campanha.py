"""Copia o save da versão 1.1 para uma instalação 1.2 ainda sem campanha (ADB)."""
from pathlib import Path
import sqlite3
import subprocess
import tempfile

OLD = "br.uemg.passos.farois"
NEW = OLD + ".aurora"
DB = "databases/expedicao.db"


def adb(*args, data=None, check=True):
    return subprocess.run(["adb", *args], input=data, capture_output=True, check=check)


def exists(package, path):
    return adb("shell", "run-as", package, "test", "-f", path, check=False).returncode == 0


def main():
    # Ambos os aplicativos ficam parados enquanto os arquivos do SQLite são lidos.
    for package in (OLD, NEW):
        adb("shell", "run-as", package, "id")
        adb("shell", "am", "force-stop", package)
    if not exists(OLD, DB):
        raise ValueError("Nenhuma campanha da versão anterior encontrada.")
    if exists(NEW, DB):
        raise ValueError("O banco da versão 1.2 já existe. Seus dados foram preservados.")
    with tempfile.TemporaryDirectory() as folder:
        snapshot = Path(folder) / "origem.db"
        for suffix in ("", "-wal", "-journal"):
            if exists(OLD, DB + suffix):
                raw = adb("exec-out", "run-as", OLD, "cat", DB + suffix).stdout
                Path(str(snapshot) + suffix).write_bytes(raw)
        export = Path(folder) / "campanha.db"
        with sqlite3.connect(snapshot) as src, sqlite3.connect(export) as dst:
            if src.execute("PRAGMA integrity_check").fetchone() != ("ok",):
                raise ValueError("O banco da versão anterior falhou na verificação.")
            src.execute("SELECT stage, points, lives FROM save").fetchall()
            src.execute("SELECT points FROM ranking").fetchall()
            src.backup(dst)
        payload = export.read_bytes()
        adb("shell", "run-as", NEW, "mkdir", "-p", "databases")
        tmp = DB + ".import"
        adb("shell", "-T", "run-as", NEW, "tee", tmp, data=payload)
        if adb("exec-out", "run-as", NEW, "cat", tmp).stdout != payload:
            raise ValueError("A cópia não passou na conferência.")
        # A criação do link falha se o jogo já tiver criado outro banco nesse intervalo.
        adb("shell", "run-as", NEW, "ln", tmp, DB)
        adb("shell", "run-as", NEW, "rm", tmp)
    print("Campanha e ranking copiados. Abra a versão 1.2.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, subprocess.CalledProcessError, ValueError, sqlite3.Error) as error:
        raise SystemExit(f"Importação interrompida: {error}")
