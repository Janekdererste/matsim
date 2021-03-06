/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.scenariogen.casimpleexp;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class CAExperimentsGridlock {
	private static String inputDir = "/Users/laemmel/devel/casimgridlock/input";
	private static String outputDir = "/Users/laemmel/devel/casimgridlock/output";

	private static final int nrAgents = 6;

	public static void main(String[] args) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);

		createNetwork(sc);

		((NetworkImpl) sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(.71);
		((NetworkImpl) sc.getNetwork()).setCapacityPeriod(3600);

		c.network().setInputFile(inputDir + "/network.xml.gz");

		// c.strategy().addParam("Module_1",
		// "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "50");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(100);

		c.plans().setInputFile(inputDir + "/population.xml.gz");

		ActivityParams pre = new ActivityParams("origin");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
									// running a simulation one gets
									// "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in
		// ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		ActivityParams post = new ActivityParams("destination");
		post.setTypicalDuration(49); // dito
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);

		sc.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		sc.getConfig().planCalcScore().setPerforming_utils_hr(0.);

		QSimConfigGroup qsim = sc.getConfig().qsim();
		// qsim.setEndTime(20 * 60);
		c.controler().setMobsim("casim");
		c.global().setCoordinateSystem("EPSG:3395");

		c.qsim().setEndTime(2 * 3600);

		new ConfigWriter(c).write(inputDir + "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());

		createPopulation(sc);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans()
				.getInputFile());

	}

	private static void createPopulation(Scenario sc) {
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents / 2; i++) {
			Person pers = fac.createPerson(Id.create("b" + i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin",
					Id.create("l0", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("walkca");
			plan.addLeg(leg);

			Activity act1;
			act1 = fac
					.createActivityFromLinkId("origin", Id.createLinkId("l2"));
			plan.addActivity(act1);
			;
			act1.setEndTime(0);
			plan.addLeg(leg);

			Activity act2 = fac.createActivityFromLinkId("destination",
					Id.create("l0_rev", Link.class));
			plan.addActivity(act2);
			pop.addPerson(pers);
		}
		for (int i = nrAgents / 2; i < nrAgents; i++) {
			Person pers = fac.createPerson(Id.create("a" + i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin",
					Id.create("l4_rev", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("walkca");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination",
					Id.create("l0_rev", Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
	}

	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		final double x2 = -40;
		Node n0 = fac.createNode(Id.create("n0", Node.class), new Coord(x2, (double) 0));
		final double x1 = -10;
		Node n1 = fac.createNode(Id.create("n1", Node.class), new Coord(x1, (double) 0));
		final double x = -5;
		Node n2 = fac.createNode(Id.create("n2", Node.class), new Coord(x, (double) 0));
		Node n2b = fac.createNode(Id.create("n2b", Node.class), new Coord((double) 0, (double) 20));
		Node n3 = fac.createNode(Id.create("n3", Node.class), new Coord((double) 5, (double) 0));
		Node n4 = fac.createNode(Id.create("n4", Node.class), new Coord((double) (10 + 5 + 5), (double) 0));
		Node n5 = fac.createNode(Id.create("n5", Node.class), new Coord((double) (40 + 5 + 5), (double) 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n2b);
		net.addNode(n4);
		net.addNode(n5);
		net.addNode(n3);
		double flow = 2;
		Link l0 = fac.createLink(Id.create("l0", Link.class), n0, n1);
		Link l1 = fac.createLink(Id.create("l1", Link.class), n1, n2);
		Link l2 = fac.createLink(Id.create("l2", Link.class), n2, n3);
		Link l3 = fac.createLink(Id.create("l3", Link.class), n3, n4);
		Link l4 = fac.createLink(Id.create("l4", Link.class), n4, n5);

		Link l0Rev = fac.createLink(Id.create("l0_rev", Link.class), n1, n0);
		Link l1Rev = fac.createLink(Id.create("l1_rev", Link.class), n2, n1);
		Link l2Rev = fac.createLink(Id.create("l2_rev", Link.class), n3, n2);
		Link l3Rev = fac.createLink(Id.create("l3_rev", Link.class), n4, n3);
		Link l4Rev = fac.createLink(Id.create("l4_rev", Link.class), n5, n4);

		Set<String> modes = new HashSet<String>();
		modes.add("walkca");
		l0.setLength(30);
		l1.setLength(5);
		l2.setLength(10);
		l3.setLength(10 + 5);
		l4.setLength(30);

		l0Rev.setLength(30);
		l1Rev.setLength(5);
		l2Rev.setLength(10);
		l3Rev.setLength(10 + 5);
		l4Rev.setLength(30);

		l0.setAllowedModes(modes);
		l1.setAllowedModes(modes);
		l2.setAllowedModes(modes);
		l3.setAllowedModes(modes);
		l4.setAllowedModes(modes);

		l0Rev.setAllowedModes(modes);
		l1Rev.setAllowedModes(modes);
		l2Rev.setAllowedModes(modes);
		l3Rev.setAllowedModes(modes);
		l4Rev.setAllowedModes(modes);

		l0.setFreespeed(1.34);
		l1.setFreespeed(1.34);
		l2.setFreespeed(1.34);
		l3.setFreespeed(1.34);
		l4.setFreespeed(1.34);

		l0Rev.setFreespeed(1.34);
		l1Rev.setFreespeed(1.34);
		l2Rev.setFreespeed(1.34);
		l3Rev.setFreespeed(1.34);
		l4Rev.setFreespeed(1.34);

		l0.setCapacity(flow);
		l1.setCapacity(flow);
		l2.setCapacity(flow / 5);
		l3.setCapacity(flow / 5);
		l4.setCapacity(flow);

		l0Rev.setCapacity(flow);
		l1Rev.setCapacity(flow);
		l2Rev.setCapacity(flow / 5);
		l3Rev.setCapacity(flow / 5);
		l4Rev.setCapacity(flow);

		double lanes = 2 / 0.71;
		l0.setNumberOfLanes(lanes);
		l1.setNumberOfLanes(lanes);
		l2.setNumberOfLanes(lanes / 5);
		l3.setNumberOfLanes(lanes);
		l4.setNumberOfLanes(lanes);

		l0Rev.setNumberOfLanes(lanes);
		l1Rev.setNumberOfLanes(lanes);
		l2Rev.setNumberOfLanes(lanes / 5);
		l3Rev.setNumberOfLanes(lanes);
		l4Rev.setNumberOfLanes(lanes);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);

		net.addLink(l0Rev);
		net.addLink(l1Rev);
		net.addLink(l2Rev);
		net.addLink(l3Rev);
		net.addLink(l4Rev);

		((NetworkImpl) net).setCapacityPeriod(1);
		((NetworkImpl) net).setEffectiveCellSize(.26);
		((NetworkImpl) net).setEffectiveLaneWidth(.71);
	}
}
