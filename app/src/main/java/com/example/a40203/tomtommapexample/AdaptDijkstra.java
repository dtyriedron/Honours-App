package com.example.a40203.tomtommapexample;

import java.util.Random;

public class AdaptDijkstra {
//    public static void getResponse(String reposnseString) {
//        try {
//            JSONObject obj = new JSONObject(reposnseString);
//            Log.w("cheese", obj.get("streetname").toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
    //if(database has contains road){
    //     get speed limit
    // }
    //else{
    // predictSpeed();
    // }
//    streetName = "";
//
//
//    double predictTime = routeDistance * (speed);
//
//    private int predictSpeed(LatLng point1, LatLng point2){
//        int speed =0;
//        //MainActivity.setStreetName(point1);
////        String point1StreetName = MainActivity.getStreetName();
////        MainActivity.setStreetName(point2);
////        String point2StreetName = streetName;
//
////        if(point1StreetName.equals(point2StreetName)){
////
////        }
//
//
//        return speed;
//    }
        //apply different speed factors to the distances between points in the distances list
    public static double factorSpeed(double distance){
        Random rand = new Random();

        boolean speedBump;
        boolean trafficLight;
        boolean pedestrian;

        //the chance of the car encountering each speed factor
        speedBump = rand.nextDouble() <=0.05;
        trafficLight = rand.nextDouble() <= 0.30;
        pedestrian = rand.nextDouble() <= 0.10;

        //the percentage of speed that the car is decreased by in various situations
        double speedBumpDecrease = 1.6;
        double trafficLightDecrease = 1.6;
        double pedestrianDecrease = 1.9;


            if(speedBump){
                distance = distance * speedBumpDecrease;
            }
            if(trafficLight){
                distance = distance * trafficLightDecrease;
            }
            if(pedestrian){
                distance = distance * pedestrianDecrease;
            }

        return distance;
    }

}
