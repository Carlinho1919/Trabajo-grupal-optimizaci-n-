/*
 21:maxcut
 java:
   - Solución Base    : Fuerza Bruta (bitmask 2^n)
   - Solución Mejorada: Greedy + Búsqueda Local
 Compilar: javac MaxCut.java
 Ejecutar: java MaxCut
*/

import java.util.*;

public class MaxCut {

    //grafo

    static class Edge {
        int u, v;
        double w;
        Edge(int u, int v, double w) { this.u = u; this.v = v; this.w = w; }
    }

    static class Graph {
        int n;
        List<Edge> edges = new ArrayList<>();
        List<List<int[]>> adj;   // adj[v] = lista de {vecino, peso*100}

        Graph(int n) {
            this.n = n;
            adj = new ArrayList<>();
            for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        }

        void addEdge(int u, int v, double w) {
            edges.add(new Edge(u, v, w));
            adj.get(u).add(new int[]{v, (int)(w * 100)});
            adj.get(v).add(new int[]{u, (int)(w * 100)});
        }

        double cutWeight(long mask) {
            double total = 0.0;
            for (Edge e : edges) {
                boolean uInS = ((mask >> e.u) & 1) == 1;
                boolean vInS = ((mask >> e.v) & 1) == 1;
                if (uInS != vInS) total += e.w;
            }
            return total;
        }

        double cutWeight(boolean[] inS) {
            double total = 0.0;
            for (Edge e : edges)
                if (inS[e.u] != inS[e.v]) total += e.w;
            return total;
        }

        static Graph randomGraph(int n, double density, int seed) {
            Random rng = new Random(seed);
            Graph g = new Graph(n);
            for (int u = 0; u < n; u++)
                for (int v = u+1; v < n; v++)
                    if (rng.nextDouble() < density) {
                        double w = Math.round((1.0 + rng.nextDouble() * 9.0) * 100.0) / 100.0;
                        g.addEdge(u, v, w);
                    }
            return g;
        }
    }

    //fuerza bruta

    static long[] maxCutBruteForce(Graph g) {
        // Complejidad: O(2^n * |E|)
        long bestMask = 0;
        double bestVal = 0.0;
        long total = 1L << g.n;

        for (long mask = 0; mask < total; mask++) {
            double val = g.cutWeight(mask);
            if (val > bestVal) {
                bestVal = val;
                bestMask = mask;
            }
        }
        // Devuelve [mask, val*1000] (enteros para simplicidad)
        return new long[]{bestMask, (long)(bestVal * 1000)};
    }

    //greedy

    static boolean[] greedyMaxCut(Graph g) {
        // Complejidad: O(n * deg_avg)
        boolean[] inS = new boolean[g.n];
        for (int v = 0; v < g.n; v++) {
            double gainS = 0.0, gainT = 0.0;
            for (int[] nb : g.adj.get(v)) {
                double w = nb[1] / 100.0;
                if (inS[nb[0]]) gainS += w;
                else            gainT += w;
            }
            inS[v] = (gainT >= gainS);
        }
        return inS;
    }

    //busqueda local
    static double oneFlipGain(Graph g, boolean[] inS, int v) {
        double gain = 0.0;
        for (int[] nb : g.adj.get(v)) {
            double w = nb[1] / 100.0;
            boolean samePartition = (inS[nb[0]] == inS[v]);
            gain += samePartition ? w : -w;
        }
        return gain;
    }

