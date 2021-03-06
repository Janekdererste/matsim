package opdytsintegration;

import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import floetteroed.utilities.math.Vector;

/**
 * Identifies the approximately best out of a set of decision variables.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class MATSimDecisionVariableSetEvaluator
		implements StartupListener, IterationEndsListener, ShutdownListener {

	// -------------------- MEMBERS --------------------

	private final TrajectorySampler trajectorySampler;

	private final MATSimStateFactory stateFactory;

	private int binSize_s = 3600;

	private int startBin = 0;

	private int binCnt = 24;

	private int memory = 1;

	private boolean averageMemory = false;

	// created during runtime:

	private VolumesAnalyzer volumesAnalyzer = null;

	private SortedSet<Id<Link>> sortedLinkIds = null;

	private LinkedList<Vector> stateList = null;

	private MATSimState finalState = null;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @see MATSimStateFactory
	 */
	public MATSimDecisionVariableSetEvaluator(
			final TrajectorySampler trajectorySampler,
			// final Set<? extends DecisionVariable> decisionVariables,
			// final ObjectBasedObjectiveFunction objectiveFunction,
			// final ConvergenceCriterion convergenceCriterion,
			final MATSimStateFactory stateFactory
	// final double equilibriumGapWeight, final double uniformityGapWeight,
	// final Random rnd
	) {
		// this.evaluator = new TrajectorySampler(
		// decisionVariables, objectiveFunction, convergenceCriterion,
		// rnd, equilibriumGapWeight, uniformityGapWeight);
		this.trajectorySampler = trajectorySampler;
		this.stateFactory = stateFactory;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public boolean foundSolution() {
		return this.trajectorySampler.foundSolution();
	}

	/**
	 * The vector representation of MATSim's instantaneous state omits some
	 * memory effects, meaning that it is not perfectly precise. Setting the
	 * memory parameter to more than one (its default value) will increase the
	 * precision at the cost of a larger computer memory usage. Too large values
	 * (perhaps larger than ten) may again impair the optimization performance.
	 */
	public void setMemory(final int memory) {
		this.memory = memory;
	}

	public int getMemory() {
		return this.memory;
	}

	/**
	 * <b>WARNING Use this option only if you know what you are doing.</b>
	 * <p>
	 * Setting this to true saves <em>computer</em> memory by averaging the
	 * <em>simulation process</em> memory instead of completely keeping track of
	 * it. This only works if the right (and problem-specific!) memory length is
	 * chosen.
	 * 
	 * @param averageMemory
	 */
	public void setAverageMemory(final boolean averageMemory) {
		this.averageMemory = averageMemory;
	}

	public boolean getAverageMemory() {
		return this.averageMemory;
	}

	/**
	 * Where to write logging information.
	 */
	public void setStandardLogFileName(final String logFileName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The time discretization (in seconds) according to which the simulated
	 * conditions in MATSim affect the evaluation of a decision variable.
	 * 
	 * @param binSize_s
	 */
	public void setBinSize_s(final int binSize_s) {
		this.binSize_s = binSize_s;
	}

	public int getBinSize_s() {
		return binSize_s;
	}

	/**
	 * The earliest time bin in which the simulated conditions in MATSim affect
	 * the evaluation of a decision variable. Setting this parameter tightly
	 * saves computer memory.
	 */
	public void setStartBin(final int startBin) {
		this.startBin = startBin;
	}

	public int getStartBin() {
		return this.startBin;
	}

	/**
	 * The number of time bins within which the simulated conditions in MATSim
	 * affect the evaluation of a decision variable. Setting this parameter
	 * tightly saves computer memory.
	 * 
	 * @param binCnt
	 */
	public void setBinCnt(final int binCnt) {
		this.binCnt = binCnt;
	}

	public int getBinCnt() {
		return binCnt;
	}

	// --------------- CONTROLLER LISTENER IMPLEMENTATIONS ---------------

	@Override
	public void notifyStartup(final StartupEvent event) {

		this.sortedLinkIds = new TreeSet<Id<Link>>(event.getControler()
				.getScenario().getNetwork().getLinks().keySet());
		this.stateList = new LinkedList<Vector>();

		/*
		 * MICHAEL: Ich erzeuge hier meinen eigenen VolumesAnalyzer, weil ich
		 * Kontrolle über die bin size und die end time brauche. Weiss nicht, ob
		 * sich das verlässlich während der MATSim-Initialisierung machen lässt
		 * -- und vielleicht will man hier ohnehin davon unabhängig sein.
		 */
		this.volumesAnalyzer = new VolumesAnalyzer(this.binSize_s,
				this.binSize_s * (this.startBin + this.binCnt), event
						.getControler().getScenario().getNetwork());
		event.getControler().getEvents().addHandler(this.volumesAnalyzer);

		this.trajectorySampler.initialize();
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		{
			/*
			 * (1) Extract the instantaneous state vector.
			 */
			final Vector newInstantaneousStateVector = new Vector(
					this.sortedLinkIds.size() * this.binCnt);
			int i = 0;
			for (Id<Link> linkId : this.sortedLinkIds) {
				final int[] volumes = this.volumesAnalyzer
						.getVolumesForLink(linkId);
				if (volumes == null) {
					for (int j = 0; j < this.binCnt; j++) {
						newInstantaneousStateVector.set(i++, 0.0);
					}
				} else {
					for (int j = this.startBin; j < (this.startBin + this.binCnt); j++) {
						newInstantaneousStateVector.set(i++, volumes[j]);
					}
				}
			}

			/*
			 * (2) Add instantaneous state vector to the list of past state
			 * vectors and ensure that the size of this list is equal to what
			 * the memory parameter prescribes.
			 */
			this.stateList.addFirst(newInstantaneousStateVector);
			while (this.stateList.size() < this.memory) {
				this.stateList.addFirst(newInstantaneousStateVector);
			}
			while (this.stateList.size() > this.memory) {
				this.stateList.removeLast();
			}
		}

		{
			final MATSimState newState = createState(event.getControler().getScenario().getPopulation());
			this.trajectorySampler.afterIteration(newState);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.finalState = createState(event.getControler().getScenario().getPopulation());
	}

	private MATSimState createState(Population population) {
    /*
     * (3) Create a new summary state vector, either by averaging or by
     * concatenating past instantaneous state vectors.
     */
		final Vector newSummaryStateVector;
		if (this.averageMemory) {
            // average state vectors
            newSummaryStateVector = this.stateList.getFirst().copy();
            for (int i = 1; i < this.memory; i++) {
                newSummaryStateVector.add(this.stateList.get(i));
            }
            newSummaryStateVector.mult(1.0 / this.memory);
        } else {
            // concatenate state vectors
            newSummaryStateVector = Vector.concat(this.stateList);
        }

			/*
			 * (4) Extract the current MATSim state and inform the evaluator
			 * that one iteration has been completed. The evaluator takes care
			 * of selecting a new trial decision variable and of implementing
			 * that decision variable in the simulation.
			 */
		return this.stateFactory.newState(population,
                newSummaryStateVector,
                this.trajectorySampler.getCurrentDecisionVariable());
	}

	public MATSimState getFinalState() {
		return finalState;
	}

}
