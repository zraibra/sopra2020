package solutions.exercise5;

import java.util.Map;
import java.util.Optional;
import org.sopra.api.exercises.ExerciseSubmission;
import org.sopra.api.exercises.exercise3.*;
import org.sopra.api.exercises.exercise4.*;
import org.sopra.api.exercises.exercise5.*;
import org.sopra.api.model.EnergyNode;
import org.sopra.api.model.Graph;
import org.sopra.api.model.PowerLine;
import org.sopra.api.model.consumer.Consumer;
import org.sopra.api.model.producer.Producer;
import solutions.exercise3.*;
import solutions.exercise4.*;

/**
 * This class calculates the maximum degree of capacity utilization of
 * powerlines, consumers and producers in an energy network
 * 
 * @author G03T03
 */
public class EnergyNetworkAnalyzerImpl extends AbstractEnergyNetworkAnalyzer implements ExerciseSubmission {

	/**
	 * Constructor of EnergyNetworkAnalyzerImpl. It invokes the constructor of the
	 * upper class
	 * 
	 * @param graph              the energy graph to be analyzed
	 * @param producerCapacities map which represents the degree of current capacity
	 *                           utilization of producers
	 * @param consumerCapacities map which represents the degree of current capacity
	 *                           utilization of consumers
	 */
	public EnergyNetworkAnalyzerImpl(Graph<EnergyNode, PowerLine> graph,
			Optional<Map<Producer, Integer>> producerCapacities, Optional<Map<Consumer, Integer>> consumerCapacities) {
		super(graph, producerCapacities, consumerCapacities);
	}

	/**
	 * Calculates the maximum flow between super source and super sink in a flow
	 * graph. The current capacity utilization of consumers, producers and
	 * powerlines is stored in maps "consumerLevels", "producerLevels" and
	 * "powerlineLevels"
	 * 
	 */
	public void calculateMaxFlow() {
		FordFulkerson<EnergyNode> ff = new FordFulkersonImpl<EnergyNode>();
		ff.findMaxFlow(flowGraph, super_source, super_sink);
		for (FlowEdge<EnergyNode> edge : flowGraph.edgesFrom(super_source)) {
			producerLevels.put((Producer) edge.getEnd(), edge.getFlow());
		}
		for (EnergyNode node : flowGraph.getNodes()) {
			if (node instanceof Consumer) {
				consumerLevels.put((Consumer) node, flowGraph.getEdge(node, super_sink).getFlow());
			}
		}
		for (FlowEdge<EnergyNode> edge : flowGraph.getEdges()) {

			for (PowerLine powerline : powerlineLevels.keySet()) {

				if ((powerline.getStart().getName() == edge.getStart().getName()
						|| powerline.getStart().getName() == edge.getEnd().getName())
						&& (powerline.getEnd().getName() == edge.getEnd().getName()
								|| powerline.getEnd().getName() == edge.getStart().getName())) {

					if (powerlineLevels.get(powerline) < edge.getFlow()) {
						powerlineLevels.replace(powerline, edge.getFlow());
					}
				}
			}
		}
	}

	/**
	 * turns a energy graph in a flow graph. All nodes stay the same but a
	 * super_source and super_sink is added. For each powerline, two flow edges with
	 * opposed directions and equal capacities are created. Additionally edges from
	 * super_sink/super_source to consumers/producers are created with either given
	 * producer/consumer capacities or provided/required energy level
	 * 
	 * @param graph              the energy graph to be analyzed
	 * @param producerCapacities map which represents the degree of current capacity
	 *                           utilization of producers
	 * @param consumerCapacities map which represents the degree of current capacity
	 *                           utilization of consumers
	 * @return
	 */
	public FlowGraph<EnergyNode> createFlowGraph(Graph<EnergyNode, PowerLine> graph,
			Optional<Map<Producer, Integer>> producerCapacities, Optional<Map<Consumer, Integer>> consumerCapacities) {
		FlowGraph<EnergyNode> flowgraph = new FlowGraphImpl<EnergyNode>();
		for (EnergyNode node : graph.getNodes()) {
			flowgraph.addNode(node);
		}

		for (PowerLine powerline : graph.getEdges()) {
			flowgraph.addEdge(powerline.getStart(), powerline.getEnd(), powerline.getCapacity());
			flowgraph.addEdge(powerline.getEnd(), powerline.getStart(), powerline.getCapacity());
		}
		flowgraph.addNode(super_source);
		flowgraph.addNode(super_sink);
		for (EnergyNode node : flowgraph.getNodes()) {
			if (node instanceof Producer) {
				if (producerCapacities.isEmpty()) {
					flowgraph.addEdge(super_source, node, ((Producer) node).getProvidedPower());
					flowgraph.addEdge(node, super_source, ((Producer) node).getProvidedPower());

				} else {
					flowgraph.addEdge(super_source, node, producerCapacities.get().get((Producer) node));
					flowgraph.addEdge(node, super_source, producerCapacities.get().get((Producer) node));

				}
			}
			if (node instanceof Consumer) {
				if (consumerCapacities.isEmpty()) {
					flowgraph.addEdge(node, super_sink, ((Consumer) node).getRequiredPower());
					flowgraph.addEdge(super_sink, node, ((Consumer) node).getRequiredPower());

				} else {
					flowgraph.addEdge(node, super_sink, consumerCapacities.get().get((Consumer) node));
					flowgraph.addEdge(super_sink, node, consumerCapacities.get().get((Consumer) node));

				}
			}
		}
		return flowgraph;
	}

	/**
	* Returns Team Identifier
	* @return String with team identification
	*/
	public String getTeamIdentifier() {
		return "G03T03";
	}

}

