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

package playground.johannes.gsv.synPop.mid;

import java.io.IOException;
import java.util.Map;

import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class Generator {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String personFile = "/home/johannes/gsv/mid2008/MiD2008_PUF_Personen.txt";
		String legFile = "/home/johannes/gsv/mid2008/MiD2008_PUF_Wege.txt";
		
		TXTReader reader = new TXTReader();
		Map<String, ProxyPerson> persons = reader.read(personFile, legFile);
		
		System.out.println(String.format("Generated %s persons.", persons.size()));
	}

}