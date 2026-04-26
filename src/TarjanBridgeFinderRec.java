import java.util.*;

/**
 * Implementação do algoritmo de Tarjan (1974) para encontrar pontes.
 * Versão recursiva (não utilizada para evitar Stack Overflow)
 * Implementado apenas para entendimento do algoritmo
 * Complexidade: O(V + E)
 */
public class TarjanBridgeFinderRec implements BridgeFinder {

    private int time; // DFS counting

    @Override
    public List<int[]> findBridges(Graph g) {
        int n = g.getNumVertices(); // let G have V vertices;

        boolean[] visited = new boolean[n];
        int[] disc = new int[n]; // discovery time
		// int[] ND = new int[n];
		// int[] L = new int[n];
		// int[] H = new int[n];
		int[] low = new int[n]; // L(v)
        int[] parent = new int[n]; // spanning tree T

        Arrays.fill(parent, -1);

        List<int[]> bridges = new ArrayList<>();
        time = 0;
		
		// for each connected component Gi of G do begin
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfs(g, i, visited, disc, low, parent, bridges); // find a spanning tree T of Gl;
            }
        }

        return bridges;
    }

    private void dfs(Graph g, int u, boolean[] visited, int[] disc,
    int[] low, int[] parent, List<int[]> bridges) {

        visited[u] = true;
        // number the vertices of T in postorder (using preorder actually);
		time++;
		low[u] = time;
		disc[u] = low[u];
		
		// ND[u] = 1;				// ND(v) := 1
		// L[u] = disc[u];			// L(v)
		// H[u] = disc[u];			// H(v)

		// for v := 1 until V1 do begin
        for (int v : g.getNeighbors(u)) {

            if (!visited[v]) {
                parent[v] = u; // convert T to a directed, rooted tree T;

                dfs(g, v, visited, disc, low, parent, bridges);

                // ND(v) := 1 + sum(ND(w);
				// ND[u] += ND[v];

				// L(v) :+ min{v-ND(v)} U {L(w)|v->w} U {w|v--w};
				// L[u] = Math.min(L[u], L[v]);              

				// H(v) := max{v} U {H(w) | v -> w} U {w|v--w};
				// H[u] = Math.max(H[u], H[v]);
				low[u] = Math.min(low[u], low[v]);

                // for v -> w do if H(w) <= w and L(w) > w - ND(w) then
                // if(H[v] <= disc[v] && L[v] > disc[v] - ND[v]) {
				if(low[v] > disc[u]) {
					bridges.add(new int[]{u, v}); // denote (v,w) a bridge;
                }
			// {w|v--w}
            } else if (v != parent[u]) {
            	// L[u] = Math.min(L[u], disc[v]);
				// H[u] = Math.max(H[u], disc[v]);
				low[u] = Math.min(low[u], disc[v]);
			}
        }
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
