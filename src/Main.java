import java.util.*;

/**
 * Menu interativo para testar as funcionalidades do projeto.
 *
 * Modos de execução:
 *   - Interativo (padrao):       java Main
 *   - Experimentos completos:    java Main --experiments
 *   - Experimentos modo seguro:  java Main --experiments-safe
 *
 * Fluxo interativo:
 *   1. Usuário escolhe o número de vértices (100 / 1.000 / 10.000 / 100.000)
 *   2. Usuário escolhe a densidade do grafo
 *   3. Usuário escolhe o tipo do grafo (euleriano / semi / não / genérico)
 *   4. O grafo é gerado e classificado automaticamente
 *   5. Usuário escolhe qual funcionalidade testar (incluindo Fleury)
 *
 * O modo "--tests" existe apenas para verificação interna do desenvolvedor
 * (276 casos paramétricos garantindo Naive == Tarjan, Fleury válido, etc.)
 * e não aparece no menu nem na ajuda.
 */
public class Main {

    static final Scanner scanner = new Scanner(System.in);

    static int    N          = 0;
    static Graph  grafoAtual = null;
    static String tipoAtual  = "";
    static int    ultimaOpcao = -1;

    public static void main(String[] args) {
        // ---- Modo CLI batch ----
        if (args.length > 0) {
            switch (args[0]) {
                case "--tests"            -> { new TableTests().runAll();                          return; }
                case "--experiments"      -> { new Experiments(true,  3, 42L).runAll();           return; }
                case "--experiments-safe" -> { new Experiments(false, 3, 42L).runAll();           return; }
                case "--help", "-h"       -> { imprimirHelp();                                     return; }
                default -> {
                    System.out.println("Argumento desconhecido: " + args[0]);
                    imprimirHelp();
                    return;
                }
            }
        }

        // ---- Modo interativo ----
        int opcao;
        do {
            int op = menuPrincipal();
            if (op == 0) { System.out.println("Encerrando..."); break; }
            if (op == 8) {
                System.out.println();
                System.out.print("Modo completo (roda Naive em n grande)? (s/n): ");
                boolean completo = scanner.next().trim().equalsIgnoreCase("s");
                System.out.println();
                new Experiments(completo, 3, 42L).runAll();
                continue;
            }

            if (!selecionarGrafo()) break;

            do {
                exibirMenuFuncionalidades();
                opcao = lerInt();
                System.out.println();

                switch (opcao) {
                    case 1  -> testeEstrutura();
                    case 2  -> testeDFSeBFS();
                    case 3  -> testeConectividade();
                    case 4  -> testePontesNaive();
                    case 5  -> testePontesTarjan();
                    case 6  -> testeCompararPontes();
                    case 7  -> testeClassificacaoEuleriana();
                    case 8  -> testeFleury(new NaiveBridgeFinder());
                    case 9  -> testeFleury(new TarjanBridgeFinder());
                    case 10 -> testeFleuryComparativo();
                    case 11 -> {
                        testeEstrutura();
                        testeDFSeBFS();
                        testeConectividade();
                        testePontesTarjan();
                        testeClassificacaoEuleriana();
                        testeFleury(new TarjanBridgeFinder());
                    }
                    case 12 -> { /* voltar */ }
                    case 0  -> { ultimaOpcao = 0; System.out.println("Encerrando..."); }
                    default -> System.out.println("Opcao invalida.\n");
                }
            } while (opcao != 0 && opcao != 12);

        } while (ultimaOpcao != 0);
    }

    // =========================================================
    // MENU PRINCIPAL (antes de gerar grafo)
    // =========================================================

    static int menuPrincipal() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   TP01 - TEORIA DOS GRAFOS                  ║");
        System.out.println("║   Pontes (Naïve / Tarjan) + Fleury          ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║  1. Gerar grafo e explorar funcionalidades   ║");
        System.out.println("║  8. Executar experimentos (tabela de tempos) ║");
        System.out.println("║  0. Sair                                     ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.print("Opcao: ");
        return lerInt();
    }

