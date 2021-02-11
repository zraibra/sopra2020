package solutions.exercise6;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.sopra.api.model.consumer.Consumer;
import org.sopra.api.model.consumer.ControllableConsumer;
import org.sopra.api.model.consumer.IndustrialPark;
import org.sopra.api.model.producer.BioGasFiredPowerPlant;
import org.sopra.api.model.producer.CoalFiredPowerPlant;
import org.sopra.api.model.producer.ControllableProducer;
import org.sopra.api.model.producer.GasFiredPowerPlant;
import org.sopra.api.model.producer.HydroPowerPlant;
import org.sopra.api.model.producer.NuclearPowerPlant;
import org.sopra.api.model.producer.Producer;
import org.sopra.api.model.producer.ProducerType;
import org.sopra.api.model.producer.SolarPowerPlant;
import org.sopra.api.model.producer.WindPowerPlant;
import org.sopra.exceptions.CannotAssignCommandException;
import org.sopra.exceptions.CannotExecuteCommandException;

import solutions.exercise5.EnergyNetworkAnalyzerImpl;

public class ScenarioImpl implements ExerciseSubmission, Game {

//factor which whill determine the ratio windpowerplants will be calculated
	int windppMaxProduce = 0;
	double windRatio = 0.3;

//Look up Table for Energylevels 
	Map<Integer, Map<Producer, Integer>> lut = new HashMap<>();

//List of nodes ordered with descending adjustment time
	
