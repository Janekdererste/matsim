/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.analysis.activityhistogram;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.jointtrips.JointMainModeIdentifier;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.EventsToActivities;
import playground.ivt.utils.TripModeShares;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author thibautd
 */
public class ActivityHistogramModule extends AbstractModule {
	@Override
	public void install() {
		bind(ActivityHistogram.class);
		addEventHandlerBinding().to(ActivityHistogram.class);
		addEventHandlerBinding().toProvider(
				new Provider<EventHandler>() {
					@Inject ActivityHistogram hist;

					@Override
					public EventHandler get() {
						final EventsToActivities e2a = new EventsToActivities();
						e2a.setActivityHandler( hist );
						return e2a;
					}
				});
		addControlerListenerBinding().to(ActivityHistogramListener.class);
	}
}
