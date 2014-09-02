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

package playground.julia.newSpatialAveraging;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


public class SimpleLinkWeightUtil implements LinkWeightUtil {

	public SimpleLinkWeightUtil(double xMin, double xMax, double yMin,
			double yMax, int noOfXbins, int noOfYbins,
			double smoothingRadius_m, String munichShapeFile,
			CoordinateReferenceSystem targetCRS) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Double getWeightFromLink(Link link, Coord cellCentroid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getNormalizationFactor() {
		// TODO Auto-generated method stub
		return null;
	}

}