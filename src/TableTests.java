import java.util.*;

/**
 * Bateria de testes em formato de tabela (table-driven tests).
 *
 * Cada caso de teste é uma linha com:
 *   - Descrição
 *   - Construção do grafo
 *   - Resultado esperado (tipo, número de pontes, etc.)
 *
 * Cobre:
 *   - Classificação Eulerian/Semi/Non
 *   - Detecção de pontes (Naïve == Tarjan)
 *   - Fleury (caminho percorre todas as arestas, começa/termina nos vértices certos)
 *   - Equivalência Naïve ↔ Tarjan em grafos aleatórios
 *   - Casos degenerados: K3, K4, K5, ciclo simples, "barbell", lollipop
 *
 * Uso: java Main, opção "Executar table-tests"
 *      ou: chamar TableTests.runAll() de qualquer lugar.
 */
public class TableTests {

    private int passados = 0;
    private int falhados = 0;
    private final List<String> falhas = new ArrayList<>();

    // =====================================================================
    // ENTRADA PRINCIPAL
    // =====================================================================

    public static void main(String[] args) {
        new TableTests().runAll();
    }

    public void runAll() {
        System.out.println();
        System.out.println("========================================================");
        System.out.println("  TABLE-TESTS - Validacao automatizada");
        System.out.println("========================================================");
        System.out.println();

        secao("Classificacao Euleriana (grafos fixos)");
        testarClassificacao();

        secao("Pontes em grafos fixos (Naive vs Tarjan)");
        testarPontesFixas();

        secao("Equivalencia Naive == Tarjan (grafos aleatorios)");
        testarEquivalenciaAleatoria();

        secao("Fleury em grafos Eulerianos pequenos");
        testarFleuryEuleriano();

        secao("Fleury em grafos Semi-Eulerianos pequenos");
        testarFleurySemi();

        secao("Fleury rejeita grafos Nao-Eulerianos");
        testarFleuryNao();

        secao("Fleury: Naive e Tarjan produzem caminho valido");
        testarFleuryAmbasEstrategias();

        secao("GraphGenerator gera o tipo correto");
        testarGenerator();

        secao("Casos degenerados (K3, K4, K5, ciclo, barbell, lollipop, ponte)");
        testarCasosDegenerados();

        relatorio();
    }

    // =====================================================================
    // 1. CLASSIFICAÇÃO
    // =====================================================================

    private void testarClassificacao() {
        // Caso 1: triângulo K3 → Euleriano (todos grau 2)
        Graph g1 = new Graph(3);
        g1.addEdge(0,1); g1.addEdge(1,2); g1.addEdge(2,0);
        check("K3 (triangulo)", GraphUtils.classifyGraph(g1),
            GraphUtils.EulerianType.EULERIAN);

        // Caso 2: caminho 0-1-2 → Semi-Euleriano (graus 1,2,1)
        Graph g2 = new Graph(3);
        g2.addEdge(0,1); g2.addEdge(1,2);
        check("Caminho P3", GraphUtils.classifyGraph(g2),
            GraphUtils.EulerianType.SEMI_EULERIAN);

        // Caso 3: K4 (4 vértices, todos grau 3) → Não-Euleriano
        Graph g3 = new Graph(4);
        for (int i = 0; i < 4; i++)
            for (int j = i+1; j < 4; j++) g3.addEdge(i, j);
        check("K4 (todos grau 3)", GraphUtils.classifyGraph(g3),
            GraphUtils.EulerianType.NON_EULERIAN);

        // Caso 4: K5 → Euleriano (todos grau 4)
        Graph g4 = new Graph(5);
        for (int i = 0; i < 5; i++)
            for (int j = i+1; j < 5; j++) g4.addEdge(i, j);
        check("K5 (todos grau 4)", GraphUtils.classifyGraph(g4),
            GraphUtils.EulerianType.EULERIAN);

        // Caso 5: ciclo C6 → Euleriano
        Graph g5 = new Graph(6);
        for (int i = 0; i < 6; i++) g5.addEdge(i, (i+1)%6);
        check("Ciclo C6", GraphUtils.classifyGraph(g5),
            GraphUtils.EulerianType.EULERIAN);

        // Caso 6: grafo desconexo (dois triângulos separados) → Não-Euleriano
        Graph g6 = new Graph(6);
        g6.addEdge(0,1); g6.addEdge(1,2); g6.addEdge(2,0);
        g6.addEdge(3,4); g6.addEdge(4,5); g6.addEdge(5,3);
        check("Desconexo (dois K3)", GraphUtils.classifyGraph(g6),
            GraphUtils.EulerianType.NON_EULERIAN);

        // Caso 7: grafo com ponte (lollipop) — graus 1,2,2,2,3 → Não-Euleriano (3 impares? não: 1 e 3)
        // Vamos construir: vértice 4 ligado só a 0, e 0-1-2-3-0 forma ciclo
        // Graus: 0=3, 1=2, 2=2, 3=2, 4=1. Impares: {0, 4} → Semi-Euleriano
        Graph g7 = new Graph(5);
        g7.addEdge(0,1); g7.addEdge(1,2); g7.addEdge(2,3); g7.addEdge(3,0); g7.addEdge(0,4);
        check("Lollipop (ciclo C4 + cauda)", GraphUtils.classifyGraph(g7),
            GraphUtils.EulerianType.SEMI_EULERIAN);
    }