	@Override
	public void buildPhase(Scenario scenario) {
		//variable max consumption as budget
		int maxconsume = 0;
		//adding maxEnergyLevel to maxconsume
		for(EnergyNode node : scenario.getGraph().getNodes()) {
			if(node instanceof Consumer) {
				maxconsume += node.getMaximumEnergyLevel();
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
		
		//build gas and hydro power plants, only if max consume budget is still higher than zero
		for(PlantLocation transformer : transformerlist) {
			if(!transformer.isBuilt() && maxconsume > 0) {
				try {
					
					//check if playfield is sea, beach or river, if it is, then it will build a hydro power plant, else it will built gas power plants
					if(transformer.getPlayfieldElement().getElementType() == ElementType.SEA || transformer.getPlayfieldElement().getElementType() == ElementType.BEACH || transformer.getPlayfieldElement().getElementType() == ElementType.RIVER) {
						scenario.getCommandFactory().createBuildPlantCommand(transformer, ProducerType.HYDRO_POWER_PLANT).execute();
						if(transformer.getPlayfieldElement().getElementType() == ElementType.BEACH) {
							maxconsume -= 540;
						} else {
							maxconsume -= 600;
						}
					} else {
						scenario.getCommandFactory().createBuildPlantCommand(transformer, ProducerType.GASFIRED_POWER_PLANT).execute();
						maxconsume -= 500;
					}
				} catch (CannotExecuteCommandException e) {
					System.out.println("couldn't build hydro or gas power plant on top of" + transformer.getPlayfieldElement().getElementType());
					e.printStackTrace();
				}
			}
		}
		
		//upgrade all powerlines to high voltage
		int i = 0;
		do {
			i = 0;
			Map<PowerLine, Integer> powerlineMap = predictHour(scenario, 8, true).getPowerLineLevels();
			for(PowerLine powerline : powerlineMap.keySet()) {
				try {
					if(powerline.getType() == PowerLineType.LOW_VOLTAGE && powerline.getCapacity() == powerlineMap.get(powerline)) {
						scenario.getCommandFactory().createUpgradeLineCommand(powerline, PowerLineType.MEDIUM_VOLTAGE).execute();
						i++;
					}
					if(powerline.getType() == PowerLineType.MEDIUM_VOLTAGE && powerline.getCapacity() == powerlineMap.get(powerline)) {
						scenario.getCommandFactory().createUpgradeLineCommand(powerline, PowerLineType.HIGH_VOLTAGE).execute();
						i++;
					}
				} catch (CannotExecuteCommandException e) {
					System.out.println("couldn't upgrade powerline");
					e.printStackTrace();
				}
			}
		} while (i > 0);
			
		//generate Look Up Table
		for(int hour = 0; hour < 24; hour++) {
			lut.put(hour, predictHour(scenario, hour, false).getProducerLevels());
		}
	}

	
	
	@Override
	public void executionPhase(Scenario scenario, int round) {
		//calculate daytime
		int hour = round % 24;
		
		//calculate change of windpowerlevel
		double windchange = windppMaxProduce*scenario.getStatistics().getWindStrength() - windppMaxProduce*windRatio;
		
		//calculate difference between max output and energy level of hydro pp
		int hydroDiff = 0;
		
		for(EnergyNode node : scenario.getGraph().getNodes()) {
			if(node instanceof HydroPowerPlant) {
					if(lut.get(hour).get(node) - node.getMaximumEnergyLevel() < 0) hydroDiff += lut.get(hour).get(node) - node.getMaximumEnergyLevel();		
			}
		}
		//differences which have to be newly calculated every round, used as a budget
		int difference3 = 0;
		int difference2 = 0;
		int difference = 0;
		
		if(round == 0) {
			for(EnergyNode node : scenario.getGraph().getNodes()) {
				if(node instanceof IndustrialPark) {
					try {
						scenario.getCommandFactory().createAdjustConsumerCommand((ControllableConsumer) node, -900).assign();
					} catch (CannotAssignCommandException e) {
						e.printStackTrace();
					}
				}
				if(node instanceof  ControllableProducer) {
					try {
						scenario.getCommandFactory().createAdjustProducerCommand((ControllableProducer) node, lut.get(0).get(node)).assign();
					} catch (CannotAssignCommandException e) {
						e.printStackTrace();
					}
				}
				
				if(node instanceof WindPowerPlant) {
					windppMaxProduce += node.getMaximumEnergyLevel();
				}
			}
		} else {
			for(EnergyNode node : scenario.getGraph().getNodes()) {
				int change = 0;
				
				if(node instanceof NuclearPowerPlant) {
					if(!((NuclearPowerPlant) node).isAdjusting()) {
						if(hour < 15) {
							change = lut.get(hour + 9).get(node) - ((Producer) node).getProvidedPower();
						} else {
							change = lut.get(hour - 15).get(node) - ((Producer) node).getProvidedPower();
						}
						try {
							scenario.getCommandFactory().createAdjustProducerCommand((ControllableProducer) node, change).assign();
						} catch (CannotAssignCommandException e) {
							e.printStackTrace();
						}
					} else {
						//changes in smaller time frames will be compensated through other producer with lower adjustmenttime
						if(hour < 22) {
							difference3 += lut.get(hour + 2).get(node) - ((Producer) node).getProvidedPower();
						} else {
							difference3 += lut.get(hour - 22).get(node) - ((Producer) node).getProvidedPower();
						}
					}
				}
				if(node instanceof CoalFiredPowerPlant) {
					
					if(!((ControllableProducer) node).isAdjusting()) {
						if(hour < 22) {
							//change = lut.get(hour + 2).get(node) - ((Producer) node).getProvidedPower() + difference3;
							
							//check if change is too high
							if (node.getMaximumEnergyLevel() < lut.get(hour + 2).get(node) +difference3) {
								change = node.getMaximumEnergyLevel() - ((Producer)node).getProvidedPower();
								difference3 -= node.getMaximumEnergyLevel() - lut.get(hour + 2).get(node);
							} else if (0 > lut.get(hour + 2).get(node) +difference3) {
								change = - ((Producer)node).getProvidedPower();
								difference3 += lut.get(hour + 2).get(node);
							} else {
								change = lut.get(hour + 2).get(node) - ((Producer)node).getProvidedPower() +difference3;
								difference3 = 0;
							}
							
						} else {
							//change = lut.get(hour - 22).get(node) - ((Producer) node).getProvidedPower() + difference3;
						
							//check if change is too high
							if (node.getMaximumEnergyLevel() < lut.get(hour - 22).get(node) +difference3) {
								change = node.getMaximumEnergyLevel() - ((Producer)node).getProvidedPower();
								difference3 -= node.getMaximumEnergyLevel() - lut.get(hour - 22).get(node);
							} else if (0 > lut.get(hour - 22).get(node) +difference3) {
								change = - ((Producer)node).getProvidedPower();
								difference3 += lut.get(hour - 22).get(node);
							} else {
								change = lut.get(hour - 22).get(node) - ((Producer)node).getProvidedPower() +difference3;
								difference3 = 0;
							}
						}
						try {
							scenario.getCommandFactory().createAdjustProducerCommand((ControllableProducer) node, change).assign();
						}  catch (CannotAssignCommandException e) {
							e.printStackTrace();
						}
					} else {
						//changes in smaller time frames will be compensated through other producer with lower adjustmenttime
						if(hour != 23) {
							difference2 += lut.get(hour + 1).get(node) - ((Producer) node).getProvidedPower();
						} else {
							difference2 += lut.get(hour - 23).get(node) - ((Producer) node).getProvidedPower();
						}
					}
				}
				
				if(node instanceof BioGasFiredPowerPlant) {
					if(!((ControllableProducer) node).isAdjusting()) {
						if(hour < 23) {
							
							//check if change is too high
							if (node.getMaximumEnergyLevel() < lut.get(hour + 1).get(node) + difference2) {
								change = node.getMaximumEnergyLevel() - ((Producer)node).getProvidedPower();
								difference2 -= node.getMaximumEnergyLevel() - lut.get(hour + 1).get(node);
							} else if (0 > lut.get(hour + 1).get(node) + difference2) {
								change = - ((Producer)node).getProvidedPower();
								difference2 += lut.get(hour + 1).get(node);
							} else {
								change = lut.get(hour + 1).get(node) - ((Producer)node).getProvidedPower() + difference2;
								difference2 = 0;
							}
						} else {
							
							//check if change is too high
							if (node.getMaximumEnergyLevel() < lut.get(hour - 23).get(node) + difference2) {
								change = node.getMaximumEnergyLevel() - ((Producer)node).getProvidedPower();
								difference2 -= node.getMaximumEnergyLevel() - lut.get(hour - 23).get(node);
							} else if (0 > lut.get(hour - 23).get(node) + difference2) {
								change = - ((Producer)node).getProvidedPower();
								difference2 += lut.get(hour - 23).get(node);
							} else {
								change = lut.get(hour - 23).get(node) - ((Producer)node).getProvidedPower() + difference2;
								difference2 = 0;
							}
						}
						try {
							scenario.getCommandFactory().createAdjustProducerCommand((ControllableProducer) node, change).assign();
						}  catch (CannotAssignCommandException e) {
							e.printStackTrace();
						}
					} else {
						
						//changes in smaller time frames will be compensated through other producer with lower adjustmenttime
						difference += lut.get(hour).get(node) - ((Producer) node).getProvidedPower();
					}
				}	
				
				//gas power plants will be used to compensate the wind power plants
				if(node instanceof GasFiredPowerPlant) {
					//checks if change would be too high, or too low and corrects budget terms
					if(node.getMaximumEnergyLevel() < lut.get(hour).get(node) + difference) {
						change = node.getMaximumEnergyLevel() - ((Producer)node).getProvidedPower();
						difference -= node.getMaximumEnergyLevel() - lut.get(hour).get(node);
					} else if (0 > lut.get(hour).get(node) + difference) {
						change = - ((Producer)node).getProvidedPower();
						difference += lut.get(hour).get(node);
					} else if (difference != 0) {
						change = lut.get(hour).get(node) - ((Producer)node).getProvidedPower() + difference;
						difference = 0;
						
					//if there are still some gas power plants free, they will adjust to wind power plants	
					} else if (node.getMaximumEnergyLevel() < lut.get(hour).get(node) + windchange) {
						change = node.getMaximumEnergyLevel() - ((Producer)node).getProvidedPower();
						windchange -= node.getMaximumEnergyLevel() - lut.get(hour).get(node);
					} else if (0 > lut.get(hour).get(node) + windchange) {
						change = - ((Producer)node).getProvidedPower();
						windchange += lut.get(hour).get(node);
					} else if (windchange != 0) {
						change = (int) (lut.get(hour).get(node) - ((Producer)node).getProvidedPower() + windchange);
						windchange = 0;
					} else {
						change = lut.get(hour).get(node) -  ((Producer)node).getProvidedPower();
					}
					
					//if there are still some gas power plants free, they will adjust to water power plants	
					if (0 > change + ((Producer)node).getProvidedPower() + hydroDiff) {
						hydroDiff += change + ((Producer)node).getProvidedPower();
						change = -((Producer)node).getProvidedPower();
					} else if (hydroDiff != 0) {
						change += hydroDiff;
						hydroDiff = 0;
					} 
					//adjust power level
					try {
						scenario.getCommandFactory().createAdjustProducerCommand((ControllableProducer) node, change).assign();
					} catch (CannotAssignCommandException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private EnergyNetworkAnalyzerImpl predictHour(Scenario scenario, int hour, boolean pwrlineCalc) {
		Map<Producer, Integer> producerlevels = new HashMap<>();
		Map<Consumer, Integer> consumerlevels = new HashMap<>();
		
		//iterates through all nodes and puts their respective level into the maps
		for(EnergyNode node : scenario.getGraph().getNodes()) {
			
			if(node instanceof SolarPowerPlant) {
				producerlevels.put((Producer) node, (int)(node.getMaximumEnergyLevel()*scenario.getStatistics().getSunIntensityPerDay()[hour]));
			
			} else if(node instanceof WindPowerPlant) {
				producerlevels.put((Producer) node, (int) (node.getMaximumEnergyLevel()*windRatio)); 
		
			} else if(node instanceof City) {
				consumerlevels.put((Consumer) node, (int) (node.getMaximumEnergyLevel()*scenario.getEnergyNodeConfig().getLoadProfileCity()[hour]));
			
			} else if(node instanceof CommercialPark) {
				consumerlevels.put((Consumer) node, (int) (node.getMaximumEnergyLevel()*scenario.getEnergyNodeConfig().getLoadProfileCommercialPark()[hour]));
			
			} else if(node instanceof IndustrialPark) {
				if(pwrlineCalc) { 
					consumerlevels.put((Consumer) node, node.getMaximumEnergyLevel());
				} else {
					consumerlevels.put((Consumer) node, 0);
				}
			
			} else if(node instanceof Producer){
				producerlevels.put((Producer) node, node.getMaximumEnergyLevel());
			}
		}
		
		Optional<Map<Producer, Integer>> producerOptional = Optional.of(producerlevels);
		Optional<Map<Consumer, Integer>> consumerOptional = Optional.of(consumerlevels);
		
		EnergyNetworkAnalyzerImpl analyzer = new EnergyNetworkAnalyzerImpl(scenario.getGraph(), producerOptional, consumerOptional);
		
		analyzer.calculateMaxFlow();
		
		return analyzer;
	}
	
	@Override
	public String getTeamIdentifier() {
		return "G03T03";
	}

}
