import java.util.*;
import java.io.*;

/**
 * Bateria de experimentos exigida pelo enunciado.
 *
 * Mede tempo médio para encontrar pontes (e caminho Euleriano via Fleury)
 * com cada estratégia (Naïve vs Tarjan) em grafos aleatórios de tamanhos
 * 100, 1.000, 10.000 e 100.000 vértices, nos 3 tipos eulerianos.
 *
 * Resultados:
 *   - Imprime tabela formatada no console
 *   - Salva CSV em "resultados/experimentos.csv" para uso no relatório
 *
 * Estratégia anti-timeout:
 *   - Naïve em n >= 10.000 é proibitivo (O(E²)) → pulado por padrão
 *   - Pode ser forçado com flag "completo"
 *   - Fleury com Naïve só roda em n <= 1.000 por padrão
 */
public class Experiments {

    public static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    public static final String[] TYPES = {"EULERIANO", "SEMI", "NAO"};
    public static final int DEFAULT_REPS = 3;

    /**
     * Teto de tempo (segundos) para abortar configurações inviáveis.
     * Cada execução individual respeita esse limite (estimado).
     */
    private static final int NAIVE_BRIDGES_MAX_N    = 1_000;   // até aqui é viável (~5s)
    private static final int NAIVE_FLEURY_MAX_N     = 1_000;   // Fleury+Naive: O(E^3)
    private static final int TARJAN_FLEURY_MAX_N    = 10_000;  // Fleury+Tarjan: ~22s/run em 10k, ~30min em 100k

    private final boolean modoCompleto;
    private final int repeticoes;
    private final long seed;

    public Experiments() {
        this(false, DEFAULT_REPS, 42L);
    }

    public Experiments(boolean modoCompleto, int repeticoes, long seed) {
        this.modoCompleto = modoCompleto;
        this.repeticoes = repeticoes;
        this.seed = seed;
    }

    // =====================================================================
    // ENTRADA PRINCIPAL
    // =====================================================================

    public void runAll() {
        System.out.println();
        System.out.println("========================================================");
        System.out.println("  EXPERIMENTOS - TP01 Teoria dos Grafos");
        System.out.println("========================================================");
        System.out.printf ("  Modo: %s | Repeticoes: %d | Seed: %d%n",
            modoCompleto ? "COMPLETO" : "SEGURO (pula naive em n grande)",
            repeticoes, seed);
        System.out.println("========================================================");
        System.out.println();

        List<Resultado> resultados = new ArrayList<>();

        // ---- Bloco A: Tempo médio de findBridges (Naive vs Tarjan) ----
        System.out.println(">>> BLOCO A: Tempo medio de findBridges (deteccao de pontes)");
        System.out.println();
        for (int n : SIZES) {
            for (String tipo : TYPES) {
                resultados.addAll(rodarBridges(n, tipo));
            }
        }
        imprimirTabelaBridges(resultados);

        // ---- Bloco B: Tempo médio de Fleury (com cada estratégia) ----
        System.out.println();
        System.out.println(">>> BLOCO B: Tempo medio de Fleury (caminho Euleriano completo)");
        System.out.println();
        List<Resultado> resFleury = new ArrayList<>();
        for (int n : SIZES) {
            for (String tipo : TYPES) {
                resFleury.addAll(rodarFleury(n, tipo));
            }
        }
        imprimirTabelaFleury(resFleury);

        // ---- Persistência em CSV ----
        try {
            salvarCSV(resultados, resFleury);
            System.out.println();
            System.out.println("CSV salvo em: resultados/experimentos.csv");
        } catch (IOException e) {
            System.out.println("Aviso: falha ao salvar CSV: " + e.getMessage());
        }

        System.out.println();
        System.out.println("========================================================");
        System.out.println("  Experimentos finalizados.");
        System.out.println("========================================================");
        System.out.println();
    }

    // =====================================================================
    // BLOCO A — findBridges (Naïve vs Tarjan)
    // =====================================================================