    // =====================================================================
    // 2. PONTES EM GRAFOS FIXOS
    // =====================================================================

    private void testarPontesFixas() {
        // Grafo em forma de halter: dois triângulos ligados por uma ponte
        // 0-1-2-0  e  3-4-5-3, com aresta (0,3) ponte
        Graph g1 = new Graph(6);
        g1.addEdge(0,1); g1.addEdge(1,2); g1.addEdge(2,0);
        g1.addEdge(3,4); g1.addEdge(4,5); g1.addEdge(5,3);
        g1.addEdge(0,3);

        int qNaive = new NaiveBridgeFinder().findBridges(g1).size();
        int qTarjan = new TarjanBridgeFinder().findBridges(g1).size();
        check("Halter (dois K3 + ponte): naive=1", qNaive, 1);
        check("Halter (dois K3 + ponte): tarjan=1", qTarjan, 1);

        // Caminho P5: 4 arestas, todas pontes
        Graph g2 = new Graph(5);
        g2.addEdge(0,1); g2.addEdge(1,2); g2.addEdge(2,3); g2.addEdge(3,4);
        check("P5: naive=4 pontes", new NaiveBridgeFinder().findBridges(g2).size(), 4);
        check("P5: tarjan=4 pontes", new TarjanBridgeFinder().findBridges(g2).size(), 4);

        // Ciclo C5: nenhuma aresta é ponte
        Graph g3 = new Graph(5);
        for (int i = 0; i < 5; i++) g3.addEdge(i, (i+1)%5);
        check("C5: naive=0 pontes", new NaiveBridgeFinder().findBridges(g3).size(), 0);
        check("C5: tarjan=0 pontes", new TarjanBridgeFinder().findBridges(g3).size(), 0);

        // Grafo da Figura 1 (aproximado): tripla ligado em pirâmide
        // K4 -> nenhuma ponte
        Graph g4 = new Graph(4);
        for (int i = 0; i < 4; i++)
            for (int j = i+1; j < 4; j++) g4.addEdge(i, j);
        check("K4: naive=0 pontes", new NaiveBridgeFinder().findBridges(g4).size(), 0);
        check("K4: tarjan=0 pontes", new TarjanBridgeFinder().findBridges(g4).size(), 0);

        // Árvore (binary tree balanceada): TODAS arestas são pontes
        Graph g5 = new Graph(7); // 0 raiz, filhos 1,2; 1->3,4; 2->5,6
        g5.addEdge(0,1); g5.addEdge(0,2); g5.addEdge(1,3); g5.addEdge(1,4);
        g5.addEdge(2,5); g5.addEdge(2,6);
        check("Arvore binaria n=7: naive=6 pontes", new NaiveBridgeFinder().findBridges(g5).size(), 6);
        check("Arvore binaria n=7: tarjan=6 pontes", new TarjanBridgeFinder().findBridges(g5).size(), 6);
    }

    // =====================================================================
    // 3. EQUIVALÊNCIA NAIVE == TARJAN EM GRAFOS ALEATÓRIOS
    // =====================================================================

