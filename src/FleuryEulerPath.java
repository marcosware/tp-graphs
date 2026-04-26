import java.util.*;

/**
 * Algoritmo de Fleury para caminho/circuito Euleriano.
 *
 * Recebe a estratégia de detecção de pontes via BridgeFinder. O algoritmo
 * em si é o clássico: prefira sempre uma aresta que não seja ponte; só
 * atravesse uma ponte como último recurso. Trabalha em uma cópia do grafo.
 *
 * Custo: O(E * (V+E)) com qualquer das duas estratégias. Fleury não é ótimo
 * pra esse problema (Hierholzer resolve em O(V+E)), mas é o que o enunciado
 * pede e tem o mérito de tornar explícita a importância das pontes.
 */
public class FleuryEulerPath {

    private final BridgeFinder bridgeFinder;

    public FleuryEulerPath(BridgeFinder bridgeFinder) {
        this.bridgeFinder = bridgeFinder;
    }

    /**
     * Encontra um caminho/circuito Euleriano no grafo.
     * O grafo original NÃO é modificado.
     */
    public Result findPath(Graph original) {
        long startNs = System.nanoTime();

        GraphUtils.EulerianType type = GraphUtils.classifyGraph(original);

        if (type == GraphUtils.EulerianType.NON_EULERIAN) {
            long elapsed = System.nanoTime() - startNs;
            return new Result(false, type, Collections.emptyList(),
                              0, 0, original.getNumEdges(), elapsed,
                              bridgeFinder.getName());
        }

        Graph g = GraphUtils.copyGraph(original);
        int start = chooseStart(g, type);

        List<Integer> path = new ArrayList<>(g.getNumEdges() + 1);
        path.add(start);

        long bridgeChecks = 0;
        long edgesTraversed = 0;
        int totalEdges = g.getNumEdges();
        int u = start;

        while (g.degree(u) > 0) {
            List<Integer> neighbors = g.getNeighbors(u);
            int next = -1;

            if (neighbors.size() == 1) {
                next = neighbors.get(0);
            } else {
                for (int v : neighbors) {
                    bridgeChecks++;
                    if (!bridgeFinder.isBridge(g, u, v)) {
                        next = v;
                        break;
                    }
                }
                // todas pontes — caso degenerado, pega a primeira
                if (next == -1) next = neighbors.get(0);
            }

            g.removeEdge(u, next);
            path.add(next);
            u = next;
            edgesTraversed++;
        }

        long elapsed = System.nanoTime() - startNs;
        boolean success = (edgesTraversed == totalEdges);

        return new Result(success, type, path, bridgeChecks,
                          edgesTraversed, totalEdges, elapsed,
                          bridgeFinder.getName());
    }

    /**
     * Escolhe vértice inicial conforme tipo do grafo.
     */
    private int chooseStart(Graph g, GraphUtils.EulerianType type) {
        if (type == GraphUtils.EulerianType.SEMI_EULERIAN) {
            List<Integer> odd = g.getOddDegreeVertices();
            if (!odd.isEmpty()) return odd.get(0);
        }
        for (int v = 0; v < g.getNumVertices(); v++) {
            if (g.degree(v) > 0) return v;
        }
        return 0;
    }

    /**
     * Resultado da execução do Fleury — encapsula caminho + métricas.
     */
    public static class Result {
        public final boolean exists;
        public final GraphUtils.EulerianType type;
        public final List<Integer> path;
        public final long bridgeChecks;
        public final long edgesTraversed;
        public final long edgesTotal;
        public final long elapsedNanos;
        public final String strategyName;

        public Result(boolean exists, GraphUtils.EulerianType type,
                      List<Integer> path, long bridgeChecks,
                      long edgesTraversed, long edgesTotal,
                      long elapsedNanos, String strategyName) {
            this.exists = exists;
            this.type = type;
            this.path = path;
            this.bridgeChecks = bridgeChecks;
            this.edgesTraversed = edgesTraversed;
            this.edgesTotal = edgesTotal;
            this.elapsedNanos = elapsedNanos;
            this.strategyName = strategyName;
        }

        public double elapsedMillis() { return elapsedNanos / 1_000_000.0; }
        public double elapsedSeconds() { return elapsedNanos / 1_000_000_000.0; }

        /**
         * Resumo curto (1 linha) para tabelas e logs.
         */
        public String summary() {
            if (!exists) {
                return String.format("[%s] NÃO existe caminho Euleriano (tipo=%s)",
                    strategyName, type);
            }
            return String.format(
                "[%s] %s | path=%d vert | edges=%d/%d | bridge-checks=%d | %.3f ms",
                strategyName, type, path.size(),
                edgesTraversed, edgesTotal, bridgeChecks, elapsedMillis());
        }
    }
}