    private List<Resultado> rodarBridges(int n, String tipo) {
        List<Resultado> out = new ArrayList<>();
        System.out.printf("  [n=%-7d tipo=%-5s] gerando grafo...%n", n, tipo);

        Graph g = gerarGrafo(n, tipo);
        int arestas = g.getNumEdges();

        // --- Naïve (com guarda de tempo) ---
        boolean rodarNaive = modoCompleto || n <= NAIVE_BRIDGES_MAX_N;
        if (rodarNaive) {
            double mediaMs = medirMedio("Naive findBridges",
                () -> new NaiveBridgeFinder().findBridges(g),
                repeticoes);
            out.add(new Resultado("findBridges", "Naive", n, tipo, arestas, mediaMs, true));
        } else {
            out.add(new Resultado("findBridges", "Naive", n, tipo, arestas, -1, false));
            System.out.printf("    Naive ............ PULADO (n > %d)%n", NAIVE_BRIDGES_MAX_N);
        }

        // --- Tarjan ---
        double mediaTarjan = medirMedio("Tarjan findBridges",
            () -> new TarjanBridgeFinder().findBridges(g),
            repeticoes);
        out.add(new Resultado("findBridges", "Tarjan", n, tipo, arestas, mediaTarjan, true));

        // Validação cruzada (Naive == Tarjan) já é feita pelos table-tests;
        // não rerunamos aqui para evitar duplicar custo de medição.
        System.out.println();
        return out;
    }

    // =====================================================================
    // BLOCO B — Fleury completo (com cada estratégia)
    // =====================================================================

    private List<Resultado> rodarFleury(int n, String tipo) {
        List<Resultado> out = new ArrayList<>();
        System.out.printf("  [n=%-7d tipo=%-5s] Fleury...%n", n, tipo);

        Graph g = gerarGrafo(n, tipo);
        int arestas = g.getNumEdges();

        // Fleury + Naïve
        boolean rodarNaive = modoCompleto || n <= NAIVE_FLEURY_MAX_N;
        if (rodarNaive) {
            double mediaMs = medirMedio("Fleury+Naive",
                () -> new FleuryEulerPath(new NaiveBridgeFinder()).findPath(g),
                repeticoes);
            out.add(new Resultado("Fleury", "Naive", n, tipo, arestas, mediaMs, true));
        } else {
            out.add(new Resultado("Fleury", "Naive", n, tipo, arestas, -1, false));
            System.out.printf("    Fleury+Naive ..... PULADO (n > %d)%n", NAIVE_FLEURY_MAX_N);
        }

        // Fleury + Tarjan
        boolean rodarTarjan = modoCompleto || n <= TARJAN_FLEURY_MAX_N;
        if (rodarTarjan) {
            double mediaMs = medirMedio("Fleury+Tarjan",
                () -> new FleuryEulerPath(new TarjanBridgeFinder()).findPath(g),
                repeticoes);
            out.add(new Resultado("Fleury", "Tarjan", n, tipo, arestas, mediaMs, true));
        } else {
            out.add(new Resultado("Fleury", "Tarjan", n, tipo, arestas, -1, false));
            System.out.printf("    Fleury+Tarjan .... PULADO (n > %d)%n", TARJAN_FLEURY_MAX_N);
        }

        System.out.println();
        return out;
    }

    // =====================================================================
    // GERAÇÃO DE GRAFOS
    // =====================================================================

    private Graph gerarGrafo(int n, String tipo) {
        GraphGenerator gen = new GraphGenerator(seed + n + tipo.hashCode());
        // Usa densidade baixa para não saturar Naive em n grande
        int extra = Math.max(4, n / 50);
        if (extra % 2 != 0) extra++;
        return switch (tipo) {
            case "EULERIANO" -> gen.generateEulerian(n, extra);
            case "SEMI"      -> gen.generateSemiEulerian(n, extra);
            case "NAO"       -> gen.generateNonEulerian(n, extra);
            default          -> gen.generateConnectedRandom(n, n + extra);
        };
    }

    // =====================================================================
    // MEDIÇÃO DE TEMPO MÉDIO
    // =====================================================================

    /**
     * Roda a tarefa N vezes e retorna a média (ms).
     * Imprime cada execução para acompanhamento.
     */
    private double medirMedio(String label, Runnable tarefa, int reps) {
        double soma = 0;
        for (int i = 1; i <= reps; i++) {
            long t0 = System.nanoTime();
            tarefa.run();
            long t1 = System.nanoTime();
            double ms = (t1 - t0) / 1_000_000.0;
            soma += ms;
            System.out.printf("    %-20s  rep %d/%d : %12.3f ms%n", label, i, reps, ms);
        }
        double media = soma / reps;
        System.out.printf("    %-20s  MEDIA      : %12.3f ms%n", label, media);
        return media;
    }

    // =====================================================================
    // FORMATAÇÃO DE TABELAS
    // =====================================================================

