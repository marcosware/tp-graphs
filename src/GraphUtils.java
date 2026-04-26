import java.util.*;

/**
 * Utilitários de travessia e conectividade para grafos.
 *
 * Separados da classe Graph para manter responsabilidade única:
 * - Graph: estrutura e dados
 * - GraphUtils: algoritmos sobre a estrutura
 */
public class GraphUtils {

    /**
     * DFS iterativa a partir de 'start'. Retorna conjunto de vértices visitados.
     *
     * Usada por:
     * - isConnected() para verificar conectividade geral
     * - método naïve após remoção de aresta
     *
     * Optamos por DFS iterativa (pilha explícita) para evitar
     * StackOverflowError em grafos grandes (100k vértices).
     */
    public static Set<Integer> dfs(Graph g, int start) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            int v = stack.pop();
            if (visited.contains(v)) continue;
            visited.add(v);
            for (int neighbor : g.getNeighbors(v)) {
                if (!visited.contains(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }
        return visited;
    }

    /**
     * BFS a partir de 'start'. Retorna conjunto de vértices visitados.
     *
     * Alternativa à DFS com mesma complexidade O(V + E).
     * Útil para verificar conectividade quando se prefere percurso por largura.
     */
    public static Set<Integer> bfs(Graph g, int start) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            int v = queue.poll();
            for (int neighbor : g.getNeighbors(v)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }

    /**
     * Verifica se o grafo é conexo considerando todos os vértices
     * que TINHAM arestas antes da última remoção.
     *
     * Usada pelo Naïve: após remover (u,v), verifica se u ainda
     * alcança v. Se não alcança, a aresta era ponte.
     *
     * Estratégia eficiente: em vez de varrer todos os N vértices,
     * faz DFS a partir de u e verifica apenas se v foi alcançado.
     * Custo real = O(componente de u), muito menor que O(N) quando
     * o grafo é esparso com muitos vértices isolados.
     */
    public static boolean isConnected(Graph g) {
        int n = g.getNumVertices();
        if (n == 0) return true;

        // Encontra primeiro vértice com grau > 0
        int start = -1;
        for (int v = 0; v < n; v++) {
            if (g.degree(v) > 0) { start = v; break; }
        }

        // Sem arestas: trivialmente conexo (nada a desconectar)
        if (start == -1) return true;

        Set<Integer> visited = dfs(g, start);

        // Verifica apenas vértices que têm arestas —
        // vértices originalmente isolados (grau 0 antes e depois) não contam
        for (int v = 0; v < n; v++) {
            if (g.degree(v) > 0 && !visited.contains(v)) return false;
        }
        return true;
    }

    /**
     * Verifica se a aresta (u,v) ainda conecta após remoção — versão
     * otimizada para o Naïve.
     *
     * Em vez de varrer todos os vértices, faz DFS a partir de u e
     * verifica apenas se v é alcançável. Muito mais rápido em grafos
     * esparsos com muitos vértices isolados.
     */
    public static boolean isReachable(Graph g, int u, int v) {
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        stack.push(u);
        while (!stack.isEmpty()) {
            int cur = stack.pop();
            if (cur == v) return true;
            if (visited.contains(cur)) continue;
            visited.add(cur);
            for (int nb : g.getNeighbors(cur)) {
                if (!visited.contains(nb)) stack.push(nb);
            }
        }
        return false;
    }

    /**
     * Verifica conectividade ignorando vértices isolados (grau 0).
     *
     * Usada pela classificação euleriana: vértices sem arestas não
     * interferem na existência de circuito/caminho euleriano.
     */
    public static boolean isConnectedIgnoringIsolated(Graph g) {
        int n = g.getNumVertices();

        int start = -1;
        for (int v = 0; v < n; v++) {
            if (g.degree(v) > 0) { start = v; break; }
        }

        // Grafo sem arestas: trivialmente conexo
        if (start == -1) return true;

        Set<Integer> visited = dfs(g, start);

        for (int v = 0; v < n; v++) {
            if (g.degree(v) > 0 && !visited.contains(v)) return false;
        }
        return true;
    }

    /**
     * Classifica o tipo euleriano do grafo:
     * - EULERIAN: circuito euleriano existe (todos vértices com grau par, conexo)
     * - SEMI_EULERIAN: caminho euleriano existe (exatamente 2 vértices com grau ímpar, conexo)
     * - NON_EULERIAN: não existe caminho nem circuito euleriano
     *
     * Teorema de Euler:
     * - Circuito euleriano ↔ grafo conexo e todos os graus pares
     * - Caminho euleriano ↔ grafo conexo e exatamente 2 vértices de grau ímpar
     */
    public static EulerianType classifyGraph(Graph g) {
        if (!isConnectedIgnoringIsolated(g)) return EulerianType.NON_EULERIAN;

        List<Integer> oddVertices = g.getOddDegreeVertices();
        int oddCount = oddVertices.size();

        if (oddCount == 0) return EulerianType.EULERIAN;
        if (oddCount == 2) return EulerianType.SEMI_EULERIAN;
        return EulerianType.NON_EULERIAN;
    }

    /**
     * Enum para o tipo euleriano do grafo.
     */
    public enum EulerianType {
        EULERIAN,       // circuito euleriano
        SEMI_EULERIAN,  // caminho euleriano (sem circuito)
        NON_EULERIAN    // nem caminho nem circuito
    }

    /**
     * Cópia profunda do grafo. Útil quando o algoritmo é destrutivo
     * (Naïve.findBridges, Fleury) e queremos preservar o original.
     */
    public static Graph copyGraph(Graph g) {
        Graph c = new Graph(g.getNumVertices());
        int n = g.getNumVertices();
        for (int u = 0; u < n; u++) {
            for (int v : g.getNeighbors(u)) {
                if (u < v) c.addEdge(u, v); // evita duplicar (u,v) e (v,u)
            }
        }
        return c;
    }

    /**
     * Chave canônica de uma aresta não-direcionada empacotada em long
     * (32 bits para min, 32 para max). Substitui concatenação de string.
     */
    public static long edgeKey(int u, int v) {
        int a = Math.min(u, v);
        int b = Math.max(u, v);
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }
}