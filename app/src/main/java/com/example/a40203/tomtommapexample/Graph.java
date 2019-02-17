package com.example.a40203.tomtommapexample;

import android.util.Log;

import java.util.ArrayList;

public class Graph {

    private Node[] nodes;
    private int noOfNodes;
    private Edge[] edges;
    private int noOfEdges;
    String output;
    String points;

    public Graph(Edge[] edges) {
        this.edges = edges;

        //create all nodes ready to be updated with the edges
        this.noOfNodes = calculateNoOfNodes(edges);
        this.nodes = new Node[this.noOfNodes];

        for (int n =0; n<this.noOfNodes; n++) {
            this.nodes[n] = new Node();
        }

        //add all the edges to the nodes, each edge added to two nodes
        this.noOfEdges = edges.length;

        for (int edgeToAdd = 0; edgeToAdd< this.noOfEdges;edgeToAdd++) {
            this.nodes[edges[edgeToAdd].getFromNodeIndex()].getEdges().add(edges[edgeToAdd]);
            this.nodes[edges[edgeToAdd].getToNodeIndex()].getEdges().add(edges[edgeToAdd]);
        }
    }

    private int calculateNoOfNodes(Edge[] edges) {
        int noOfNodes = 0;

        for (Edge e: edges) {
            if (e.getToNodeIndex() > noOfNodes) {
                noOfNodes = e.getToNodeIndex();
            }
            if (e.getFromNodeIndex() > noOfNodes) {
                noOfNodes = e.getFromNodeIndex();
            }
        }
        noOfNodes++;

        return noOfNodes;
    }

    public void calculateShortestDistances() {
        //node 0 as source
        this.nodes[0].setDistanceFromSource(0);
        int nextNode = 0;

        //visit every node
        for (int i = 0; i < this.nodes.length; i++) {
            //loop around the edges of current node
            ArrayList<Edge> currentNodeEdges = this.nodes[nextNode].getEdges();

            for (int joinedEdge = 0; joinedEdge < currentNodeEdges.size(); joinedEdge++) {
                int neighbourIndex = currentNodeEdges.get(joinedEdge).getNeighbourIndex(nextNode);

                //only if not visited
                if (!this.nodes[neighbourIndex].isVisited()) {
                    double tenative = this.nodes[nextNode].getDistanceFromSource() + currentNodeEdges.get(joinedEdge).getLength();

                    if (tenative < nodes[neighbourIndex].getDistanceFromSource()) {
                        nodes[neighbourIndex].setDistanceFromSource(tenative);
                    }
                }
            }

            //all neighbours checked soi node visited
            nodes[nextNode].setVisited(true);

            //next node must be with shortest distance
            nextNode = getNodeShortestDistanced();
        }
    }

    private int getNodeShortestDistanced(){
        int storedNodeIndex = 0;
        double storedDist = Double.MAX_VALUE;

        for (int i =0; i < this.nodes.length;i++){
            double currentDist = this.nodes[i].getDistanceFromSource();

            if(!this.nodes[i].isVisited() && currentDist < storedDist){
                storedDist = currentDist;
                storedNodeIndex = i;
            }
        }

        return storedNodeIndex;
    }

    //display result
    public void printResult(){
        output = "Number of nodes= " + this.noOfNodes;
        output += "\nNumber of edges= " + this.noOfEdges;

        for (int i =this.nodes.length-3; i< this.nodes.length;i++){
            output += ("\nThe shortest ditance from node 0 to node " +i + " is " + nodes[i].getDistanceFromSource());
        }

        Log.d("cheese", output);
    }

    public String printString(){
        String output = "Number of nodes= " + this.noOfNodes;
        output += "\nNumber of edges= " + this.noOfEdges;

        for (int i =0; i< this.nodes.length;i++){
            output += ("\nThe shortest ditance from node 0 to node " +i + " is " + nodes[i].getDistanceFromSource());
        }
        return output;
    }

    public Node[] getNodes(){
        return nodes;
    }

    public int getNoOfNodes(){
        return noOfNodes;
    }

    public Edge[] getEdges(){
        return edges;
    }

    public int getNOOfEdges(){
        return noOfEdges;
    }
}
