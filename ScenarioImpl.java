package solutions.exercise6;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.sopra.api.ConstructionCostCalculator;
import org.sopra.api.Game;
import org.sopra.api.Scenario;
import org.sopra.api.exercises.ExerciseSubmission;
import org.sopra.api.model.EnergyNode;
import org.sopra.api.model.PlantLocation;
import org.sopra.api.model.PlayfieldElement.ElementType;
import org.sopra.api.model.PowerLine;
import org.sopra.api.model.PowerLineType;
import org.sopra.api.model.consumer.City;
import org.sopra.api.model.consumer.CommercialPark;
import org.sopra.api.model.consumer.IndustrialPark;
import org.sopra.api.model.producer.GasFiredPowerPlant;
import org.sopra.api.model.producer.HydroPowerPlant;
import org.sopra.api.model.producer.Producer;
import org.sopra.api.model.producer.ProducerType;
import org.sopra.api.model.producer.SolarPowerPlant;
import org.sopra.exceptions.CannotAssignCommandException;
import org.sopra.exceptions.CannotExecuteCommandException;

public class ScenarioImpl implements ExerciseSubmission, Game {

//variables for max consumable energy level, C - city, CP - commercial park, IP - industrial park and max produceable energy H - Hydro power plant, S - Solar Power plant 
	int maxconsumeC = 0;
	int maxconsumeCP = 0;
	int maxconsumeIP = 0;
	int maxproduceH = 0;
	int maxproduceS = 0;

//list with controllable consumer and producer
	ArrayList<GasFiredPowerPlant> gaspowerplants = new ArrayList<>();
	ArrayList<IndustrialPark> industrialparks = new ArrayList<>();
	
