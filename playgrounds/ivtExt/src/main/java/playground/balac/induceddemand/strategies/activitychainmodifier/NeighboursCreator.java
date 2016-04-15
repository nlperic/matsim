package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;


public class NeighboursCreator {
	private final StageActivityTypes stageActivityTypes;
	private QuadTree<ActivityFacility> shopFacilityQuadTree;
	private QuadTree<ActivityFacility> leisureFacilityQuadTree;
	private Scenario scenario;
	private LeastCostPathCalculator pathCalculator;
	private ScoringFunctionFactory scoringFunctionFactory;
	private static final Logger logger = Logger.getLogger(NeighboursCreator.class);

	public NeighboursCreator(StageActivityTypes stageActivityTypes
			,
			QuadTree<ActivityFacility> shopFacilityQuadTree, QuadTree<ActivityFacility> leisureFacilityQuadTree,
			Scenario scenario, LeastCostPathCalculator pathCalculator, ScoringFunctionFactory scoringFunctionFactory){
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
		this.scenario = scenario;
		this.stageActivityTypes = stageActivityTypes;
		this.pathCalculator = pathCalculator;
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	public void findBestNeighbour(Plan plan) {

		//location choice is necessary for the inserted activity
		List<Plan> newPlans = new LinkedList<Plan>();
		
		newPlans.addAll(getAllChainsWthRemoving(plan));
		newPlans.addAll(getAllChainsWthSwapping(plan));
		newPlans.addAll(getAllChainsWthInserting(plan));
		updateTimesForScoring(newPlans);
		scoreChains(newPlans);
		double score = plan.getScore();
		for (Plan newPlan : newPlans) {
			if (newPlan.getScore() > score) {
				PlanUtils.copyFrom(newPlan, plan);

				score = newPlan.getScore();
			}
			logger.info(newPlan.getScore());
		}
		
		updateTimes(plan);
	}
	
	private void updateTimes(Plan plan) {

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				
				if (((Activity) pe).getMaximumDuration() != Time.UNDEFINED_TIME) {
					((Activity) pe).setEndTime(Time.UNDEFINED_TIME);
				}
			}
		}
		
	}

	private void updateTimesForScoring(List<Plan> newPlans) {

		for (Plan plan : newPlans) {
			double time = 0.0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					((Activity)pe).setStartTime(time);
					if (((Activity) pe).getEndTime() == Time.UNDEFINED_TIME)
						((Activity) pe).setEndTime(time + ((Activity)pe).getMaximumDuration());
					
					if (((Activity) pe).getEndTime() != Time.UNDEFINED_TIME)
						time = ((Activity) pe).getEndTime();
					else
						time += ((Activity) pe).getMaximumDuration();
					
			//		if (time + ((Activity)pe).getMaximumDuration() < ((Activity)pe).getEndTime())
			//			time += ((Activity) pe).getMaximumDuration();
			//		else
			//			time = ((Activity) pe).getEndTime();
				}
				else
					time += ((Leg)pe).getTravelTime();
				
				
			}
			
		}
		
		
	}

	private List<Plan> getAllChainsWthSwapping(Plan plan) {
		List<Plan> newPlans = new LinkedList<Plan>();

		return newPlans;		
		
	}
	
	private List<Plan> getAllChainsWthInserting(Plan plan) {
		List<Plan> newPlans = new LinkedList<Plan>();
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		NetworkImpl network = (NetworkImpl)scenario.getNetwork();
		Person person = plan.getPerson();
		Set<String> insertableActivities = new TreeSet<String>();
		insertableActivities.add("leisure");
		insertableActivities.add("shop");
		insertableActivities.add("home");

		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		for (int index = 1; index < t.size() - 1; index++) {

			int actIndex = plan.getPlanElements().indexOf(t.get(index));

			for (String actType:insertableActivities) {
				Plan newPlan = PlanUtils.createCopy(plan);
				newPlan.setPerson(plan.getPerson());
				Activity newActivity;
				ActivityFacility actFacility;
				Link startLink;
				if (actType.equals("home")) {
					
					Activity primaryActivity = getPersonHomeLocation(t);					
					startLink = network.getLinks().get(primaryActivity.getLinkId());
					newActivity = pf.createActivityFromCoord(actType,
							primaryActivity.getCoord());
					newActivity.setMaximumDuration(3600.0);
					newActivity.setFacilityId(primaryActivity.getFacilityId());
					//newActivity.setEndTime(  t.get(index - 1).getEndTime() + 3600.0);
					
				}
				else {
					
					actFacility = findActivityLocation(actType, 
							((Activity)plan.getPlanElements().get(actIndex)).getCoord());
					
					
					startLink = network.getNearestLinkExactly(actFacility.getCoord());
					newActivity = pf.createActivityFromCoord(actType,
							actFacility.getCoord());
					newActivity.setFacilityId(actFacility.getId());
					newActivity.setMaximumDuration(3600.0);

				//	newActivity.setEndTime(  t.get(index - 1).getEndTime() + 3600.0);
				}
				
				//newActivity.setStartTime(t.get(index - 1).getEndTime());
				
				newPlan.getPlanElements().add(actIndex, newActivity);
				
				Leg legAfterInsertedActivity = ( (Leg) newPlan.getPlanElements().get(actIndex + 2) );
				
				Leg newLeg = pf.createLeg(legAfterInsertedActivity.getMode());
				Link endLink = network.getNearestLinkExactly(t.get(index).getCoord());
				double now = 0;
				
				//estimating the time of the departure for the trip to be inserted
				//(only duration of the activities is considered and not the length of the trips)
				if (t.get(index - 1).getEndTime() != Time.UNDEFINED_TIME)
				
					now = t.get(index - 1).getEndTime();
				else {
					
					for (int i = index - 2; i > -1; i--) {
						if (t.get(i).getEndTime() != Time.UNDEFINED_TIME) {
							
							now = t.get(i).getEndTime();
							for (int j = i + 1; j < index; j++) {
								now += t.get(j).getMaximumDuration();
							}
							break;
							
							
						}
						
					}
				}
				
				double startOfThePreviousLeg = now;
				now += 3600.0; //the duration of the inserted activity
				
				double estimatedTravelTime = estimateTravelTime(startLink, endLink, person, now);
				
				newLeg.setTravelTime(estimatedTravelTime);
				newPlan.getPlanElements().add(actIndex + 1, newLeg);
				
				Link startLinkPreviousLeg = network.getNearestLinkExactly(t.get(index - 1).getCoord());
				
				double previousTravelTime = estimateTravelTime(startLinkPreviousLeg, startLink, person, startOfThePreviousLeg);
				Leg previousLeg = (Leg) plan.getPlanElements().get(actIndex - 1);
				previousLeg.setTravelTime(previousTravelTime);
				newPlans.add(newPlan);
			}
			
		}
		
		
		return newPlans;		
		
	}
	
	private double estimateTravelTime(Link startLink, Link endLink, Person person, double now) {

		double travelTime = 0.0;
		Path path = this.pathCalculator.calcLeastCostPath(startLink.getToNode(), endLink.getFromNode(), 
				now, person, null ) ;
		travelTime = path.travelTime;
		return travelTime;
	}

	private List<Plan> getAllChainsWthRemoving(Plan plan) {
		
		List<Plan> newPlans = new LinkedList<Plan>();
		
		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		if (t.size() == 1 || t.size() == 2)
			return newPlans;
		
		for (int index = 1; index < t.size() - 2; index++) {
		
			//Plan newPlan = new PlanImpl(plan.getPerson());
			Plan newPlan = PlanUtils.createCopy(plan);
			newPlan.setPerson(plan.getPerson());

			
			int activitiesCount = countActivities(t.get(index).getType(), t);
			
			if ((t.get(index).getType().equals("work") || t.get(index).getType().equals("education")))// || activitiesCount == 1)
				continue;
			int actIndex = plan.getPlanElements().indexOf(t.get(index));

			
			boolean previous = false;
			boolean next = false;
			
			if (((Leg) newPlan.getPlanElements().get(actIndex - 1)).getMode().equals("car") ||
					((Leg) newPlan.getPlanElements().get(actIndex - 1)).getMode().equals("bike"))
				previous = true;
			
			if (((Leg) newPlan.getPlanElements().get(actIndex + 1)).getMode().equals("car") ||
					((Leg) newPlan.getPlanElements().get(actIndex + 1)).getMode().equals("bike"))
				next = true;
			
			if  (((Leg) newPlan.getPlanElements().get(actIndex - 1)).getMode()
					.equals(((Leg) newPlan.getPlanElements().get(actIndex - 1)).getMode())) {
				
				newPlan.getPlanElements().remove(actIndex);
				newPlan.getPlanElements().remove(actIndex);
				
				
				
	
			}
			else if (!previous && !next) {
				newPlan.getPlanElements().remove(actIndex);
				newPlan.getPlanElements().remove(actIndex);
				
			}
			else {
				
				String previousLegMode = ( (Leg) newPlan.getPlanElements().get(actIndex - 1) ).getMode();
				
				newPlan.getPlanElements().remove(actIndex);
				newPlan.getPlanElements().remove(actIndex);
				
				for(PlanElement pe : newPlan.getPlanElements()) {
					
					if (pe instanceof Leg)
						((Leg) pe).setMode(previousLegMode);
				}			
				
			}	
			
			
			newPlans.add(newPlan);
		}
		return newPlans;
		
	}
	
	private void scoreChains(List<Plan> plansToScore){
		
		for (Plan plan : plansToScore) {
			ScoringFunction scoringFunction = this.scoringFunctionFactory.createNewScoringFunction(plan.getPerson());

			for (PlanElement pe: plan.getPlanElements()) {
				
				if (pe instanceof Leg) {
					scoringFunction.handleLeg((Leg) pe);
				}
				else {
					if (((Activity) pe).getEndTime() == Time.UNDEFINED_TIME) {
						((Activity)pe).setEndTime(24*3600*60);
					}
				
					scoringFunction.handleActivity((Activity) pe);
				}
				
				
			}
			scoringFunction.finish();
			double score = scoringFunction.getScore();
			plan.setScore(score);
		}
	}
	
	private Activity getPersonHomeLocation(List<Activity> allActivities) {
		
		for(Activity a : allActivities) 
			
			if (a.getType().equals("home"))
				
				return a;
		
		throw new NullPointerException("The activity type home is not known to the agent!");
	}
	
	private int countActivities(String type, List<Activity> t) {
		
		int count = 0;
		for (Activity a : t) {
			
			if (a.getType().equals(type))
				count++;
		}
		return count;
	}
	private ActivityFacility findActivityLocation(String actType, Coord coord) {		
		
		if (actType.equals("leisure"))
			return (ActivityFacility)leisureFacilityQuadTree.getClosest(coord.getX(), coord.getY());		

		else if (actType.equals("shop"))
		
			return (ActivityFacility)shopFacilityQuadTree.getClosest(coord.getX(), coord.getY());		
		else 
			throw new NullPointerException("The activity type: " + actType + " ,is not known!");
		
	}

}