    static void imprimirHelp() {
        System.out.println();
        System.out.println("Uso: java Main [flag]");
        System.out.println();
        System.out.println("Flags:");
        System.out.println("  (sem flag)            Modo interativo");
        System.out.println("  --experiments         Roda experimentos COMPLETOS (lento em 100k)");
        System.out.println("  --experiments-safe    Roda experimentos pulando Naive em n grande");
        System.out.println("  --help, -h            Mostra esta ajuda");
        System.out.println();
    }

    // =========================================================
    // PASSO 1: TAMANHO + DENSIDADE + TIPO
    // =========================================================

    static boolean selecionarGrafo() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Selecione o numero de vertices     ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1.       100 vertices               ║");
        System.out.println("║  2.     1.000 vertices               ║");
        System.out.println("║  3.    10.000 vertices               ║");
        System.out.println("║  4.   100.000 vertices               ║");
        System.out.println("║  0.   Voltar                         ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Opcao: ");

        int op = lerInt();
        System.out.println();

        N = switch (op) {
            case 1 ->     100;
            case 2 ->   1_000;
            case 3 ->  10_000;
            case 4 -> 100_000;
            case 0 -> { ultimaOpcao = -1; yield 0; }
            default -> { System.out.println("Opcao invalida.\n"); yield 0; }
        };
        if (N == 0) return false;

        // --- Densidade ---
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Selecione a densidade do grafo     ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1. Pre-definida (menu)              ║");
        System.out.println("║  2. Manual (digitar valor 0.0 a 1.0) ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Opcao: ");
        int modoDens = lerInt();
        System.out.println();

        double densidade = (modoDens == 2) ? lerDensidadeManual() : selecionarDensidadeMenu();

        long maxArestas   = (long) N * (N - 1) / 2;
        int  totalArestas = (int) Math.max(N - 1, Math.round(densidade * maxArestas));

        long   estimativaOps = (long) totalArestas * (N + totalArestas);
        double estimativaSeg = estimativaOps / 200_000_000.0;

        System.out.printf("Densidade: %.6f | Arestas estimadas: %d%n", densidade, totalArestas);
        System.out.printf("Estimativa Naive findBridges: %.1f seg (~%.1f min)%n",
            estimativaSeg, estimativaSeg / 60.0);

        if (estimativaSeg > 60) {
            System.out.println("AVISO: Naive pode ser muito lento. Considere densidade menor.");
            System.out.print("Continuar mesmo assim? (s/n): ");
            if (!scanner.next().trim().equalsIgnoreCase("s")) {
                System.out.println("Cancelado.\n");
                return selecionarGrafo();
            }
        }
        System.out.println();

        // --- Tipo do grafo ---
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Selecione o tipo do grafo          ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1. Euleriano                        ║");
        System.out.println("║     (todos graus pares - circuito)   ║");
        System.out.println("║  2. Semi-euleriano                   ║");
        System.out.println("║     (2 graus impares - caminho)      ║");
        System.out.println("║  3. Nao-euleriano                    ║");
        System.out.println("║     (>2 graus impares)               ║");
        System.out.println("║  4. Generico (conexo aleatorio)      ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Opcao: ");
        int tipoOp = lerInt();
        System.out.println();

        int extra = Math.max(4, (totalArestas / 10) % 2 == 0
            ? totalArestas / 10
            : totalArestas / 10 + 1);

        GraphGenerator gen = new GraphGenerator();

