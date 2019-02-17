package com.example.a40203.tomtommapexample;

public class Edge {
    private int fromNodeIndex;
    private int toNodeIndex;
    private double length;

    public Edge(int fromNodeIndex, int toNodeIndex, double length){
        this.fromNodeIndex = fromNodeIndex;
        this.toNodeIndex = toNodeIndex;
        this.length = length;
    }

    public int getFromNodeIndex(){
        return fromNodeIndex;
    }

    public int getToNodeIndex(){
        return toNodeIndex;
    }

    public double getLength(){
        return length;
    }

    //determines the neighbouring node of a supplied node, based on the two nodes that are connected by this edge
    public int getNeighbourIndex(int nodeIndex){
        if(this.fromNodeIndex == nodeIndex){
            return this.toNodeIndex;
        } else{
            return this.fromNodeIndex;
        }
    }
}