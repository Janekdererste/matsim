package playground.boescpa.ivtBaseline;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;

import java.io.File;

/**
 * Basic main for the ivt baseline scenarios.
 *
 * Based on playground/ivt/teaching/RunZurichScenario.java by thibautd
 *
 * @author boescpa
 */
public class RunIVTBaseline {

    public static void main(String[] args) {
        final String configFile = args[0];

        // This allows to get a log file containing the log messages happening
        // before controler init.
        OutputDirectoryLogging.catchLogEntries();

        // This is the location choice MultiNodeDijkstra.
        // Suppress all log messages of level below error --- to avoid spaming the config
        // file with zillions of "not route found" messages.
        Logger.getLogger( org.matsim.core.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR ); // this is location choice
        Logger.getLogger( org.matsim.pt.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR );

        // It is suggested to use the config created by playground/boescpa/baseline/ConfigCreator.java.
        final Config config = ConfigUtils.loadConfig(configFile,
                new KtiLikeScoringConfigGroup(), new DestinationChoiceConfigGroup());

        // This is currently needed for location choice: initializing
        // the location choice writes K-values files to the output directory, which:
        // - fails if the directory does not exist
        // - makes the controler crash latter if the unsafe setOverwriteFiles( true )
        // is not called.
        // This ensures that we get safety with location choice working as expected,
        // before we sort this out and definitely kick out setOverwriteFiles.
        createEmptyDirectoryOrFailIfExists(config.controler().getOutputDirectory());

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        final Controler controler = new Controler(scenario);

        controler.getConfig().controler().setOverwriteFileSetting(
                OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        connectFacilitiesWithNetwork(controler);

        initializeLocationChoice(controler);

        // We use a specific scoring function, that uses individual preferences for activity durations.
        controler.setScoringFunctionFactory(
                new MATSim2010ScoringFunctionFactory(controler.getScenario(),
                        new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)));

        controler.run();
    }

    private static void connectFacilitiesWithNetwork(Controler controler) {
        ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
        NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();
        WorldConnectLocations wcl = new WorldConnectLocations(controler.getConfig());
        wcl.connectFacilitiesWithLinks(facilities, network);
    }

    private static void initializeLocationChoice(Controler controler) {
        Scenario scenario = controler.getScenario();
        DestinationChoiceBestResponseContext lcContext =
                new DestinationChoiceBestResponseContext(scenario);
        lcContext.init();
        controler.addControlerListener(new DestinationChoiceInitializer(lcContext));
    }

    private static void createEmptyDirectoryOrFailIfExists(String directory) {
        File file = new File( directory +"/" );
        if ( file.exists() && file.list().length > 0 ) {
            throw new UncheckedIOException( "Directory "+directory+" exists and is not empty!" );
        }
        file.mkdirs();
    }

}