    static boolean[] localSearchMaxCut(Graph g, int restarts, int seed) {
        // Complejidad por reinicio: O(iter * n * deg_avg)
        Random rng = new Random(seed);
        boolean[] bestInS = new boolean[g.n];
        double bestValGlobal = 0.0;

        for (int r = 0; r < restarts; r++) {
            boolean[] inS;
            if (r == 0) {
                inS = greedyMaxCut(g);
            } else {
                inS = new boolean[g.n];
                for (int i = 0; i < g.n; i++) inS[i] = rng.nextBoolean();
            }

            boolean improved = true;
            while (improved) {
                improved = false;
                double bestGain = 1e-9;
                int    bestV    = -1;
                for (int v = 0; v < g.n; v++) {
                    double gain = oneFlipGain(g, inS, v);
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestV    = v;
                    }
                }
                if (bestV != -1) {
                    inS[bestV] = !inS[bestV];
                    improved = true;
                }
            }

            double val = g.cutWeight(inS);
            if (val > bestValGlobal) {
                bestValGlobal = val;
                bestInS = Arrays.copyOf(inS, g.n);
            }
        }
        return bestInS;
    }

    //utilidades

    static String setStr(boolean[] inS) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i < inS.length; i++)
            if (inS[i]) { if (!first) sb.append(","); sb.append(i); first = false; }
        sb.append("}");
        return sb.toString();
    }

    static String maskStr(long mask, int n) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i < n; i++)
            if (((mask >> i) & 1) == 1) { if (!first) sb.append(","); sb.append(i); first = false; }
        sb.append("}");
        return sb.toString();
    }

    //analisis experimental

    public static void main(String[] args) {
        System.out.println("=".repeat(65));
        System.out.println("  MAX CUT - Análisis Experimental (Java)");
        System.out.println("=".repeat(65));

        //test de correctitud (n = 6)
        System.out.println("\n[1] Test de correctitud (n = 6):");
        Graph gSmall = new Graph(6);
        int[][] edgesS = {{0,1,300},{0,2,100},{1,2,200},{1,3,400},{2,4,200},{3,4,500},{3,5,300},{4,5,100}};
        for (int[] e : edgesS) gSmall.addEdge(e[0], e[1], e[2]/100.0);

        long[] bfRes = maxCutBruteForce(gSmall);
        double valBf = bfRes[1] / 1000.0;
        boolean[] inSGr = greedyMaxCut(gSmall);
        boolean[] inSLs = localSearchMaxCut(gSmall, 3, 42);
        double valGr = gSmall.cutWeight(inSGr);
        double valLs = gSmall.cutWeight(inSLs);

        System.out.printf("  Fuerza Bruta : S=%s, corte=%.2f%n", maskStr(bfRes[0], gSmall.n), valBf);
        System.out.printf("  Greedy       : S=%s, corte=%.2f%n", setStr(inSGr), valGr);
        System.out.printf("  Búsq. Local  : S=%s, corte=%.2f%n", setStr(inSLs), valLs);
        System.out.printf("  Ratio Greedy vs Óptimo: %.3f%n", valGr/valBf);
        System.out.printf("  Ratio Local  vs Óptimo: %.3f%n", valLs/valBf);

        //tiempos de ejecucion
        System.out.println("\n[2] Tiempos de ejecución:");
        System.out.printf("  %5s | %6s | %18s | %12s | %16s%n",
                          "n", "|E|", "Fuerza Bruta (s)", "Greedy (s)", "Local Search (s)");
        System.out.println("  " + "-".repeat(63));

        int[] bfSizes = {6, 8, 10, 12, 14, 16, 18};
        for (int n : bfSizes) {
            Graph g = Graph.randomGraph(n, 0.5, 7);
            int ne = g.edges.size();

            long t0 = System.nanoTime();
            maxCutBruteForce(g);
            double tBf = (System.nanoTime() - t0) / 1e9;

            t0 = System.nanoTime();
            greedyMaxCut(g);
            double tGr = (System.nanoTime() - t0) / 1e9;

            t0 = System.nanoTime();
            localSearchMaxCut(g, 3, 42);
            double tLs = (System.nanoTime() - t0) / 1e9;

            System.out.printf("  %5d | %6d | %18.6f | %12.6f | %16.6f%n", n, ne, tBf, tGr, tLs);
        }

        System.out.println();
        System.out.printf("  %5s | %6s | %14s | %16s%n", "n", "|E|", "Greedy (s)", "Local Search (s)");
        System.out.println("  " + "-".repeat(45));

        int[] impSizes = {50, 100, 200, 500, 1000, 2000};
        for (int n : impSizes) {
            Graph g = Graph.randomGraph(n, 0.4, 7);
            int ne = g.edges.size();

            long t0 = System.nanoTime();
            greedyMaxCut(g);
            double tGr = (System.nanoTime() - t0) / 1e9;

            t0 = System.nanoTime();
            localSearchMaxCut(g, 5, 42);
            double tLs = (System.nanoTime() - t0) / 1e9;

            System.out.printf("  %5d | %6d | %14.6f | %16.6f%n", n, ne, tGr, tLs);
        }

        //solucion
        System.out.println("\n[3] Calidad de solución (ratio vs óptimo):");
        System.out.printf("  %5s | %10s | %10s | %10s | %10s | %10s%n",
                          "n", "Óptimo", "Greedy", "Ratio Gr", "Local S", "Ratio LS");
        System.out.println("  " + "-".repeat(65));

        for (int n : bfSizes) {
            Graph g = Graph.randomGraph(n, 0.5, 99);
            double vOpt = maxCutBruteForce(g)[1] / 1000.0;
            double vGr2 = g.cutWeight(greedyMaxCut(g));
            double vLs2 = g.cutWeight(localSearchMaxCut(g, 5, 42));
            System.out.printf("  %5d | %10.2f | %10.2f | %10.3f | %10.2f | %10.3f%n",
                              n, vOpt, vGr2, vGr2/vOpt, vLs2, vLs2/vOpt);
        }

        System.out.println("\n[✓] Experimento completado.");
    }
}
