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
package playground.agarwalamit.analysis.congestion;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;

/**
 * This analyzer calculates delay from link enter and link leave events and therefore provides only experienced delay.
 * <p> In order to get the caused delay for each person, see {@link CausedDelayAnalyzer}
 * 
 * @author amit
 */
public class ExperiencedDelayAnalyzer extends AbstractAnalysisModule {
	
	private final Logger logger = Logger.getLogger(ExperiencedDelayAnalyzer.class);
	private final String eventsFile;
	private ExperiencedDelayHandler congestionHandler;
	private EventsManager eventsManager;
	private Scenario scenario;

	public ExperiencedDelayAnalyzer(String eventFile, Scenario scenario, int noOfTimeBins) {
		super(ExperiencedDelayAnalyzer.class.getSimpleName());
		this.scenario = scenario;
		this.eventsFile = eventFile;
		this.congestionHandler = new ExperiencedDelayHandler(this.scenario, noOfTimeBins);
	}
	
	public ExperiencedDelayAnalyzer(String eventFile, Scenario scenario, int noOfTimeBins, boolean isSortingForInsideMunich) {
		super(ExperiencedDelayAnalyzer.class.getSimpleName());
		this.eventsFile = eventFile;
		this.scenario = scenario;
		this.congestionHandler = new ExperiencedDelayHandler(this.scenario, noOfTimeBins, isSortingForInsideMunich);
	}
	
	public void run(){
		preProcessData();
		postProcessData();
		checkTotalDelayUsingAlternativeMethod();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		return handler;
	}

	@Override
	public void preProcessData() {
		this.eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader eventsReader = new MatsimEventsReader(this.eventsManager);
		this.eventsManager.addHandler(this.congestionHandler);
		eventsReader.readFile(this.eventsFile);
	}

	@Override
	public void postProcessData() {
	}

	@Override
	public void writeResults(String outputFolder) {
		logger.error("Not implemented yet.");
	}

	public double getTotalDelaysInHours (){
		return this.congestionHandler.getTotalDelayInHours();
	}
	
	public SortedMap<Double, Map<Id<Person>, Double>> getTimeBin2AffectedPersonId2Delay() {
		return this.congestionHandler.getDelayPerPersonAndTimeInterval();
	}
	
	public Map<Double, Map<Id<Link>, Double>> getTimeBin2LinkId2Delay() {
		return this.congestionHandler.getDelayPerLinkAndTimeInterval();
	}
	
	public Map<Double, Map<Id<Link>, Integer>> getTimeBin2LinkLeaveCount(){
		return this.congestionHandler.getTime2linkIdLeaveCount();
	}
	
	public void checkTotalDelayUsingAlternativeMethod(){
		EventsManager em = EventsUtils.createEventsManager();
		CongestionHandlerImplV3 implV3 = new CongestionHandlerImplV3(em, (MutableScenario) this.scenario);
		MatsimEventsReader eventsReader = new MatsimEventsReader(em);
		em.addHandler(implV3);
		eventsReader.readFile(this.eventsFile);
		if(implV3.getTotalDelay()/3600!=this.congestionHandler.getTotalDelayInHours())
			throw new RuntimeException("Total Delays are not equal using two methods; values are "+implV3.getTotalDelay()/3600+","+this.congestionHandler.getTotalDelayInHours());
	}
}
