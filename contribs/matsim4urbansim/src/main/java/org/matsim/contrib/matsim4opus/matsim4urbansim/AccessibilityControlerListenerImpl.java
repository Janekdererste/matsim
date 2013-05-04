package org.matsim.contrib.matsim4opus.matsim4urbansim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.config.AccessibilityParameterConfigModule;
import org.matsim.contrib.matsim4opus.config.ConfigurationUtils;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.Zone;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.improvedpseudopt.PtMatrix;
import org.matsim.contrib.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import org.matsim.contrib.matsim4opus.utils.LeastCostPathTreeExtended;
import org.matsim.contrib.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Benchmark;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Distances;
import org.matsim.contrib.matsim4opus.utils.helperObjects.SpatialReferenceObject;
import org.matsim.contrib.matsim4opus.utils.io.writer.AnalysisWorkplaceCSVWriter;
import org.matsim.contrib.matsim4opus.utils.misc.ProgressBar;
import org.matsim.contrib.matsim4opus.utils.network.NetworkUtil;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.utils.LeastCostPathTree;

import com.vividsolutions.jts.geom.Point;

/**
 * improvements aug'12
 * - accessibility calculation of unified for cell- and zone-base approach
 * - large computing savings due reduction of "least cost path tree" execution:
 *   In a pre-processing step all nearest nodes of measuring points (origins) are determined. 
 *   The "least cost path tree" for measuring points with the same nearest node are now only executed once. 
 *   Only the cost calculations from the measuring point to the network is done individually.
 *   
 * improvements nov'12
 * - bug fixed aggregatedOpportunities method for compound cost factors like time and distance    
 * 
 * improvements jan'13
 * - added pt for accessibility calculation
 *     
 * @author thomas
 *
 */
public class AccessibilityControlerListenerImpl{
	
	protected static final Logger log = Logger.getLogger(AccessibilityControlerListenerImpl.class);
	
	public static final String FREESEED_FILENAME= "freeSpeedAccessibility_cellsize_";
	public static final String CAR_FILENAME 	= "carAccessibility_cellsize_";
	public static final String BIKE_FILENAME 	= "bikeAccessibility_cellsize_";
	public static final String WALK_FILENAME 	= "walkAccessibility_cellsize_";
	public static final String PT_FILENAME 		= "ptAccessibility_cellsize_";
	
	protected MATSim4UrbanSimInterface main = null;
	
	protected static int ZONE_BASED 	= 0;
	protected static int PARCEL_BASED 	= 1;
	
	// start points, measuring accessibility (cell based approach)
	protected ZoneLayer<Id> measuringPointsCell;
	// start points, measuring accessibility (zone based approach)
	protected ZoneLayer<Id> measuringPointsZone;
	protected ActivityFacilitiesImpl zones; // tnicolai: this is old! replace!!!
	// containing parcel coordinates for accessibility feedback
	protected ActivityFacilitiesImpl parcels; 
	// destinations, opportunities like jobs etc ...
	protected AggregateObject2NearestNode[] aggregatedOpportunities;
	
	// storing the accessibility results
	protected SpatialGrid freeSpeedGrid;
	protected SpatialGrid carGrid;
	protected SpatialGrid bikeGrid;
	protected SpatialGrid walkGrid;
	protected SpatialGrid ptGrid;
	
	// storing pt matrix
	protected PtMatrix ptMatrix;
	
	// accessibility parameter
	protected boolean useRawSum	= false;
	protected double logitScaleParameter;
	protected double inverseOfLogitScaleParameter;
	protected double betaCarTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr() 
	protected double betaCarTTPower;
	protected double betaCarLnTT;
	protected double betaCarTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	protected double betaCarTDPower;
	protected double betaCarLnTD;
	protected double betaCarTMC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	protected double betaCarTMCPower;
	protected double betaCarLnTMC;
	protected double betaBikeTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingBike_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	protected double betaBikeTTPower;
	protected double betaBikeLnTT;
	protected double betaBikeTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateBike doesn't exist: 
	protected double betaBikeTDPower;
	protected double betaBikeLnTD;
	protected double betaBikeTMC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	protected double betaBikeTMCPower;
	protected double betaBikeLnTMC;
	protected double betaWalkTT;	// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
	protected double betaWalkTTPower;
	protected double betaWalkLnTT;
	protected double betaWalkTD;	// in MATSim this is 0 !!! since getMonetaryDistanceCostRateWalk doesn't exist: 
	protected double betaWalkTDPower;
	protected double betaWalkLnTD;
	protected double betaWalkTMC;	// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	protected double betaWalkTMCPower;
	protected double betaWalkLnTMC;
	protected double betaPtTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr() 
	protected double betaPtTTPower;
	protected double betaPtLnTT;
	protected double betaPtTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	protected double betaPtTDPower;
	protected double betaPtLnTD;
	protected double betaPtTMC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	protected double betaPtTMCPower;
	protected double betaPtLnTMC;
	
	protected double constCar;
	protected double constBike;
	protected double constWalk;
	protected double constPt;
	
	protected boolean usingCarParameterFromMATSim;	// free speed and congested car
	protected boolean usingBikeParameterFromMATSim;	// bicycle
	protected boolean usingWalkParameterFromMATSim;	// traveling on foot
	protected boolean usingPtParameterFromMATSim;	// public transport
	
	protected double VijCarTT, VijCarTTPower, VijCarLnTT, VijCarTD, VijCarTDPower, VijCarLnTD, VijCarTMC, VijCarTMCPower, VijCarLnTMC,
		   VijWalkTT, VijWalkTTPower, VijWalkLnTT, VijWalkTD, VijWalkTDPower, VijWalkLnTD, VijWalkTMC, VijWalkTMCPower, VijWalkLnTMC,
		   VijBikeTT, VijBikeTTPower, VijBikeLnTT, VijBikeTD, VijBikeTDPower, VijBikeLnTD, VijBikeTMC, VijBikeTMCPower, VijBikeLnTMC,
		   VijFreeTT, VijFreeTTPower, VijFreeLnTT, VijFreeTD, VijFreeTDPower, VijFreeLnTD, VijFreeTC, VijFreeTCPower, VijFreeLnTC,
		   VijPtTT, VijPtTTPower, VijPtLnTT, VijPtTD, VijPtTDPower, VijPtLnTD, VijPtTMC, VijPtTMCPower, VijPtLnTMC;
	
	protected double depatureTime;
	protected double bikeSpeedMeterPerHour = -1;
	protected double walkSpeedMeterPerHour = -1;
	Benchmark benchmark;
	
