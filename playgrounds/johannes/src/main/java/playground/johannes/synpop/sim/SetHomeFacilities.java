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

package playground.johannes.synpop.sim;

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.function.TDoubleFunction;
import gnu.trove.impl.Constants;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.processing.PersonsTask;

import java.util.*;

/**
 * @author johannes
 */
public class SetHomeFacilities implements PersonsTask {

    private static final Logger logger = Logger.getLogger(SetHomeFacilities.class);

    private final DataPool dataPool;

    private final Random random;

    private final String zoneLayerKey;

    private TObjectDoubleHashMap<Zone> zoneWeights;

    public SetHomeFacilities(DataPool dataPool, String zoneLayerKey, Random random) {
        this.dataPool = dataPool;
        this.random = random;
        this.zoneLayerKey = zoneLayerKey;
    }

    public void setZoneWeights(TObjectDoubleHashMap<Zone> zoneWeights) {
        this.zoneWeights = zoneWeights;
    }

    @Override
    public void apply(Collection<? extends Person> persons) {
        ZoneCollection zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer(zoneLayerKey);
        FacilityData facilityData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);

        if(zoneWeights == null) {
            zoneWeights = new TObjectDoubleHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, 1.0);
        }
        TObjectDoubleHashMap<Zone> probabilities = calculateProbabilities(zones, zoneWeights);

        logger.info("Assigning facilities to zones...");
        List<ActivityFacility> homeFacilities = facilityData.getFacilities(ActivityTypes.HOME);
        Map<Zone, List<ActivityFacility>> facilityMapping = assignToZones(homeFacilities, zones);

        logger.info("Assigning facilities to persons...");
        List<Person> shuffledPersons = new ArrayList<>(persons);
        Collections.shuffle(shuffledPersons, random);
        assignToPersons(shuffledPersons, facilityMapping, probabilities);

        // this is only necessary if there are zones with a population but no facilities
        logger.info("Checking for homeless persons...");
        validate(shuffledPersons, homeFacilities);


    }

    private TObjectDoubleHashMap<Zone> calculateProbabilities(ZoneCollection zones, TObjectDoubleHashMap<Zone>
            weights) {
        TObjectDoubleHashMap<Zone> zoneValues = new TObjectDoubleHashMap<>();

        double sum = 0;
        for (Zone zone : zones.getZones()) {
            String value = zone.getAttribute(ZoneData.POPULATION_KEY);
            if (value != null & !value.isEmpty()) {
                double population = Double.parseDouble(value) * weights.get(zone);
                zoneValues.put(zone, population);
                sum += population;
            }
        }

        final double total = sum;
        zoneValues.transformValues(new TDoubleFunction() {
            @Override
            public double execute(double v) {
                return v / total;
            }
        });

        return zoneValues;
    }

    private Map<Zone, List<ActivityFacility>> assignToZones(Collection<ActivityFacility> homeFacils, ZoneCollection zones) {
        Map<Zone, List<ActivityFacility>> facilityMapping = new IdentityHashMap<>(zones.getZones().size());

        int unassigned = 0;

        ProgressLogger.init(homeFacils.size(), 2, 10);
        for (ActivityFacility facility : homeFacils) {
            Zone zone = zones.get(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));

            if (zone != null) {
                List<ActivityFacility> facilities = facilityMapping.get(zone);

                if (facilities == null) {
                    facilities = new ArrayList<>();
                    facilityMapping.put(zone, facilities);
                }

                facilities.add(facility);

            } else {
                unassigned++;
            }

            ProgressLogger.step();
        }

        ProgressLogger.termiante();

        if (unassigned > 0) {
            logger.warn(String.format("%s facilities are out of zone bounds.", unassigned));
        }

        return facilityMapping;
    }

    private void assignToPersons(List<Person> persons, Map<Zone, List<ActivityFacility>> facilityMapping, TObjectDoubleHashMap<Zone> probabilities) {
        ProgressLogger.init(persons.size(), 2, 10);

        TObjectDoubleIterator<Zone> it = probabilities.iterator();
        int total = 0;

        // go through all zones
        for (int zoneIdx = 0; zoneIdx < probabilities.size(); zoneIdx++) {
            it.advance();

            // round number of inhabitants up to ensure that all persons are assigned
            int n = (int) Math.ceil(persons.size() * it.value());

            List<ActivityFacility> facilities = facilityMapping.get(it.key());
            if (facilities != null) {

                // check for out of bounds
                if (n + total > persons.size()) {
                    n = persons.size() - total;
                }

                for (int idx = total; idx < (total + n); idx++) {
                    ActivityFacility f = facilities.get(random.nextInt(facilities.size()));
                    setHomeFacility(persons.get(idx), f);

                    ProgressLogger.step();
                }

                total += n;
            }
        }

        ProgressLogger.termiante();

        if (total < persons.size()) {
            logger.warn("Not all persons precessed. Check facilities and zones!");
        }

    }

    private void validate(List<Person> shuffledPersons, List<ActivityFacility> homeFacilities) {
        int cnt = 0;
        for (Person person : shuffledPersons) {
            boolean invalid = false;

            for (Episode episode : person.getEpisodes()) {
                for (Segment act : episode.getActivities()) {
                    if (ActivityTypes.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                        if (act.getAttribute(CommonKeys.ACTIVITY_FACILITY) == null) {
                            invalid = true;
                            break;
                        }
                    }
                }
            }

            if (invalid) {
                ActivityFacility f = homeFacilities.get(random.nextInt(homeFacilities.size()));
                setHomeFacility(person, f);
                cnt++;
            }
        }

        if (cnt > 0) {
            logger.warn(String.format("Assigned %s persons a random home. Check if each zone has as at least one facility.", cnt));
        }
    }

    private void setHomeFacility(Person person, ActivityFacility facility) {
        for (Episode e : person.getEpisodes()) {
            for (Segment act : e.getActivities()) {
                if (ActivityTypes.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                    act.setAttribute(CommonKeys.ACTIVITY_FACILITY, facility.getId().toString());
                }
            }
        }
    }
}