	@Override
	public void buildPhase(Scenario scenario) {
		
		//adding maxEnergyLevel to maxconsume
		for(EnergyNode node : scenario.getGraph().getNodes()) {
			if(node instanceof City) {
				maxconsumeC += node.getMaximumEnergyLevel();
			}
					
			if(node instanceof CommercialPark) {
				maxconsumeCP += node.getMaximumEnergyLevel();
			}
			
			if(node instanceof IndustrialPark) {
				maxconsumeIP += node.getMaximumEnergyLevel();
			}
		}
		
		//list of all transformer stations 
		List<PlantLocation> transformerlist = new ArrayList<>();
		for(PlantLocation plantlocation : scenario.getPlantLocations()) {
			if(plantlocation.getTransformerStation().isPresent()) {
				transformerlist.add(plantlocation);
			}
		}
		
		//create cost map for gas power plant
		HashMap<PlantLocation, Double> costMap = new HashMap<>();
		
		//create ConstructionCostCalculator
		ConstructionCostCalculator constrcalc = scenario.getCostCalculatorFactory().createConstructionCostCalculator();

		//put every transformer into map with their respective cost for building a gas power plant
		for(PlantLocation transformer : transformerlist) {
			if(transformer.getPlayfieldElement().getElementType() != ElementType.SEA)costMap.put(transformer, constrcalc.calculateCost(transformer.getPlayfieldElement(), ProducerType.GASFIRED_POWER_PLANT));
			//puts transformer stations at sea in list, so they will be sorted to be the first ones, but later they will be filtered
			else costMap.put(transformer, 0.0);
		}
		
		//sort list with ascending costs (simplesort)
		for(int a = 0; a < transformerlist.size(); a++) {
			
			//a + 1, because don't need to check if cost of a < cost of a
			for(int b = a + 1; b < transformerlist.size(); b++) {
				if(costMap.get(transformerlist.get(b)) < costMap.get(transformerlist.get(a))) {
					
					//switch both nodes
					PlantLocation help = transformerlist.set(a, transformerlist.get(b));
					transformerlist.set(b, help);
				}
				
			}
			
		}
		
		//budget variable
		double budget = maxconsumeC + maxconsumeCP;
		
		//build gaspowerplants so that max consume level can be produced 
		for(PlantLocation transformer : transformerlist) {
			
			//as long as the max consumption isn't fully covered and the PlayfieldElement is not sea power plants are built
			if(budget > 0 && transformer.getPlayfieldElement().getElementType() != ElementType.SEA) {
				try {
					scenario.getCommandFactory().createBuildPlantCommand(transformer, ProducerType.GASFIRED_POWER_PLANT).execute();
					budget -= 500;
				} catch (CannotExecuteCommandException e) {
					System.out.println("problem building a gas power plant");
					e.printStackTrace();
				}
			}
		}
		
		//fill lists with nodes
		for(EnergyNode node : scenario.getGraph().getNodes()) {
			if(node instanceof GasFiredPowerPlant) {
				gaspowerplants.add((GasFiredPowerPlant) node);
			}
			
			if(node instanceof IndustrialPark) {
				industrialparks.add((IndustrialPark) node);
			}
		}
		
		//maxconsume budget which should be covered with hydro and solar power plants
		budget = maxconsumeIP;
		
		//build not controllable producer for industrial power plants
		for(PlantLocation transformer : transformerlist) {
			if(budget > 0 && !transformer.isBuilt()) {
				try {
					
					//check if playfield is sea, beach or river, if it is, then it will build a hydro power plant, else it will built solar power plants
					if(transformer.getPlayfieldElement().getElementType() == ElementType.SEA || transformer.getPlayfieldElement().getElementType() == ElementType.BEACH || transformer.getPlayfieldElement().getElementType() == ElementType.RIVER) {
						scenario.getCommandFactory().createBuildPlantCommand(transformer, ProducerType.HYDRO_POWER_PLANT).execute();
						
						budget -= 600*transformer.getLocationMultiplicator();
					} else {
						scenario.getCommandFactory().createBuildPlantCommand(transformer, ProducerType.SOLAR_POWER_PLANT).execute();
						budget -= 200*transformer.getLocationMultiplicator();
					}
				} catch (CannotExecuteCommandException e) {
					System.out.println("couldn't build solar or hydro power plant");
					e.printStackTrace();
				}
			}
		}
		
		//calculate max produce for hydro power plant and solar power plant
		for(EnergyNode node : scenario.getGraph().getNodes()) {
			if(node instanceof HydroPowerPlant) {
				maxproduceH += node.getMaximumEnergyLevel();
			} else if(node instanceof SolarPowerPlant) {
				maxproduceS += node.getMaximumEnergyLevel();
			}
		}
		
		//upgrade all powerlines to high voltage
		for(PowerLine powerline : scenario.getGraph().getEdges()) {
			try {
				scenario.getCommandFactory().createUpgradeLineCommand(powerline, PowerLineType.HIGH_VOLTAGE).execute();
			} catch (CannotExecuteCommandException e) {
				System.out.println("couldn't upgrade powerline");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void executionPhase(Scenario scenario, int round) {
		//calculate daytime
		int hour = round % 24;
		
		//budget variables of consumer and producer
		int conbudget;
		int probudget;
		
		//calculate amount of required energy which has to be adjusted maxconsume(now) 
		if(round == 0) {
			conbudget = (int) (maxconsumeC*scenario.getEnergyNodeConfig().getLoadProfileCity()[hour] + maxconsumeCP*scenario.getEnergyNodeConfig().getLoadProfileCommercialPark()[hour]);
		} else if(hour != 0) {
			conbudget = (int) (maxconsumeC*scenario.getEnergyNodeConfig().getLoadProfileCity()[hour] + maxconsumeCP*scenario.getEnergyNodeConfig().getLoadProfileCommercialPark()[hour] - maxconsumeC*scenario.getEnergyNodeConfig().getLoadProfileCity()[hour-1] - maxconsumeCP*scenario.getEnergyNodeConfig().getLoadProfileCommercialPark()[hour-1]);
		} else {
			conbudget = (int) (maxconsumeC*scenario.getEnergyNodeConfig().getLoadProfileCity()[hour] + maxconsumeCP*scenario.getEnergyNodeConfig().getLoadProfileCommercialPark()[hour] - maxconsumeC*scenario.getEnergyNodeConfig().getLoadProfileCity()[23] - maxconsumeCP*scenario.getEnergyNodeConfig().getLoadProfileCommercialPark()[23]);
		}
		
		//calculate increase or decrease of produced energy amount, that controllable consumer can be adjusted, maxproduce(in three hours) - maxproduce(right now)
		if(hour < 22) {
			probudget = (int) (maxproduceS*scenario.getStatistics().getSunIntensityPerDay()[hour+2] - maxproduceS*scenario.getStatistics().getSunIntensityPerDay()[hour]); 
		} else {
			probudget = (int) (maxproduceS*scenario.getStatistics().getSunIntensityPerDay()[hour-22] - maxproduceS*scenario.getStatistics().getSunIntensityPerDay()[hour]);
		}
		
	
		//increase or decrease output of gas power plants, it will always try to get budget = 0
		for(GasFiredPowerPlant gfpp : gaspowerplants) {
			
			//check if production has to be increased or decreased
			if(conbudget > 0) {
				try {
					
					//check if budget is higher than possible increase, or lower
					if(conbudget > gfpp.getMaximumEnergyLevel() - gfpp.getProvidedPower()) {
						scenario.getCommandFactory().createAdjustProducerCommand(gfpp, gfpp.getMaximumEnergyLevel()-gfpp.getProvidedPower()).assign();
						conbudget -= (gfpp.getMaximumEnergyLevel()-gfpp.getProvidedPower());
					} else {
						scenario.getCommandFactory().createAdjustProducerCommand(gfpp, conbudget).assign();
						conbudget = 0;
					}
				} catch (CannotAssignCommandException e) {
					System.out.println("couldn't increase output of power plants");
					e.printStackTrace();
				}
			} else if(conbudget < 0) {
				try {
					
					//check if budget is lower than possible decrease 
					if(conbudget < -gfpp.getProvidedPower()) {
						scenario.getCommandFactory().createAdjustProducerCommand(gfpp, -gfpp.getProvidedPower()).assign();
						conbudget += gfpp.getProvidedPower();
					} else {
						scenario.getCommandFactory().createAdjustProducerCommand(gfpp, conbudget).assign();
						conbudget = 0;
					}
				} catch (CannotAssignCommandException e) {
					System.out.println("couldn't decrease output of power plants");
					e.printStackTrace();
				}
			}
		}
	
		//increases or decreases consumption of industrial park in first round, or if they are not already adjusting
		for(IndustrialPark ip : industrialparks) {
			if(round == 0) {
				try {

					//adjusts to steady power level of hydro power plant, beware in round 0 all industrial parks require max energy level
					if(maxproduceH > ip.getRequiredPower()) {
						maxproduceH -= ip.getRequiredPower();
					} else {
						scenario.getCommandFactory().createAdjustConsumerCommand(ip, maxproduceH - ip.getRequiredPower()).assign();
						maxproduceH = 0;
					}
				} catch (CannotAssignCommandException e){
					System.out.println("couldn't adjust industrial park " + ip.getName() + " in the first round");
					e.printStackTrace();
				}
			} else if(!ip.isAdjusting() && probudget != 0) {
				try {
					
					//adjusts industrial park to the changing production level of the solar power plants
					if(probudget > ip.getMaximumEnergyLevel() - ip.getRequiredPower()) {
						scenario.getCommandFactory().createAdjustConsumerCommand(ip, ip.getMaximumEnergyLevel() - ip.getRequiredPower()).assign();
						probudget -= (ip.getMaximumEnergyLevel() - ip.getRequiredPower());
					} else if (probudget < -ip.getRequiredPower()) {
						scenario.getCommandFactory().createAdjustConsumerCommand(ip, -ip.getRequiredPower()).assign();
						probudget += ip.getRequiredPower();
					} else if (probudget < 0 || probudget > 0) {
						scenario.getCommandFactory().createAdjustConsumerCommand(ip, probudget).assign();
						probudget = 0;
					}
				} catch (CannotAssignCommandException e) {
					System.out.println("couldn't adjust industrial park " + ip.getName() + " at hour: " + hour);
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public String getTeamIdentifier() {
		return "G03T03";
	}

}
