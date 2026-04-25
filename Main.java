import java.util.*;

/**
 * Menu interativo para testar as funcionalidades do projeto.
 *
 * Fluxo:
 *   1. Usuário escolhe o número de vértices (100 / 1.000 / 10.000 / 100.000)
 *   2. Um grafo aleatório conexo é gerado com esse tamanho
 *   3. Usuário escolhe qual funcionalidade testar sobre esse grafo
 */
public class Main {

    static final Scanner scanner = new Scanner(System.in);

    // Grafo compartilhado entre os testes — gerado na seleção de tamanho
    static int    N = 0;
    static Graph  grafoAtual = null;

    public static void main(String[] args) {
        int opcao;
        do {
            // Passo 1: sempre pede o tamanho antes de qualquer teste
            if (!selecionarTamanho()) break;

            // Passo 2: menu de funcionalidades
            do {
                exibirMenuFuncionalidades();
                opcao = lerInt();
                System.out.println();

                switch (opcao) {
                    case 1 -> testeEstrutura();
                    case 2 -> testeDFSeBFS();
                    case 3 -> testeConectividade();
                    case 4 -> testePontesNaive();
                    case 5 -> testeGeracao();
                    case 6 -> {
                        testeEstrutura();
                        testeDFSeBFS();
                        testeConectividade();
                        testePontesNaive();
                        testeGeracao();
                    }
                    case 7 -> { /* voltar ao menu de tamanho */ }
                    case 0 -> System.out.println("Encerrando...");
                    default -> System.out.println("Opcao invalida.\n");
                }
            } while (opcao != 0 && opcao != 7);

        } while (opcao() != 0);
    }

    // Variável auxiliar para saber se usuário pediu sair
    static int ultimaOpcao = -1;
    static int opcao() { return ultimaOpcao; }

    // =========================================================
    // SELEÇÃO DE TAMANHO
    // =========================================================

    /**
     * Exibe o menu de tamanhos e gera o grafo base.
     * Retorna false se o usuário escolher sair.
     */
    static boolean selecionarTamanho() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Selecione o numero de vertices     ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1.       100 vertices               ║");
        System.out.println("║  2.     1.000 vertices               ║");
        System.out.println("║  3.    10.000 vertices               ║");
        System.out.println("║  4.   100.000 vertices               ║");
        System.out.println("║  0.   Sair                           ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Opcao: ");

        int opcao = lerInt();
        System.out.println();

        N = switch (opcao) {
            case 1 ->     100;
            case 2 ->   1_000;
            case 3 ->  10_000;
            case 4 -> 100_000;
            case 0 -> { ultimaOpcao = 0; yield 0; }
            default -> { System.out.println("Opcao invalida.\n"); yield 0; }
        };

        if (N == 0) return false;

        System.out.println("Gerando grafo conexo com " + N + " vertices...");
        GraphGenerator gen = new GraphGenerator();
        int extra = Math.max(2, N / 5);
        grafoAtual = gen.generateConnectedRandom(N, N + extra);
        System.out.println("Grafo gerado: " + N + " vertices, " + grafoAtual.getNumEdges() + " arestas.\n");

