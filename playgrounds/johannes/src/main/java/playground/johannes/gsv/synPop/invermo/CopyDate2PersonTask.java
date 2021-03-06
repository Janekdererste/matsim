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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.processing.PersonTask;

/**
 * @author johannes
 * 
 */
public class CopyDate2PersonTask implements PersonTask {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.johannes.synpop.processing.PersonTask#apply(playground.johannes
	 * .gsv.synPop.PlainPerson)
	 */
	@Override
	public void apply(Person person) {
		if (person.getEpisodes().size() > 0) {
			Episode plan = person.getEpisodes().get(0);

			person.setAttribute("day", plan.getAttribute("day"));
			person.setAttribute("month", plan.getAttribute("month"));
		}
	}

}