    private void testarEquivalenciaAleatoria() {
        long[] seeds = {1, 2, 3, 7, 13, 42, 99, 123, 777, 2024};
        int[] tamanhos = {10, 30, 50, 100};
        String[] tipos = {"EULERIANO", "SEMI", "NAO"};

        for (long seed : seeds) {
            for (int n : tamanhos) {
                for (String tipo : tipos) {
                    GraphGenerator gen = new GraphGenerator(seed);
                    int extra = Math.max(4, n / 5);
                    if (extra % 2 != 0) extra++;
                    Graph g = switch (tipo) {
                        case "EULERIANO" -> gen.generateEulerian(n, extra);
                        case "SEMI"      -> gen.generateSemiEulerian(n, extra);
                        default          -> gen.generateNonEulerian(n, extra);
                    };
                    Set<Long> sNaive  = pontesAsSet(new NaiveBridgeFinder().findBridges(g));
                    Set<Long> sTarjan = pontesAsSet(new TarjanBridgeFinder().findBridges(g));
                    check(String.format("seed=%d n=%d %s: naive==tarjan", seed, n, tipo),
                        sNaive, sTarjan);
                }
            }
        }
    }

    private Set<Long> pontesAsSet(List<int[]> pontes) {
        Set<Long> s = new HashSet<>();
        for (int[] p : pontes) s.add(GraphUtils.edgeKey(p[0], p[1]));
        return s;
    }

    // =====================================================================
    // 4. FLEURY EM GRAFOS EULERIANOS
    // =====================================================================

    private void testarFleuryEuleriano() {
        // K5 (todos grau 4) → circuito euleriano de 10 arestas, retorna ao início
        Graph g1 = new Graph(5);
        for (int i = 0; i < 5; i++)
            for (int j = i+1; j < 5; j++) g1.addEdge(i, j);

        for (BridgeFinder bf : List.of(new NaiveBridgeFinder(), new TarjanBridgeFinder())) {
            FleuryEulerPath.Result r = new FleuryEulerPath(bf).findPath(g1);
            check("K5 + " + bf.getName() + ": existe",        r.exists, true);
            check("K5 + " + bf.getName() + ": tipo EULERIAN", r.type, GraphUtils.EulerianType.EULERIAN);
            check("K5 + " + bf.getName() + ": percorre 10 arestas", r.edgesTraversed, 10L);
            check("K5 + " + bf.getName() + ": circuito (inicio==fim)",
                r.path.get(0), r.path.get(r.path.size()-1));
        }

        // Ciclo C6 → 6 arestas, circuito
        Graph g2 = new Graph(6);
        for (int i = 0; i < 6; i++) g2.addEdge(i, (i+1)%6);
        FleuryEulerPath.Result r2 = new FleuryEulerPath(new TarjanBridgeFinder()).findPath(g2);
        check("C6 percorre 6 arestas", r2.edgesTraversed, 6L);
        check("C6 circuito",            r2.path.get(0), r2.path.get(r2.path.size()-1));
    }

    // =====================================================================
    // 5. FLEURY EM GRAFOS SEMI-EULERIANOS
    // =====================================================================

    private void testarFleurySemi() {
        // Caminho 0-1-2 → 2 arestas, caminho 0→2 (ou 2→0)
        Graph g1 = new Graph(3);
        g1.addEdge(0,1); g1.addEdge(1,2);

        for (BridgeFinder bf : List.of(new NaiveBridgeFinder(), new TarjanBridgeFinder())) {
            FleuryEulerPath.Result r = new FleuryEulerPath(bf).findPath(g1);
            check("P3 + " + bf.getName() + ": existe",  r.exists, true);
            check("P3 + " + bf.getName() + ": SEMI",    r.type, GraphUtils.EulerianType.SEMI_EULERIAN);
            check("P3 + " + bf.getName() + ": 2 arestas", r.edgesTraversed, 2L);
            // Inicia em vértice ímpar (0 ou 2)
            int inicio = r.path.get(0);
            int fim    = r.path.get(r.path.size()-1);
            check("P3 + " + bf.getName() + ": comeca em impar", inicio == 0 || inicio == 2, true);
            check("P3 + " + bf.getName() + ": termina em impar", fim == 0 || fim == 2, true);
            check("P3 + " + bf.getName() + ": inicio != fim", inicio != fim, true);
        }

        // Lollipop (C4 + cauda em 0): 5 arestas, semi-euleriano
        Graph g2 = new Graph(5);
        g2.addEdge(0,1); g2.addEdge(1,2); g2.addEdge(2,3); g2.addEdge(3,0); g2.addEdge(0,4);
        FleuryEulerPath.Result r = new FleuryEulerPath(new TarjanBridgeFinder()).findPath(g2);
        check("Lollipop: existe",        r.exists, true);
        check("Lollipop: 5 arestas",     r.edgesTraversed, 5L);
        check("Lollipop: SEMI",          r.type, GraphUtils.EulerianType.SEMI_EULERIAN);
    }

    // =====================================================================
    // 6. FLEURY REJEITA NÃO-EULERIANOS
    // =====================================================================

