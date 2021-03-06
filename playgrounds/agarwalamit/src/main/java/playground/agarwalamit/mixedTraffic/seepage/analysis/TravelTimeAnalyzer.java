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
package playground.agarwalamit.mixedTraffic.seepage.analysis;

import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.mixedTraffic.seepage.TestSetUp.SeepageControler;

/**
 * @author amit
 */
public class TravelTimeAnalyzer  {

	public static void main(String[] args) {
		String outputDir = SeepageControler.outputDir;
		ModalTravelTimeAnalyzer ptta = new ModalTravelTimeAnalyzer(outputDir+"/events.xml");
		ptta.preProcessData();
		ptta.postProcessData();
		ptta.writeResults(outputDir);
	}
}