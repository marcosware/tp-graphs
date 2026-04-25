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
     * Verifica se o grafo é conexo.
     *
     * Estratégia: encontra o primeiro vértice com grau > 0 e faz DFS.
     * Se o número de vértices alcançados for igual ao total de vértices
     * com grau > 0, o grafo é conexo.
     *
     * Vértices isolados (grau 0) são ignorados na verificação de conectividade
     * para fins de existência de caminho euleriano.
     */
    public static boolean isConnected(Graph g) {
        int n = g.getNumVertices();

        // Encontra um vértice com grau > 0 como ponto de partida
        int start = -1;
        for (int v = 0; v < n; v++) {
            if (g.degree(v) > 0) {
                start = v;
                break;
            }
        }

        // Grafo sem arestas é trivialmente conexo (ou vazio)
        if (start == -1) return true;

        Set<Integer> visited = dfs(g, start);

        // Verifica se todos os vértices com grau > 0 foram alcançados
        for (int v = 0; v < n; v++) {
            if (g.degree(v) > 0 && !visited.contains(v)) {
                return false;
            }
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
        if (!isConnected(g)) return EulerianType.NON_EULERIAN;

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
}
