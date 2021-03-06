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

package playground.michalm.ev;

import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;


public class ChargingAuxDischargingHandler
    implements MobsimAfterSimStepListener
{
    private final Iterable<? extends Charger> chargers;
    private final int chargePeriod;

    private final Iterable<? extends ElectricVehicle> vehicles;
    private final int auxDischargePeriod;


    public ChargingAuxDischargingHandler(Iterable<? extends Charger> chargers, int chargePeriod,
            Iterable<? extends ElectricVehicle> vehicles, int auxDischargePeriod)
    {
        this.chargers = chargers;
        this.chargePeriod = chargePeriod;

        this.vehicles = vehicles;
        this.auxDischargePeriod = auxDischargePeriod;
    }


    @Override
    public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e)
    {
        if (lastSimStepInPeriod(e.getSimulationTime(), auxDischargePeriod)) {
            for (ElectricVehicle v : vehicles) {
                v.getAuxEnergyConsumption().useEnergy(auxDischargePeriod);
            }
        }

        if (lastSimStepInPeriod(e.getSimulationTime(), chargePeriod)) {
            for (Charger c : chargers) {
                c.getLogic().chargeVehicles(chargePeriod);
            }
        }
    }


    private boolean lastSimStepInPeriod(double simTime, double period)
    {
        return (simTime + 1) % period == 0;
    }
}