	RoadPricingSchemeImpl scheme;

	
	/**
	 * setting parameter for accessibility calculation
	 * @param scenario
	 */
	protected final void initAccessibilityParameter(ScenarioImpl scenario){
		
		AccessibilityParameterConfigModule moduleAPCM = ConfigurationUtils.getAccessibilityParameterConfigModule(scenario);
		
		useRawSum			= moduleAPCM.usingRawSumsWithoutLn();
		logitScaleParameter = moduleAPCM.getLogitScaleParameter();
		inverseOfLogitScaleParameter = 1/(logitScaleParameter); // logitScaleParameter = same as brainExpBeta on 2-aug-12. kai
		walkSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getWalkSpeed() * 3600.;
		bikeSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getBikeSpeed() * 3600.; // should be something like 15000
		
		usingCarParameterFromMATSim = moduleAPCM.usingCarParameterFromMATSim();
		usingBikeParameterFromMATSim= moduleAPCM.usingBikeParameterFromMATSim();
		usingWalkParameterFromMATSim= moduleAPCM.usingWalkParameterFromMATSim();
		usingPtParameterFromMATSim	= moduleAPCM.usingPtParameterFromMATSim();
		
		betaCarTT 	   	= moduleAPCM.getBetaCarTravelTime();
		betaCarTTPower	= moduleAPCM.getBetaCarTravelTimePower2();
		betaCarLnTT		= moduleAPCM.getBetaCarLnTravelTime();
		betaCarTD		= moduleAPCM.getBetaCarTravelDistance();
		betaCarTDPower	= moduleAPCM.getBetaCarTravelDistancePower2();
		betaCarLnTD		= moduleAPCM.getBetaCarLnTravelDistance();
		betaCarTMC		= moduleAPCM.getBetaCarTravelMonetaryCost();
		betaCarTMCPower	= moduleAPCM.getBetaCarTravelMonetaryCostPower2();
		betaCarLnTMC	= moduleAPCM.getBetaCarLnTravelMonetaryCost();
		
		betaBikeTT		= moduleAPCM.getBetaBikeTravelTime();
		betaBikeTTPower	= moduleAPCM.getBetaBikeTravelTimePower2();
		betaBikeLnTT	= moduleAPCM.getBetaBikeLnTravelTime();
		betaBikeTD		= moduleAPCM.getBetaBikeTravelDistance();
		betaBikeTDPower	= moduleAPCM.getBetaBikeTravelDistancePower2();
		betaBikeLnTD	= moduleAPCM.getBetaBikeLnTravelDistance();
		betaBikeTMC		= moduleAPCM.getBetaBikeTravelMonetaryCost();
		betaBikeTMCPower= moduleAPCM.getBetaBikeTravelMonetaryCostPower2();
		betaBikeLnTMC	= moduleAPCM.getBetaBikeLnTravelMonetaryCost();
		
		betaWalkTT		= moduleAPCM.getBetaWalkTravelTime();
		betaWalkTTPower	= moduleAPCM.getBetaWalkTravelTimePower2();
		betaWalkLnTT	= moduleAPCM.getBetaWalkLnTravelTime();
		betaWalkTD		= moduleAPCM.getBetaWalkTravelDistance();
		betaWalkTDPower	= moduleAPCM.getBetaWalkTravelDistancePower2();
		betaWalkLnTD	= moduleAPCM.getBetaWalkLnTravelDistance();
		betaWalkTMC		= moduleAPCM.getBetaWalkTravelMonetaryCost();
		betaWalkTMCPower= moduleAPCM.getBetaWalkTravelMonetaryCostPower2();
		betaWalkLnTMC	= moduleAPCM.getBetaWalkLnTravelMonetaryCost();
		
		betaPtTT		= moduleAPCM.getBetaPtTravelTime();
		betaPtTTPower	= moduleAPCM.getBetaPtTravelTimePower2();
		betaPtLnTT		= moduleAPCM.getBetaPtLnTravelTime();
		betaPtTD		= moduleAPCM.getBetaPtTravelDistance();
		betaPtTDPower	= moduleAPCM.getBetaPtTravelDistancePower2();
		betaPtLnTD		= moduleAPCM.getBetaPtLnTravelDistance();
		betaPtTMC		= moduleAPCM.getBetaPtTravelMonetaryCost();
		betaPtTMCPower	= moduleAPCM.getBetaPtTravelMonetaryCostPower2();
		betaPtLnTMC		= moduleAPCM.getBetaPtLnTravelMonetaryCost();
		
		constCar		= scenario.getConfig().planCalcScore().getConstantCar();
		constBike		= scenario.getConfig().planCalcScore().getConstantBike();
		constWalk		= scenario.getConfig().planCalcScore().getConstantWalk();
		constPt			= scenario.getConfig().planCalcScore().getConstantPt();
		
		depatureTime 	= this.main.getTimeOfDay(); // 8.*3600;	
		printParameterSettings();
	}
	
	/**
	 * displays settings
	 */
	protected final void printParameterSettings(){
		log.info("Computing and writing grid based accessibility measures with following settings:" );
		log.info("Returning raw sum (not logsum): " + useRawSum);
		log.info("Logit Scale Parameter: " + logitScaleParameter);
		log.info("Inverse of logit Scale Parameter: " + inverseOfLogitScaleParameter);
		log.info("Walk speed (meter/h): " + this.walkSpeedMeterPerHour + " ("+this.walkSpeedMeterPerHour/3600. +" meter/s)");
		log.info("Bike speed (meter/h): " + this.bikeSpeedMeterPerHour + " ("+this.bikeSpeedMeterPerHour/3600. +" meter/s)");
		log.info("Using Car (congested and free speed) Parameter from MATSim: " + usingCarParameterFromMATSim);
		log.info("Using Bicycle Parameter from MATSim: " + usingBikeParameterFromMATSim);
		log.info("Using Walk Parameter from MATSim: " + usingWalkParameterFromMATSim);
		log.info("Using Pt Parameter from MATSim: " + usingPtParameterFromMATSim);
		log.info("Depature time (in seconds): " + depatureTime);
		log.info("Beta Car Travel Time: " + betaCarTT );
		log.info("Beta Car Travel Time Power2: " + betaCarTTPower );
		log.info("Beta Car Ln Travel Time: " + betaCarLnTT );
		log.info("Beta Car Travel Distance: " + betaCarTD );
		log.info("Beta Car Travel Distance Power2: " + betaCarTDPower );
		log.info("Beta Car Ln Travel Distance: " + betaCarLnTD );
		log.info("Beta Car Travel Monetary Cost: " + betaCarTMC );
		log.info("Beta Car Travel Monetary Cost Power2: " + betaCarTMCPower );
		log.info("Beta Car Ln Travel Monetary Cost: " + betaCarLnTMC );
		log.info("Beta Bike Travel Time: " + betaBikeTT );
		log.info("Beta Bike Travel Time Power2: " + betaBikeTTPower );
		log.info("Beta Bike Ln Travel Time: " + betaBikeLnTT );
		log.info("Beta Bike Travel Distance: " + betaBikeTD );
		log.info("Beta Bike Travel Distance Power2: " + betaBikeTDPower );
		log.info("Beta Bike Ln Travel Distance: " + betaBikeLnTD );
		log.info("Beta Bike Travel Monetary Cost: " + betaBikeTMC );
		log.info("Beta Bike Travel Monetary Cost Power2: " + betaBikeTMCPower );
		log.info("Beta Bike Ln Travel Monetary Cost: " + betaBikeLnTMC );
		log.info("Beta Walk Travel Time: " + betaWalkTT );
		log.info("Beta Walk Travel Time Power2: " + betaWalkTTPower );
		log.info("Beta Walk Ln Travel Time: " + betaWalkLnTT );
		log.info("Beta Walk Travel Distance: " + betaWalkTD );
		log.info("Beta Walk Travel Distance Power2: " + betaWalkTDPower );
		log.info("Beta Walk Ln Travel Distance: " + betaWalkLnTD );
		log.info("Beta Walk Travel Monetary Cost: " + betaWalkTMC );
		log.info("Beta Walk Travel Monetary Cost Power2: " + betaWalkTMCPower );
		log.info("Beta Walk Ln Travel Monetary Cost: " + betaWalkLnTMC );
	}
	
