package com.example.a40203.tomtommapexample;

import com.tomtom.online.sdk.common.location.LatLng;

import java.util.ArrayList;

public class Helper {

    public static ArrayList<LatLng> mindistToFirstPoint(ArrayList<LatLng> lat, LatLng firstPoint){
//        LatLng minlatLng = null;
//
//        for(int i=0; i<lat.size()-1;++i){
//            if((lat.get(i) != firstPoint) && ((calculateDistance(firstPoint, lat.get(i))) < calculateDistance(firstPoint, lat.get(i+1)))){
//                minlatLng = lat.get(i);
//            }
//        }

        ArrayList<LatLng> tempLat = (ArrayList<LatLng>) lat.clone();
        LatLng temp;

        for(int i =0; i< tempLat.size()-1;i++){
            for(int j =1; j< tempLat.size()-i;j++){
                if(calculateDistance(firstPoint,tempLat.get(j-1)) > calculateDistance(firstPoint,tempLat.get(j))){
                    temp = tempLat.get(j-1);
                    tempLat.set(j-1, tempLat.get(j));
                    tempLat.set(j, temp);
                }
            }
        }

        return tempLat;
    }

    public static double calculateDistance(LatLng latlng1, LatLng latlng2){
//        double radlat1 = Math.PI * latlng1.getLatitude()/180;
//        double radlat2 = Math.PI * latlng2.getLatitude()/180;
//        double theta = latlng1.getLongitude()-latlng2.getLongitude();
//        double radtheta = Math.PI * theta/180;
//        double dist = Math.sin(radlat1) * Math.sin(radlat2) + Math.cos(radlat1) * Math.cos(radlat2) * Math.cos(radtheta);
//        dist = Math.acos(dist);
//        dist = dist*180/Math.PI;
////        dist = dist * 60 * 1.1515;
//        return dist;
        double R = 6371*Math.exp(3);
        double theta1 = Math.PI * latlng1.getLatitude()/180;
        double theta2 = Math.PI * latlng2.getLatitude()/180;
        double deltaTheta = Math.PI * (latlng2.getLatitude()-latlng1.getLatitude())/180;
        double deltaLambda = Math.PI * (latlng2.getLongitude()-latlng1.getLongitude())/180;

        double a = (Math.sin(deltaTheta/2) * Math.sin(deltaTheta/2)) +
                (Math.cos(theta1) * Math.cos(theta2)) *
                        (Math.sin(deltaLambda/2) * Math.sin(deltaLambda/2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }
}
