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
     * Para simplificar (e manter corretude), reutiliza findBridges.
     */
    @Override
    public boolean isBridge(Graph g, int u, int v) {
        for (int[] e : findBridges(g)) {
            if ((e[0] == u && e[1] == v) || (e[0] == v && e[1] == u)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "Tarjan (O(V+E))";
    }
}