    private void testarFleuryNao() {
        // K4 (todos grau 3) → 4 vértices ímpares → Não-Euleriano
        Graph g = new Graph(4);
        for (int i = 0; i < 4; i++)
            for (int j = i+1; j < 4; j++) g.addEdge(i, j);

        FleuryEulerPath.Result rN = new FleuryEulerPath(new NaiveBridgeFinder()).findPath(g);
        FleuryEulerPath.Result rT = new FleuryEulerPath(new TarjanBridgeFinder()).findPath(g);
        check("K4 + Naive: NAO existe",  rN.exists, false);
        check("K4 + Tarjan: NAO existe", rT.exists, false);
        check("K4 + Naive: tipo NON",    rN.type, GraphUtils.EulerianType.NON_EULERIAN);
        check("K4 + Tarjan: tipo NON",   rT.type, GraphUtils.EulerianType.NON_EULERIAN);

        // Grafo desconexo
        Graph g2 = new Graph(6);
        g2.addEdge(0,1); g2.addEdge(1,2); g2.addEdge(2,0); // K3
        g2.addEdge(3,4); g2.addEdge(4,5); g2.addEdge(5,3); // outro K3
        FleuryEulerPath.Result r2 = new FleuryEulerPath(new TarjanBridgeFinder()).findPath(g2);
        check("Desconexo: NAO existe", r2.exists, false);
    }

    // =====================================================================
    // 7. FLEURY: AMBAS ESTRATÉGIAS PRODUZEM CAMINHO VÁLIDO
    // =====================================================================

    private void testarFleuryAmbasEstrategias() {
        // Grafo aleatório euleriano: as duas estratégias devem percorrer
        // todas as arestas (caminhos podem ser diferentes, mas válidos).
        long[] seeds = {1L, 7L, 42L};
        int[] tamanhos = {10, 30, 100};

        for (long s : seeds) {
            for (int n : tamanhos) {
                GraphGenerator gen = new GraphGenerator(s);
                int extra = Math.max(4, n / 5);
                if (extra % 2 != 0) extra++;
                Graph g = gen.generateEulerian(n, extra);

                FleuryEulerPath.Result rN = new FleuryEulerPath(new NaiveBridgeFinder()).findPath(g);
                FleuryEulerPath.Result rT = new FleuryEulerPath(new TarjanBridgeFinder()).findPath(g);

                check(String.format("seed=%d n=%d Eul: Naive percorre tudo",  s, n),
                    rN.edgesTraversed, (long) g.getNumEdges());
                check(String.format("seed=%d n=%d Eul: Tarjan percorre tudo", s, n),
                    rT.edgesTraversed, (long) g.getNumEdges());
                check(String.format("seed=%d n=%d Eul: Naive caminho valido (path %d > arestas %d)",
                    s, n, rN.path.size(), g.getNumEdges()),
                    rN.path.size() == g.getNumEdges() + 1, true);
                check(String.format("seed=%d n=%d Eul: Tarjan caminho valido", s, n),
                    rT.path.size() == g.getNumEdges() + 1, true);
                // Caminho deve usar arestas reais
                check(String.format("seed=%d n=%d Eul: Naive arestas reais",  s, n),
                    caminhoUsaArestasReais(g, rN.path), true);
                check(String.format("seed=%d n=%d Eul: Tarjan arestas reais", s, n),
                    caminhoUsaArestasReais(g, rT.path), true);
            }
        }
    }

    /**
     * Verifica que cada par consecutivo (path[i], path[i+1]) era aresta no grafo
     * E que nenhuma aresta é repetida (Euler: cada aresta percorrida exatamente 1×).
     */
    private boolean caminhoUsaArestasReais(Graph g, List<Integer> path) {
        Set<String> arestasGrafo = new HashSet<>();
        for (int[] e : g.getAllEdges()) {
            arestasGrafo.add(Math.min(e[0],e[1]) + "-" + Math.max(e[0],e[1]));
        }
        Set<String> usadas = new HashSet<>();
        for (int i = 0; i < path.size() - 1; i++) {
            int u = path.get(i), v = path.get(i+1);
            String chave = Math.min(u,v) + "-" + Math.max(u,v);
            if (!arestasGrafo.contains(chave)) return false;
            if (!usadas.add(chave)) return false; // aresta repetida
        }
        return usadas.size() == arestasGrafo.size();
    }

    // =====================================================================
    // 8. GERADOR PRODUZ TIPO CORRETO
    // =====================================================================

