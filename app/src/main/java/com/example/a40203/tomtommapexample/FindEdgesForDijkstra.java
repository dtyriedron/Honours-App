package com.example.a40203.tomtommapexample;

import android.util.Log;

import com.tomtom.online.sdk.common.location.LatLng;
import com.tomtom.online.sdk.routing.data.FullRoute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FindEdgesForDijkstra {


    public static void calculate(List<FullRoute> routes, double[][] dijkstraPoints){

        //map of points and their neighbours
        Map<LatLng, ArrayList<LatLng>> pointsAndNeighbours = new LinkedHashMap<>();

        //list of points that can be adapted to our needs
        ArrayList<LatLng> pointList = new ArrayList<>();


        for(FullRoute r: routes){
            for(LatLng c: r.getCoordinates()){
                pointList.add(c);
                pointsAndNeighbours.put(c, new ArrayList<>());
            }
        }


        int numThreads = Runtime.getRuntime().availableProcessors();
        Log.w("debug", "number of available threads: " + numThreads);
        //service pool of tasks to execute
        ExecutorService service = Executors.newFixedThreadPool(pointList.size());
//        submit all the tasks using for loop
        for(int o=0;o<4;++o) {
            int finalO = o;
            service.submit(() -> {
                findNeighbours(finalO, routes, pointsAndNeighbours);
            });
        }

        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //add to dijkstra list
        for(int i=0;i<pointList.size();i++){
            ArrayList<LatLng> value = pointsAndNeighbours.get(pointList.get(i));
            for (int j = 0; j < value.size(); j++) {
                dijkstraPoints[i][pointList.indexOf(value.get(j))] = pointList.get(i).toLocation().distanceTo(value.get(j).toLocation());
                dijkstraPoints[pointList.indexOf(value.get(j))][i] = pointList.get(i).toLocation().distanceTo(value.get(j).toLocation());
            }
        }
        //adjust dijkstra
        for(int i=0;i<dijkstraPoints.length;i++){
            for(int j=0;j<dijkstraPoints[i].length;j++){
                if(j!=0){
                    dijkstraPoints[i][j] = AdaptDijkstra.factorSpeed(dijkstraPoints[i][j]);
                }
            }
        }
    }


    private static void findNeighbours(int routeIndex, List<FullRoute> routes, Map<LatLng, ArrayList<LatLng>> pointsAndNeighbours){
        for (int i = routeIndex; i < (routes.size()/4) * routeIndex+1; ++i) {
            // Log.w("points", "route: "+i + "size: "+routes.get(i).getCoordinates().size());

            //for every point within every route
            for (int j = 0; j < routes.get(i).getCoordinates().size(); j++)
            {
                ArrayList neighbours = pointsAndNeighbours.get(routes.get(i).getCoordinates().get(j));

                //if it is within the limits of the array and its not already in the map!
                if(j+1<routes.get(i).getCoordinates().size() && !neighbours.contains(routes.get(i).getCoordinates().get(j+1)))
                {
                    //add the next point as first neighbour
                    neighbours.add(routes.get(i).getCoordinates().get(j + 1));
                }
                else if((j+1>=routes.get(i).getCoordinates().size())&& !neighbours.contains(routes.get(i).getCoordinates().get(j-1)))
                {
                    //add the next point as first neighbours
                    neighbours.add(routes.get(i).getCoordinates().get(j - 1));
                }

                if(i+1<routes.size())
                {
                    //start on the next route
                    for (int y = i + 1; y < routes.size(); ++y)
                    {
                        //start at the first routes neighbour
                        for (int l = j; l < routes.get(y).getCoordinates().size(); ++l)
                        {
                            //check the current point with all the points to find matches
                            if (routes.get(i).getCoordinates().get(j).toString().equals(routes.get(y).getCoordinates().get(l).toString()))
                            {
                                //add all the neighbours of the points that match with the original point
                                if (l + 1 < routes.get(y).getCoordinates().size())
                                {
                                    //add point beyond match as a neighbour
                                    neighbours.add(routes.get(y).getCoordinates().get(l + 1));
                                    //Log.w("points", "adding point to neighbours: " + routes.get(y).getCoordinates().get(l + 1));
                                } else if((j+1>=routes.get(i).getCoordinates().size()))
                                {
                                    //if last point then get the point that links to it previously
                                    //add point previous to match as a neighbour
                                    neighbours.add(routes.get(y).getCoordinates().get(l - 1));
                                    //Log.w("points", "adding last point to neighbours: " + routes.get(y).getCoordinates().get(l - 1));
                                }

                            }
                        }
                    }
                }
                //add the point and its neighbours
                pointsAndNeighbours.put(routes.get(i).getCoordinates().get(j), neighbours);
            }
        }
    }
}