        switch (tipoOp) {
            case 1 -> {
                tipoAtual = "EULERIANO";
                System.out.println("Gerando grafo Euleriano com " + N + " vertices...");
                grafoAtual = gen.generateEulerian(N, extra);
            }
            case 2 -> {
                tipoAtual = "SEMI-EULERIANO";
                System.out.println("Gerando grafo Semi-euleriano com " + N + " vertices...");
                grafoAtual = gen.generateSemiEulerian(N, extra);
            }
            case 3 -> {
                tipoAtual = "NAO-EULERIANO";
                System.out.println("Gerando grafo Nao-euleriano com " + N + " vertices...");
                grafoAtual = gen.generateNonEulerian(N, extra);
            }
            default -> {
                tipoAtual = "GENERICO";
                System.out.println("Gerando grafo conexo generico com " + N + " vertices...");
                grafoAtual = gen.generateConnectedRandom(N, totalArestas);
            }
        }

        GraphUtils.EulerianType classificado = GraphUtils.classifyGraph(grafoAtual);
        int impares = grafoAtual.getOddDegreeVertices().size();

        System.out.printf("Grafo gerado: %d vertices | %d arestas | %d vertices de grau impar%n",
            N, grafoAtual.getNumEdges(), impares);
        System.out.printf("Tipo informado: %-15s | Classificacao real: %s%n%n",
            tipoAtual, classificado);

