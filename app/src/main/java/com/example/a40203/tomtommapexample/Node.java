package com.example.a40203.tomtommapexample;

import java.util.ArrayList;

public class Node {

    private double distanceFromSource = Double.MAX_VALUE;
    private boolean visited;
    private ArrayList<Edge> edges = new ArrayList<Edge>();

    public double getDistanceFromSource(){
        return distanceFromSource;
    }

    public void setDistanceFromSource(double distanceFromSource){
        this.distanceFromSource = distanceFromSource;
    }

    public boolean isVisited(){
        return visited;
    }

    public void setVisited(boolean visited){
        this.visited = visited;
    }

    public ArrayList<Edge> getEdges(){
        return edges;
    }

    public void setEdges(ArrayList<Edge> edges){
        this.edges = edges;
    }
}