package com.example.a40203.tomtommapexample;

import android.util.Log;


import java.util.ArrayList;

public class CalcDijkstra {



    static double totalDist;

    private static final int NO_PARENT = -1;
    static ArrayList<Integer> routeList = new ArrayList<>();


    // Function that implements Dijkstra's
    // single source shortest path
    // algorithm for a graph represented
    // using adjacency matrix
    // representation
    public static void calculate(double[][] adjacencyMatrix,
                                      int startVertex) {
        int nVertices = adjacencyMatrix[0].length;

        // shortestDistances[i] will hold the
        // shortest distance from src to i
        double[] shortestDistances = new double[nVertices];

        totalDist= 0.0;

        routeList.clear();

        // added[i] will true if vertex i is
        // included / in shortest path tree
        // or shortest distance from src to
        // i is finalized
        Boolean[] added = new Boolean[nVertices];

        // Initialize all distances as
        // INFINITE and added[] as false
        for (int vertexIndex = 0; vertexIndex < nVertices;
             vertexIndex++) {
            shortestDistances[vertexIndex] = Integer.MAX_VALUE;
            added[vertexIndex] = false;
//            added[vertexIndex].set(false);
        }

        // Distance of source vertex from
        // itself is always 0
        shortestDistances[startVertex] = 0;

        // Parent array to store shortest
        // path tree
        int[] parents = new int[nVertices];

        // The starting vertex does not
        // have a parent
        parents[startVertex] = NO_PARENT;

        // Find shortest path for all
        // vertices
        for (int i = 1; i < nVertices; i++) {


            int nearestVertex = 0;
            double shortestDistance = Integer.MAX_VALUE;

            for (int vertexIndex = 0;
                 vertexIndex < nVertices;
                 vertexIndex++) {
                if (!added[vertexIndex] &&
                        shortestDistances[vertexIndex] <
                                shortestDistance) {
                    nearestVertex = vertexIndex;
                    shortestDistance = shortestDistances[vertexIndex];
                }
            }


            // Mark the picked vertex as
            // processed
            added[nearestVertex] = true;

            // Update dist value of the
            // adjacent vertices of the
            // picked vertex.
            for (int vertexIndex = 0;
                 vertexIndex < nVertices;
                 vertexIndex++) {
                double edgeDistance = adjacencyMatrix[nearestVertex][vertexIndex];

                if (edgeDistance > 0
                        && ((shortestDistance + edgeDistance) <
                        shortestDistances[vertexIndex])) {
                    parents[vertexIndex] = nearestVertex;
                    shortestDistances[vertexIndex] = shortestDistance +
                            edgeDistance;
                }
            }
        }



        Log.w("cheese", "without speed decrease");
        printSolution(startVertex, shortestDistances, parents);
    }

    // A utility function to print
    // the constructed distances
    // array and shortest paths

    private static String printer;
    private static void printSolution(int startVertex,
                                      double[] distances,
                                      int[] parents)
    {
        int nVertices = distances.length;
        printer = "com.example.a40203.tomtommapexample.Vertex\t Distance\tPath";

        for (int vertexIndex = 0;
             vertexIndex < nVertices;
             vertexIndex++)
        {
            if (vertexIndex != startVertex)
            {
                printer += "\n" + startVertex + " -> ";
                printer += vertexIndex + " \t\t ";
                printer += distances[vertexIndex] + "\t\t";
                printPath(vertexIndex, parents);
                if(vertexIndex == nVertices-1){
                    setRoute(vertexIndex,parents);
                    setDistance(distances[vertexIndex]);
                }

            }
        }
        Log.w("cheese", printer);
    }

    // Function to print shortest path
    // from source to currentVertex
    // using parents array
    private static void printPath(int currentVertex,
                                  int[] parents)
    {

        // Base case : Source node has
        // been processed
        if (currentVertex == NO_PARENT)
        {
            return;
        }
        printPath(parents[currentVertex], parents);
        printer += currentVertex + " ";
    }

    public static ArrayList<Integer> getRoute(){
        return routeList;
    }

    private static void setRoute(int currentVertex,
                                   int[] parents){
        if (currentVertex == NO_PARENT)
        {
            return;
        }
        setRoute(parents[currentVertex], parents);
        routeList.add(currentVertex);
    }

    public static String getPrinter(){
        return printer;
    }

    private static void setDistance(double distance){
        totalDist = distance;
    }

    public static double getDistance(){
        return totalDist;
    }

}
