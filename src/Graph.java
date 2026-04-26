import java.util.*;

/**
 * Representa um grafo simples não-direcionado usando lista de adjacência.
 *
 * Escolha: lista de adjacência em vez de matriz de adjacência porque:
 * - Memória O(V + E), ideal para grafos esparsos (comuns em problemas reais)
 * - Iteração sobre vizinhos é O(grau(v)), não O(V)
 * - Remoção/reinserção de arestas eficiente com LinkedList
 */
public class Graph {

    private final int numVertices;
    // Cada posição guarda a lista de vizinhos do vértice i
    private final LinkedList<Integer>[] adjList;

    // Contador de versão: incrementado a cada modificação estrutural.
    // Permite caches externos (ex.: Tarjan.isBridge) invalidarem com
    // segurança usando comparação de inteiros — O(1).
    private long version = 0;

    // Mantido em O(1) por add/removeEdge — evita varredura de getAllEdges().
    private int numEdges = 0;

    public Graph(int numVertices) {
        this.numVertices = numVertices;
        adjList = new LinkedList[numVertices];
        for (int i = 0; i < numVertices; i++) {
            adjList[i] = new LinkedList<>();
        }
    }

    /**
     * Adiciona aresta não-direcionada entre u e v.
     * Grafo simples: sem laços (u != v) e sem arestas paralelas.
     */
    public void addEdge(int u, int v) {
        if (u == v) return; // sem laços
        if (!adjList[u].contains(v)) {
            adjList[u].add(v);
            adjList[v].add(u);
            version++;
            numEdges++;
        }
    }

    /**
     * Remove aresta não-direcionada entre u e v.
     * Usado pelo método naïve: remove → testa → reinsere.
     */
    public void removeEdge(int u, int v) {
        boolean changed = adjList[u].remove(Integer.valueOf(v));
        adjList[v].remove(Integer.valueOf(u));
        if (changed) {
            version++;
            numEdges--;
        }
    }

    /**
     * Versão atual do grafo: muda a cada add/remove de aresta.
     * Usado para invalidar caches em algoritmos externos.
     */
    public long getVersion() { return version; }

    /**
     * Verifica se a aresta (u, v) existe.
     */
    public boolean hasEdge(int u, int v) {
        return adjList[u].contains(v);
    }

    public int getNumVertices() {
        return numVertices;
    }

    /**
     * Retorna lista de vizinhos de v (cópia para evitar modificação externa acidental).
     */
    public List<Integer> getNeighbors(int v) {
        return new ArrayList<>(adjList[v]);
    }

    /**
     * Retorna grau do vértice v.
     */
    public int degree(int v) {
        return adjList[v].size();
    }

    /**
     * Retorna todas as arestas do grafo (sem duplicatas).
     * Aresta representada como int[]{u, v} com u < v para unicidade.
     */
    public List<int[]> getAllEdges() {
        List<int[]> edges = new ArrayList<>();
        for (int u = 0; u < numVertices; u++) {
            for (int v : adjList[u]) {
                if (u < v) { // evita duplicata (u,v) e (v,u)
                    edges.add(new int[]{u, v});
                }
            }
        }
        return edges;
    }

    /**
     * Retorna o número de arestas do grafo (O(1)).
     */
    public int getNumEdges() {
        return numEdges;
    }

    /**
     * Retorna vértices com grau ímpar — usado para verificar tipo euleriano.
     */
    public List<Integer> getOddDegreeVertices() {
        List<Integer> odd = new ArrayList<>();
        for (int v = 0; v < numVertices; v++) {
            if (degree(v) % 2 != 0) {
                odd.add(v);
            }
        }
        return odd;
    }

    /**
     * Representação textual do grafo (útil para debug).
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph (").append(numVertices).append(" vértices, ")
          .append(getNumEdges()).append(" arestas)\n");
        for (int v = 0; v < numVertices; v++) {
            sb.append(v).append(" -> ").append(adjList[v]).append("\n");
        }
        return sb.toString();
    }
}
