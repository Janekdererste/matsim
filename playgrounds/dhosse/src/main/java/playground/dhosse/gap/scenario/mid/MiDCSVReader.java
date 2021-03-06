package playground.dhosse.gap.scenario.mid;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.population.utils.EgapPopulationUtils;
import playground.dhosse.gap.scenario.population.utils.EgapPopulationUtilsV2;
import playground.dhosse.utils.EgapHashGenerator;

public class MiDCSVReader {

	private final int idxPersonId = 0;
	private final int idxMode = 2;
	private final int idxPurpose = 3;
	private final int idxDistance = 4;
	private final int idxDuration = 5;
	private final int idxStart = 6;
	private final int idxEnd = 7;
	private final int idxSex = 8;
	private final int idxAge = 9;
	private final int idxCarAvailability = 10;
	private final int idxLicense = 11;
	private final int idxEmployment = 13;
	
	public Map<String, MiDSurveyPerson> persons;
	
	public MiDCSVReader(){
		this.persons = new HashMap<>();
	}
	
	public void readV2(String file, MiDPersonGroupTemplates templates){
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		String pId = null; //current person id
		String previousPId = null; //person id read in previous line
		
		String acts = null;
		String legs = null;
		String times = null;
		String distance = null;
		
		String pHash = null;
		
		try{
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] parts = line.split("\t");
				
				pId = parts[idxPersonId];
				
				if(previousPId != null){
					
					if(!pId.equals(previousPId)){
						
						if(!times.contains("NULL")){
							
							MiDTravelChain tChain = new MiDTravelChain(previousPId, legs.split("_"), acts.split("_"), times.split("_"), distance.split("_"));
							templates.addTravelChainToPattern(pHash, acts, tChain);
							
						}
						
						acts = null;
						legs = null;
						times = null;
						pHash = null;
						distance = null;
						
					}
					
				}

				if(pHash == null){

					String age = parts[idxAge];
					int sex = parts[idxSex].equals("male") ? 0 : 1;
					String employed = parts[idxEmployment];
					String license = parts[idxLicense];
					String carAvail = parts[idxCarAvailability].equals("never") ? "false" : "true";
					if(license.equals("false")){
						carAvail = "false";
					}
					
					pHash = EgapHashGenerator.generatePersonHash(Integer.parseInt(age), sex, Boolean.parseBoolean(carAvail), Boolean.parseBoolean(license), Boolean.parseBoolean(employed));
					
				}
				
				String nextAct = parts[idxPurpose];
				String mode = parts[idxMode];
				String departure = parts[idxStart];
				String arrival = parts[idxEnd];
				String d = parts[idxDistance];
				
				if(acts == null){
					
					if(nextAct.equals(Global.ActType.home.name())){
						
						acts = Global.ActType.other.name();
						
					} else{
						
						acts = Global.ActType.home.name();
						
					}
					
				}
				acts += "_" + nextAct;
				
				if(legs == null){
					
					legs = mode;
					
				} else {
					
					legs += "_" + mode;
					
				}
				
				if(times == null){
					
					times = departure + "-" + arrival;
					
				} else{
					
					times += "_" + departure + "-" + arrival;
					
				}
				
				if(distance == null){
					
					distance = d;
					
				} else{
					
					distance += "_" + d;
					
				}
				
				previousPId = pId;
				
			}
			
