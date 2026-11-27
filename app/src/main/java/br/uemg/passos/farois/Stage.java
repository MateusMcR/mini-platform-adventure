package br.uemg.passos.farois;

/** Cada ilha tem largura, altura e distância até a próxima. Índices de guardas começam em zero. */
final class Stage {
  final String name, hint;
  final int[] left, width, top, guards;
  final int checkpoint, goal;

  Stage(String name, String hint, int[] width, int[] top, int[] gaps, int... guards) {
    this.name = name;
    this.hint = hint;
    this.width = width;
    this.top = top;
    this.guards = guards;
    left = new int[width.length];
    for (int i = 1; i < left.length; i++) left[i] = left[i - 1] + width[i - 1] + gaps[i - 1];
    checkpoint = left.length / 2;
    goal = left[left.length - 1] + width[width.length - 1] - 64;
  }

  static final Stage[] ALL = {
    new Stage(
        "Enseada Verde",
        "Segure ▶ e toque em PULO perto da borda.",
        new int[] {360, 196, 220, 240, 200, 310},
        new int[] {300, 300, 286, 300, 282, 300},
        new int[] {38, 48, 54, 58, 62},
        4),
    new Stage(
        "Falésias de Cobre",
        "Caia sobre um guarda para ganhar 50 pontos.",
        new int[] {270, 168, 170, 234, 152, 178, 280},
        new int[] {304, 282, 260, 292, 272, 252, 288},
        new int[] {54, 58, 62, 52, 60, 64},
        2,
        5),
    new Stage(
        "Bosque da Névoa",
        "O ECO atordoa os guardas próximos por dois segundos.",
        new int[] {262, 160, 144, 166, 218, 152, 142, 274},
        new int[] {302, 286, 264, 284, 304, 276, 254, 282},
        new int[] {60, 65, 72, 62, 70, 63, 76},
        1,
        3,
        6),
    new Stage(
        "Salinas de Vidro",
        "A bandeira azul marca seu ponto de retorno.",
        new int[] {248, 144, 138, 154, 200, 142, 156, 136, 252},
        new int[] {306, 280, 254, 282, 296, 270, 248, 270, 290},
        new int[] {70, 74, 78, 72, 74, 80, 72, 78},
        1,
        3,
        5,
        7),
    new Stage(
        "Caldeira Rubra",
        "Use o ECO antes de pousar numa ilha ocupada.",
        new int[] {228, 130, 138, 128, 170, 200, 138, 128, 136, 244},
        new int[] {304, 284, 260, 282, 254, 284, 260, 238, 266, 294},
        new int[] {76, 80, 84, 77, 85, 80, 76, 86, 83},
        1,
        3,
        4,
        6,
        8),
    new Stage(
        "Farol das Estrelas",
        "O último farol fica depois da subida.",
        new int[] {236, 136, 128, 132, 126, 192, 128, 124, 130, 122, 270},
        new int[] {303, 281, 258, 278, 253, 283, 256, 234, 259, 236, 262},
        new int[] {82, 85, 90, 82, 90, 86, 83, 92, 86, 94},
        1,
        2,
        4,
        6,
        8,
        9)
  };
}
