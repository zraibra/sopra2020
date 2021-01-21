package solutions.exercise5;

import java.util.ArrayList;
import java.util.List;

import org.sopra.api.Scenario;
import org.sopra.api.exercises.ExerciseSubmission;
import org.sopra.api.exercises.exercise5.AbstractSunEnergyBroker;
import org.sopra.api.model.EnergyNode;
import org.sopra.api.model.consumer.ControllableConsumer;
import org.sopra.api.model.producer.SolarPowerPlant;
import org.sopra.exceptions.CannotAssignCommandException;

public class SunEnergyBroker extends AbstractSunEnergyBroker implements ExerciseSubmission {

	@Override
	public void executionPhase(Scenario scenario, int round) {
		//requested energy has to be satisfied and at least 50% of produced energy has to be consumed
		
		//translate round into daytime
		int daytime = round % 24;
		
		//get SolarPowerPlant and IndustrialPark list
		List<SolarPowerPlant> spplist = new ArrayList<>();
		List<ControllableConsumer> iplist = new ArrayList<>();
		
		//variable for maximum produceable amount of energy
		int maxproduce = 0;
		
		for(EnergyNode node : scenario.getGraph().getNodes()) {
			
			//check if node is a industrial park
			if(node instanceof ControllableConsumer) {
				iplist.add((ControllableConsumer) node);	//don't like casting, but it seems like there is no other way
			}
			
			if(node instanceof SolarPowerPlant) {
				spplist.add((SolarPowerPlant) node);
				maxproduce += node.getMaximumEnergyLevel();
			}
		}
		
		//calculate max produced energy
		int mp30 = (int) (maxproduce * 0.3);
		int mp40 = (int) (maxproduce * 0.4);
		
		
		// in specific rounds sun power plants will increase/ decrease output to 30%,60% or 100% so industrial park output has to be assigned in the daytime -3  
		System.out.println("round: " + round);
		for(ControllableConsumer ipark : iplist) {
			System.out.println("name: " + ipark.getName());
			System.out.println("req: " + ipark.getRequiredPower() + " energy level: " + ipark.getEnergyLevel());
			
		}
		for(SolarPowerPlant spp : spplist) {
			System.out.println("max produce: " + spp.getProvidedPower() + " energy level: " + spp.getEnergyLevel());
		}
		switch(daytime) {
		
		//decrease to 0% at the first round
		case 0: 
				for(ControllableConsumer ipark : iplist) {
					try {
						if(ipark.getRequiredPower() == 900) 
							scenario.getCommandFactory().createAdjustConsumerCommand(ipark, -900).assign();
					} catch (CannotAssignCommandException e) {
						e.printStackTrace();
					}
				}
				break;
		
		//increase to 30% 
		case 1: 
				changeConsumption(iplist, mp30, scenario, true); 
				break;	
		//increase to 60%
		case 4:	changeConsumption(iplist, mp30, scenario, true); 
				break;	
			
		//increase to 100%
		case 7: changeConsumption(iplist, mp40, scenario, true); 
				break;	
			
		//decrease to 60%
		case 12: changeConsumption(iplist, mp40, scenario, false); 
				 break;
			
		//decrease to 30%
		case 15: changeConsumption(iplist, mp30, scenario, false); 
				 break;
			
		//decrease to 0%
		case 19: changeConsumption(iplist, mp30, scenario, false); 
		 		 break;	
		default: break;	
		}
		
	}
	
	private void changeConsumption (List<ControllableConsumer> iplist, int budget, Scenario scenario, boolean increase) {
		
		//increases energy level of every industrial park until the budget is used up
		for(ControllableConsumer ipark : iplist) {
			//check if consumption should be increased or decreased
			if(increase) {
				//check if industrial park can increase consumption
				if(ipark.getRequiredPower() != 900) {
					try {
						
						//System.out.println(budget + " und " + ipark.getRequiredPower() + "Bedingung" + (budget + ipark.getRequiredPower() >= 900));
						//check if increasing the energy level of the industrial park by the remaining budget would increase the energy level over the max consumption level
						if(budget + ipark.getRequiredPower() >= 900) {
							scenario.getCommandFactory().createAdjustConsumerCommand(ipark, 900 - ipark.getRequiredPower()).assign();

							//adjust budget
							budget -= (900-ipark.getRequiredPower());
						} else {
							scenario.getCommandFactory().createAdjustConsumerCommand(ipark, budget).assign();

							//adjust budget
							budget = 0;
						}
					} catch (CannotAssignCommandException e) {
						//System.out.println("couldn't increase industrial park consumption at position x = " + ipark.getXPos() + " and y = " + ipark.getYPos() + " from " + ipark.getRequiredPower() + " with " + (900-ipark.getRequiredPower()));
						System.out.println(e);
						//System.out.println(budget);
					}
			
				}
			} else {
				
				//check if industrial park can decrease consumption
				if(ipark.getRequiredPower() != 0) {
					try {
						
						//check if decreasing the consumption level with the remaining budget would put it below 0
						if(ipark.getRequiredPower() - budget <= 0) {
							scenario.getCommandFactory().createAdjustConsumerCommand(ipark, -ipark.getRequiredPower()).assign();
							
							//adjust budget
							budget -= ipark.getRequiredPower();
						} else {
							scenario.getCommandFactory().createAdjustConsumerCommand(ipark, -budget).assign();
							
							//adjust budget
							budget = 0;
						}
					} catch (CannotAssignCommandException e) {
						//System.out.println("couldn't decrease industrial park consumption at position x = " + ipark.getXPos() + " and y = " + ipark.getYPos() + " from " + ipark.getRequiredPower() + " with " + (0 - ipark.getRequiredPower()));
						System.out.println(e);
					}
			
				}
			}	
		}
	}
	/**
	* Returns Team Identifier
	* @return String with team identification
	*/
	@Override
	public String getTeamIdentifier() {
		return "G03T03";
	}

}
