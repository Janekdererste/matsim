/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOpertaions;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;
import playground.johannes.gsv.zones.io.ODMatrixXMLReader;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.sna.math.DescriptivePiStatistics;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.WGS84DistanceCalculator;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;
import playground.johannes.socialnetworks.statistics.WeightedSampleMean;

import com.vividsolutions.jts.algorithm.MinimumDiameter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 * 
 */
public class MatrixCompare2 {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String runId = "522";
		
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/matrices/raw/ivv/ivv.xml");
		KeyMatrix m1 = reader.getMatrix();
//		ODMatrixXMLReader reader = new ODMatrixXMLReader();
//		reader.setValidating(false);
//		reader.parse("/home/johannes/gsv/matrices/itp.xml");
//		KeyMatrix m1 = reader.getMatrix().toKeyMatrix("gsvId");
		MatrixOpertaions.applyFactor(m1, 1 / 365.0);

//		reader = new ODMatrixXMLReader();
//		reader.setValidating(false);
//		reader.parse("/home/johannes/gsv/matrices/miv." + runId + ".xml");
//		KeyMatrix m2 = reader.getMatrix().toKeyMatrix("gsvId");

		 KeyMatrixXMLReader reader2 =new KeyMatrixXMLReader();
		 reader2.setValidating(false);
		 reader2.parse("/home/johannes/gsv/matrices/miv.avr2.xml");
		 KeyMatrix m2 = reader2.getMatrix();

//		MatrixOpertaions.applyFactor(m2, 11.0);
//		MatrixOpertaions.applyDiagonalFactor(m2, 1.3);

		ZoneCollection zones = new ZoneCollection();
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/de.nuts3.json")));
		zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");

		System.out.println(String.format("Intraplan sum: %s", MatrixOpertaions.sum(m1)));
		System.out.println(String.format("%s sum: %s", runId, MatrixOpertaions.sum(m2)));

		KeyMatrix mErr = MatrixOpertaions.errorMatrix(m1, m2);
		writeErrorRank(mErr, m1, m2, zones);

		KeyMatrix itp_d = distanceMatrix(m1, zones);
		TDoubleDoubleHashMap hist = writeDistanceHist(m1, itp_d);
		TXTWriter.writeMap(hist, "d", "p", "/home/johannes/gsv/matrices/itp.dist.txt");

		KeyMatrix m_d = distanceMatrix(m2, zones);
		hist = writeDistanceHist(m2, m_d);
		TXTWriter.writeMap(hist, "d", "p", "/home/johannes/gsv/matrices/" + runId + ".dist.txt");
		
		System.out.println(String.format("PKM Intraplan (> 100 KM): %s", pkm(m1, itp_d)));
		System.out.println(String.format("PKM matsim (> 100 KM): %s", pkm(m2, m_d)));
		
