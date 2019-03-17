package com.example.a40203.tomtommapexample;

import android.util.Log;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class CalcDijkstra {


    public static AtomicDouble atomicDouble = new AtomicDouble(Integer.MAX_VALUE);

    private static int index = 0;

    private static final int NO_PARENT = -1;

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

//            atomicDouble.set(Integer.MAX_VALUE);
//
//            int numThreads = Runtime.getRuntime().availableProcessors();
//
//            Vector<Thread> thread_pool = new Vector<>();
//
//            for(int j=0; j< numThreads; ++j){
//                int finalJ = j;
//                thread_pool.add(j, new Thread(){
//                    public void run(){
//                        minDistIndex(nVertices, added, shortestDistances, adjacencyMatrix, parents, finalJ);
//                    }
//                });
//            }
//
//            for(Thread t: thread_pool){
//                try {
//                    t.join();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            //set the nearestVertex to the current shortestDistance index and set the shortestDistance to shortestDistance found by any of the threads
//            int nearestVertex = index;
//            double shortestDistance = atomicDouble.get();

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
        Log.w("cheese", " with speed decrease");
        printSolution(startVertex, speedFactor(shortestDistances), parents);
    }

//    private static void minDistIndex(int nVertices, Boolean[] added, double[] shortestDistances, double[][] adjacencyMatrix, int[] parents, int threadNum) {
//
//        int numThreads = Runtime.getRuntime().availableProcessors()-1;
//        int firstInt= nVertices/ (numThreads * (threadNum-1));
//        int secondInt = nVertices/ (numThreads * threadNum);
//
//        // Pick the minimum distance vertex
//        // from the set of vertices not yet
//        // processed within the current thread's section. nearestVertex is
//        // always equal to startNode in
//        // first iteration.
//
//
//        if(shortestDistance<atomicDouble.get()){
//            atomicDouble.set(shortestDistance);
//            index = nearestVertex;
//        }
//    }

    // A utility function to print
    // the constructed distances
    // array and shortest paths

    private static String printer;
    private static void printSolution(int startVertex,
                                      double[] distances,
                                      int[] parents)
    {
        int nVertices = distances.length;
        printer = "Vertex\t Distance\tPath";

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

    public static String getPrinter(){
        return printer;
    }

    //apply different speed factors to the distances between points in the distances list
    public static double[] speedFactor(double[] distances){
        Random rand = new Random();

        //the percentage of speed that the car is decreased by in various situations
        double speedBumpDecrease = 0.25;
        double trafficLightDecrease = 0.5;
        double pedestrianDecrease = 0.1;

        boolean speedBump;
        boolean trafficLight;
        boolean pedestrian;

        for(int i=0; i<distances.length;++i){

            //the chance of the car encountering each speed factor
            speedBump = rand.nextDouble() <=0.3;
            trafficLight = rand.nextDouble() <= 0.6;
            pedestrian = rand.nextDouble() <= 0.8;

            if(speedBump){
                distances[i] = distances[i] * speedBumpDecrease;
            }
            if(trafficLight){
                distances[i] = distances[i] * trafficLightDecrease;
            }
            if(pedestrian){
                distances[i] = distances[i] * pedestrianDecrease;
            }
        }

        return distances;

    }

}
