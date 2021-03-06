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
package playground.agarwalamit.mixedTraffic.simTime;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * Idea is to read mosbim time from the stopwatch.txt and then compare it with other traffic/link dynamics. 
 *  
 * @author amit
 */

public class MobsimTimeComparator {

	private String respectiveFileDirectory = "../../../../repos/runs-svn/patnaIndia/run107/";
	private BufferedWriter writer;

	public static void main(String[] args) {
		MobsimTimeComparator mtc = new MobsimTimeComparator();
		mtc.openFile();
		mtc.startProcessing();
		mtc.closeFile();
	}

	public void openFile(){
		writer = IOUtils.getBufferedWriter(respectiveFileDirectory+"/mobsimTime.txt");
		try {
			writeString("scenario \t mobsimTimeInSec \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	private void writeString(String str){
		try{
			writer.write(str);
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	public void closeFile(){
		try{
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	public void startProcessing(){
		for (LinkDynamics ld : LinkDynamics.values() ) {
			for ( TrafficDynamics td : TrafficDynamics.values()){
				String queueModel = ld+"_"+td;
				writeString(queueModel+"\t");
				for(int i=2;i<12;i++){
					String stopwatchFile = respectiveFileDirectory + "/output_"+queueModel+"_"+i+"/stopwatch.txt";
					double mobsimTime = readAndReturnMobsimTime(stopwatchFile);
					writeString(mobsimTime+"\t");
				}
				writeString("\n");
			}
		}
	}

	private double readAndReturnMobsimTime(String stopwatchFile){

		double totalMobsimTime = 0;

		BufferedReader reader = IOUtils.getBufferedReader(stopwatchFile);
		try {
			String line = reader.readLine();
			while(line!=null) {
				if(line.startsWith("Iteration")) {
					line = reader.readLine();
					continue;
				}
				String [] parts = line.split("\t");
				String mobsimStartTime = parts[8];
				String mobsimEndTime = parts[9];
				double mobsimTime = getMobsimTime(mobsimStartTime, mobsimEndTime);
				totalMobsimTime += mobsimTime;
				line = reader.readLine();
			} ;
		} catch (Exception e) {
			throw new RuntimeException("File not found. Reason "+ e);
		}
		return totalMobsimTime;
	}

	private double getMobsimTime(String mobsimStartTime, String mobsimEndTime) {
		double simTime = Time.parseTime(mobsimEndTime) - Time.parseTime(mobsimStartTime);
		if (simTime < 0) simTime = simTime + 24. * 3600.;
		return simTime;
	}
}