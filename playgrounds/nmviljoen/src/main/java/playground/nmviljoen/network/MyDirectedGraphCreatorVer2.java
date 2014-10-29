/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.nmviljoen.network;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations;
import edu.uci.ics.jung.algorithms.shortestpath.BFSDistanceLabeler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

//import network.MyDirectedGraphCreatorVer2.MyLink;
//import network.MyDirectedGraphCreatorVer2.MyNode;

import org.apache.commons.collections15.Transformer;
import org.geotools.filter.expression.ThisPropertyAccessorFactory;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;



//import com.csvreader.CsvWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.metrics.Metrics;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.Dimension;

import javax.swing.JFrame;


/**
 *This class:
 * Creates a DirectedSparseMultigraph from two csv files: node.csv and links.csv
 * 
 * It also contains a self check procedure that writes the 
 * graph list, node sets and edge sets to txt for manual checking
 * By default the self check is commented out
 * 
 * The graph is then visualized using BasicVisualizationServer and JFrame 
 * Graph elements are set using RendererContext and Transformers
 */

public class MyDirectedGraphCreatorVer2 {
	
		private final static double SCALE = 1.3*10e-3;
		private static Double aspectRatioXY = null;

		/** MyNode and MyLink are classes at the end of the script*/
		DirectedGraph<MyNode, MyLink> myGraph; 

		/** Creates a new instance of BasicDirectedGraph */
		public MyDirectedGraphCreatorVer2() {       
		}

