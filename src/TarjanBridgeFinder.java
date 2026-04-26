import java.util.*;

/**
 * Implementação do algoritmo de Tarjan (1974) para encontrar pontes.
 * Versão iterativa (simulação da DFS recursiva com pilha explícita)
 * Implementada para evitar Stack Overflow
 * Complexidade: O(V + E)
 */

class Stack {
	private int[] values;
	private int top;
	
	public Stack(int size) {
		values = new int[size];
		top = -1;
	}

	public void push(int value) {
		top += 1;
		values[top] = value;
	}

	public int pop() {
		int value = values[top];
		top -= 1;
		return value;
	}

	public int peek() {
		return values[top];
	}

	public boolean isEmpty() {
		return top == -1;
	}
}

public class TarjanBridgeFinder implements BridgeFinder {

    private int time;

    // Cache para acelerar isBridge() quando chamado múltiplas vezes
    // sobre o mesmo grafo (ex.: Fleury). Invalidado pela versão do grafo.
    private Graph    cacheGraph   = null;
    private long     cacheVersion = -1;
    private Set<Long> cacheBridgeKeys = null;

    @Override
    public List<int[]> findBridges(Graph g) {

        int n = g.getNumVertices(); // let Gi have Vi vertices;

        boolean[] visited = new boolean[n];
        int[] disc = new int[n];   // discovery time
        int[] low = new int[n];    // L(v)
        int[] parent = new int[n]; // spanning tree T

        Arrays.fill(parent, -1);

        List<int[]> bridges = new ArrayList<>();
        time = 0;

		// for each connected component Gi of G do begin
        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;

			// find a spanning tree T of Gi;
            Stack stack = new Stack(2 * n);
            stack.push(start);

            while (!stack.isEmpty()) {
                int u = stack.peek();
				// number the vertices of T in postorder; (preorder actually)
                if (!visited[u]) {
                    visited[u] = true;
                    disc[u] = low[u] = ++time;
                }

                boolean finished = true;
				// for v := 1 until V do begin
                for (int v : g.getNeighbors(u)) {
                    if (v == parent[u]) continue;
                    if (!visited[v]) {
						// convert T to a directed, rooted tree T;
                        parent[v] = u;
                        stack.push(v);
                        finished = false;
                        break;
                    } else {
						// {w | v -- w}
                        low[u] = Math.min(low[u], disc[v]);
                    }
                }

                if (finished) {

                    stack.pop();
                    if (parent[u] != -1) {
                        int p = parent[u];
						// ND(v) := 1 + sum(ND(w));
						// L(v) := min{v-ND(v)} U {L(w)|v->w} U {w|v--w};
						// H(v) := max{v} U {H(w)|v->w} U {w|v--w};
                        low[p] = Math.min(low[p], low[u]); // L(v)
						// for v -> w do if H(w) <= w and L(w) > w - ND(w) then
                        if (low[u] > disc[p]) {
                            bridges.add(new int[]{p, u}); // denote (v, w) a bridge;
                        }
                    }
                }
            }
        }

        return bridges;
    }

    /**
     * Verificação de ponte individual.
     *
     * Otimização: usa cache invalidado pela versão do grafo. Em algoritmos
     * que chamam isBridge várias vezes entre modificações (ex.: Fleury
     * iterando vizinhos do vértice atual), só recomputa findBridges quando
     * o grafo muda — reduz Fleury+Tarjan de O(deg·E·(V+E)) para O(E·(V+E)).
     */
    @Override
    public boolean isBridge(Graph g, int u, int v) {
        if (g != cacheGraph || g.getVersion() != cacheVersion) {
            cacheGraph = g;
            cacheVersion = g.getVersion();
            cacheBridgeKeys = new HashSet<>();
            for (int[] e : findBridges(g)) {
                cacheBridgeKeys.add(GraphUtils.edgeKey(e[0], e[1]));
            }
        }
        return cacheBridgeKeys.contains(GraphUtils.edgeKey(u, v));
    }

    @Override
    public String getName() {
        return "Tarjan (O(V+E))";
    }
}
