/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus;

import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.DynAction;

import playground.jbischoff.taxibus.scheduler.*;



public class TaxibusActionCreator
    implements VrpAgentLogic.DynActionCreator
{
    private final PassengerEngine passengerEngine;
    private final VrpLegs.LegCreator legCreator;
    private final double pickupDuration;


    public TaxibusActionCreator(PassengerEngine passengerEngine, VrpLegs.LegCreator legCreator,
            double pickupDuration)
    {
        this.passengerEngine = passengerEngine;
        this.legCreator = legCreator;
        this.pickupDuration = pickupDuration;
    }


    @Override
    public DynAction createAction(final Task task, double now)
    {
        TaxibusTask tt = (TaxibusTask)task;

        switch (tt.getTaxibusTaskType()) {
            case DRIVE_EMPTY:
            case DRIVE_WITH_PASSENGER:
                return legCreator.createLeg((DriveTask)task);

            case PICKUP:
                final TaxibusPickupTask pst = (TaxibusPickupTask)task;
                return new SinglePassengerPickupActivity(passengerEngine, pst, pst.getRequest(),
                        pickupDuration);

            case DROPOFF:
                final TaxibusDropoffTask dst = (TaxibusDropoffTask)task;
                return new SinglePassengerDropoffActivity(passengerEngine, dst, dst.getRequest());

            case STAY:
                return new VrpActivity("Stay", (TaxibusStayTask)task);

            default:
                throw new IllegalStateException();
        }
    }
}