        return true;
    }

    // =========================================================
    // MENU DE FUNCIONALIDADES
    // =========================================================

    static void exibirMenuFuncionalidades() {
        GraphUtils.EulerianType tipo = GraphUtils.classifyGraph(grafoAtual);
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.printf ("║  Vertices: %-6d  Arestas: %-10d     ║%n",
            N, grafoAtual.getNumEdges());
        System.out.printf ("║  Tipo: %-38s║%n", tipoAtual + " (" + tipo + ")");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║  1. Estrutura do grafo                       ║");
        System.out.println("║  2. DFS e BFS                                ║");
        System.out.println("║  3. Verificacao de conectividade             ║");
        System.out.println("║  4. Pontes - Metodo Naive                    ║");
        System.out.println("║  5. Pontes - Metodo Tarjan                   ║");
        System.out.println("║  6. Comparar pontes (Naive vs Tarjan)        ║");
        System.out.println("║  7. Classificacao euleriana                  ║");
        System.out.println("║  8. Caminho Euleriano (Fleury + Naive)       ║");
        System.out.println("║  9. Caminho Euleriano (Fleury + Tarjan)      ║");
        System.out.println("║ 10. Comparar Fleury (Naive vs Tarjan)        ║");
        System.out.println("║ 11. Todos os testes (rapidos)                ║");
        System.out.println("║ 12. Voltar (novo grafo)                      ║");
        System.out.println("║  0. Sair                                     ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.print("Opcao: ");
    }

    // =========================================================
    // TESTES
    // =========================================================

    static void testeEstrutura() {
        System.out.println("=== 1. Estrutura do Grafo (n=" + N + ") ===");
        Graph g = copiar(grafoAtual);

        if (N <= 20) {
            System.out.println(g);
        } else {
            System.out.println("Graph (" + N + " vertices, " + g.getNumEdges() + " arestas)");
            System.out.println("(lista de adjacencia omitida para n > 20)");
        }

        int u = 0, v = g.getNeighbors(0).get(0), grauAntes = g.degree(u);

        checar("Vertice 0 tem grau > 0",                g.degree(u) > 0,                          true);
        checar("Aresta (0," + v + ") existe",            g.hasEdge(u, v),                          true);
        checar("getNumEdges == getAllEdges.size",         g.getNumEdges() == g.getAllEdges().size(), true);

        g.removeEdge(u, v);
        checar("Apos remover (0," + v + "): nao existe", g.hasEdge(u, v),              false);
        checar("Grau de 0 diminuiu em 1",                  g.degree(u) == grauAntes - 1,  true);
        g.addEdge(u, v);
        checar("Apos reinserir (0," + v + "): existe",     g.hasEdge(u, v),              true);
        checar("Grau de 0 restaurado",                     g.degree(u) == grauAntes,      true);
        System.out.println();
    }

    static void testeDFSeBFS() {
        System.out.println("=== 2. DFS e BFS (n=" + N + ") ===");

        long t0 = System.nanoTime();
        Set<Integer> dfs = GraphUtils.dfs(grafoAtual, 0);
        long tempoDFS = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        Set<Integer> bfs = GraphUtils.bfs(grafoAtual, 0);
        long tempoBFS = System.nanoTime() - t1;

        checar("DFS alcanca todos os " + N + " vertices", dfs.size() == N, true);
        checar("BFS alcanca todos os " + N + " vertices", bfs.size() == N, true);
        checar("DFS e BFS alcancam os mesmos vertices",   dfs.equals(bfs), true);
        System.out.printf("  Tempo DFS: %.3f ms%n", tempoDFS / 1_000_000.0);
        System.out.printf("  Tempo BFS: %.3f ms%n", tempoBFS / 1_000_000.0);
        System.out.println();
    }

    static void testeConectividade() {
        System.out.println("=== 3. Verificacao de Conectividade (n=" + N + ") ===");

        long t0 = System.nanoTime();
        boolean conexo = GraphUtils.isConnected(grafoAtual);
        long tempo = System.nanoTime() - t0;

        checar("Grafo gerado e conexo", conexo, true);
        System.out.printf("  Tempo isConnected: %.3f ms%n", tempo / 1_000_000.0);

        Graph desconexo = new Graph(N + 2);
        for (int[] e : grafoAtual.getAllEdges()) desconexo.addEdge(e[0], e[1]);
        desconexo.addEdge(N, N + 1);
        checar("Grafo com componente extra e desconexo", GraphUtils.isConnected(desconexo), false);
        System.out.println();
    }

    static void testePontesNaive() {
        System.out.println("=== 4. Pontes - Metodo Naive (n=" + N + ") ===");

        if (N > 1_000) {
            System.out.print("  AVISO: Naive em n=" + N + " pode ser lento. Continuar? (s/n): ");
            if (!scanner.next().trim().equalsIgnoreCase("s")) {
                System.out.println("  Teste cancelado.\n");
                return;
            }
            System.out.println();
        }

        NaiveBridgeFinder naive = new NaiveBridgeFinder();

        Graph fixo = new Graph(6);
        fixo.addEdge(0,1); fixo.addEdge(1,2); fixo.addEdge(2,4);
        fixo.addEdge(4,3); fixo.addEdge(3,0); fixo.addEdge(3,5);

        List<int[]> pontesFixo = naive.findBridges(fixo);
        boolean tem35 = pontesFixo.stream()
            .anyMatch(p -> (p[0]==3&&p[1]==5)||(p[0]==5&&p[1]==3));

        checar("Grafo fixo: 1 ponte encontrada",  pontesFixo.size() == 1,      true);
        checar("Grafo fixo: ponte e (3,5)",        tem35,                       true);
        checar("isBridge(3,5) == true",            naive.isBridge(fixo, 3, 5), true);
        checar("isBridge(0,1) == false",           naive.isBridge(fixo, 0, 1), false);

        System.out.println("  Executando Naive no grafo atual (" + tipoAtual + ")...");
        Graph g = copiar(grafoAtual);
        long t0 = System.nanoTime();
        List<int[]> pontes = naive.findBridges(g);
        long tempo = System.nanoTime() - t0;

        System.out.println("  Pontes encontradas: " + pontes.size());
        System.out.printf("  Tempo Naive (n=%d): %.3f ms%n", N, tempo / 1_000_000.0);
        System.out.println();
    }

    static void testePontesTarjan() {
        System.out.println("=== 5. Pontes - Metodo Tarjan (n=" + N + ") ===");

        TarjanBridgeFinder tarjan = new TarjanBridgeFinder();

        Graph fixo = new Graph(6);
        fixo.addEdge(0,1); fixo.addEdge(1,2); fixo.addEdge(2,4);
        fixo.addEdge(4,3); fixo.addEdge(3,0); fixo.addEdge(3,5);

        List<int[]> pontesFixo = tarjan.findBridges(fixo);
        boolean tem35 = pontesFixo.stream()
            .anyMatch(p -> (p[0]==3&&p[1]==5)||(p[0]==5&&p[1]==3));

        checar("Grafo fixo: 1 ponte encontrada",  pontesFixo.size() == 1,       true);
        checar("Grafo fixo: ponte e (3,5)",        tem35,                        true);
        checar("isBridge(3,5) == true",            tarjan.isBridge(fixo, 3, 5), true);
        checar("isBridge(0,1) == false",           tarjan.isBridge(fixo, 0, 1), false);

        System.out.println("  Executando Tarjan no grafo atual (" + tipoAtual + ")...");
        Graph g = copiar(grafoAtual);
        long t0 = System.nanoTime();
        List<int[]> pontes = tarjan.findBridges(g);
        long tempo = System.nanoTime() - t0;

        System.out.println("  Pontes encontradas: " + pontes.size());
        System.out.printf("  Tempo Tarjan (n=%d): %.3f ms%n", N, tempo / 1_000_000.0);
        System.out.println();
    }

    static void testeCompararPontes() {
        System.out.println("=== 6. Comparar Pontes - Naive vs Tarjan (n=" + N + ") ===");

        boolean rodarNaive = (N <= 1_000);
        if (!rodarNaive) {
            System.out.print("  AVISO: Naive em n=" + N + " pode ser lento. Continuar? (s/n): ");
            rodarNaive = scanner.next().trim().equalsIgnoreCase("s");
            System.out.println();
        }

        Graph g = copiar(grafoAtual);

        long t1 = System.nanoTime();
        List<int[]> pontesT = new TarjanBridgeFinder().findBridges(g);
        long tT = System.nanoTime() - t1;

        long tN = -1;
        List<int[]> pontesN = null;
        if (rodarNaive) {
            long t0 = System.nanoTime();
            pontesN = new NaiveBridgeFinder().findBridges(g);
            tN = System.nanoTime() - t0;
        }

        System.out.println("  +---------+--------+--------------+");
        System.out.println("  | Metodo  | Pontes |  Tempo (ms)  |");
        System.out.println("  +---------+--------+--------------+");
        if (rodarNaive) {
            System.out.printf("  | Naive   | %6d | %12.3f |%n", pontesN.size(), tN / 1_000_000.0);
        } else {
            System.out.println("  | Naive   |   --   |     SKIP     |");
        }
        System.out.printf  ("  | Tarjan  | %6d | %12.3f |%n", pontesT.size(), tT / 1_000_000.0);
        System.out.println("  +---------+--------+--------------+");

        if (rodarNaive) {
            Set<Long> sN = new HashSet<>();
            for (int[] p : pontesN) sN.add(GraphUtils.edgeKey(p[0], p[1]));
            Set<Long> sT = new HashSet<>();
            for (int[] p : pontesT) sT.add(GraphUtils.edgeKey(p[0], p[1]));
            checar("Mesmos conjuntos de pontes (Naive == Tarjan)", sN.equals(sT), true);
            if (tN > 0) {
                System.out.printf("  Speedup Tarjan: %.1fx%n", (double) tN / tT);
            }
        }
        System.out.println();
    }

    static void testeClassificacaoEuleriana() {
        System.out.println("=== 7. Classificacao Euleriana (n=" + N + ") ===");

        GraphUtils.EulerianType tipoReal = GraphUtils.classifyGraph(grafoAtual);
        int impares = grafoAtual.getOddDegreeVertices().size();

        System.out.println("  Tipo informado na geracao : " + tipoAtual);
        System.out.println("  Classificacao pelo algoritmo: " + tipoReal);
        System.out.println("  Vertices de grau impar    : " + impares);
        System.out.println("  Arestas totais            : " + grafoAtual.getNumEdges());
        System.out.println();

        switch (tipoReal) {
            case EULERIAN ->
                System.out.println("  => Existe CIRCUITO euleriano (todos os graus sao pares).");
            case SEMI_EULERIAN ->
                System.out.println("  => Existe CAMINHO euleriano (exatamente 2 vertices de grau impar).");
            case NON_EULERIAN ->
                System.out.println("  => NAO existe caminho nem circuito euleriano (" + impares + " vertices de grau impar).");
        }

        boolean bate = switch (tipoAtual) {
            case "EULERIANO"      -> tipoReal == GraphUtils.EulerianType.EULERIAN;
            case "SEMI-EULERIANO" -> tipoReal == GraphUtils.EulerianType.SEMI_EULERIAN;
            case "NAO-EULERIANO"  -> tipoReal == GraphUtils.EulerianType.NON_EULERIAN;
            default               -> true;
        };

        System.out.println();
        checar("Classificacao confere com o tipo gerado", bate, true);
        System.out.println();
    }

    // -------------------------------------------------------
    // 8/9. Fleury com cada estratégia
    // -------------------------------------------------------
    static void testeFleury(BridgeFinder bf) {
        String estrat = bf.getName();
        System.out.println("=== Fleury - " + estrat + " (n=" + N + ") ===");

        // Aviso de tempo para Naive em n grande
        boolean isNaive = bf instanceof NaiveBridgeFinder;
        if (isNaive && N > 1_000) {
            System.out.print("  AVISO: Fleury+Naive em n=" + N + " pode ser MUITO lento. Continuar? (s/n): ");
            if (!scanner.next().trim().equalsIgnoreCase("s")) {
                System.out.println("  Teste cancelado.\n");
                return;
            }
            System.out.println();
        }

        FleuryEulerPath fleury = new FleuryEulerPath(bf);
        FleuryEulerPath.Result r = fleury.findPath(grafoAtual);

        System.out.println("  " + r.summary());

        if (r.exists) {
            checar("Caminho percorre TODAS as arestas",
                r.edgesTraversed == r.edgesTotal, true);

            if (r.type == GraphUtils.EulerianType.EULERIAN) {
                checar("Eulerian: caminho e circuito (inicio == fim)",
                    r.path.get(0).equals(r.path.get(r.path.size()-1)), true);
            } else if (r.type == GraphUtils.EulerianType.SEMI_EULERIAN) {
                int inicio = r.path.get(0);
                int fim    = r.path.get(r.path.size()-1);
                List<Integer> impares = grafoAtual.getOddDegreeVertices();
                checar("Semi: inicio em vertice impar", impares.contains(inicio), true);
                checar("Semi: fim    em vertice impar", impares.contains(fim),    true);
                checar("Semi: inicio != fim",            inicio != fim,            true);
            }

            // Para n pequeno, mostra o caminho
            if (N <= 30) {
                System.out.println("  Caminho: " + r.path);
            } else {
                System.out.printf("  Caminho: %d vertices (omitido p/ n>30)%n", r.path.size());
            }
        } else {
            checar("NAO-Euleriano detectado corretamente",
                r.type == GraphUtils.EulerianType.NON_EULERIAN, true);
        }
        System.out.println();
    }

    static void testeFleuryComparativo() {
        System.out.println("=== 10. Comparar Fleury - Naive vs Tarjan (n=" + N + ") ===");

        boolean rodarNaive = (N <= 1_000);
        if (!rodarNaive) {
            System.out.print("  AVISO: Fleury+Naive em n=" + N + " pode ser MUITO lento. Continuar? (s/n): ");
            rodarNaive = scanner.next().trim().equalsIgnoreCase("s");
            System.out.println();
        }

        FleuryEulerPath.Result rT = new FleuryEulerPath(new TarjanBridgeFinder()).findPath(grafoAtual);
        FleuryEulerPath.Result rN = rodarNaive
            ? new FleuryEulerPath(new NaiveBridgeFinder()).findPath(grafoAtual)
            : null;

        System.out.println("  +-------------+----------+--------------+--------------+");
        System.out.println("  | Estrategia  | Existe?  | Arestas Perc |  Tempo (ms)  |");
        System.out.println("  +-------------+----------+--------------+--------------+");
        if (rN != null) {
            System.out.printf("  | Naive       | %-8s | %12d | %12.3f |%n",
                rN.exists, rN.edgesTraversed, rN.elapsedMillis());
        } else {
            System.out.println("  | Naive       |   SKIP   |      --      |     SKIP     |");
        }
        System.out.printf  ("  | Tarjan      | %-8s | %12d | %12.3f |%n",
            rT.exists, rT.edgesTraversed, rT.elapsedMillis());
        System.out.println("  +-------------+----------+--------------+--------------+");

        if (rN != null && rT.elapsedNanos > 0) {
            System.out.printf("  Speedup Tarjan: %.1fx%n", (double) rN.elapsedNanos / rT.elapsedNanos);
        }

        // Validação cruzada
        if (rN != null) {
            checar("Mesmo numero de arestas percorridas",
                rN.edgesTraversed, rT.edgesTraversed);
            checar("Mesmo veredicto (existe/nao existe)",
                rN.exists == rT.exists, true);
        }
        System.out.println();
    }

    // =========================================================
    // DENSIDADE
    // =========================================================

    static double selecionarDensidadeMenu() {
        long max = (long) N * (N - 1) / 2;
        long e1 = Math.max(N - 1, Math.round(0.000010 * max));
        long e2 = Math.max(N - 1, Math.round(0.000100 * max));
        long e3 = Math.max(N - 1, Math.round(0.001000 * max));
        long e4 = Math.max(N - 1, Math.round(0.010000 * max));

        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   Densidades pre-definidas                 ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.printf ("║  1. 0.000010  (~%10d arestas)         ║%n", e1);
        System.out.printf ("║  2. 0.000100  (~%10d arestas)         ║%n", e2);
        System.out.printf ("║  3. 0.001000  (~%10d arestas)         ║%n", e3);
        System.out.printf ("║  4. 0.010000  (~%10d arestas)         ║%n", e4);
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.print("Opcao: ");
        int op = lerInt();
        System.out.println();
        return switch (op) {
            case 1  -> 0.000010;
            case 2  -> 0.000100;
            case 3  -> 0.001000;
            case 4  -> 0.010000;
            default -> { System.out.println("Opcao invalida. Usando 0.000010."); yield 0.000010; }
        };
    }

    static double lerDensidadeManual() {
        double d = -1;
        while (d < 0 || d > 1) {
            System.out.print("Digite a densidade (ex: 0.000010): ");
            try {
                d = Double.parseDouble(scanner.next().trim().replace(",", "."));
                if (d < 0 || d > 1) { System.out.println("Fora do intervalo [0,1]."); d = -1; }
            } catch (NumberFormatException e) {
                System.out.println("Formato invalido. Use ponto decimal.");
            }
        }
        System.out.println();
        return d;
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    static Graph copiar(Graph g) { return GraphUtils.copyGraph(g); }

    static <T> void checar(String desc, T obtido, T esperado) {
        boolean ok = Objects.equals(obtido, esperado);
        System.out.println((ok ? "  [PASS] " : "  [FAIL] esperado=" + esperado + " obtido=" + obtido + " | ") + desc);
    }

    static void checar(String desc, boolean obtido, boolean esperado) {
        System.out.println((obtido == esperado ? "  [PASS] " : "  [FAIL] esperado="
            + esperado + " obtido=" + obtido + " | ") + desc);
    }

    static int lerInt() {
        try { return Integer.parseInt(scanner.next().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}
