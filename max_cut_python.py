"""

21: maxCut
Python:
  - Solución Base: Fuerza Bruta (2^n subconjuntos)
  - Solución Mejorada: Greedy + Búsqueda Local
"""

import random
import time
import itertools
from typing import List, Tuple, Dict, Set

#grafo

class Graph:
    """Grafo ponderado no dirigido representado como lista de adyacencia."""

    def __init__(self, n: int):
        self.n = n  # número de vértices (0..n-1)
        self.adj: Dict[int, List[Tuple[int, float]]] = {i: [] for i in range(n)}
        self.edges: List[Tuple[int, int, float]] = []

    def add_edge(self, u: int, v: int, w: float = 1.0):
        if u != v:
            self.adj[u].append((v, w))
            self.adj[v].append((u, w))
            self.edges.append((u, v, w))

    def cut_weight(self, S: Set[int]) -> float:
        """Calcula el peso del corte entre S y V\\S."""
        total = 0.0
        for u, v, w in self.edges:
            if (u in S) != (v in S):
                total += w
        return total

    @staticmethod
    def random_graph(n: int, density: float = 0.5, max_weight: float = 10.0, seed: int = 42) -> 'Graph':
        """Genera un grafo aleatorio con n vértices y densidad dada."""
        random.seed(seed)
        g = Graph(n)
        for u in range(n):
            for v in range(u + 1, n):
                if random.random() < density:
                    w = round(random.uniform(1.0, max_weight), 2)
                    g.add_edge(u, v, w)
        return g

#fuerza Bruta

def max_cut_brute_force(g: Graph) -> Tuple[Set[int], float]:
    """
    Fuerza bruta: enumera los 2^n subconjuntos de V y retorna el corte
    de peso máximo.
    Complejidad: O(2^n * |E|)
    Solo viable para n <= 25 aprox.
    """
    best_S: Set[int] = set()
    best_val: float = 0.0

    vertices = list(range(g.n))
    # Iteramos solo sobre la mitad para evitar duplicados simétricos
    for r in range(g.n + 1):
        for subset in itertools.combinations(vertices, r):
            S = set(subset)
            val = g.cut_weight(S)
            if val > best_val:
                best_val = val
                best_S = S

    return best_S, best_val

#greedy y busqueda local

def greedy_max_cut(g: Graph) -> Tuple[Set[int], float]:
    """
    Algoritmo Greedy: asigna cada vértice al lado del corte que
    maximiza la contribución inmediata al corte.
    Garantía de aproximación: ≥ W/2 (donde W es el peso total de aristas).
    Complejidad: O(n * |E|)
    """
    S: Set[int] = set()
    T: Set[int] = set()

    for v in range(g.n):
        # Ganancia si v va a S
        gain_S = sum(w for u, w in g.adj[v] if u in T)
        # Ganancia si v va a T
        gain_T = sum(w for u, w in g.adj[v] if u in S)

        if gain_S >= gain_T:
            S.add(v)
        else:
            T.add(v)

    return S, g.cut_weight(S)


def local_search_max_cut(g: Graph, max_iter: int = 1000, restarts: int = 5, seed: int = 42) -> Tuple[Set[int], float]:
    """
    Búsqueda Local con múltiples reinicios aleatorios:
    - Parte de una partición aleatoria (o la greedy).
    - En cada iteración, mueve el vértice que más mejore el corte.
    - Repite hasta no haber mejoras (óptimo local).
    Complejidad por reinicio: O(iter * n * deg_avg)
    """
    random.seed(seed)

    def one_flip_gain(S: Set[int], v: int) -> float:
        """Ganancia neta de mover v al otro lado del corte."""
        gain = 0.0
        for u, w in g.adj[v]:
            if u in S:
                # arista (v,u) actualmente no cruza (ambos en S) → cruzará si v sale
                if v in S:
                    gain += w
                # arista (v,u) actualmente cruza (v no en S, u en S) → dejará de cruzar si v entra
                else:
                    gain -= w
            else:
                # arista (v,u) actualmente cruza (v en S, u no en S) → dejará de cruzar si v sale
                if v in S:
                    gain -= w
                # arista (v,u) actualmente no cruza → cruzará si v entra
                else:
                    gain += w
        return gain

    best_S_global: Set[int] = set()
    best_val_global: float = 0.0

    for _ in range(restarts):
        # Inicialización: partición aleatoria o greedy en primer reinicio
        if _ == 0:
            S, _ = greedy_max_cut(g)
        else:
            S = set(v for v in range(g.n) if random.random() < 0.5)

        improved = True
        iters = 0
        while improved and iters < max_iter:
            improved = False
            iters += 1
            best_gain = 1e-9  # umbral mínimo de mejora
            best_v = -1
            for v in range(g.n):
                gain = one_flip_gain(S, v)
                if gain > best_gain:
                    best_gain = gain
                    best_v = v
            if best_v != -1:
                if best_v in S:
                    S.remove(best_v)
                else:
                    S.add(best_v)
                improved = True

        val = g.cut_weight(S)
        if val > best_val_global:
            best_val_global = val
            best_S_global = set(S)

    return best_S_global, best_val_global

