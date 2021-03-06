package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;

public class TransitScheduleAdaptation {
	Scenario scenario;
	public TransitScheduleAdaptation(Scenario scenario) {
		this.scenario=scenario;
	}
	/*
	 * This function removes all departures whose vehicle have been deleted, all routes with zero departures
	 * and all lines with zero routes.
	 */
	public void removeDeletedVehicleDepartures(){
		ArrayList<TransitLine> linestodelete = new ArrayList<TransitLine>();
		CollectionUtil cutil = new CollectionUtil();
		Vehicles vehicles = scenario.getTransitVehicles();
		ArrayList<Id<Vehicle>> vehids = cutil.toArrayList(vehicles.getVehicles().keySet().iterator());
		TransitSchedule schedule = scenario.getTransitSchedule();
		Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
		Iterator<Id<TransitLine>> lineids = lines.keySet().iterator();
		while(lineids.hasNext()){
			ArrayList<TransitRoute> routestodelete = new ArrayList<TransitRoute>();
			Id<TransitLine> lineid = lineids.next();
			TransitLine tline = lines.get(lineid);
			Map<Id<TransitRoute>, TransitRoute> routes = tline.getRoutes();
			Iterator<Id<TransitRoute>> routeids = routes.keySet().iterator();
			while(routeids.hasNext()){
				ArrayList<Departure> departurestodelete = new ArrayList<Departure>();
				Id<TransitRoute> routeid = routeids.next();
				TransitRoute troute = routes.get(routeid);
				Map<Id<Departure>, Departure> departures = troute.getDepartures();
				Iterator<Id<Departure>> departureids = departures.keySet().iterator();
				while(departureids.hasNext()){
					Id<Departure> depid = departureids.next();
					Departure departure = departures.get(depid);
					Id<Vehicle> vehid = departure.getVehicleId();
					if(!vehids.contains(vehid)){//If the vehilce does not exist
						departurestodelete.add(departure);//To be deleted
					}
				}
				Iterator<Departure> deptodltitr = departurestodelete.iterator();
				while(deptodltitr.hasNext()){
					Departure departure = deptodltitr.next();
					System.out.println("Departure Removed: " + departure.getId() + " : " + departure.getDepartureTime());
					troute.removeDeparture(departure);
				}
				if(troute.getDepartures().size()==0){
					routestodelete.add(troute);
				}
			}
			Iterator<TransitRoute> rtstodltitr = routestodelete.iterator();
			while(rtstodltitr.hasNext()){
				TransitRoute troute = rtstodltitr.next();
				System.out.println("Transit Route Removed: " + troute.getId());
//				tline.removeRoute(troute);
			}
			if(tline.getRoutes().size()==0){
				linestodelete.add(tline);
			}
		}
		Iterator<TransitLine> lnstodltitr = linestodelete.iterator();
		while(lnstodltitr.hasNext()){
			TransitLine tline = lnstodltitr.next();
			System.out.println("Transit Line Removed: " + tline.getId());
//			schedule.removeTransitLine(tline);
		}
	}
}
