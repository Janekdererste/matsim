/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.examples;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.matsim.testcases.TestDepth;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.matsim.examples");
		//$JUnit-BEGIN$
		suite.addTestSuite(EquilTest.class);
		suite.addTestSuite(TriangleTest.class);
		suite.addTestSuite(PlanomatRunTest.class);

		if (TestDepth.getDepth() == TestDepth.extended) {
			suite.addTestSuite(BetaTravelTest.class);
			suite.addTestSuite(OnePercentBerlin10sTest.class);
		}
		//$JUnit-END$
		return suite;
	}

}