#analisis experimental

def run_experiment():
    print("=" * 65)
    print("  MAX CUT - Análisis Experimental (Python)")
    print("=" * 65)

    #test de correctitud con n pequeño
    print("\n[1] Test de correctitud (n = 6):")
    g_small = Graph(6)
    edges_small = [(0,1,3),(0,2,1),(1,2,2),(1,3,4),(2,4,2),(3,4,5),(3,5,3),(4,5,1)]
    for u, v, w in edges_small:
        g_small.add_edge(u, v, w)

    s_bf, v_bf = max_cut_brute_force(g_small)
    s_gr, v_gr = greedy_max_cut(g_small)
    s_ls, v_ls = local_search_max_cut(g_small, restarts=3)

    print(f"  Fuerza Bruta : S={sorted(s_bf)}, corte={v_bf:.2f}")
    print(f"  Greedy       : S={sorted(s_gr)}, corte={v_gr:.2f}")
    print(f"  Búsq. Local  : S={sorted(s_ls)}, corte={v_ls:.2f}")
    opt_ratio_gr = v_gr / v_bf if v_bf > 0 else 1.0
    opt_ratio_ls = v_ls / v_bf if v_bf > 0 else 1.0
    print(f"  Ratio Greedy vs Óptimo : {opt_ratio_gr:.3f}")
    print(f"  Ratio Local  vs Óptimo : {opt_ratio_ls:.3f}")

    #comparacion de tiempos en base a tamaño
    print("\n[2] Comparación de tiempos de ejecución:")
    print(f"  {'n':>5} | {'|E|':>6} | {'Fuerza Bruta (s)':>18} | {'Greedy (s)':>12} | {'Local Search (s)':>16}")
    print("  " + "-" * 65)

    bf_sizes = [6, 8, 10, 12, 14, 16]
    improved_sizes = [10, 20, 50, 100, 200, 500]

    #fuerza bruta (n pequeños)
    for n in bf_sizes:
        g = Graph.random_graph(n, density=0.5, seed=7)
        ne = len(g.edges)

        t0 = time.perf_counter()
        max_cut_brute_force(g)
        t_bf = time.perf_counter() - t0

        t0 = time.perf_counter()
        greedy_max_cut(g)
        t_gr = time.perf_counter() - t0

        t0 = time.perf_counter()
        local_search_max_cut(g, restarts=3)
        t_ls = time.perf_counter() - t0

        print(f"  {n:>5} | {ne:>6} | {t_bf:>18.6f} | {t_gr:>12.6f} | {t_ls:>16.6f}")

    print()
    print(f"  {'n':>5} | {'|E|':>6} | {'Greedy (s)':>12} | {'Local Search (s)':>16}")
    print("  " + "-" * 40)

    #mejorados de n grande
    for n in improved_sizes:
        g = Graph.random_graph(n, density=0.4, seed=7)
        ne = len(g.edges)

        t0 = time.perf_counter()
        greedy_max_cut(g)
        t_gr = time.perf_counter() - t0

        t0 = time.perf_counter()
        local_search_max_cut(g, restarts=5)
        t_ls = time.perf_counter() - t0

        print(f"  {n:>5} | {ne:>6} | {t_gr:>12.6f} | {t_ls:>16.6f}")

    #calidad de solucion
    print("\n[3] Calidad de solución (ratio vs óptimo, n=6..16):")
    print(f"  {'n':>5} | {'Óptimo':>10} | {'Greedy':>10} | {'Ratio Gr':>10} | {'Local S':>10} | {'Ratio LS':>10}")
    print("  " + "-" * 65)
    for n in bf_sizes:
        g = Graph.random_graph(n, density=0.5, seed=99)
        _, v_opt = max_cut_brute_force(g)
        _, v_gr2 = greedy_max_cut(g)
        _, v_ls2 = local_search_max_cut(g, restarts=5)
        r_gr = v_gr2 / v_opt if v_opt > 0 else 1.0
        r_ls = v_ls2 / v_opt if v_opt > 0 else 1.0
        print(f"  {n:>5} | {v_opt:>10.2f} | {v_gr2:>10.2f} | {r_gr:>10.3f} | {v_ls2:>10.2f} | {r_ls:>10.3f}")

    print("\n[✓] Experimento completado.")


if __name__ == "__main__":
    run_experiment()
