package org.matsim.contrib.carsharing.qsim;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;

public class CarsharingAgentFactory implements AgentFactory{
	private final Netsim simulation;
	private final Scenario scenario;
	private CarSharingVehicles carSharingVehicles;

	private TripRouter tripRouter;
	
	
	public CarsharingAgentFactory(final Netsim simulation, final Scenario scenario, final Provider<TripRouter> tripRouterProvider, CarSharingVehicles carSharingVehicles) {
		this.simulation = simulation;
		this.scenario = scenario;
		this.carSharingVehicles = carSharingVehicles;
				
		Provider<TripRouter> tripRouterFactory = tripRouterProvider;
		
		tripRouter = tripRouterFactory.get();		
		
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		
		
		//if we want to simulate all agents then we have a normal AllCSModesPersonDriverAgentImpl agent 
		//if we want to simulate only carsharing members we would have PersonDriverAgentOnlyMembersImpl agent here

		MobsimDriverAgent agent ;
//		agent = new CarsharingPersonDriverAgentImplOLD(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()),
//				this.simulation, this.scenario, this.carSharingVehicles, this.tripRouter); 
		agent = new CarsharingPersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation,
				this.carSharingVehicles, this.tripRouter); 
		return agent;
	}
}
