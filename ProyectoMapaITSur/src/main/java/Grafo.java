package main.java;

import java.util.*;

public class Grafo 
{
    private int numVertices; //numero  de vertices que tendra el grafo
    private Map<Integer, Lugar> vertices; //hace que cada objeto gurdado en lugar se asisgne un id para identificar es un 
    private List<List<Arista>> listaA;//lista de listas para representar lass conexiones 

    public Grafo(int numVertices) 
    {
        this.numVertices = numVertices;
        this.vertices = new HashMap<>();
        this.listaA = new ArrayList<>(numVertices);
        
        for (int i = 0; i < numVertices; i++) 
        {
            this.listaA.add(new ArrayList<>());
        }
    }

   
    //registrar nuevos edificios crea un insatancia de Lugar con su id y nombre y lo guarda en vertices usando su id
    public void agregarLugar(int id, String nombre)
    {
        vertices.put(id, new Lugar(id, nombre));
    }

    //crea las calles 
    public void agregarCamino(int origen, int destino, int distancia) 
    {
        //busca en la lista el id, crea una arista yla mete a las lisat
        listaA.get(origen).add(new Arista(destino, distancia));
        listaA.get(destino).add(new Arista(origen, distancia)); 
    }

    public Map<Integer, Lugar> getVertices() 
    { 
        return vertices; 
    }

    public List<List<Arista>> getListaA() 
    {
        return listaA;
    }
   
}
