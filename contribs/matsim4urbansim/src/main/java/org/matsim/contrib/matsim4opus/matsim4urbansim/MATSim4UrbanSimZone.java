/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.matsim4opus.matsim4urbansim;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matsim4opus.analysis.DanielAnalysisListenerEvents;
import org.matsim.contrib.matsim4opus.analysis.KaiAnalysisListener;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.algorithms.NetworkScenarioCut;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;



/**
 * @author thomas
 * 
 * improvements jan'12:
 * 
 * - This class is a revised version of "MATSim4UrbanSim".
 * - Increased configurability: 
 * 	First approach to increase the configurability of MATSim4UrbanSim modules such as
 * 	the zonz2zone impedance matrix, zone based- and grid based accessibility computation. Modules can be en-disabled
 * 	additional modules can be added by other classes extending MATSim4UrbanSimV2.
 * - Data Processing on Demand:
 *  Particular input data is processed when a corresponding module is enabled, e.g. an array of aggregated work places will
 *  be generated when either the zone based- or grid based accessibility computation is activated.
 * - Extensibility:
 * 	This class provides standard functionality such as configuring MATSim, reading UrbanSim input data, running the 
 * 	mobility simulation and so forth... This functionality can be extended by an inheriting class (e.g. MATSim4UrbanSimZurichAccessibility) 
 * 	by implementing certain stub methods such as "addFurtherControlerListener", "modifyNetwork", "modifyPopulation" ...
 * - Backup Results:
 *  This was also available before but not documented. Some data is overwritten with each run, e.g. the zone2zone impedance matrix or data
 *  in the MATSim output folder. If the backup is activated the most imported files (see BackupRun class) are saved in a new folder. In order 
 *  to match the saved data with the corresponding run or year the folder names contain the "simulation year" and a time stamp.
 * - Other improvements:
 * 	For a better readability some functionality is out-sourced into helper classes
 * 
 * improvements jan'13:
 * 
 * - The MATSim4URbanSim zone version now supports warm and hot start, i.e. it is possible to reuse a plans file from a previous MATSim run. 
 *  The plans file will be merged with the current UrbanSim population. This means, persons that emigrated, moved into a new home, 
 *  changed their employment status or got a new job are getting new plans.
 * 
 */
public class MATSim4UrbanSimZone extends MATSim4UrbanSimParcel{

	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimZone.class);
	
	static final boolean BRUSSELS_SCENARIO_MODIFY_NETWORK = true ;
	static final boolean BRUSSELS_SCENARIO_CALCULATE_ZONE2ZONE_MATRIX = false ;

	/**
	 * constructor
	 * 
	 * @param args contains at least a reference to 
	 * 		  MATSim4UrbanSim configuration generated by UrbanSim
	 */
	MATSim4UrbanSimZone(String args[]){
		super(args);
		// set flag to false (needed for ReadFromUrbanSimModel to choose the right method)
		isParcelMode = false;
	}
	
	@Override
	void modifyNetwork( Network net ) {

		if ( BRUSSELS_SCENARIO_MODIFY_NETWORK ) {

			log.error("cutting the Brussels network.  It is really really dangerous to leave this in the code as such.  kai, apr'13") ;
			Coord min = new CoordImpl(94902.,112575.) ;
			Coord max = new CoordImpl(220907.,220000.) ;
			NetworkScenarioCut nsc = new NetworkScenarioCut(min, max) ;
			nsc.run(net) ;

			log.error("modifying the Brussels network.  It is really really dangerous to leave this in the code as such.  kai, apr'13") ;
			long cnt = 0 ;
			for ( Link link : net.getLinks().values() ) {
				double spd = link.getFreespeed() ;
				double cap = link.getCapacity() ;
				// the free speeds do not seem very reliable (frontage roads sometimes have faster speeds than main roads) 
				if ( cap <= 1000. ) {
					// something like a one-lane local street
					link.setFreespeed(15./3.6) ;
					cnt++ ;
				} else if ( cap <= 2000. && spd < 61./3.6 ) {
					// something like a two-lane slow arterial
					link.setFreespeed( 20./3.6 ) ;
					cnt++ ;
				} else if ( cap <= 3000. && spd < 61/3.6 ) {
					// something like a three-lane boulevard
					link.setFreespeed( 25./3.6 ) ;
					cnt++ ;
				} else if ( cap <= 3500. && spd < 81/3.6 ) {
					// something like a narrow two-lane tunnel
					link.setFreespeed( 60./3.6 ) ;
					cnt++ ;
				}
			}
			log.warn("modified the free speed on " + cnt + " of " + net.getLinks().size() + " links.") ;
			
		}

	}
	
	@Override
	void addFurtherControlerListener(ActivityFacilities zones, ActivityFacilities parcels, Controler controler) {
		controler.addControlerListener(new KaiAnalysisListener()) ;
		// not very nice, but the correct folder is not specified anywhere and change with every new urbansim-run... // Daniel May'13
//		String cleFile = this.getUrbanSimParameterConfig().getMATSim4OpusTemp().replaceFirst("/tmp/", "/") + "cle.csv";
		String cleFile = this.getUrbanSimParameterConfig().getMATSim4Opus() + "cle.csv";
		if(new File(cleFile).exists()){
			log.info("loading " + DanielAnalysisListenerEvents.class.getSimpleName() + " with " + cleFile + "...");
			List<Tuple<Integer, Integer>> timeslots = new ArrayList<Tuple<Integer,Integer>>();
			timeslots.add(new Tuple<Integer, Integer>(0, 6));
			timeslots.add(new Tuple<Integer, Integer>(6, 10));
			timeslots.add(new Tuple<Integer, Integer>(10, 14));
			timeslots.add(new Tuple<Integer, Integer>(14, 18));
			timeslots.add(new Tuple<Integer, Integer>(18, 24));
			timeslots.add(new Tuple<Integer, Integer>(0, 24));
			controler.addControlerListener(new DanielAnalysisListenerEvents(cleFile, zones, timeslots));
		}else{
			log.error("can not find " + cleFile);
//			throw new RuntimeException("can not find " + cleFile);
		}
	}

	/**
	 * Entry point
	 * @param args UrbanSim command prompt
	 */
	public static void main(String args[]){
		
		long start = System.currentTimeMillis();
		
		MATSim4UrbanSimZone m4u = new MATSim4UrbanSimZone(args);
		m4u.run();
		m4u.matsim4UrbanSimShutdown();
		MATSim4UrbanSimZone.isSuccessfulMATSimRun = Boolean.TRUE;
		// copy the zones file to the outputfolder...
		IOUtils.copyFile(new File(m4u.getUrbanSimParameterConfig().getMATSim4OpusTemp() + "/zones.csv"), 
				new File(m4u.getUrbanSimParameterConfig().getMATSim4OpusOutput() + "/zones.csv"));
		IOUtils.copyFile(new File(m4u.getUrbanSimParameterConfig().getMATSim4OpusTemp() + "/zones_complete.csv"), 
				new File(m4u.getUrbanSimParameterConfig().getMATSim4OpusOutput() + "/zones_complete.csv"));
		
		log.info("Computation took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}
}
