package solutions.exercise3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.sopra.api.exercises.ExerciseSubmission;
import org.sopra.api.exercises.exercise3.FlowEdge;
import org.sopra.api.exercises.exercise3.FlowGraph;

public class FlowGraphImpl<V> implements FlowGraph<V>, ExerciseSubmission {

	/**
	 * mainMap represents the the graph as an Map with maps inside it
	 */
	
	private Map<V, Map<V, FlowEdge<V>>> mainMap;
	//		Map<startNode, Map<endNode, FlowEgde<V>>>
	/**
	 * constructor for the flow graph
	 * 
	 */
	
	public FlowGraphImpl() {
		mainMap = new HashMap<>();
	}
	
	/**
	 * Adds a new flow edge to a start and a destination node with initial flow of 0. If either start or destination node does not exist in graph, throws an NoSuchElementException. Returns the existing edge if edge already exists.
	 * @param start - start node
	 * @param dest - end node
	 * @param capacity - capacity of the edge
	 * @exception NoSuchElementException if one of the given nodes doesn't exist in the graph
	 * @return newly created or existing edge
	 */
	
	@Override
	public FlowEdge<V> addEdge(V start, V dest, int capacity) throws NoSuchElementException{
		if(!containsNode(start) || !containsNode(dest)) {
			throw new NoSuchElementException();
		}
		
		if(mainMap.containsValue(dest)) { //is it enough to check if there is a Edge??
			//gets the existing edge
			return mainMap.get(start).get(dest);
		} else{
			//creates new edge
			FlowEdge<V> newEdge = new FlowEdgeImpl<>(start, dest, capacity);
			
			//adds other node and edge to each nodes(keys)
			mainMap.get(start).put(dest, newEdge);
			mainMap.get(dest).put(start, newEdge);
			
			return newEdge;
		}
	}

	/**
	 * Adds new node to flow graph if node does not already exist and if node is not null. If node is added returns true, else returns false.
	 * @param node - Node to add
	 * @return true if the node was successfully added, false otherwise.
	 */
	
	@Override
	public boolean addNode(V node) {
		if(!containsNode(node) && node != null) {
			//puts new node with an empty map in the main map, if node doesn't exist in it
			mainMap.put(node, new HashMap<V, FlowEdge<V>>());
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns list of all edges from graph.
	 * @return Returns a List off all flow edges
	 */
	@Override
	public List<FlowEdge<V>> getEdges() {
		List<FlowEdge<V>> edgeList = new ArrayList<>();
		
		//get the key set of the main map
		Set<V> keySet = mainMap.keySet();
		
		//for each loop to get every edge of every node(key)
		for(V node : keySet) {
			
			//get all edges from a node
			Collection<FlowEdge<V>> edgeCollection = mainMap.get(node).values(); 
			for(FlowEdge<V> edge : edgeCollection) {
				
				//if edge doesn't already exist in list it will be added
				if(!edgeList.contains(edge)) {
					edgeList.add(edge);
				}
			}
		}
		return edgeList;
	}
	
	/**
	 * Returns a set of all nodes from graph.
	 * @return a Set of all nodes
	 */

	@Override
	public Set<V> getNodes() {
		return mainMap.keySet();
	}
	
	/**
	 * Returns true if the given node is contained in the graph. Otherwise returns false
	 * @param node - Node to be tested if contained in graph
	 * @return true if node is contained in the graph otherwise false
	 */
	
	@Override
	public boolean containsNode(V node) {
		if(mainMap.containsKey(node)) {
			return true;
		} else{
			return false;
		}
	}
	
	/**
	 * Returns all edges from given node. If node is not given in the graph, throws a NoSuchElementException.
	 * @param node - Node whose edges should be retrieved
	 * @exception NoSuchElementException if node is not given in the graph
	 * @return A Collection of all flow edges leaving the node
	 */

	@Override
	public Collection<FlowEdge<V>> edgesFrom(V node) {
		if(!containsNode(node)) {
			throw new NoSuchElementException();
		}
		Collection<FlowEdge<V>> returnColl = mainMap.get(node).values();
		Collection<FlowEdge<V>> removeList = new ArrayList<>();
		
		for(FlowEdge<V> edge : returnColl) {
			if(edge.getEnd() == node) {
				removeList.add(edge);
			}
		}
		
		returnColl.removeAll(removeList);
		
		return returnColl;
	}
	
	/**
	 * Returns a flow edge going from start to end. Returns null if flow edge is not present or at least one parameter is null.
	 * @param start - Start of this edge.
	 * @param end - End of this edge.
	 * @return A flow edge or null if one parameter is null or a flow edge is not present
	 */
	@Override
	public FlowEdge<V> getEdge(V start, V end) {
		if(start == null || end == null) {
			return null;
		} else {
			return mainMap.get(start).get(end); //if there is no edge is it null?
		}
	}


	
	
	/**
	 * returns team id
	 * @return team id as a String
	 */

	@Override
	public String getTeamIdentifier() {
		return "G03T03";
	}

}