		/** This method receives as parameters the paths to the .csv files that 
		 * contain the node and link detail as well as the number of nodes 
		 * 
		 * It then iteratively reads in the nodes, adds them to the graph 
		 * AND adds them to an ArrayList for later duplicate prevention*/
		public void constructGraph(String nodeFile, String numNodes, String linkFile, String weightI) throws FileNotFoundException{
			System.out.println("Loading nodes into array list...");
			myGraph = new DirectedSparseMultigraph<MyNode, MyLink>();

			//read in and assign nodes
			String csvFile1 = nodeFile;
			BufferedReader br1 = null;
			String lineNode = "";
			String counterID;
			String currentNodeId;
			double currentNodeX;
			double currentNodeY;
			int number = Integer.parseInt(numNodes);
			int weightIndex = Integer.parseInt(weightI);
			String[] nodeId = new String[number];
			int counter =0;
			ArrayList<MyNode> nodeList = new ArrayList<MyNode>();
			MyNode currentNode = new MyNode("","", 0, 0);
			try {
				br1 = new BufferedReader(new FileReader(csvFile1));
				while ((lineNode = br1.readLine()) != null) {
					String[] nodeData = lineNode.split(",");
					counterID = Integer.toString(counter);
//					currentNodeId = nodeData[1]; //for road data files
					currentNodeId = nodeData[0]; //for test data files
//					currentNodeX = Double.parseDouble(nodeData[3]);//for road data files
//					currentNodeY = Double.parseDouble(nodeData[5]);//for road data files
					currentNodeX = Double.parseDouble(nodeData[1]);//for test data files
					currentNodeY = Double.parseDouble(nodeData[2]);//for test data files
					currentNode = new MyNode(counterID,currentNodeId,currentNodeX,currentNodeY);
					nodeId[counter]=currentNodeId;
					nodeList.add(counter, currentNode);
					counter++;
				}


			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br1 != null) {
					try {
						br1.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("Nodes loaded in array list");
			System.out.println("Adding links to graph....");
			//System.out.println("(go make coffee, this takes about 15 minutes)");

			//The approach followed to prevent duplicate nodes being added to the graph
			//as edges are added is pretty tedious as it searches through the whole
			//ArrayList for each node of each OD pair for each link added to match
			//the O and D nodes to nodes already in the graph.
			// 
			//I would like to finder a better way to avoid duplicates

			//read in and assign edges
			String csvFile2 = linkFile;
			BufferedReader br2 = null;
			String lineLink = "";
			String currentLinkId;
			String currentFromId;
			String currentToId;
			double currentLinkWeight;
			double currentTransProb;
			int indexTo;
			int indexFrom;
			int counter1=0; //only used when calling testFiles()
			try { 
				br2 = new BufferedReader(new FileReader(csvFile2));
				while ((lineLink = br2.readLine()) != null) {
					String[] linkData = lineLink.split(",");
//					currentLinkId = linkData[1];//for road data
					currentLinkId = linkData[0];//for test data
//					currentFromId =linkData[3];//for road data
//					currentToId =linkData[5];//for road data
					currentFromId =linkData[1];//for test data
					currentToId =linkData[2];//for test data
					currentLinkWeight = Double.parseDouble(linkData[weightIndex]);   
					indexFrom = getNode(nodeList,currentFromId);
					indexTo = getNode(nodeList,currentToId);
					currentTransProb = 0;
					myGraph.addEdge(new MyLink(currentLinkId,currentLinkWeight,currentTransProb),nodeList.get(indexFrom),nodeList.get(indexTo),EdgeType.DIRECTED);
					//System.out.println("Edge "+counter1+" added"); //if you want to track progress
					counter1++;
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br2 != null) {
					try {
						br2.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			LinkedList<MyLink> linkList = new LinkedList(myGraph.getEdges());
			System.out.println("Graph created");

		}

		public void testFiles(DirectedGraph<MyNode, MyLink> myGraph2, String graphTestFile, String nodeTestFile, String linkTestFile) throws FileNotFoundException{
			System.out.println("Writing graph, node set and link set to files... (self-check procedure)");
			try {
				File fileNode = new File(graphTestFile);
				FileWriter fw = new FileWriter(fileNode.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(myGraph2.toString());
				bw.close();
				System.out.println("Graph written to file");
				System.out.println("Writing nodes...");
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				File fileNode = new File(nodeTestFile);
				FileWriter fw = new FileWriter(fileNode.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(myGraph2.getVertices().toString());
				bw.close();
				System.out.println("Nodes written to file");
				System.out.println("Writing links...");
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				File fileEdge = new File(linkTestFile);
				FileWriter fw = new FileWriter(fileEdge.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(myGraph2.getEdges().toString());
				bw.close();
				System.out.println("Edges written to file");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * @param args in the run config
		 * file path to csv file with node set
		 * number of nodes in csv file
		 * column number in the links.csv file that contains the weight of the link
		 * file path to csv file with links set
		 * file path to output txt file for graph list
		 * file path to output txt file for node list
		 * file path to output txt file for edge list
		 * file path to csv file with node betweenness results
		 * file path to csv file with edge betweenness results
		 * file path to csv file with node closeness results
		 * file path to csv file with clustering coefficient results
		 * file path to csv file with Dijsktra shortest path results for an unweighted graph
		 * file path to csv file with Dijsktra shortest path results for an weighted graph
		 */
		public static void main(String[] args) throws FileNotFoundException{
			//input files and parameter
			String nodeFile = args[0];
			String numNodes = args[1];
			String weightI = args[2];
			String linkFile = args[3];

			//test files
			String graphTestFile = args[4]; //only for testFiles()
			String nodeTestFile = args[5]; //only for testFiles()
			String linkTestFile = args[6]; //only for testFiles()
			//centrality
			String nodeBetUnweighted = args[7];
			String edgeBetUnweighted = args[8];
			String nodeBetWeighted = args[9];
			String edgeBetWeighted = args[10];
			String nodeCloseUnweighted = args[11];
			String nodeCloseWeighted = args[12];
			String nodeEigenUnweighted = args[13];
			String nodeEigenWeighted = args[14];
//			String nodePageRankUnweighted = args[19];
//			String nodePageRankWeighted = args[20];

			//clustering measures
			String clusterFile = args[15];
			String weakCompFile = args[16];
			String triadFile = args[17];

			//distance statistics
			String distUnweightedFile = args[18];


			//String dijkstraFile = args[10];
			//String dijkstraFileWeight = args[11];

			final MyDirectedGraphCreatorVer2 myApp = new MyDirectedGraphCreatorVer2(); //this was made "final" by one of the visualization transformers
			myApp.constructGraph(nodeFile,numNodes,linkFile, weightI);  
			System.out.println("Graph constructed");
			LinkedList<MyLink> linkList = new LinkedList(myApp.myGraph.getEdges());
			ArrayList<MyNode> nodeList = new ArrayList<MyNode>(myApp.myGraph.getVertices());
			


			//Centrality scores
					JungCentrality.calculateAndWriteUnweightedBetweenness(myApp.myGraph,nodeBetUnweighted, edgeBetUnweighted,nodeList, linkList);
					JungCentrality.calculateAndWriteUnweightedCloseness(myApp.myGraph, nodeCloseUnweighted, nodeList);
					JungCentrality.calculateAndWriteWeightedBetweenness(myApp.myGraph,nodeBetWeighted, edgeBetWeighted,nodeList, linkList);
					JungCentrality.calculateAndWriteWeightedCloseness(myApp.myGraph, nodeCloseWeighted, nodeList);
					JungCentrality.calculateAndWriteUnweightedEigenvector(myApp.myGraph, nodeEigenUnweighted, nodeList);
					JungCentrality.calculateAndWriteWeightedEigenvector(myApp.myGraph, nodeEigenWeighted, nodeList,linkList);

// I thought that I could use PageRank with alpha = 0 to overcome my issue with the weighted eigenvectorCentrality					
//					JungCentrality.calculateAndWriteUnweightedPageRank(myApp.myGraph, nodePageRankUnweighted, nodeList);
//					JungCentrality.calculateAndWriteWeightedPageRank(myApp.myGraph, nodePageRankWeighted, nodeList);

			//Clustering
					JungClusters.calculateAndWriteClusteringCoefficient(myApp.myGraph, clusterFile);
					JungClusters.calculateAndWriteWeakComponents(myApp.myGraph, weakCompFile);
					JungClusters.calculateAndWriteTriadicCensus(myApp.myGraph, triadFile);

			//Graph distance
			//		JungGraphDistance.calculateAndWriteUnweightedDistances(myApp.myGraph, distUnweightedFile);

			//ShortestPaths.metricCalc(myApp.myGraph, clustFile, dijkstraFile, dijkstraFileWeight);

//			 myApp.testFiles(myApp.myGraph, graphTestFile, nodeTestFile, linkTestFile);//self-check procedure

		}
		//
		private int getNode(ArrayList<MyNode> list,String id){
			int index = 999999;
			for (int i=0; i<list.size();i++){ 
				if(list.get(i).getId().equals(id)){
					index=i;
					break;
				} 
			}
			return index;
		}

		class MyNode {
			String intID;
			String id;
			double X;
			double Y;
			public MyNode(String intID, String id, double X, double Y) {
				this.intID = intID;
				this.id = id;
				this.X =X;
				this.Y=Y;
			}
			public String toString() {
				return intID +" "+ id+" "+X+" "+Y+'\n';
			}        
			public String getId(){
				return id;
			}
			public String getX(){
				return Double.toString(X);
			}
			public String getY(){
				return Double.toString(Y);
			}
		}

		class MyLink {
			String id;
			double weight;//capacity in this case
			double transProb;
			public MyLink(String id, double weight, double transProb) {
				this.id = id;
				this.weight = weight;
				this.transProb = -99;
			} 
			public String getId(){
				return id;
			}
			public double getWeight(){
				return weight;
			}
			public void setTransProb(double newTransProb){
				transProb = newTransProb;
			}
			public double getTransProb(){
				return transProb;
			}
			public String toString() {
				return id+" "+weight+'\n';
			}
			

		}

	
}