import java.util.List;

/**
 * Interface comum para algoritmos de identificação de pontes.
 *
 * Permite trocar a estratégia (Naïve ou Tarjan) sem alterar
 * o código do algoritmo de Fleury — padrão Strategy.
 *
 * Ambas as implementações devem:
 * - findBridges: retornar todas as pontes do grafo
 * - isBridge: verificar se uma aresta específica é ponte
 *   (versão mais eficiente para uso dentro do Fleury)
 */
public interface BridgeFinder {

    /**
     * Encontra todas as pontes do grafo.
     * @return lista de arestas [u, v] que são pontes
     */
    List<int[]> findBridges(Graph g);

    /**
     * Verifica se a aresta (u, v) é ponte no estado atual do grafo.
     * Chamada a cada passo do algoritmo de Fleury.
     */
    boolean isBridge(Graph g, int u, int v);

    /**
     * Nome da estratégia, para identificação nos experimentos.
     */
    String getName();
}