    private void testarGenerator() {
        long[] seeds = {1L, 2L, 7L, 42L, 999L};
        int[] tamanhos = {20, 50, 200};

        for (long s : seeds) {
            for (int n : tamanhos) {
                GraphGenerator gen = new GraphGenerator(s);
                int extra = Math.max(4, n / 5);
                if (extra % 2 != 0) extra++;

                Graph gE = gen.generateEulerian(n, extra);
                check(String.format("seed=%d n=%d gera EULERIANO", s, n),
                    GraphUtils.classifyGraph(gE), GraphUtils.EulerianType.EULERIAN);

                Graph gS = gen.generateSemiEulerian(n, extra);
                check(String.format("seed=%d n=%d gera SEMI", s, n),
                    GraphUtils.classifyGraph(gS), GraphUtils.EulerianType.SEMI_EULERIAN);

                Graph gN = gen.generateNonEulerian(n, extra);
                check(String.format("seed=%d n=%d gera NAO", s, n),
                    GraphUtils.classifyGraph(gN), GraphUtils.EulerianType.NON_EULERIAN);
            }
        }
    }

    // =====================================================================
    // 9. CASOS DEGENERADOS
    // =====================================================================

    private void testarCasosDegenerados() {
        // Aresta única: ponte
        Graph g1 = new Graph(2);
        g1.addEdge(0, 1);
        check("Aresta unica: 1 ponte (Naive)",  new NaiveBridgeFinder().findBridges(g1).size(), 1);
        check("Aresta unica: 1 ponte (Tarjan)", new TarjanBridgeFinder().findBridges(g1).size(), 1);
        check("Aresta unica: SEMI", GraphUtils.classifyGraph(g1),
            GraphUtils.EulerianType.SEMI_EULERIAN);

        // Grafo sem arestas
        Graph g2 = new Graph(5);
        check("Sem arestas: 0 pontes (Naive)",  new NaiveBridgeFinder().findBridges(g2).size(), 0);
        check("Sem arestas: 0 pontes (Tarjan)", new TarjanBridgeFinder().findBridges(g2).size(), 0);

        // Triângulo + cauda longa: cauda toda é ponte
        Graph g3 = new Graph(6);
        g3.addEdge(0,1); g3.addEdge(1,2); g3.addEdge(2,0); // K3
        g3.addEdge(0,3); g3.addEdge(3,4); g3.addEdge(4,5); // cauda 3 arestas
        check("K3+cauda: 3 pontes (Naive)",  new NaiveBridgeFinder().findBridges(g3).size(), 3);
        check("K3+cauda: 3 pontes (Tarjan)", new TarjanBridgeFinder().findBridges(g3).size(), 3);

        // Ciclo + diagonal: nenhuma ponte
        Graph g4 = new Graph(4);
        g4.addEdge(0,1); g4.addEdge(1,2); g4.addEdge(2,3); g4.addEdge(3,0); g4.addEdge(0,2);
        check("C4+diagonal: 0 pontes",  new NaiveBridgeFinder().findBridges(g4).size(), 0);

        // Fleury num grafo MUITO simples (só uma aresta)
        FleuryEulerPath.Result rUnit = new FleuryEulerPath(new TarjanBridgeFinder()).findPath(g1);
        check("Fleury aresta unica: existe",       rUnit.exists, true);
        check("Fleury aresta unica: 1 percorrida", rUnit.edgesTraversed, 1L);
    }

    // =====================================================================
    // INFRAESTRUTURA DE TESTES
    // =====================================================================

    private void secao(String titulo) {
        System.out.println();
        System.out.println("--- " + titulo + " ---");
    }

    private <T> void check(String desc, T obtido, T esperado) {
        boolean ok = Objects.equals(obtido, esperado);
        if (ok) {
            passados++;
            System.out.println("  [PASS] " + desc);
        } else {
            falhados++;
            String msg = "[FAIL] " + desc + " | esperado=" + esperado + " obtido=" + obtido;
            falhas.add(msg);
            System.out.println("  " + msg);
        }
    }

    private void relatorio() {
        System.out.println();
        System.out.println("========================================================");
        System.out.printf ("  TOTAL: %d testes | PASS: %d | FAIL: %d%n",
            passados + falhados, passados, falhados);
        System.out.println("========================================================");
        if (!falhas.isEmpty()) {
            System.out.println("FALHAS:");
            for (String f : falhas) System.out.println("  " + f);
        } else {
            System.out.println("Tudo verde! 100% dos testes passaram.");
        }
        System.out.println();
    }
}