			reader.close();
			
				
		} catch (IOException e) {
		
			e.printStackTrace();
		
		}
		
	}
	
	public Map<String, MiDSurveyPerson> getPersons(){
		return this.persons;
	}
	
	
	public static class MiDData{
		
		String personId;
		int sex;
		int age;
		boolean carAvail;
		boolean hasLicense;
		boolean isEmployed;
		
		private LinkedList<MiDWaypoint> waypoints;
		
		public MiDData(String id, String sex, String mode, String age, String carAvail, String license, String employed){
			
			this.personId = id;
			this.sex = sex.equals("male") ? 0 : 1;
			this.age = !age.equals("NULL") ? Integer.parseInt(age) : Integer.MIN_VALUE;
			
			if(carAvail.equals("always") || carAvail.equals("sometimes") || mode.equals(TransportMode.car)){
				
				this.carAvail = true;
				
			} else{
				
				this.carAvail = false;
				
			}
			
			this.hasLicense = Boolean.parseBoolean(license);
			this.isEmployed = Boolean.parseBoolean(employed);
			
			this.waypoints = new LinkedList<>();
			
		}
		
		public LinkedList<MiDWaypoint> getWayPoints(){
			
			return this.waypoints;
		}
		
		public String generateWaypointHash(){
			
			StringBuffer sb = new StringBuffer();
			for(MiDWaypoint p : this.waypoints){
				sb.append(p.purpose + "-");
			}
			
			return sb.toString();
			
		}
		
		void addWaypoint(MiDWaypoint p){
			
			this.waypoints.addLast(p);
			
		}
		
		public String generateHash(){
			
			String ageBounds = null;
			if(this.age < 10){
				ageBounds = "0-9";
			} else if(this.age >= 10 && this.age < 20){
				ageBounds = "10-19";
			} else if(this.age >= 20 && this.age < 30){
				ageBounds = "20-29";
			} else if(this.age >= 30 && this.age < 40){
				ageBounds = "30-39";
			} else if(this.age >= 40 && this.age < 50){
				ageBounds = "40-49";
			} else if(this.age >= 50 && this.age < 60){
				ageBounds = "50-59";
			} else if(this.age >= 60 && this.age < 70){
				ageBounds = "60-69";
			} else if(this.age >= 70 && this.age < 80){
				ageBounds = "70-79";
			} else{
				ageBounds = "80-";
			}
			
//			System.out.println("age: " + this.age + ", sex: " + this.sex + ", age: " + ageBounds + ", car available: " + this.carAvail + ", license: " + this.hasLicense + ", employed: " + this.isEmployed);
//			for(MiDWaypoint p : this.waypoints){
//				System.out.print(p.purpose + "\t");
//			}
//			System.out.println("\ndistance stats - mean: " + this.distanceStats.getMean() + ", std dev: " + this.distanceStats.getStdDev());
//			System.out.println("duration stats - mean: " + this.durationStats.getMean() + ", std dev: " + this.durationStats.getStdDev());
//			System.out.println("start time stats - mean: " + Time.writeTime(this.startTimeStats.getMean()) + ", std dev: " + Time.writeTime(this.startTimeStats.getStdDev()));
//			System.out.println("end time stats - mean: " + Time.writeTime(this.endTimeStats.getMean()) + ", std dev: " + Time.writeTime(this.endTimeStats.getStdDev()));
//			System.out.println("######################################################################");
			
			return (this.sex + "_" + ageBounds + "_" + this.carAvail + "_" + this.hasLicense + "_" + this.isEmployed);
			
		}
		
	}
	
	public static class MiDWaypoint{
		
		String mode;
		String purpose;
		double distance;
		double duration;
		double startTime;
		double endTime;
		
		MiDWaypoint(String mode, String purpose, String distance, String duration, String startTime, String endTime) {
			
			this.mode = mode;
			this.purpose = purpose;
			this.distance = !distance.equals("NULL") ? Double.parseDouble(distance.replace(",", ".")) * 1000 : Double.NEGATIVE_INFINITY;
			this.duration = !duration.equals("NULL") ? Double.parseDouble(duration) * 60 : Double.NEGATIVE_INFINITY;
			this.startTime = !startTime.equals("NULL") ? Time.parseTime(startTime) : Double.NEGATIVE_INFINITY;
			this.endTime = !endTime.equals("NULL") ? Time.parseTime(endTime) : Double.NEGATIVE_INFINITY;
			
			
		}

		public String getMode() {
			return mode;
		}

		public String getPurpose() {
			return purpose;
		}

		public double getDistance() {
			return distance;
		}

		public double getDuration() {
			return duration;
		}

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}
		
		
	}
	
}
