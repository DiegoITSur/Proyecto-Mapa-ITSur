package main.java;

import java.util.*;

public class Main{
    private int distanciaTotalRuta = 0;
    private Integer idOrigenSeleccionado = null;
    private Integer idDestinoSeleccionado = null;
    private Grafo campus;
    public static void main(String[] args) {

    }

    //calcular metros
    private int calcularMetros(int origen, int destino) {
        int n = campus.getListaA().size();
        int[] distancias = new int[n];
        Arrays.fill(distancias, Integer.MAX_VALUE);
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));

        distancias[origen] = 0;
        pq.add(new int[]{origen, 0});

        while (!pq.isEmpty()) {
            int[] actual = pq.poll();
            int u = actual[0];
            int distU = actual[1];

            if (distU > distancias[u]) {
                continue;
            }

            for (Arista arista : campus.getListaA().get(u)) {
                int v = arista.destino;
                int peso = arista.distanciaMetros;
                if (distancias[u] + peso < distancias[v]) {
                    distancias[v] = distancias[u] + peso;
                    pq.add(new int[]{v, distancias[v]});
                }
            }
        }
        return distancias[destino];
    }

    // el coso de dijkstra
    private List ejecutarDijkstra(int origen, int destino) {
        int n = campus.getListaA().size();
        int[] distancias = new int[n];
        int[] predecesores = new int[n];
        Arrays.fill(distancias, Integer.MAX_VALUE);
        Arrays.fill(predecesores, -1);
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        distancias[origen] = 0;
        pq.add(new int[]{origen, 0});
        while (!pq.isEmpty()) {
            int[] actual = pq.poll();
            int u = actual[0];
            if (actual[1] > distancias[u]) {
                continue;
            }
            for (Arista arista : campus.getListaA().get(u)) {
                int v = arista.destino;
                int peso = arista.distanciaMetros;
                if (distancias[u] + peso < distancias[v]) {
                    distancias[v] = distancias[u] + peso;
                    predecesores[v] = u;
                    pq.add(new int[]{v, distancias[v]});
                }
            }
        }
        if (distancias[destino] == Integer.MAX_VALUE) {
            return null;
        }
        List ruta = new ArrayList<>();
        int paso = destino;
        while (paso != -1) {
            ruta.add(paso);
            paso = predecesores[paso];
        }
        Collections.reverse(ruta);
        return ruta;
    }
}
