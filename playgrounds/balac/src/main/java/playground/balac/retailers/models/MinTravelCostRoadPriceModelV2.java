package playground.balac.retailers.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.balac.retailers.data.LinkRetailersImpl;
import playground.balac.retailers.data.PersonPrimaryActivity;
import playground.balac.retailers.utils.Utils;



public class MinTravelCostRoadPriceModelV2 extends RetailerModelImpl
{
  private static final Logger log = Logger.getLogger(MaxActivityModel.class);

  private TreeMap<Id, LinkRetailersImpl> availableLinks = new TreeMap<Id, LinkRetailersImpl>();

  public MinTravelCostRoadPriceModelV2(Controler controler, Map<Id, ActivityFacilityImpl> retailerFacilities)
  {
    this.controler = controler;
    this.retailerFacilities = retailerFacilities;
    this.controlerFacilities = this.controler.getFacilities();
    this.shops = findScenarioShops(this.controlerFacilities.getFacilities().values());

    for (Person p : controler.getPopulation().getPersons().values()) {
      PersonImpl pi = (PersonImpl)p;
      this.persons.put(pi.getId(), pi);
    }
  }

  public void init(TreeMap<Integer, String> first)
  {
    this.first = first;

    setInitialSolution(this.first.size());
    log.info("Initial solution = " + getInitialSolution());
    findScenarioShops(this.controlerFacilities.getFacilities().values());
    Gbl.printMemoryUsage();
   /* for (PersonImpl pi : this.persons.values()) {
      PersonRetailersImpl pr = new PersonRetailersImpl(pi);
      this.retailersPersons.put(pr.getId(), pr);
    }*/
    
    Utils.setShopsQuadTree(Utils.createShopsQuadTree(this.controler));
    //we need to do this with the plans and activities before and after shopping
    for (Integer i = Integer.valueOf(0); i.intValue() < first.size(); i = Integer.valueOf(i.intValue() + 1)) {
        String linkId = this.first.get(i);
        LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getNetwork().getLinks().get(new IdImpl(linkId)), this.controler.getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
        double centerX = 683217.0; 
        double centerY = 247300.0;
        CoordImpl coord = new CoordImpl(centerX, centerY);
      
        boolean shopgroceryInside = false;
     
        
        if (CoordUtils.calcDistance(link.getCoord(), coord) < 4000) {
			shopgroceryInside = true;
			
		}
        double score = 0.0;
        for (Person person:controler.getPopulation().getPersons().values()) {
    	
        	double scoreTemp = 0.0;
        	boolean lastPrimaryActivityInside = false;
            boolean nextPrimaryActivityInside = false;
            boolean lastPrimaryActivityInsideCatchmentArea = false;
            boolean nextPrimaryActivityInsideCatchmentArea = false;
        	Plan plan = person.getSelectedPlan();
        	Link linklpa = null;
            Link linknpa = null;
        	for (PlanElement pe: plan.getPlanElements()) {
    		
        		if (pe instanceof Activity) {
    			
        			 if (!((Activity) pe).getType().equals("shopgrocery")) { 
        				if (CoordUtils.calcDistance(controler.getNetwork().getLinks().get(((Activity)pe).getLinkId()).getCoord(), coord) < 4000) 
        					lastPrimaryActivityInside = true;
        				else
            				lastPrimaryActivityInside = false;
        				
        					if(CoordUtils.calcDistance(controler.getNetwork().getLinks().get(((Activity)pe).getLinkId()).getCoord(), link.getCoord()) < 3000) {
    						
        						lastPrimaryActivityInsideCatchmentArea = true;
    						
        						linklpa = controler.getNetwork().getLinks().get(((Activity)pe).getLinkId());
    						
        					}
        					else
        						lastPrimaryActivityInsideCatchmentArea = false;
    					
        				
        				
        			 }
        			
        			 if (((Activity) pe).getType().equals("shopgrocery")) { 	
    				
        				int index = plan.getPlanElements().indexOf(pe);
    				
        				for (int j = index; j < plan.getPlanElements().size(); j++) {
        					PlanElement pe1 = plan.getPlanElements().get(j);
        					if (pe1 instanceof Activity) {
        						if (((Activity) pe1).getType().equals("home") || ((Activity) pe1).getType().startsWith("work") || ((Activity) pe1).getType().startsWith("education") ||((Activity) pe1).getType().startsWith("leisure") ) {
    							
        							if (CoordUtils.calcDistance(controler.getNetwork().getLinks().get(((Activity)pe1).getLinkId()).getCoord(), coord) < 4000) {
        								nextPrimaryActivityInside = true;
    		    					
        							}
        							else
        								nextPrimaryActivityInside = false;
        							if(CoordUtils.calcDistance(controler.getNetwork().getLinks().get(((Activity)pe1).getLinkId()).getCoord(), link.getCoord()) < 3000) {
    	    						
        								nextPrimaryActivityInsideCatchmentArea = true;
        								linknpa = controler.getNetwork().getLinks().get(((Activity)pe1).getLinkId());
        							}
        							else
        								nextPrimaryActivityInsideCatchmentArea = false;
    							
        							break;
    							
        						}
    		    			
        					}
    					
        				}
        				
        				
        				
        				if (lastPrimaryActivityInsideCatchmentArea) {
        					scoreTemp = 0;
        					Network network = this.controler.getNetwork();
        					TravelTime travelTime = this.controler.getLinkTravelTimes();
        					TravelDisutility travelCost = this.controler.getTravelDisutilityFactory().createTravelDisutility(travelTime, this.controler.getConfig().planCalcScore());

        					LeastCostPathCalculator routeAlgo = this.controler.getLeastCostPathCalculatorFactory().createPathCalculator(network, travelCost, travelTime);

        					LegImpl li = new LegImpl(TransportMode.car);
        					li.setDepartureTime(0.0D);
        					handleCarLeg(li, link, this.controler.getNetwork().getLinks().get(linklpa.getId()), network, routeAlgo);

        					Plan plan1 = this.controler.getPopulation().getFactory().createPlan();
        					plan1.addActivity(null);
        					plan1.addLeg(li);
        					plan1.addActivity(null);

        					ScoringFunction scoringFunction = this.controler.getScoringFunctionFactory().createNewScoringFunction(person);
        					scoreTemp = getLegScore(li, scoringFunction);
        					if (shopgroceryInside && !nextPrimaryActivityInside && !lastPrimaryActivityInside) {
        						scoreTemp += -3.0;
        					
        					}
        					else if (!shopgroceryInside && nextPrimaryActivityInside && lastPrimaryActivityInside) {
        						scoreTemp += -3.0;
        					}
        					score += 1.0/(-1.0 + scoreTemp);
        				
        				}
        			
        				if (nextPrimaryActivityInsideCatchmentArea) {
        					scoreTemp = 0;
        					Network network = this.controler.getNetwork();
        					TravelTime travelTime = this.controler.getLinkTravelTimes();
        					TravelDisutility travelCost = this.controler.getTravelDisutilityFactory().createTravelDisutility(travelTime, this.controler.getConfig().planCalcScore());

        					LeastCostPathCalculator routeAlgo = this.controler.getLeastCostPathCalculatorFactory().createPathCalculator(network, travelCost, travelTime);

        					LegImpl li = new LegImpl(TransportMode.car);
        					li.setDepartureTime(0.0D);
        					handleCarLeg(li, link, this.controler.getNetwork().getLinks().get(linknpa.getId()), network, routeAlgo);

        					Plan plan1 = this.controler.getPopulation().getFactory().createPlan();
        					plan1.addActivity(null);
        					plan1.addLeg(li);
        					plan1.addActivity(null);

        					ScoringFunction scoringFunction = this.controler.getScoringFunctionFactory().createNewScoringFunction(person);
        					scoreTemp = getLegScore(li, scoringFunction);
        					if (shopgroceryInside && !nextPrimaryActivityInside && !lastPrimaryActivityInside) {
        						scoreTemp += -3.0;
        					
        					}
        					else if (!shopgroceryInside && nextPrimaryActivityInside && lastPrimaryActivityInside) {
        						scoreTemp += -3.0;
        					}
        					score += 1.0/(-1.0 + scoreTemp);				
        				}
        				
    				
        				
    			}
    			
    		}
    	}
        	
        	
			
		
			
			
			}
			
			link.setScoreSum(score);
			link.setPotentialCustomers(score);
			this.availableLinks.put(link.getId(), link);
    	
    	
        }
    
    }
    
    
  

