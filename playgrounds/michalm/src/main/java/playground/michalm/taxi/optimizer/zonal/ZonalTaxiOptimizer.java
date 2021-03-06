/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer.zonal;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.StayTask;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration.Goal;
import playground.michalm.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import playground.michalm.zone.*;


public class ZonalTaxiOptimizer
    extends RuleBasedTaxiOptimizer
{
    private static final double EXPANSION_DISTANCE = 200;

    private static final Comparator<Vehicle> FIRST_LONGEST_WAITING = new Comparator<Vehicle>() {
        public int compare(Vehicle v1, Vehicle v2)
        {
            double beginTime1 = v1.getSchedule().getCurrentTask().getBeginTime();
            double beginTime2 = v2.getSchedule().getCurrentTask().getBeginTime();
            return Double.compare(beginTime1, beginTime2);
        }
    };

    private final Map<Id<Zone>, Zone> zones;
    private Map<Id<Zone>, PriorityQueue<Vehicle>> zoneToIdleVehicleQueue;
    private final Map<Id<Link>, Zone> linkToZone;


    public ZonalTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        super(optimConfig);

        if (optimConfig.goal != Goal.MIN_WAIT_TIME) {
            throw new RuntimeException("Only MIN_WAIT_TIME allowed");//TODO
        }

        this.zones = optimConfig.zones;
        this.linkToZone = NetworkWithZonesUtils.createLinkToZoneMap(
                optimConfig.context.getScenario().getNetwork(),
                new ZoneFinderImpl(zones, EXPANSION_DISTANCE));
    }


    @Override
    protected void scheduleUnplannedRequests()
    {
        initIdleVehiclesInZones();
        scheduleUnplannedRequestsWithinZones();

        if (!unplannedRequests.isEmpty()) {
            super.scheduleUnplannedRequests();
        }
    }


    private void initIdleVehiclesInZones()
    {
        zoneToIdleVehicleQueue = new HashMap<>();
        for (Id<Zone> zoneId : zones.keySet()) {
            zoneToIdleVehicleQueue.put(zoneId,
                    new PriorityQueue<Vehicle>(10, FIRST_LONGEST_WAITING));
        }

        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles().values()) {
            if (optimConfig.scheduler.isIdle(veh)) {
                Link link = ((StayTask)veh.getSchedule().getCurrentTask()).getLink();
                Zone zone = linkToZone.get(link.getId());
                if (zone != null) {
                    PriorityQueue<Vehicle> queue = zoneToIdleVehicleQueue.get(zone.getId());
                    queue.add(veh);
                }
            }
        }
    }


    private void scheduleUnplannedRequestsWithinZones()
    {
        Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
        while (reqIter.hasNext()) {
            TaxiRequest req = reqIter.next();

            Zone zone = linkToZone.get(req.getFromLink().getId());
            if (zone == null) {
                continue;
            }

            PriorityQueue<Vehicle> idleVehsInZone = zoneToIdleVehicleQueue.get(zone.getId());
            if (idleVehsInZone.isEmpty()) {
                continue;
            }

            Iterable<Vehicle> filteredVehs = Collections.singleton(idleVehsInZone.peek());
            BestDispatchFinder.Dispatch best = dispatchFinder
                    .findBestVehicleForRequest(req, filteredVehs);

            if (best != null) {
                optimConfig.scheduler.scheduleRequest(best.vehicle, best.request, best.path);
                reqIter.remove();
                idleVehsInZone.remove(best.vehicle);
            }
        }
    }
}
