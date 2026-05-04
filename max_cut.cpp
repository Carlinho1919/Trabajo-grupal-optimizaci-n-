/*
 21:maxcut
 C++:
   - Solución Base   : Fuerza Bruta (bitmask 2^n)
   - Solución Mejorada: Greedy + Búsqueda Local
 Compilar: g++ -O2 -std=c++17 -o max_cut max_cut.cpp

*/

#include <bits/stdc++.h>
using namespace std;

//grafo


struct Edge {
    int u, v;
    double w;
};

struct Graph {
    int n;
    vector<Edge> edges;
    vector<vector<pair<int,double>>> adj;

    Graph(int n) : n(n), adj(n) {}

    void add_edge(int u, int v, double w = 1.0) {
        edges.push_back({u, v, w});
        adj[u].push_back({v, w});
        adj[v].push_back({u, w});
    }

    double cut_weight(uint64_t mask) const {
        double total = 0.0;
        for (auto& e : edges) {
            bool uInS = (mask >> e.u) & 1;
            bool vInS = (mask >> e.v) & 1;
            if (uInS != vInS) total += e.w;
        }
        return total;
    }

    double cut_weight(const vector<bool>& inS) const {
        double total = 0.0;
        for (auto& e : edges)
            if (inS[e.u] != inS[e.v]) total += e.w;
        return total;
    }
};

Graph random_graph(int n, double density = 0.5, int seed = 42) {
    mt19937 rng(seed);
    uniform_real_distribution<double> prob(0.0, 1.0);
    uniform_real_distribution<double> weight(1.0, 10.0);
    Graph g(n);
    for (int u = 0; u < n; u++)
        for (int v = u+1; v < n; v++)
            if (prob(rng) < density)
                g.add_edge(u, v, round(weight(rng) * 100) / 100.0);
    return g;
}

//fuerza bruta

pair<uint64_t, double> max_cut_brute_force(const Graph& g) {
    // Complejidad: O(2^n * |E|)
    uint64_t best_mask = 0;
    double   best_val  = 0.0;
    uint64_t total = 1ULL << g.n;

    for (uint64_t mask = 0; mask < total; mask++) {
        double val = g.cut_weight(mask);
        if (val > best_val) {
            best_val  = val;
            best_mask = mask;
        }
    }
    return {best_mask, best_val};
}

//greedy

pair<vector<bool>, double> greedy_max_cut(const Graph& g) {
    // Asigna cada vértice al lado que maximiza la contribución inmediata
    // Complejidad: O(n * deg_avg)
    vector<bool> inS(g.n, false);

    for (int v = 0; v < g.n; v++) {
        double gainS = 0.0, gainT = 0.0;
        for (auto [u, w] : g.adj[v]) {
            if (inS[u])  gainS += w;  // u en S → contribuye si v va a T
            else         gainT += w;  // u en T → contribuye si v va a S
        }
        inS[v] = (gainT >= gainS);
    }
    return {inS, g.cut_weight(inS)};
}
//busqueda local

pair<vector<bool>, double> local_search_max_cut(const Graph& g, int restarts = 5, int seed = 42) {
    // Complejidad por reinicio: O(iter * n * deg_avg)
    mt19937 rng(seed);
    uniform_int_distribution<int> flip(0, 1);

    auto one_flip_gain = [&](const vector<bool>& inS, int v) -> double {
        double gain = 0.0;
        for (auto [u, w] : g.adj[v]) {
            bool samePartition = (inS[u] == inS[v]);
            gain += samePartition ? w : -w;
        }
        return gain;
    };

    vector<bool> best_inS(g.n, false);
    double       best_val_global = 0.0;

    for (int r = 0; r < restarts; r++) {
        vector<bool> inS(g.n);
        if (r == 0) {
            inS = greedy_max_cut(g).first;
        } else {
            for (int i = 0; i < g.n; i++) inS[i] = flip(rng);
        }

        bool improved = true;
        while (improved) {
            improved = false;
            double best_gain = 1e-9;
            int    best_v    = -1;
            for (int v = 0; v < g.n; v++) {
                double gain = one_flip_gain(inS, v);
                if (gain > best_gain) {
                    best_gain = gain;
                    best_v    = v;
                }
            }
            if (best_v != -1) {
                inS[best_v] = !inS[best_v];
                improved = true;
            }
        }

        double val = g.cut_weight(inS);
        if (val > best_val_global) {
            best_val_global = val;
            best_inS        = inS;
        }
    }
    return {best_inS, best_val_global};
}

//utilidades

using hrc = chrono::high_resolution_clock;
using dsec = chrono::duration<double>;

template<typename F>
double timed(F&& f) {
    auto t0 = hrc::now();
    f();
    return dsec(hrc::now() - t0).count();
}

void print_set(const vector<bool>& inS) {
    cout << "{";
    bool first = true;
    for (int i = 0; i < (int)inS.size(); i++)
        if (inS[i]) { if (!first) cout << ","; cout << i; first = false; }
    cout << "}";
}