	/**
	 * This aggregates the disjutilities Vjk to get from node j to all k that are attached to j.
	 * Finally the sum(Vjk) is assigned to node j, which is done in this method.
	 * 
	 *     j---k1 
	 *     |\
	 *     | \
	 *     k2 k3
	 *     
	 * @param parcelsOrZones opportunities like work places either given at a parcel- or zone level
	 * @param jobSample allows to reduce the sample size of opportunities
	 * @param network the road network
	 * @return the sum of disutilities Vjk, i.e. the disutilities to reach all opportunities k that are assigned to j from node j 
	 */
	protected final AggregateObject2NearestNode[] aggregatedOpportunities(final ActivityFacilitiesImpl parcelsOrZones, final double jobSample, final NetworkImpl network, final boolean isParcelMode){
		
		// readJobs creates a hash map of job with key = job id
		// this hash map includes jobs according to job sample size
		List<SpatialReferenceObject> jobSampleList = this.main.getReadFromUrbanSimModel().readJobs(parcelsOrZones, jobSample, isParcelMode);
		assert( jobSampleList != null );
		
		// Since the aggregated opportunities in jobClusterArray does contain coordinates of their nearest node 
		// this result is dumped out here    tnicolai dec'12
		AnalysisWorkplaceCSVWriter.writeWorkplaceData2CSV(InternalConstants.MATSIM_4_OPUS_TEMP + "workplaces.csv", jobSampleList);
		
		log.info("Aggregating workplaces with identical nearest node ...");
		Map<Id, AggregateObject2NearestNode> opportunityClusterMap = new ConcurrentHashMap<Id, AggregateObject2NearestNode>();
		
		ProgressBar bar = new ProgressBar( jobSampleList.size() );

		for(int i = 0; i < jobSampleList.size(); i++){
			bar.update();
			
			SpatialReferenceObject sro = jobSampleList.get( i );
			assert( sro.getCoord() != null );
			Node nearestNode = network.getNearestNode( sro.getCoord() );
			assert( nearestNode != null );

			// get euclidian distance to nearest node
			double distance_meter 	= NetworkUtil.getEuclidianDistance(sro.getCoord(), nearestNode.getCoord());
			double walkTravelTime_h = distance_meter / this.walkSpeedMeterPerHour;
			
			double VjkWalkTravelTime	= this.betaWalkTT * walkTravelTime_h;
			double VjkWalkPowerTravelTime=0.; // this.betaWalkTTPower * (walkTravelTime_h * walkTravelTime_h);
			double VjkWalkLnTravelTime	= 0.; // this.betaWalkLnTT * Math.log(walkTravelTime_h);
			
			double VjkWalkDistance 		= this.betaWalkTD * distance_meter;
			double VjkWalkPowerDistnace	= 0.; //this.betaWalkTDPower * (distance_meter * distance_meter);
			double VjkWalkLnDistance 	= 0.; //this.betaWalkLnTD * Math.log(distance_meter);
			
			double VjkWalkMoney			= this.betaWalkTMC * 0.; 		// no monetary costs for walking
			double VjkWalkPowerMoney	= 0.; //this.betaWalkTDPower * 0.; 	// no monetary costs for walking
			double VjkWalkLnMoney		= 0.; //this.betaWalkLnTMC *0.; 		// no monetary costs for walking

			double Vjk					= Math.exp(this.logitScaleParameter * (VjkWalkTravelTime + VjkWalkPowerTravelTime + VjkWalkLnTravelTime +
																			   VjkWalkDistance   + VjkWalkPowerDistnace   + VjkWalkLnDistance +
																			   VjkWalkMoney      + VjkWalkPowerMoney      + VjkWalkLnMoney) );
			// add Vjk to sum
			if( opportunityClusterMap.containsKey( nearestNode.getId() ) ){
				AggregateObject2NearestNode jco = opportunityClusterMap.get( nearestNode.getId() );
				jco.addObject( sro.getObjectID(), Vjk);
			}
			// assign Vjk to given network node
			else
				opportunityClusterMap.put(
						nearestNode.getId(),
						new AggregateObject2NearestNode(sro.getObjectID(), 
														sro.getParcelID(), 
														sro.getZoneID(), 
														nearestNode.getCoord(), 
														nearestNode, 
														Vjk));
		}
		
		// convert map to array
		AggregateObject2NearestNode jobClusterArray []  = new AggregateObject2NearestNode[ opportunityClusterMap.size() ];
		Iterator<AggregateObject2NearestNode> jobClusterIterator = opportunityClusterMap.values().iterator();

		for(int i = 0; jobClusterIterator.hasNext(); i++)
			jobClusterArray[i] = jobClusterIterator.next();
		
		log.info("Aggregated " + jobSampleList.size() + " number of workplaces (sampling rate: " + jobSample + ") to " + jobClusterArray.length + " nodes.");
		
		return jobClusterArray;
	}
	
	
	/**
	 * @param ttc
	 * @param lcptFreeSpeedCarTravelTime
	 * @param lcptCongestedCarTravelTime
	 * @param lcptTravelDistance
	 * @param network
	 * @param inverseOfLogitScaleParameter
	 * @param accCsvWriter
	 * @param measuringPointIterator
	 */
	protected final void accessibilityComputation(TravelTime ttc,
											LeastCostPathTreeExtended lcptExtFreeSpeedCarTravelTime,
											LeastCostPathTreeExtended lcptExtCongestedCarTravelTime,
											LeastCostPathTree lcptTravelDistance, 
											PtMatrix ptMatrix,
											NetworkImpl network,
											Iterator<Zone<Id>> measuringPointIterator,
											int numberOfMeasuringPoints, 
											int mode,
											Controler contorler) {

		GeneralizedCostSum gcs = new GeneralizedCostSum();
		
//			// tnicolai: only for testing, disable afterwards
//			ZoneLayer<Id> testSet = createTestPoints();
//			measuringPointIterator = testSet.getZones().iterator();

		// this data structure condense measuring points (origins) that have the same nearest node on the network ...
		Map<Id,ArrayList<Zone<Id>>> aggregatedMeasurementPoints = new ConcurrentHashMap<Id, ArrayList<Zone<Id>>>();

		// go through all measuring points ...
		while( measuringPointIterator.hasNext() ){

			Zone<Id> measurePoint = measuringPointIterator.next();
			Point point = measurePoint.getGeometry().getCentroid();
			// get coordinate from origin (start point)
			Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
			// captures the distance (as walk time) between a cell centroid and the road network
			Link nearestLink = network.getNearestLinkExactly(coordFromZone);
			// determine nearest network node (from- or toNode) based on the link 
			Node fromNode = NetworkUtil.getNearestNode(coordFromZone, nearestLink);
			
			// this is used as a key for hash map lookups
			Id id = fromNode.getId();
			
			// create new entry if key does not exist!
			if(!aggregatedMeasurementPoints.containsKey(id))
				aggregatedMeasurementPoints.put(id, new ArrayList<Zone<Id>>());
			// assign measure point (origin) to it's nearest node
			aggregatedMeasurementPoints.get(id).add(measurePoint);
		}
		
		log.info("");
		log.info("Number of measure points: " + numberOfMeasuringPoints);
		log.info("Number of aggregated measure points: " + aggregatedMeasurementPoints.size());
		log.info("");
		

		ProgressBar bar = new ProgressBar( aggregatedMeasurementPoints.size() );
		
		// contains all nodes that have a measuring point (origin) assigned
		Iterator<Id> keyIterator = aggregatedMeasurementPoints.keySet().iterator();
		// contains all network nodes
		Map<Id, Node> networkNodesMap = network.getNodes();
		
		// go through all nodes (key's) that have a measuring point (origin) assigned
		while( keyIterator.hasNext() ){
			
			bar.update();
			
			Id nodeId = keyIterator.next();
			Node fromNode = networkNodesMap.get( nodeId );
			
			// run dijkstra on network
			// this is done once for all origins in the "origins" list, see below
			lcptExtFreeSpeedCarTravelTime.calculateExtended(network, fromNode, depatureTime);
			lcptExtCongestedCarTravelTime.calculateExtended(network, fromNode, depatureTime);		
			lcptTravelDistance.calculate(network, fromNode, depatureTime);
			
			// get list with origins that are assigned to "fromNode"
			ArrayList<Zone<Id>> origins = aggregatedMeasurementPoints.get( nodeId );
			Iterator<Zone<Id>> originsIterator = origins.iterator();
			
			while( originsIterator.hasNext() ){
				
				Zone<Id> measurePoint = originsIterator.next();
				Point point = measurePoint.getGeometry().getCentroid();
				// get coordinate from origin (start point)
				Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
				assert( coordFromZone!=null );
				// captures the distance (as walk time) between a cell centroid and the road network
				LinkImpl nearestLink = (LinkImpl)network.getNearestLinkExactly(coordFromZone);
				
				// captures the distance (as walk time) between a zone centroid and its nearest node
				Distances distance = NetworkUtil.getDistance2Node(nearestLink, point, fromNode);
				
				double distanceMeasuringPoint2Road_meter 	= distance.getDistancePoint2Road(); // distance measuring point 2 road (link or node)
				double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	// distance intersection 2 node (only for orthogonal distance), this is zero if projection is on a node 
				
				// traveling on foot from measuring point to the network (link or node)
				double walkTravelTimeMeasuringPoint2Road_h 	= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour;

				// get free speed and congested car travel times on a certain link
				double freeSpeedTravelTimeOnNearestLink_meterpersec = nearestLink.getFreespeedTravelTime(depatureTime);
				double carTravelTimeOnNearestLink_meterpersec= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, depatureTime, null, null);
				// travel time in hours to get from link intersection (position on a link given by orthogonal projection from measuring point) to the corresponding node
				double road2NodeFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTimeOnNearestLink_meterpersec * 3600);
				double road2NodeCongestedCarTime_h 			= distanceRoad2Node_meter / (carTravelTimeOnNearestLink_meterpersec * 3600.);
				double road2NodeBikeTime_h					= distanceRoad2Node_meter / this.bikeSpeedMeterPerHour;
				double road2NodeWalkTime_h					= distanceRoad2Node_meter / this.walkSpeedMeterPerHour;
				double road2NodeToll_money 					= getToll(nearestLink); // tnicolai: add this to car disutility ??? depends on the road pricing scheme ...
				
				// this contains the current toll based on the toll scheme
				double toll_money 							= 0.;
				if(this.scheme != null && RoadPricingScheme.TOLL_TYPE_CORDON.equals(this.scheme.getType()))
					toll_money = road2NodeToll_money;
				else if(this.scheme != null && RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType()))
					toll_money = road2NodeToll_money * distanceRoad2Node_meter;
				