  private void computePotentialCustomers() {
	  for (Integer i = Integer.valueOf(0); i.intValue() < first.size(); i = Integer.valueOf(i.intValue() + 1)) {
	      String linkId = this.first.get(i);
	     
	      LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getNetwork().getLinks().get(new IdImpl(linkId)), this.controler.getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
	      
	      Collection<ActivityFacility> facilities = Utils.getShopsQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), 3000.0D);
	        
	      int numberShops = facilities.size();
	      
	      if (numberShops == 1 || numberShops == 0)
	    	  link.setPotentialCustomers(availableLinks.get(link.getId()).getScoreSum());
	      else{
	    	  link.setPotentialCustomers(availableLinks.get(link.getId()).getScoreSum() / (double)(numberShops));
	      }
	      
	      link.setScoreSum(availableLinks.get(link.getId()).getScoreSum());
	      this.availableLinks.put(link.getId(), link);
	    }
	  
  }
  
  private double getLegScore(Leg leg, ScoringFunction function)
  {
	  if ((leg instanceof LegImpl))
	    {
	      function.handleLeg(leg);
	    }

	    function.finish();
	    return function.getScore();
  }

  private double handleCarLeg(Leg leg, Link fromLink, Link toLink, Network network, LeastCostPathCalculator routeAlgo)
    throws RuntimeException
  {
    NetworkRoute route;
    double travTime = 0.0D;
    double depTime = leg.getDepartureTime();

    if (fromLink == null) throw new RuntimeException("fromLink missing.");
    if (toLink == null) throw new RuntimeException("toLink missing.");

    Node startNode = fromLink.getToNode();
    Node endNode = toLink.getFromNode();

    LeastCostPathCalculator.Path path = null;
    if (toLink != fromLink)
    {
      path = routeAlgo.calcLeastCostPath(startNode, endNode, depTime, null, null);
      if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");

      route = new LinkNetworkRouteImpl(fromLink.getId(), toLink.getId());
      route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
      route.setTravelTime((int)path.travelTime);
      route.setTravelCost(path.travelCost);
      route.setDistance(RouteUtils.calcDistance(route, network));
      leg.setRoute(route);
      travTime = (int)path.travelTime;
    }
    else {
      route = new LinkNetworkRouteImpl( fromLink.getId(), toLink.getId());
      route.setTravelTime(0.0D);
      route.setDistance(0.0D);
      leg.setRoute(route);
      travTime = 0.0D;
    }

    leg.setDepartureTime(depTime);
    leg.setTravelTime(travTime);
    ((LegImpl)leg).setArrivalTime(depTime + travTime);
    return travTime;
  }

  @Override
	public double computePotential(ArrayList<Integer> solution) {
	  
	  Double Fitness = 0.0D;

	  ActivityFacilityImpl af = (ActivityFacilityImpl) retailerFacilities.values().toArray()[0];
	  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
		  String linkId = this.first.get(solution.get(s));
		 // Coord coord = new CoordImpl(1,1);
		  Utils.addShopToShopsQuadTree(this.availableLinks.get(new IdImpl(linkId)).getCoord().getX(), this.availableLinks.get(new IdImpl(linkId)).getCoord().getY(), af);
	  }
	  computePotentialCustomers();
	  //log.info("computed potential");
	  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
		  String linkId = this.first.get(solution.get(s));
		  Fitness +=  this.availableLinks.get(new IdImpl(linkId)).getPotentialCustomers();
	  }

	  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
		  String linkId = this.first.get(solution.get(s));		 
		  Utils.removeShopFromShopsQuadTree(this.availableLinks.get(new IdImpl(linkId)).getCoord().getX(), this.availableLinks.get(new IdImpl(linkId)).getCoord().getY(), af);
	  }
	  return Fitness;
  }

  public Map<Id, ActivityFacilityImpl> getScenarioShops() {
    return this.shops;
  }
}