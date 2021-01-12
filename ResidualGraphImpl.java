package solutions.exercise3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.sopra.api.exercises.ExerciseSubmission;
import org.sopra.api.exercises.exercise3.FlowEdge;
import org.sopra.api.exercises.exercise3.FlowGraph;
import org.sopra.api.exercises.exercise3.ResidualEdge;
import org.sopra.api.exercises.exercise3.ResidualGraph;

public class ResidualGraphImpl<V> implements ResidualGraph<V>, ExerciseSubmission {

	/**
	 * map which represents the residual graph
	 */
	private Map<V, List<ResidualEdge<V>>> graph;
	
	/**
	 * constructor of this residual graph which initializes this graph with a HashMap and takes over the nodes and edges of the given flow graph 
	 * @param flowGraph - flow graph which entries will be converted into those of a residual graph
	 * @since 1.0
	 */
	
	public ResidualGraphImpl(FlowGraph<V> flowGraph) {
	//checks if flow graph is null
	if(flowGraph == null) {
		throw new IllegalArgumentException("flow graph is null");
	}
	
	//initializes graph/ the main map of this graph
	graph = new HashMap<V, List<ResidualEdge<V>>>();
	
	//adds node from the flow graph
	for(V node : flowGraph.getNodes()) {
		graph.put(node, new ArrayList<ResidualEdge<V>>());
	}	
	
	for(V node : flowGraph.getNodes()) {
		//converts flow edge into residual edge and puts it with reverse edge into the graph
		for(FlowEdge<V> flowEdge : flowGraph.edgesFrom(node)) {
			//checks if edges are already in graph
			if(!contains(flowEdge)) {
				//new edges
				ResidualEdgeImpl<V> edge1 = new ResidualEdgeImpl<V>(flowEdge.getStart(), flowEdge.getEnd(), flowEdge.getCapacity());
				ResidualEdgeImpl<V> edge2 = new ResidualEdgeImpl<V>(flowEdge.getEnd(), flowEdge.getStart(), flowEdge.getCapacity());
				
				//adding edges 
				graph.get(node).add(edge1);
				graph.get(flowEdge.getEnd()).add(edge2);
				
				//setting reverse edges
				edge1.setReverse(edge2);	//is the reference still the same?
				edge2.setReverse(edge1);
				
				//add flow
				edge1.addFlow(flowEdge.getFlow());
			} else {
				//adding flow
				getEdge(node, flowEdge.getEnd()).addFlow(flowEdge.getFlow());
			}
		}
	}
	
	
	}

	/**
	 * gives back the nodes starting from the given node
	 * @param node - the starting node which the edges start from
	 * @return a list of all residual edges which start from the given node
	 * @exception NoSuchElementException - if node doesn't exist
	 * @since 1.0
	 */
	
	@Override
	public List<ResidualEdge<V>> edgesFrom(V node) {
		//throws exception if node doesn't exist
		if(node == null || !contains(node)) {
			throw new NoSuchElementException();
		}
		//exception if node is null?
		
		//returning list of edges
		List<ResidualEdge<V>> returnList = graph.get(node);
		
		//remove list
		List<ResidualEdge<V>> removeList = new ArrayList<>();
		
		//putting incoming edges in remove list
		for(ResidualEdge<V> edge : returnList) {
			if(edge.getEnd() == node) {
				removeList.add(edge);
			}
		}
		
		//removing incoming edges
		returnList.removeAll(removeList);
		
		return returnList;
		
	}

	/**
	 * returns the edge between the given start and end node
	 * @param start - start node
	 * @param end - end node
	 * @return the residual edge between start and end node or null if the edge doesn't exist
	 * @exception IllegalArgumentException - if one parameter is null
	 * @exception NoSuchElementException - if at least one node is not contained in graph
	 * @since 1.0
	 */
	
	@Override
	public ResidualEdge<V> getEdge(V start, V end) {
		if(start == null || end == null) {
			throw new IllegalArgumentException("at least one node is null");
		}
		
		if(!contains(start) || !contains(end)) {
			throw new NoSuchElementException("at least one node is not contained in the graph");
		}
		
		//checking all elements from the arraylist of the start node if the edge is there
		for(ResidualEdge<V> edge : graph.get(start)) {
			//checks for endnode because startnode is already used to get the list
			if(edge.getEnd() == end) {
				return edge;
			}
		}
		return null;
	}

	/**
	 * gets all edges of this graph
	 * @return a list of all residual edges of this graph, if there is no edge inside this graph it will return an empty list
	 * @since 1.0
	 */
	
	@Override
	public List<ResidualEdge<V>> getEdges() {
		//new list which will be returned
		List<ResidualEdge<V>> returnList = new ArrayList<>();
		
		//goes through all nodes of this graph
		for(V node : getNodes()) {
			//adds all edges of this List into the returnlist
			returnList.addAll(graph.get(node));
		}
		return returnList;
	}

	/**
	 * returns all nodes of this graph
	 * @return a set of all nodes 
	 * @since 1.0
	 */
	
	@Override
	public Set<V> getNodes() {
		return graph.keySet();
	}
	
	/**
	 * checks if graph contains a given edge
	 * @param edge - given edge which method tries to find in graph
	 * @return true if graph contains edge, false if not
	 * @exception IllegalArgumentException - if given edge is null
	 * @since 1.0
	 */
	
	private boolean contains(org.sopra.api.model.Edge<V> e) {
		if(e == null) {
			throw new IllegalArgumentException();
		}
		
		//checks if start node exists
		if(contains(e.getStart())) {
			//checks every edge of list if there is one which has the same end node
			for(ResidualEdge<V> edge : graph.get(e.getStart())) {
					if(edge.getEnd() == e.getEnd()) {
						return true;
					}
			}
		}	
		return false;
	}
	
	/**
	 * checks if graph contains a given node, probably better that contains method of collection because that method only compares objects(class)
	 * @param node - node which is to be checked, which also has generic type
	 * @return true if graph contains this node, false if not
	 * @exception IllegalArgumentException - if node is null
	 * @since 1.0
	 */
	
	private boolean contains(V node) {
		if(node == null) {
			throw new IllegalArgumentException();
		}
		
		//checks if node exists
		for(V testNode : getNodes()) {
			if( node == testNode) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * returns team id
	 * @return team id as a String
	 * @since 1.0
	 */
	
	@Override
	public String getTeamIdentifier() {
		return "G03T03";
	}
}