    private void imprimirTabelaBridges(List<Resultado> resultados) {
        System.out.println();
        System.out.println("  TABELA A: Tempo medio findBridges (ms)");
        System.out.println("  +-----------+----------+----------+--------------+--------------+");
        System.out.println("  |     N     |  Tipo    | Arestas  |   NAIVE (ms) |  TARJAN (ms) |");
        System.out.println("  +-----------+----------+----------+--------------+--------------+");

        for (int n : SIZES) {
            for (String tipo : TYPES) {
                Resultado naive  = encontrar(resultados, "findBridges", "Naive",  n, tipo);
                Resultado tarjan = encontrar(resultados, "findBridges", "Tarjan", n, tipo);
                String sN = naive  == null || !naive.executado  ? "    --     " : String.format("%12.3f", naive.tempoMs);
                String sT = tarjan == null || !tarjan.executado ? "    --     " : String.format("%12.3f", tarjan.tempoMs);
                int arestas = (tarjan != null) ? tarjan.arestas : (naive != null ? naive.arestas : 0);
                System.out.printf("  | %9d | %-8s | %8d | %s | %s |%n",
                    n, tipo, arestas, sN, sT);
            }
        }
        System.out.println("  +-----------+----------+----------+--------------+--------------+");
    }

    private void imprimirTabelaFleury(List<Resultado> resultados) {
        System.out.println();
        System.out.println("  TABELA B: Tempo medio Fleury (ms)");
        System.out.println("  +-----------+----------+----------+--------------+--------------+");
        System.out.println("  |     N     |  Tipo    | Arestas  | FLEURY+NAIVE | FLEURY+TARJ. |");
        System.out.println("  +-----------+----------+----------+--------------+--------------+");

        for (int n : SIZES) {
            for (String tipo : TYPES) {
                Resultado naive  = encontrar(resultados, "Fleury", "Naive",  n, tipo);
                Resultado tarjan = encontrar(resultados, "Fleury", "Tarjan", n, tipo);
                String sN = naive  == null || !naive.executado  ? "    --     " : String.format("%12.3f", naive.tempoMs);
                String sT = tarjan == null || !tarjan.executado ? "    --     " : String.format("%12.3f", tarjan.tempoMs);
                int arestas = (tarjan != null) ? tarjan.arestas : (naive != null ? naive.arestas : 0);
                System.out.printf("  | %9d | %-8s | %8d | %s | %s |%n",
                    n, tipo, arestas, sN, sT);
            }
        }
        System.out.println("  +-----------+----------+----------+--------------+--------------+");
    }

    private Resultado encontrar(List<Resultado> lista, String alg, String estrat, int n, String tipo) {
        for (Resultado r : lista) {
            if (r.algoritmo.equals(alg) && r.estrategia.equals(estrat)
                && r.n == n && r.tipo.equals(tipo)) return r;
        }
        return null;
    }

    // =====================================================================
    // PERSISTÊNCIA EM CSV
    // =====================================================================

    private void salvarCSV(List<Resultado> bridges, List<Resultado> fleury) throws IOException {
        File dir = new File("resultados");
        if (!dir.exists()) dir.mkdirs();

        File arq = new File(dir, "experimentos.csv");
        try (PrintWriter w = new PrintWriter(new FileWriter(arq))) {
            w.println("algoritmo,estrategia,n,tipo,arestas,tempo_medio_ms,executado");
            List<Resultado> todos = new ArrayList<>();
            todos.addAll(bridges);
            todos.addAll(fleury);
            for (Resultado r : todos) {
                w.printf(Locale.US, "%s,%s,%d,%s,%d,%.6f,%s%n",
                    r.algoritmo, r.estrategia, r.n, r.tipo,
                    r.arestas, r.tempoMs, r.executado);
            }
        }
    }

    // =====================================================================
    // DADO INTERNO
    // =====================================================================

    static class Resultado {
        final String algoritmo;
        final String estrategia;
        final int n;
        final String tipo;
        final int arestas;
        final double tempoMs;
        final boolean executado;

        Resultado(String algoritmo, String estrategia, int n, String tipo,
                  int arestas, double tempoMs, boolean executado) {
            this.algoritmo = algoritmo;
            this.estrategia = estrategia;
            this.n = n;
            this.tipo = tipo;
            this.arestas = arestas;
            this.tempoMs = tempoMs;
            this.executado = executado;
        }
    }
}
