package com.example.a40203.tomtommapexample;

import java.util.Random;

public class AdaptDijkstra {
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