        return true;
    }

    static void exibirMenuFuncionalidades() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.printf ("║  Grafo atual: %6d vertices        ║%n", N);
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1. Estrutura do grafo               ║");
        System.out.println("║  2. DFS e BFS                        ║");
        System.out.println("║  3. Verificacao de conectividade     ║");
        System.out.println("║  4. Pontes - Metodo Naive            ║");
        System.out.println("║  5. Geracao de grafos (3 tipos)      ║");
        System.out.println("║  6. Todos os testes                  ║");
        System.out.println("║  7. Voltar (trocar tamanho)          ║");
        System.out.println("║  0. Sair                             ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Opcao: ");
    }

    // =========================================================
    // TESTES — todos usam N e grafoAtual
    // =========================================================

    // -------------------------------------------------------
    // 1. Estrutura do grafo
    // -------------------------------------------------------
    static void testeEstrutura() {
        System.out.println("=== 1. Estrutura do Grafo (n=" + N + ") ===");

        // Usa o grafoAtual gerado com N vertices
        Graph g = copiar(grafoAtual);

        if (N <= 20) {
            System.out.println(g);
        } else {
            System.out.println("Graph (" + N + " vertices, " + g.getNumEdges() + " arestas)");
            System.out.println("(lista de adjacencia omitida para n > 20)");
        }

        int u = 0;
        int v = g.getNeighbors(0).get(0);
        int grauAntes = g.degree(u);

        checar("Vertice 0 tem grau > 0",               g.degree(u) > 0,                         true);
        checar("Aresta (0," + v + ") existe",           g.hasEdge(u, v),                         true);
        checar("getNumEdges == getAllEdges.size",        g.getNumEdges() == g.getAllEdges().size(), true);

        g.removeEdge(u, v);
        checar("Apos remover (0," + v + "): nao existe", g.hasEdge(u, v),                        false);
        checar("Grau de 0 diminuiu em 1",               g.degree(u) == grauAntes - 1,             true);
        g.addEdge(u, v);
        checar("Apos reinserir (0," + v + "): existe",  g.hasEdge(u, v),                         true);
        checar("Grau de 0 restaurado",                  g.degree(u) == grauAntes,                 true);

        System.out.println();
    }

    // -------------------------------------------------------
    // 2. DFS e BFS
    // -------------------------------------------------------
    static void testeDFSeBFS() {
        System.out.println("=== 2. DFS e BFS (n=" + N + ") ===");

        Graph g = copiar(grafoAtual);

        long t0 = System.nanoTime();
        Set<Integer> dfs = GraphUtils.dfs(g, 0);
        long tempoDFS = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        Set<Integer> bfs = GraphUtils.bfs(g, 0);
        long tempoBFS = System.nanoTime() - t1;

        // Em grafo conexo, DFS e BFS a partir de 0 devem alcançar todos os vertices
        checar("DFS alcanca todos os " + N + " vertices",  dfs.size() == N, true);
        checar("BFS alcanca todos os " + N + " vertices",  bfs.size() == N, true);
        checar("DFS e BFS alcancam os mesmos vertices",    dfs.equals(bfs), true);

        System.out.printf("  Tempo DFS: %.3f ms%n", tempoDFS / 1_000_000.0);
        System.out.printf("  Tempo BFS: %.3f ms%n", tempoBFS / 1_000_000.0);

        System.out.println();
    }

    // -------------------------------------------------------
    // 3. Conectividade
    // -------------------------------------------------------
    static void testeConectividade() {
        System.out.println("=== 3. Verificacao de Conectividade (n=" + N + ") ===");

        // grafoAtual e conexo por construcao
        long t0 = System.nanoTime();
        boolean conexo = GraphUtils.isConnected(grafoAtual);
        long tempo = System.nanoTime() - t0;

        checar("Grafo gerado e conexo", conexo, true);
        System.out.printf("  Tempo isConnected: %.3f ms%n", tempo / 1_000_000.0);

        // Cria grafo desconexo adicionando componente separada
        Graph desconexo = new Graph(N + 2);
        for (int[] e : grafoAtual.getAllEdges()) desconexo.addEdge(e[0], e[1]);
        desconexo.addEdge(N, N + 1); // componente isolada
        checar("Grafo com componente extra e desconexo", GraphUtils.isConnected(desconexo), false);

        System.out.println();
    }

    // -------------------------------------------------------
    // 4. Pontes - Método Naïve
    // -------------------------------------------------------
    static void testePontesNaive() {
        System.out.println("=== 4. Pontes - Metodo Naive (n=" + N + ") ===");

        if (N > 1_000) {
            System.out.println("  AVISO: Naive em n=" + N + " pode ser muito lento (O(E*(V+E))).");
            System.out.print("  Deseja continuar mesmo assim? (s/n): ");
            String resp = scanner.next().trim().toLowerCase();
            System.out.println();
            if (!resp.equals("s")) { System.out.println("  Teste cancelado.\n"); return; }
        }

        NaiveBridgeFinder naive = new NaiveBridgeFinder();

        // Teste em grafo pequeno fixo para validar corretude
        //   0 --- 1 --- 2
        //   |           |
        //   3 ----------4
        //   |
        //   5   <- (3,5) e a unica ponte
        Graph fixo = new Graph(6);
        fixo.addEdge(0,1); fixo.addEdge(1,2); fixo.addEdge(2,4);
        fixo.addEdge(4,3); fixo.addEdge(3,0); fixo.addEdge(3,5);

        List<int[]> pontesFixo = naive.findBridges(fixo);
        boolean tem35 = pontesFixo.stream()
            .anyMatch(p -> (p[0]==3&&p[1]==5)||(p[0]==5&&p[1]==3));

        checar("Grafo fixo: encontrou 1 ponte",   pontesFixo.size() == 1,      true);
        checar("Grafo fixo: a ponte e (3,5)",      tem35,                       true);
        checar("isBridge(3,5) == true",            naive.isBridge(fixo, 3, 5), true);
        checar("isBridge(0,1) == false",           naive.isBridge(fixo, 0, 1), false);

        // Mede tempo no grafo do tamanho selecionado
        Graph g = copiar(grafoAtual);
        System.out.println("  Executando Naive no grafo com " + N + " vertices...");
        long t0 = System.nanoTime();
        List<int[]> pontes = naive.findBridges(g);
        long tempo = System.nanoTime() - t0;

        System.out.println("  Pontes encontradas: " + pontes.size());
        System.out.printf("  Tempo Naive (n=%d): %.3f ms%n", N, tempo / 1_000_000.0);

        System.out.println();
    }

    // -------------------------------------------------------
    // 5. Geração de grafos (3 tipos eulerianos)
    // -------------------------------------------------------
    static void testeGeracao() {
        System.out.println("=== 5. Geracao de Grafos - 3 tipos (n=" + N + ") ===");

        GraphGenerator gen = new GraphGenerator();
        int extra = Math.max(4, (N / 10) % 2 == 0 ? N / 10 : N / 10 + 1);

        System.out.println("  Gerando grafo Euleriano...");
        Graph euler = gen.generateEulerian(N, extra);
        checar("Euleriano: conexo",
               GraphUtils.isConnected(euler), true);
        checar("Euleriano: 0 vertices de grau impar",
               euler.getOddDegreeVertices().size() == 0, true);

        System.out.println("  Gerando grafo Semi-euleriano...");
        Graph semi = gen.generateSemiEulerian(N, extra);
        checar("Semi-euleriano: conexo",
               GraphUtils.isConnected(semi), true);
        checar("Semi-euleriano: exatamente 2 vertices de grau impar",
               semi.getOddDegreeVertices().size() == 2, true);

        System.out.println("  Gerando grafo Nao-euleriano...");
        Graph nao = gen.generateNonEulerian(N, extra);
        checar("Nao-euleriano: conexo",
               GraphUtils.isConnected(nao), true);
        checar("Nao-euleriano: mais de 2 vertices de grau impar",
               nao.getOddDegreeVertices().size() > 2, true);

        System.out.println();
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    static Graph copiar(Graph g) {
        Graph copia = new Graph(g.getNumVertices());
        for (int[] e : g.getAllEdges()) copia.addEdge(e[0], e[1]);
        return copia;
    }

    static void checar(String descricao, boolean obtido, boolean esperado) {
        boolean ok = obtido == esperado;
        System.out.println(ok
            ? "  [PASS] " + descricao
            : "  [FAIL] " + descricao + " (esperado=" + esperado + ", obtido=" + obtido + ")");
    }

    static int lerInt() {
        try { return Integer.parseInt(scanner.next().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}