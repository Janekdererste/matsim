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

package org.matsim.contrib.dvrp.extensions.vrppd;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;


public interface PickupDeliveryRequest
    extends Request
{
    Link getFromLink();


    Link getToLink();


    PickupDeliveryTask getPickupTask();


    void setPickupTask(PickupDeliveryTask pickupTask);


    PickupDeliveryTask getDeliveryTask();


    void setDeliveryTask(PickupDeliveryTask deliveryTask);
}
