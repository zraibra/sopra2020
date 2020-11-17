package solutions.exercise2;

import java.util.Comparator;

import org.sopra.api.ConstructionCostCalculatorImpl;
import org.sopra.api.Scenario;
import org.sopra.api.exercises.ExerciseSubmission;
import org.sopra.api.model.PlayfieldElement;
import org.sopra.api.model.producer.ProducerType;

/**
 *  This class provides a method to compare two PlayfieldElements of the same producerType
 * 
 * @author G03T03
 */


public class PlayfieldElementComparator implements ExerciseSubmission, Comparator<PlayfieldElement> {
	
	private ProducerType producerType;
	private ConstructionCostCalculatorImpl ccci;
	
	/**
	 * Constructor of a PlayfieldElementComparator, which creates a new instance of the class ConstructionCostCalculatorImpl
	 * object variables producerType and constructionCostCalculatorImpl are given a value
	 * 
	 * @param producerType type of the producer which will be compared
	 * @param scenario scenario data type the game is played on
	 * @exception NullPointerException if either scenario or producerType is null
	 */

	public PlayfieldElementComparator(ProducerType producerType, Scenario scenario) throws NullPointerException{
		if(producerType == null || scenario == null) {
			throw new NullPointerException("ProducerType or Scenario is not allowed to be null.");
		} else {
		ConstructionCostCalculatorImpl ccci = new ConstructionCostCalculatorImpl(scenario);
		this.producerType = producerType;
		this.ccci = ccci;
		}
	}
	

	/**
	 * compares two playfieldElements in their aptitude to build on it
	 * 
	 * @param e1 the first playfieldElement to be compared
	 * @param e2 the second playfieldElement to be compared
	 * @return a negative integer if the second playfieldElement is better, 
	 * 			zero if they are both suitable and a positive integer if the first one is better
	 * @exception NullPointerException if either one of the playfieldElements is null
	 */

	public int compare(PlayfieldElement e1, PlayfieldElement e2) throws NullPointerException {
		if(e1 == null || e2 == null) {
			throw new NullPointerException("Not allowed to be null.");
		} else {
		return (int) ccci.calculateCost(e2, producerType) - (int) ccci.calculateCost(e1, producerType);
		}
	}
	
	/**
	 * Returns Team Identifier
	 *
	 * @return String with team identifier
	 */
	public String getTeamIdentifier() {
		return "G03T03";
	}
}