				gcs.reset();

				// goes through all opportunities, e.g. jobs, (nearest network node) and calculate the accessibility
				for ( int i = 0; i < this.aggregatedOpportunities.length; i++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated work place)
					Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					
					// disutilities on the road network
					double congestedCarDisutility = - lcptExtCongestedCarTravelTime.getTree().get( nodeID ).getCost();	// travel disutility congested car on road network (including toll)
					double freeSpeedCarDisutility = - lcptExtFreeSpeedCarTravelTime.getTree().get( nodeID ).getCost();	// travel disutility free speed car on road network (including toll)
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost(); 				// travel link distances on road network for bicycle and walk

					// travel times and distances for pseudo pt
					double ptTravelTime_h		= Double.MAX_VALUE;	// travel time with pt
					double ptTotalWalkTime_h	= Double.MAX_VALUE;	// total walking time including (i) to get to pt stop and (ii) to get from destination pt stop to destination location
					double ptTravelDistance_meter=Double.MAX_VALUE; // total travel distance including walking and pt distance from/to origin/destination location
					double ptTotalWalkDistance_meter=Double.MAX_VALUE;// total walk distance  including (i) to get to pt stop and (ii) to get from destination pt stop to destination location
					if(ptMatrix != null){
						ptTravelTime_h 			= ptMatrix.getPtTravelTime_seconds(fromNode.getCoord(), destinationNode.getCoord()) / 3600.;
						ptTotalWalkTime_h		= ptMatrix.getTotalWalkTravelTime_seconds(fromNode.getCoord(), destinationNode.getCoord()) / 3600.;
						
						ptTotalWalkDistance_meter=ptMatrix.getTotalWalkTravelDistance_meter(fromNode.getCoord(), destinationNode.getCoord());
						ptTravelDistance_meter  = ptMatrix.getPtTravelDistance_meter(fromNode.getCoord(), destinationNode.getCoord());
					}
					double ptDisutility = constPt + (ptTotalWalkTime_h * betaWalkTT) + (ptTravelTime_h * betaPtTT) + (ptTotalWalkDistance_meter * betaWalkTD) + (ptTravelDistance_meter * betaPtTD);
					