		System.out.println(String.format("Avr length Intraplan (> 100 KM): %s", avr(m1, itp_d)));
		System.out.println(String.format("Avt length matsim (> 100 KM): %s", avr(m2, m_d)));
	}

	private static void writeErrorRank(KeyMatrix err, KeyMatrix m1, KeyMatrix m2, ZoneCollection zoneCollection) {
		List<Zone> zones = new ArrayList<>(urbanZones(zoneCollection.zoneSet()));
		SortedSet<Entry> rank = new TreeSet<>(new Comparator<Entry>() {

			@Override
			public int compare(Entry o1, Entry o2) {
				int result = Double.compare(o1.err, o2.err);
				if (result == 0) {
					return Integer.compare(o1.hashCode(), o2.hashCode());
				}
				return result;
			}
		});

		for (int i = 0; i < zones.size(); i++) {
			for (int j = i; j < zones.size(); j++) {
				String zi = zones.get(i).getAttribute("gsvId");
				String zj = zones.get(j).getAttribute("gsvId");

				Double errTo = err.get(zi, zj);
				if (errTo == null)
					errTo = new Double(0);

				Double errFrom = err.get(zj, zi);
				if (errFrom == null)
					errFrom = new Double(0);

				if (errTo > errFrom) {
					Entry e = new Entry();
					e.err = errTo;
					e.i = zi;
					e.j = zj;
					rank.add(e);
				} else {
					Entry e = new Entry();
					e.err = errFrom;
					e.i = zj;
					e.j = zi;
					rank.add(e);
				}
			}
		}

		for (Entry entry : rank) {
			String i = entry.i;
			String j = entry.j;

			Double errTo = err.get(i, j);
			Double errFrom = err.get(j, i);

			Double to1 = m1.get(i, j);
			Double to2 = m2.get(i, j);

			Double from1 = m1.get(j, i);
			Double from2 = m2.get(j, i);

			Zone zi = zoneCollection.get(i);
			Zone zj = zoneCollection.get(j);
			System.out.println(String.format("%s -> %s: %.3f (%.1f/%.1f) -- %.3f (%.1f/%.1f)", zi.getAttribute("nuts3_name"),
					zj.getAttribute("nuts3_name"), errTo, to1, to2, errFrom, from1, from2));
		}
	}

	private static Collection<Zone> urbanZones(Collection<Zone> zones) {
		final double threshold = 600000;

		Set<Zone> urbanZones = new HashSet<>();

		for (Zone zone : zones) {
			double pop = Double.parseDouble(zone.getAttribute("inhabitants"));
			double a = zone.getGeometry().getArea();

			double rho = pop / a;

			if (pop > threshold) {
				urbanZones.add(zone);
			}
		}

		return urbanZones;
	}

	private static class Entry {

		private Double err;

		private String i;

		private String j;
	}

	private static KeyMatrix distanceMatrix(KeyMatrix m, ZoneCollection zones) {
		GeometryFactory factory = new GeometryFactory();
		
		KeyMatrix m_d = new KeyMatrix();
		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				if(i.equals(j)) {
					Zone zone = zones.get(i);
					MinimumDiameter dia = new MinimumDiameter(zone.getGeometry());
					LineString ls = dia.getDiameter();
					Coordinate pi = ls.getCoordinateN(0);
					Coordinate pj = ls.getCoordinateN(1);
//					double d = dia.getLength();
					double d = WGS84DistanceCalculator.getInstance().distance(factory.createPoint(pi), factory.createPoint(pj));
					m_d.set(i, j, d);
					
				} else {
				Point pi = zones.get(i).getGeometry().getCentroid();
				Point pj = zones.get(j).getGeometry().getCentroid();

				// double d =
				// CartesianDistanceCalculator.getInstance().distance(pi, pj);
				double d = WGS84DistanceCalculator.getInstance().distance(pi, pj);
				m_d.set(i, j, d);
				}
			}
		}
		return m_d;
	}

	private static TDoubleDoubleHashMap writeDistanceHist(KeyMatrix m, KeyMatrix m_d) {
		Set<String> keys = m.keys();
		DescriptivePiStatistics stats = new DescriptivePiStatistics();

		for (String i : keys) {
			for (String j : keys) {
				Double val = m.get(i, j);
				// if(val == null) val = 1.0;
				if (val != null) {
					double d = m_d.get(i, j);
					stats.addValue(d, 1 / val);
				}
			}
		}

		return Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(stats.getValues(), 1, 100), true);
	}

	private static double pkm(KeyMatrix m, KeyMatrix m_d) {
		Set<String> keys = m.keys();

		double sum = 0;
		
		for (String i : keys) {
			for (String j : keys) {
				double d = m_d.get(i, j);
//				if (d > 100000) {
					Double val = m.get(i, j);
					// if(val == null) val = 1.0;
					if (val != null) {
						sum += val*d;
					}
//				}
			}
		}
		
		return sum;
	}
	
	public static double avr(KeyMatrix m, KeyMatrix m_d) {
		Set<String> keys = m.keys();
		DescriptivePiStatistics stats = new WSMStatsFactory().newInstance();
		
		for (String i : keys) {
			for (String j : keys) {
				double d = m_d.get(i, j);
				if(Double.isInfinite(d)) {
					System.err.println();
				} else if (Double.isNaN(d)) {
					System.err.println();
				}
				
				if (d > 100000 && d < 1000000) {
					Double val = m.get(i, j);
					// if(val == null) val = 1.0;
					if (val != null && val > 0) {
						stats.addValue(d, 1/val);
					}
				}
			}
		}
		
		return stats.getMean();

	}
}