//analisis experimental
int main() {
    cout << string(65, '=') << "\n";
    cout << "  MAX CUT - Análisis Experimental (C++)\n";
    cout << string(65, '=') << "\n\n";

    //test de correctitud (n = 6)
    cout << "[1] Test de correctitud (n = 6):\n";
    Graph g_small(6);
    vector<tuple<int,int,double>> edges_s = {{0,1,3},{0,2,1},{1,2,2},{1,3,4},{2,4,2},{3,4,5},{3,5,3},{4,5,1}};
    for (auto [u,v,w] : edges_s) g_small.add_edge(u,v,w);

    auto [mask_bf, val_bf] = max_cut_brute_force(g_small);
    auto [inS_gr, val_gr]  = greedy_max_cut(g_small);
    auto [inS_ls, val_ls]  = local_search_max_cut(g_small, 3);

    cout << "  Fuerza Bruta : ";
    for (int i = 0; i < g_small.n; i++) if ((mask_bf>>i)&1) cout << i << " ";
    cout << "| corte=" << val_bf << "\n";
    cout << "  Greedy       : "; print_set(inS_gr); cout << " | corte=" << val_gr << "\n";
    cout << "  Búsq. Local  : "; print_set(inS_ls); cout << " | corte=" << val_ls << "\n";
    cout << "  Ratio Greedy vs Óptimo: " << fixed << setprecision(3) << val_gr/val_bf << "\n";
    cout << "  Ratio Local  vs Óptimo: " << val_ls/val_bf << "\n\n";

    //tiempo de ejecucion
    cout << "[2] Tiempos de ejecución (fuerza bruta vs mejorado):\n";
    cout << "  " << setw(5) << "n" << " | " << setw(6) << "|E|"
         << " | " << setw(18) << "Fuerza Bruta (s)"
         << " | " << setw(12) << "Greedy (s)"
         << " | " << setw(16) << "Local Search (s)" << "\n";
    cout << "  " << string(63, '-') << "\n";

    vector<int> bf_sizes = {6, 8, 10, 12, 14, 16, 18};
    for (int n : bf_sizes) {
        Graph g = random_graph(n, 0.5, 7);
        int ne = g.edges.size();

        double t_bf = timed([&]{ max_cut_brute_force(g); });
        double t_gr = timed([&]{ greedy_max_cut(g); });
        double t_ls = timed([&]{ local_search_max_cut(g, 3); });

        cout << "  " << setw(5) << n << " | " << setw(6) << ne
             << " | " << setw(18) << scientific << setprecision(6) << t_bf
             << " | " << setw(12) << t_gr
             << " | " << setw(16) << t_ls << "\n";
    }

    cout << "\n";
    cout << "  " << setw(5) << "n" << " | " << setw(6) << "|E|"
         << " | " << setw(14) << "Greedy (s)"
         << " | " << setw(16) << "Local Search (s)" << "\n";
    cout << "  " << string(45, '-') << "\n";

    vector<int> imp_sizes = {50, 100, 200, 500, 1000, 2000};
    for (int n : imp_sizes) {
        Graph g = random_graph(n, 0.4, 7);
        int ne = g.edges.size();
        double t_gr = timed([&]{ greedy_max_cut(g); });
        double t_ls = timed([&]{ local_search_max_cut(g, 5); });
        cout << "  " << setw(5) << n << " | " << setw(6) << ne
             << " | " << setw(14) << scientific << setprecision(6) << t_gr
             << " | " << setw(16) << t_ls << "\n";
    }

    //calidad de solucion 
    cout << "\n[3] Calidad de solución (ratio vs óptimo, n=6..18):\n";
    cout << "  " << setw(5) << "n"
         << " | " << setw(10) << "Óptimo"
         << " | " << setw(10) << "Greedy"
         << " | " << setw(10) << "Ratio Gr"
         << " | " << setw(10) << "Local S"
         << " | " << setw(10) << "Ratio LS" << "\n";
    cout << "  " << string(65, '-') << "\n";

    for (int n : bf_sizes) {
        Graph g = random_graph(n, 0.5, 99);
        auto [_, v_opt] = max_cut_brute_force(g);
        auto [_2, v_gr2] = greedy_max_cut(g);
        auto [_3, v_ls2] = local_search_max_cut(g, 5);
        cout << "  " << setw(5) << n
             << " | " << setw(10) << fixed << setprecision(2) << v_opt
             << " | " << setw(10) << v_gr2
             << " | " << setw(10) << setprecision(3) << v_gr2/v_opt
             << " | " << setw(10) << setprecision(2) << v_ls2
             << " | " << setw(10) << setprecision(3) << v_ls2/v_opt << "\n";
    }

    cout << "\n[✓] Experimento completado.\n";
    return 0;
}