					// disutilities to get on or off the network
					double walkDisutilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT) + (distanceMeasuringPoint2Road_meter * betaWalkTD);
					double expVhiWalk = Math.exp(this.logitScaleParameter * walkDisutilityMeasuringPoint2Road);
					double sumExpVjkWalk = aggregatedOpportunities[i].getSumVjk();
					
					// total disutility congested car
					double congestedCarDisutilityRoad2Node = (road2NodeCongestedCarTime_h * betaCarTT) + (distanceRoad2Node_meter * betaCarTD) + (toll_money * betaCarTMC); 
					double expVijCongestedCar = Math.exp(this.logitScaleParameter * (constCar + congestedCarDisutilityRoad2Node + congestedCarDisutility) );
					double expVhkCongestedCar = expVhiWalk * expVijCongestedCar * sumExpVjkWalk;
					gcs.addCongestedCarCost( expVhkCongestedCar );
					
					// total disutility free speed car
					double freeSpeedCarDisutilityRoad2Node = (road2NodeFreeSpeedTime_h * betaCarTT) + (distanceRoad2Node_meter * betaCarTD) + (toll_money * betaCarTMC); 
					double expVijFreeSpeedCar = Math.exp(this.logitScaleParameter * (constCar + freeSpeedCarDisutilityRoad2Node + freeSpeedCarDisutility) );
					double expVhkFreeSpeedCar = expVhiWalk * expVijFreeSpeedCar * sumExpVjkWalk;
					gcs.addFreeSpeedCost( expVhkFreeSpeedCar );
					
					// total disutility bicycle
					double bikeDisutilityRoad2Node = (road2NodeBikeTime_h * betaBikeTT) + (distanceRoad2Node_meter * betaBikeTD); // toll or money ???
					double bikeDisutility = ((travelDistance_meter/this.bikeSpeedMeterPerHour) * betaBikeTT) + (travelDistance_meter * betaBikeTD);// toll or money ???
					double expVijBike = Math.exp(this.logitScaleParameter * (constBike + bikeDisutility + bikeDisutilityRoad2Node));
					double expVhkBike = expVhiWalk * expVijBike * sumExpVjkWalk;
					gcs.addBikeCost( expVhkBike );
					
					// total disutility walk
					double walkDisutilityRoad2Node = (road2NodeWalkTime_h * betaWalkTT) + (distanceRoad2Node_meter * betaWalkTD);  // toll or money ???
					double walkDisutility = ( (travelDistance_meter / this.walkSpeedMeterPerHour) * betaWalkTT) + ( travelDistance_meter * betaWalkTD);// toll or money ???
					double expVijWalk = Math.exp(this.logitScaleParameter * (constWalk + walkDisutility + walkDisutilityRoad2Node));
					double expVhkWalk = expVhiWalk * expVijWalk * sumExpVjkWalk;
					gcs.addWalkCost( expVhkWalk );
					
					double expVijPt = Math.exp(this.logitScaleParameter * ptDisutility);
					double expVhkPt = expVijPt * sumExpVjkWalk;
					gcs.addPtCost( expVhkPt );
					
