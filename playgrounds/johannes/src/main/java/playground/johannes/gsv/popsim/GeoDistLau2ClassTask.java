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

package playground.johannes.gsv.popsim;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.gsv.popsim.analysis.AbstractAnalyzerTask;
import playground.johannes.gsv.popsim.analysis.FileIOContext;
import playground.johannes.gsv.popsim.analysis.StatsContainer;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public class GeoDistLau2ClassTask extends AbstractAnalyzerTask<Collection<? extends Person>> {

    public GeoDistLau2ClassTask(FileIOContext ioContext) {
        setIoContext(ioContext);
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        TDoubleArrayList xVals = new TDoubleArrayList();
        TDoubleArrayList yVals = new TDoubleArrayList();

        for (Person person : persons) {
            String xStr = person.getAttribute(MiDKeys.PERSON_LAU2_CLASS);
            for (Episode plan : person.getEpisodes()) {
                for (Attributable leg : plan.getLegs()) {

                    String yStr = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);

                    if (xStr != null && yStr != null) {
                        xVals.add(Double.parseDouble(xStr));
                        yVals.add(Double.parseDouble(yStr));
                    }
                }
            }
        }

        if (ioContext != null) {
            try {
                String filename = String.format("%s/munic.dist.mean.txt", ioContext.getPath());
                double[] x = xVals.toArray();
                double[] y = yVals.toArray();
//                Discretizer disc = FixedSampleSizeDiscretizer.create(x, 50, 100);
                TDoubleDoubleHashMap corr = Correlations.mean(x, y);
                StatsWriter.writeHistogram(corr, "munic", "distance", filename);

//                filename = String.format("%s/munic.dist.scatter.txt", getOutputDirectory());
//                StatsWriter.writeScatterPlot(xVals, yVals, "munic", "distance", filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
