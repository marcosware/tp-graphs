import java.util.*;

/**
 * Método naïve para encontrar pontes em um grafo.
 *
 * Definição: uma aresta (u,v) é ponte se sua remoção torna o grafo desconexo.
 *
 * Algoritmo:
 *   Para cada aresta (u, v) em E:
 *     1. Remove (u, v) do grafo
 *     2. Verifica conectividade via DFS/BFS
 *     3. Se desconexo → (u, v) é ponte
 *     4. Reinsere (u, v)
 *
 * Complexidade: O(E * (V + E))
 *   - E iterações (uma por aresta)
 *   - Cada DFS/BFS custa O(V + E)
 *
 * Ineficiente para grafos grandes, mas correto e simples.
 * Serve como baseline para comparação com Tarjan O(V + E).
 */
public class NaiveBridgeFinder implements BridgeFinder {

    /**
     * Interface de estratégia de conectividade: permite trocar DFS por BFS
     * sem alterar a lógica do método naïve. Útil para experimentos.
     */
    public enum ConnectivityStrategy {
        DFS, BFS
    }

    private final ConnectivityStrategy strategy;

    public NaiveBridgeFinder() {
        this.strategy = ConnectivityStrategy.DFS; // padrão
    }

    public NaiveBridgeFinder(ConnectivityStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Retorna lista de pontes do grafo g.
     * Cada ponte é representada como int[]{u, v}.
     */
    @Override
    public List<int[]> findBridges(Graph g) {
        List<int[]> bridges = new ArrayList<>();
        List<int[]> edges = g.getAllEdges();

        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];

            // Passo 1: remove a aresta temporariamente
            g.removeEdge(u, v);

            // Passo 2: verifica conectividade
            boolean connected = checkConnectivity(g);

            // Passo 3: reinsere a aresta (estado original restaurado)
            g.addEdge(u, v);

            // Passo 4: se ficou desconexo, é ponte
            if (!connected) {
                bridges.add(new int[]{u, v});
            }
        }

        return bridges;
    }

    /**
     * Verifica conectividade usando a estratégia configurada.
     * Encapsula a escolha DFS vs BFS.
     */
    private boolean checkConnectivity(Graph g) {
        return switch (strategy) {
            case DFS -> GraphUtils.isConnected(g); // usa DFS internamente
            case BFS -> isConnectedBFS(g);
        };
    }

    /**
     * Versão de isConnected usando BFS explicitamente.
     * Mesma lógica de GraphUtils.isConnected, mas forçando BFS.
     */
    private boolean isConnectedBFS(Graph g) {
        int n = g.getNumVertices();
        int start = -1;
        for (int v = 0; v < n; v++) {
            if (g.degree(v) > 0) { start = v; break; }
        }
        if (start == -1) return true;

        Set<Integer> visited = GraphUtils.bfs(g, start);
        for (int v = 0; v < n; v++) {
            if (g.degree(v) > 0 && !visited.contains(v)) return false;
        }
        return true;
    }

    /**
     * Verifica se uma aresta específica é ponte.
     * Útil para o algoritmo de Fleury (verificação aresta a aresta).
     */
    @Override
    public boolean isBridge(Graph g, int u, int v) {
        g.removeEdge(u, v);
        boolean connected = checkConnectivity(g);
        g.addEdge(u, v);
        return !connected;
    }

    @Override
    public String getName() {
        return "Naïve (" + strategy + ")";
    }
}