//					// old version
//
//					// congested car travel times in hours
//					double congestedCarArrivalTime 	= lcptExtCongestedCarTravelTime.getTree().get( nodeID ).getTime();
//					double congestedCarTravelTime_h = ((congestedCarArrivalTime - depatureTime) / 3600.) + road2NodeCongestedCarTime_h;
//					// congested car travel distance in meter
//					double congestedCarTravelDistance_meter = lcptExtCongestedCarTravelTime.getTreeExtended().get( nodeID ).getDistance();
//					// congested car toll in money units
//					double congestedCarToll_money = lcptExtCongestedCarTravelTime.getTreeExtended().get( nodeID ).getToll();
//
//					// free speed car travel times in hours
//					double freespeedCarArrivalTime	= lcptExtFreeSpeedCarTravelTime.getTree().get( nodeID ).getTime();
//					double freeSpeedCarTravelTime_h	= ((freespeedCarArrivalTime - depatureTime) / 3600.) + road2NodeFreeSpeedTime_h;
//					// free speed car travel distance in meter
//					double freeSpeedCarTravelDistance_meter = lcptExtFreeSpeedCarTravelTime.getTreeExtended().get( nodeID ).getDistance();
//					// free speed car toll in money units
//					double freeSpeedCarToll_money 	= lcptExtFreeSpeedCarTravelTime.getTreeExtended().get( nodeID ).getToll();
//					
//					// travel distance in meter
//					// double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
//					// bike travel times in hours
//					double bikeTravelTime_h 	= (travelDistance_meter / this.bikeSpeedMeterPerHour) + road2NodeBikeTime_h; // using a constant speed of 15km/h
//					// walk travel times in hours
//					double walkTravelTime_h		= (travelDistance_meter / this.walkSpeedMeterPerHour) + road2NodeWalkTime_h;
//					
//					sumDisutilityOfTravel(gcs, 
//							this.aggregatedOpportunities[i],	// array of opportunities aggregated on their nearest node on the road network
//							distanceMeasuringPoint2Road_meter,	// orthogonal distance [meter] from measuring point to network
//							distanceRoad2Node_meter, 			// if orthogonal projection ends in a link, the distance to the nearest node is measured [meter]
//							travelDistance_meter,				// distance [meter] (sum of all links) on the road network
//							walkTravelTimeMeasuringPoint2Road_h,			// walk travel time [hour] to get from origin i to the network
//							freeSpeedCarTravelTime_h,			// free speed car travel times [hour]
//							freeSpeedCarTravelDistance_meter,	// travel distance [meter] for free speed car travel
//							freeSpeedCarToll_money,				// monetary travel cost [money unit] for free speed car travel
//							congestedCarTravelTime_h,			// congested car travel times [hour]
//							congestedCarTravelDistance_meter,	// travel distance [meter] for congested speed car travel
//							congestedCarToll_money,				// monetary travel cost [money unit] for congested speed car travel
//							bikeTravelTime_h,					// travel times on bicycle [hour]
//							walkTravelTime_h, 					// travel times on foot [hour]
//							ptTravelTime_h,						// pt travel times [hour]
//							ptTotalWalkTime_h,					// total walk time [hour], from measuring point to pt stop plus from destination pt stop to opportunity
//							ptTravelDistance_meter,				// pt travel distance [meter] 
//							ptTotalWalkDistance_meter);			// total walk distance [meter], from measuring point to pt stop plus from destination pt stop to opportunity
				}
				
				// aggregated value
				double freeSpeedAccessibility, carAccessibility, bikeAccessibility, walkAccessibility, ptAccessibility;
				if(!useRawSum){ 	// get log sum
					freeSpeedAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getFreeSpeedSum() );
					carAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getCarSum() );
					bikeAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getBikeSum() );
					walkAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getWalkSum() );
					ptAccessibility	 = inverseOfLogitScaleParameter * Math.log( gcs.getPtSum() );
				}
				else{ 				// get raw sum
					freeSpeedAccessibility = inverseOfLogitScaleParameter * gcs.getFreeSpeedSum();
					carAccessibility = inverseOfLogitScaleParameter * gcs.getCarSum();
					bikeAccessibility= inverseOfLogitScaleParameter * gcs.getBikeSum();
					walkAccessibility= inverseOfLogitScaleParameter * gcs.getWalkSum();
					ptAccessibility  = inverseOfLogitScaleParameter * gcs.getPtSum();
				}

				if(mode == PARCEL_BASED){ // only for cell-based accessibility computation
					// assign log sums to current starZone object and spatial grid
					freeSpeedGrid.setValue(freeSpeedAccessibility, measurePoint.getGeometry().getCentroid());
					carGrid.setValue(carAccessibility , measurePoint.getGeometry().getCentroid());
					bikeGrid.setValue(bikeAccessibility , measurePoint.getGeometry().getCentroid());
					walkGrid.setValue(walkAccessibility , measurePoint.getGeometry().getCentroid());
					ptGrid.setValue(ptAccessibility, measurePoint.getGeometry().getCentroid());
				}
				
				writeCSVData(measurePoint, coordFromZone, fromNode, 
						freeSpeedAccessibility, carAccessibility,
						bikeAccessibility, walkAccessibility, ptAccessibility);
			}
		}
	}

	/**
	 * @param nearestLink
	 */
	protected double getToll(Link nearestLink) {
		if(scheme != null){
			Cost cost = scheme.getLinkCostInfo(nearestLink.getId(), depatureTime, null);
			if(cost != null)
				return cost.amount;
		}
		return 0.;
	}
	
	/**
	 * This calculates the logsum for a given origin i over all opportunities k attached to node j
	 * 
	 * i ----------j---k1
	 *             | \
	 * 			   k2 k3
	 * 
	 * This caluclation is done in 2 steps:
	 * 
	 * 1) The disutilities Vjk to get from node j to all opportunities k are attached to j.
	 *    This is already done above in "aggregatedOpportunities" method and the result is 
	 *    stored in "aggregatedOpportunities" object:
	 * 
	 * S_j = sum_k_in_j (exp(Vjk)) = exp(Vjk1) + exp(Vjk2) + exp(Vjk3)
	 * 
	 * 2) The disutility Vij to get from origin location i to destination node j is calculated in this method.
	 *    Finally the following logsum is taken:   
	 * 
	 * A_i = 1/beatascale * ln (sum_j (exp(Vij) * S_j ) )
	 * 
	 * @param gcs stores the value for the term "exp(Vik)"
	 * @param distanceMeasuringPoint2Road_meter distance [meter] from origin i to the network
	 * @param distanceRoad2Node_meter if the mapping of i on the network is on a link, this is the distance [meter] from this mapping to the nearest node on the network
	 * @param travelDistance_meter travel distances [meter] on the network to get to destination node j
	 * @param walkTravelTimePoint2Road_h walk travel time [hour] to get from origin i to the network
	 * @param freeSpeedTravelTime_h free speed travel times [hour] on the network to get to destination node j
	 * @param freeSpeedCarTravelDistance_meter free speed travel distance [meter] on the network to get to destination node j
	 * @param freeSpeedCarToll_money monetary travel costs [money unit] on the network to get to destination node j
	 * @param congestedCarTravelTime_h congested car travel times [hour] on the network to get to destination node j
	 * @param congestedCarTravelDistance_meter congested car travel distance [meter] on the network to get to destination node j
	 * @param congestedCarToll_money monetary travel costs [money unit] on the network to get to destination node j
	 * @param bikeTravelTime_h bike travel times [hour] on the network to get to destination node j
	 * @param walkTravelTime_h walk travel times [hour] on the network to get to destination node j
	 * 
	 */
	protected final void sumDisutilityOfTravel(GeneralizedCostSum gcs,
									   AggregateObject2NearestNode aggregatedOpportunities,
									   double distanceMeasuringPoint2Road_meter,
									   double distanceRoad2Node_meter, 
									   double travelDistance_meter, 
									   double walkTravelTimePoint2Road_h,
									   double freeSpeedTravelTime_h,
									   double freeSpeedCarTravelDistance_meter,
									   double freeSpeedCarToll_money,
									   double congestedCarTravelTime_h,
									   double congestedCarTravelDistance_meter,
									   double congestedCarToll_money,
									   double bikeTravelTime_h,
									   double walkTravelTime_h,
									   double ptTravelTime_h,
									   double ptTotalWalkTime_h,
									   double ptTravelDistance_meter,
									   double ptTotalWalkDistance_meter) {
		
		// for debugging free speed accessibility
		VijFreeTT 	= getAsUtil(betaCarTT, freeSpeedTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		VijFreeTTPower= getAsUtil(betaCarTTPower, freeSpeedTravelTime_h * freeSpeedTravelTime_h, betaWalkTTPower, walkTravelTimePoint2Road_h * walkTravelTimePoint2Road_h);
		VijFreeLnTT = getAsUtil(betaCarLnTT, Math.log(freeSpeedTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h));
		
		VijFreeTD 	= getAsUtil(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter);
		VijFreeTDPower= getAsUtil(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
		VijFreeLnTD = getAsUtil(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
		
		VijFreeTC 	= getAsUtil(betaCarTMC, freeSpeedCarToll_money, betaWalkTMC, 0);
		VijFreeTCPower= getAsUtil(betaCarTDPower, Math.pow( freeSpeedCarToll_money , 2), betaWalkTDPower, 0);
		VijFreeLnTC = getAsUtil(betaCarLnTMC, Math.log( freeSpeedCarToll_money ), betaWalkLnTMC, 0);
		
		double expFreeSpeedVij = Math.exp(logitScaleParameter *
										 (constCar
										+ VijFreeTT + VijFreeTTPower + VijFreeLnTT
			     					    + VijFreeTD + VijFreeTDPower + VijFreeLnTD
										+ VijFreeTC + VijFreeTCPower + VijFreeLnTC) );
	
		// sum free speed travel times
		gcs.addFreeSpeedCost( expFreeSpeedVij * aggregatedOpportunities.getSumVjk());
		
		// for debugging car accessibility
		VijCarTT 	= getAsUtil(betaCarTT, congestedCarTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		VijCarTTPower= getAsUtil(betaCarTTPower, congestedCarTravelTime_h * congestedCarTravelTime_h, betaWalkTTPower, walkTravelTimePoint2Road_h * walkTravelTimePoint2Road_h);
		VijCarLnTT	= getAsUtil(betaCarLnTT, Math.log(congestedCarTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h));
		
		VijCarTD 	= getAsUtil(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter); // carOffsetWalkTime2NearestLink_meter
		VijCarTDPower= getAsUtil(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
		VijCarLnTD 	= getAsUtil(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
		
		VijCarTMC 	= getAsUtil(betaCarTMC, congestedCarToll_money, betaWalkTMC, 0);
		VijCarTMCPower= getAsUtil(betaCarTMCPower, Math.pow( congestedCarToll_money, 2), betaWalkTMCPower, 0);
		VijCarLnTMC	= getAsUtil(betaCarLnTMC, Math.log(congestedCarToll_money), betaWalkLnTMC, 0);
		
		double expCongestedCarVij = Math.exp(logitScaleParameter *
											(constCar
										   + VijCarTT + VijCarTTPower + VijCarLnTT 
										   + VijCarTD + VijCarTDPower + VijCarLnTD 
										   + VijCarTMC + VijCarTMCPower + VijCarLnTMC));
		
		// sum congested travel times
		gcs.addCongestedCarCost( expCongestedCarVij * aggregatedOpportunities.getSumVjk());
		
		// for debugging bike accessibility
		VijBikeTT 	= getAsUtil(betaBikeTT, bikeTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		VijBikeTTPower= getAsUtil(betaBikeTTPower, bikeTravelTime_h * bikeTravelTime_h, betaWalkTTPower, walkTravelTimePoint2Road_h * walkTravelTimePoint2Road_h);
		VijBikeLnTT	= getAsUtil(betaBikeLnTT, Math.log(bikeTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h));
		
		VijBikeTD 	= getAsUtil(betaBikeTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter); 
		VijBikeTDPower= getAsUtil(betaBikeTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
		VijBikeLnTD = getAsUtil(betaBikeLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
		
		VijBikeTMC 	= 0.; 	// since MATSim doesn't gives monetary costs (toll)
		VijBikeTMCPower= 0.;// since MATSim doesn't gives monetary costs (toll) 
		VijBikeLnTMC = 0.;	// since MATSim doesn't gives monetary costs (toll) 
		
		double expBikeVij = Math.exp(logitScaleParameter *
								    (constBike
								   + VijBikeTT + VijBikeTTPower + VijBikeLnTT 
								   + VijBikeTD + VijBikeTDPower + VijBikeLnTD 
								   + VijBikeTMC + VijBikeTMCPower + VijBikeLnTMC));
		
		// sum congested travel times
		gcs.addBikeCost( expBikeVij * aggregatedOpportunities.getSumVjk());
		
		// for debugging walk accessibility
		double totalWalkTravelTime = walkTravelTime_h + ((distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter)/this.walkSpeedMeterPerHour);
		double totalTravelDistance = travelDistance_meter + distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter;
		
		VijWalkTT = getAsUtil(betaWalkTT, totalWalkTravelTime,0, 0);
		VijWalkTTPower = getAsUtil(betaWalkTTPower, totalWalkTravelTime * totalWalkTravelTime, 0 ,0);
		VijWalkLnTT = getAsUtil(betaWalkLnTT, Math.log(totalWalkTravelTime), 0, 0);
		
		VijWalkTD = getAsUtil(betaWalkTD, totalTravelDistance, 0, 0);
		VijWalkTDPower = getAsUtil(betaWalkTDPower, totalTravelDistance * totalTravelDistance, 0, 0);
		VijWalkLnTD = getAsUtil(betaWalkLnTD, Math.log(totalTravelDistance), 0, 0);

		VijWalkTMC 	= 0.;	// since MATSim doesn't gives monetary costs (toll) 
		VijWalkTMCPower= 0.;// since MATSim doesn't gives monetary costs (toll) 
		VijWalkLnTMC = 0.;	// since MATSim doesn't gives monetary costs (toll) 
		
		double expWalkVij = Math.exp(logitScaleParameter *
									(constWalk
								   + VijWalkTT + VijWalkTTPower + VijWalkLnTT 
				                   + VijWalkTD + VijWalkTDPower + VijWalkLnTD 
								   + VijWalkTMC + VijWalkTMCPower + VijWalkLnTMC));

		// sum walk travel times (substitute for distances)
		gcs.addWalkCost(expWalkVij * aggregatedOpportunities.getSumVjk());
		
		// for debugging pt accessibility
		VijPtTT 	= getAsUtil(betaPtTT, ptTravelTime_h, betaWalkTT, (walkTravelTimePoint2Road_h + ptTotalWalkTime_h));
		VijPtTTPower= getAsUtil(betaPtTTPower, ptTravelTime_h * ptTravelTime_h, betaWalkTTPower, (walkTravelTimePoint2Road_h + ptTotalWalkTime_h) * (walkTravelTimePoint2Road_h + ptTotalWalkTime_h));
		VijPtLnTT	= getAsUtil(betaPtLnTT, Math.log(ptTravelTime_h), betaWalkLnTT, Math.log(walkTravelTimePoint2Road_h + ptTotalWalkTime_h));
		
		VijPtTD 	= getAsUtil(betaPtTD, ptTravelDistance_meter + distanceRoad2Node_meter, betaWalkTD, (distanceMeasuringPoint2Road_meter + ptTotalWalkDistance_meter)); 
		VijPtTDPower= getAsUtil(betaPtTDPower, Math.pow(ptTravelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, (distanceMeasuringPoint2Road_meter + ptTotalWalkDistance_meter) * (distanceMeasuringPoint2Road_meter + ptTotalWalkDistance_meter));
		VijPtLnTD 	= getAsUtil(betaPtLnTD, Math.log(ptTravelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter + ptTotalWalkDistance_meter));
		
		VijPtTMC 	= 0.; 	// since MATSim doesn't gives monetary costs (toll) 
		VijPtTMCPower= 0.;	// since MATSim doesn't gives monetary costs (toll) 
		VijPtLnTMC 	= 0.;	// since MATSim doesn't gives monetary costs (toll) 
		
		double expPtVij = Math.exp(logitScaleParameter *
								  (constPt
								 + VijPtTT + VijPtTTPower + VijPtLnTT 
								 + VijPtTD + VijPtTDPower + VijPtLnTD 
								 + VijPtTMC + VijPtTMCPower + VijPtLnTMC));
		
		gcs.addPtCost(expPtVij * aggregatedOpportunities.getSumVjk());
	}
	
	/**
	 * converts travel costs (e.g. travel times or distances) into utils by 
	 * using the corresponding marginal utilities
	 * 
	 * @param betaModeX marginal utility for a travel mode other than walk
	 * @param ModeTravelCostX travel costs like travel times or distances
	 * @param betaWalkX marginal utility for traveling on foot
	 * @param walkOrigin2NetworkX travel costs like travel times or distances for traveling on foot
	 * @return disutility of traveling
	 */
	protected final double getAsUtil(final double betaModeX, final double ModeTravelCostX, final double betaWalkX, final double walkOrigin2NetworkX){
		if(betaModeX != 0.)
			return (betaModeX * ModeTravelCostX + betaWalkX * walkOrigin2NetworkX);
		return 0.;
	}
	
//	
//	protected ZoneLayer<Id> createTestPoints(){
//		
//		GeometryFactory factory = new GeometryFactory();
//		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();
//		int setPoints = 1;
//		int srid = InternalConstants.SRID_SWITZERLAND;
//		int gridSize = 10;
//		
//		Point point1 = factory.createPoint(new Coordinate(680699.1, 250976.0)); // oben links
//		Point point2 = factory.createPoint(new Coordinate(681410.0, 250670.0)); // oben mitte
//		Point point3 = factory.createPoint(new Coordinate(682419.0, 250232.0)); // oben rechts
//		Point point4 = factory.createPoint(new Coordinate(680602.2, 250934.2)); // unten links
//		
//		createCell(factory, zones, point1, setPoints++, srid, gridSize);
//		createCell(factory, zones, point2, setPoints++, srid, gridSize);
//		createCell(factory, zones, point3, setPoints++, srid, gridSize);
//		createCell(factory, zones, point4, setPoints++, srid, gridSize);
//		
//		ZoneLayer<Id> layer = new ZoneLayer<Id>(zones);
//		return layer;
//	}
//
//	/**
//	 * This is for testing purposes only
//	 * 
//	 * @param factory
//	 * @param zones
//	 * @param setPoints
//	 * @param srid
//	 */
//	private void createCell(GeometryFactory factory, Set<Zone<Id>> zones, Point point, int setPoints, int srid, int gridSize) {
//		
//		double x = point.getCoordinate().x;
//		double y = point.getCoordinate().y;
//		
//		Coordinate[] coords = new Coordinate[5];
//		coords[0] = new Coordinate(x-gridSize, y-gridSize); 	// links unten
//		coords[1] = new Coordinate(x-gridSize, y + gridSize);	// links oben
//		coords[2] = new Coordinate(x + gridSize, y + gridSize);	// rechts oben
//		coords[3] = new Coordinate(x + gridSize, y-gridSize);	// rechts unten
//		coords[4] = new Coordinate(x-gridSize, y-gridSize); 	// links unten
//		// Linear Ring defines an artificial zone
//		LinearRing linearRing = factory.createLinearRing(coords);
//		Polygon polygon = factory.createPolygon(linearRing, null);
//		polygon.setSRID( srid ); 
//		
//		Zone<Id> zone = new Zone<Id>(polygon);
//		zone.setAttribute( new Id( setPoints ) );
//		zones.add(zone);
//	}
	
	/**
	 * Writes measured accessibilities as csv format to disc
	 * 
	 * @param measurePoint
	 * @param coordFromZone
	 * @param fromNode
	 * @param freeSpeedAccessibility
	 * @param carAccessibility
	 * @param bikeAccessibility
	 * @param walkAccessibility
	 */
	protected void writeCSVData(
			Zone<Id> measurePoint, Coord coordFromZone,
			Node fromNode, double freeSpeedAccessibility,
			double carAccessibility, double bikeAccessibility,
			double walkAccessibility, double ptAccessibility) {
		// this is just a stub and does nothing. 
		// this needs to be implemented/overwritten by an inherited class
	}
	
	// ////////////////////////////////////////////////////////////////////
	// inner classes
	// ////////////////////////////////////////////////////////////////////
	
	
	/**
	 * stores travel disutilities for different modes
	 * @author thomas
	 *
	 */
	public static class GeneralizedCostSum {
		
		private double sumFREESPEED = 0.;
		private double sumCAR  	= 0.;
		private double sumBIKE 	= 0.;
		private double sumWALK 	= 0.;
		private double sumPt   	= 0.;
		
		public void reset() {
			this.sumFREESPEED 	= 0.;
			this.sumCAR		  	= 0.;
			this.sumBIKE	  	= 0.;
			this.sumWALK	  	= 0.;
			this.sumPt		  	= 0.;
		}
		
		public void addFreeSpeedCost(double cost){
			this.sumFREESPEED += cost;
		}
		
		public void addCongestedCarCost(double cost){
			this.sumCAR += cost;
		}
		
		public void addBikeCost(double cost){
			this.sumBIKE += cost;
		}
		
		public void addWalkCost(double cost){
			this.sumWALK += cost;
		}
		
		public void addPtCost(double cost){
			this.sumPt += cost;
		}
		
		public double getFreeSpeedSum(){
			return this.sumFREESPEED;
		}
		
		public double getCarSum(){
			return this.sumCAR;
		}
		
		public double getBikeSum(){
			return this.sumBIKE;
		}
		
		public double getWalkSum(){
			return this.sumWALK;
		}
		
		public double getPtSum(){
			return this.sumPt;
		}
	}

}
