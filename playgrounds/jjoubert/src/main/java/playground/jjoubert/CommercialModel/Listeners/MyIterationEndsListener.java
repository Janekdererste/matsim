/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialLegHistogramListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.CommercialModel.Listeners;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class MyIterationEndsListener implements IterationEndsListener {
	
	private final Logger log = Logger.getLogger(MyIterationEndsListener.class);

	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("  --> End of iteration " + event.getIteration() + "/" + event.getControler().getConfig().controler().getLastIteration() );
	}
	
}
