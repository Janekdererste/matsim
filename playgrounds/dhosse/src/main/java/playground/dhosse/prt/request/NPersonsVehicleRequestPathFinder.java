package playground.dhosse.prt.request;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;

import playground.dhosse.prt.VehicleRequestPaths;
import playground.dhosse.prt.scheduler.NPersonsPickupStayTask;
import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.schedule.TaxiTask;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;
import playground.michalm.taxi.scheduler.TaxiScheduler;


public class NPersonsVehicleRequestPathFinder
{
    private final TaxiOptimizerConfiguration optimConfig;

    private final TaxiScheduler scheduler;
    private final int vehicleCapacity;
    private final LeastCostPathCalculator router;


    public NPersonsVehicleRequestPathFinder(TaxiOptimizerConfiguration optimConfig,
            int vehicleCapacity)
    {
        this.optimConfig = optimConfig;
        this.vehicleCapacity = vehicleCapacity;
        this.scheduler = optimConfig.scheduler;

        router = new Dijkstra(optimConfig.context.getScenario().getNetwork(),
                optimConfig.travelDisutility, optimConfig.travelTime);

    }


    public BestDispatchFinder.Dispatch findBestVehicleForRequest(TaxiRequest req,
            Iterable<? extends Vehicle> vehicles)
    {

        BestDispatchFinder.Dispatch bestVrp = null;
        double bestCost = Double.MAX_VALUE;

        for (Vehicle veh : vehicles) {

            VrpPathWithTravelData path = calculateVrpPath(veh, req);

            if (path == null) {
                continue;
            }

            BestDispatchFinder.Dispatch vrp = new BestDispatchFinder.Dispatch(veh, req, path);
            double cost = VehicleRequestPaths.TW_COST.getCost(vrp);

            if (cost < bestCost) {
                bestVrp = vrp;
                bestCost = cost;
            }
        }

        return bestVrp;

    }


    public BestDispatchFinder.Dispatch findBestRequestForVehicle(Vehicle veh,
            Iterable<TaxiRequest> unplannedRequests)
    {
        BestDispatchFinder.Dispatch bestVrp = null;
        double bestCost = Double.MAX_VALUE;

        for (TaxiRequest req : unplannedRequests) {
            VrpPathWithTravelData path = calculateVrpPath(veh, req);

            if (path == null) {
                continue;
            }

            BestDispatchFinder.Dispatch vrp = new BestDispatchFinder.Dispatch(veh, req, path);
            double cost = VehicleRequestPaths.TP_COST.getCost(vrp);

            if (cost < bestCost) {
                bestVrp = vrp;
                bestCost = cost;
            }
        }

        return bestVrp;
    }


    private VrpPathWithTravelData calculateVrpPath(Vehicle veh, TaxiRequest req)
    {

        TaxiTask lastTask = (TaxiTask)Schedules.getLastTask(veh.getSchedule());

        LinkTimePair departure = null;

        if (lastTask.getTaxiTaskType().equals(TaxiTaskType.PICKUP)) {

            NPersonsPickupStayTask task = (NPersonsPickupStayTask)lastTask;

            if (task.getLink().equals(req.getFromLink())
                    && task.getRequest().getToLink().equals(req.getToLink())
                    && task.getRequests().size() < this.vehicleCapacity) {

                double begin = task.getBeginTime();

                double t0 = req.getT0();

                if (t0 < begin && task.getStatus() != TaskStatus.PERFORMED
                        && task.getStatus() != TaskStatus.STARTED) {

                    departure = t0 >= veh.getT1() ? null : new LinkTimePair(req.getFromLink(), t0);

                }
            }

        }
        else if (lastTask.getTaxiTaskType().equals(TaxiTask.TaxiTaskType.STAY)) {

            if (lastTask.getStatus().equals(TaskStatus.STARTED)) {
                departure = scheduler.getEarliestIdleness(veh);
            }

        }

        return departure == null ? null
                : VrpPaths.calcAndCreatePath(departure.link, req.getFromLink(), departure.time,
                        router, optimConfig.travelTime);

    }

}
