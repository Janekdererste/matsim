package playground.balac.allcsmodestest.utils;


import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class CountMembersWIthCar {
	public void run(String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
		populationReader.readFile(args[0]);		
		
		
		int count = 0;
		for (Person person: scenario.getPopulation().getPersons().values()) {
			
			if (PersonUtils.getTravelcards(person) != null && PersonUtils.getTravelcards(person).contains("ch-HT-mobility"))
				if (!PersonUtils.getCarAvail(person).equals("never")) {
					
					count++;
				}
		}
	
		System.out.println(count);
	}
	public static void main(String[] args) {

		CountMembersWIthCar countMembersWIthCar = new CountMembersWIthCar();
		countMembersWIthCar.run(args);
		
		
	}
}
