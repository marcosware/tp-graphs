# TP01 — Teoria dos Grafos e Computabilidade

Detecção de pontes (naïve e Tarjan) e algoritmo de Fleury para caminho/circuito euleriano em grafos simples não-direcionados.

PUC Minas — Ciência da Computação — Disciplina: Teoria dos Grafos e Computabilidade — Prof. Zenilton Kleber Gonçalves do Patrocínio Júnior.

## Como usar

```bash
cd src

# Compilar
javac *.java

# Modo interativo (menu)
java Main

# Bateria de experimentos (gera tabela de tempos + CSV)
java Main --experiments

# Mesma bateria, mas pulando o naïve em N grande (recomendado)
java Main --experiments-safe

# Ajuda
java Main --help
```

Para grafos grandes vale subir os limites de stack e heap da JVM:

```bash
java -Xss64m -Xmx2g Main --experiments-safe
```

## Layout do repositório

```
tp-graphs/
├── src/            # códigos-fonte Java
├── relatorio/      # relatorio.tex e relatorio.pdf
├── resultados/     # CSV gerado pelos experimentos
└── README.md
```

### Conteúdo de `src/`

| Arquivo | Responsabilidade |
| --- | --- |
| `Graph.java` | Lista de adjacência, contador de versão e de arestas |
| `GraphUtils.java` | DFS, BFS, conectividade, classificação euleriana, helpers |
| `GraphGenerator.java` | Gera grafos aleatórios eulerianos / semi / não-eulerianos |
| `BridgeFinder.java` | Interface (Strategy) para detecção de pontes |
| `NaiveBridgeFinder.java` | Naïve: remove + testa + reinsere |
| `TarjanBridgeFinder.java` | Tarjan iterativo (1974) com cache versionado |
| `TarjanBridgeFinderRec.java` | Tarjan recursivo (apenas didático) |
| `FleuryEulerPath.java` | Fleury parametrizado por `BridgeFinder` |
| `Experiments.java` | Bateria de experimentos → CSV + tabelas |
| `Main.java` | Menu interativo + CLI batch |

## Resultados

Os experimentos salvam um CSV em `resultados/experimentos.csv` com colunas:

```
algoritmo,estrategia,n,tipo,arestas,tempo_medio_ms,executado
```

O relatório está em `relatorio/relatorio.pdf` (gerado com `pdflatex relatorio.tex`).

## Divisão do trabalho

- **Pessoa 1:** estrutura de dados (`Graph`), DFS/BFS, conectividade, naïve para pontes.
- **Pessoa 2:** Tarjan (recursivo e iterativo), gerador de grafos, classificação euleriana.
- **Pessoa 3:** Fleury, experimentos, relatório.
