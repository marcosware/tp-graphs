import java.util.*;

/**
 * Gerador de grafos aleatórios para os experimentos.
 *
 * Gera grafos nos três tipos exigidos pelo enunciado:
 * - EULERIAN: conexo, todos os vértices com grau par
 * - SEMI_EULERIAN: conexo, exatamente 2 vértices com grau ímpar
 * - NON_EULERIAN: conexo, mais de 2 vértices com grau ímpar
 */
public class GraphGenerator {

    private final Random rng;

    public GraphGenerator(long seed) { this.rng = new Random(seed); }
    public GraphGenerator()          { this.rng = new Random(); }

    // ------------------------------------------------------------------
    // Euleriano: todos os vértices com grau par
    // ------------------------------------------------------------------

    public Graph generateEulerian(int n, int extraEdges) {
        if (n < 3) throw new IllegalArgumentException("Mínimo 3 vértices");

        Graph g = new Graph(n);

        // Ciclo hamiltoniano: grau 2 (par) para todos, grafo já conexo
        List<Integer> perm = shuffle(n);
        for (int i = 0; i < n; i++)
            g.addEdge(perm.get(i), perm.get((i + 1) % n));

        // Adiciona ciclos aleatórios extras.
        // Um ciclo incrementa o grau de cada vértice participante em exatamente 2,
        // preservando a paridade de todos.
        for (int i = 0; i < extraEdges; i++)
            adicionarCicloAleatorio(g, n);

        // Correção final: se ainda restar vértices ímpares (colisões de aresta
        // que impediram o ciclo), emparelha e conecta.
        corrigirParidade(g, n);

        return g;
    }

    // ------------------------------------------------------------------
    // Semi-euleriano: exatamente 2 vértices com grau ímpar
    // ------------------------------------------------------------------

    public Graph generateSemiEulerian(int n, int extraEdges) {
        if (n < 2) throw new IllegalArgumentException("Mínimo 2 vértices");

        Graph g = generateEulerian(n, extraEdges); // começa com todos pares

        // Adiciona UMA aresta entre dois vértices de grau par:
        // ambos passam a ter grau ímpar → exatamente 2 vértices ímpares.
        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                if (!g.hasEdge(u, v)) {
                    g.addEdge(u, v);
                    return g;
                }
            }
        }
        return g; // grafo completo (todos pares impossível virar semi com essa abordagem)
    }

    // ------------------------------------------------------------------
    // Não-euleriano: mais de 2 vértices com grau ímpar
    // ------------------------------------------------------------------

    public Graph generateNonEulerian(int n, int extraEdges) {
        if (n < 4) throw new IllegalArgumentException("Mínimo 4 vértices");

        Graph g = generateEulerian(n, extraEdges);

        // Adiciona 2 arestas independentes (sem vértice em comum):
        // cada aresta cria 2 vértices ímpares → total 4 vértices ímpares.
        int added = 0;
        int attempts = 0;
        Set<Integer> usados = new HashSet<>();

        while (added < 2 && attempts < n * n) {
            attempts++;
            int u = rng.nextInt(n);
            int v = rng.nextInt(n);
            if (u != v && !g.hasEdge(u, v) && !usados.contains(u) && !usados.contains(v)) {
                g.addEdge(u, v);
                usados.add(u);
                usados.add(v);
                added++;
            }
        }

        return g;
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /**
     * Adiciona um ciclo aleatório de tamanho entre 3 e min(n, 6).
     * Cada vértice do ciclo tem grau +2 → paridade preservada.
     */
    private void adicionarCicloAleatorio(Graph g, int n) {
        int tamanho = 3 + rng.nextInt(Math.min(n - 2, 4)); // ciclo de 3 a 6 vértices
        List<Integer> perm = shuffle(n);
        List<Integer> ciclo = perm.subList(0, tamanho);

        for (int i = 0; i < tamanho; i++) {
            int u = ciclo.get(i);
            int v = ciclo.get((i + 1) % tamanho);
            if (!g.hasEdge(u, v))
                g.addEdge(u, v);
            // Se a aresta já existir, o ciclo "encurta" nesse ponto,
            // mas a correção final de paridade trata qualquer resíduo.
        }
    }

    /**
     * Corrige vértices de grau ímpar emparelhando-os dois a dois.
     * Conectar dois vértices ímpares torna ambos pares.
     * Número de vértices ímpares é sempre par (handshaking lemma).
     */
    private void corrigirParidade(Graph g, int n) {
        List<Integer> impares = g.getOddDegreeVertices();

        // Emparcela ímpares dois a dois e conecta
        for (int i = 0; i + 1 < impares.size(); i += 2) {
            int u = impares.get(i);
            int v = impares.get(i + 1);

            if (!g.hasEdge(u, v)) {
                g.addEdge(u, v);
            } else {
                // Aresta já existe: encontra intermediário w para criar caminho u-w-v
                // (adiciona 2 arestas, ambos u e v ficam pares novamente)
                for (int w = 0; w < n; w++) {
                    if (w != u && w != v && !g.hasEdge(u, w) && !g.hasEdge(w, v)) {
                        g.addEdge(u, w);
                        g.addEdge(w, v);
                        break;
                    }
                }
            }
        }
    }


    // ------------------------------------------------------------------
    // Grafo conexo genérico (qualquer número de arestas >= n-1)
    // ------------------------------------------------------------------

    public Graph generateConnectedRandom(int n, int m) {
        if (m < n - 1) throw new IllegalArgumentException("m deve ser >= n-1");

        Graph g = new Graph(n);

        // Spanning tree aleatória garante conectividade
        List<Integer> perm = shuffle(n);
        for (int i = 1; i < n; i++) {
            int parent = perm.get(rng.nextInt(i));
            g.addEdge(parent, perm.get(i));
        }

        // Arestas extras aleatórias
        int added = n - 1;
        int attempts = 0;
        while (added < m && attempts < m * 20) {
            attempts++;
            int u = rng.nextInt(n);
            int v = rng.nextInt(n);
            if (u != v && !g.hasEdge(u, v)) {
                g.addEdge(u, v);
                added++;
            }
        }
        return g;
    }

    private List<Integer> shuffle(int n) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(i);
        Collections.shuffle(list, rng);
        return list;
    }
}