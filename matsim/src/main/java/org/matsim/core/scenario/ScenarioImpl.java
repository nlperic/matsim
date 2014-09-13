/* *********************************************************************** *
 * project: org.matsim.*
 * StatelessScenarioImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.scenario;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;


/**
 * @author dgrether
 * @author mrieser
 */
public class ScenarioImpl implements Scenario {
	// setting this to final lead to 97 compile errors (many of them IMO multiple error messages of the same problem). kai, feb'14

	private static final Logger log = Logger.getLogger(ScenarioImpl.class);
	
	private boolean locked = false ;

	private final Map<String, Object> elements = new HashMap<String, Object>();

	//mandatory attributes
	private final Config config;
	private Network network;
	private Population population;
	private ActivityFacilities facilities;

	//non-mandatory attributes
	private TransitSchedule transitSchedule = null;

	private Households households;
	private Vehicles vehicles;

	protected ScenarioImpl(Config config) {
		this.config = config;
        this.network = NetworkImpl.createNetwork();
        this.population = PopulationUtils.createPopulation(this.config, this.network);
        this.facilities = new ActivityFacilitiesImpl();
        if (this.config.scenario().isUseHouseholds()){
            this.createHouseholdsContainer();
        }
        if (this.config.scenario().isUseVehicles()){
            this.createVehicleContainer();
        }
        if (this.config.scenario().isUseTransit()) {
            this.createTransitSchedule();
        }
    }

    /**
	 * Creates a vehicle container and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useVehicles
	 * config switch to true (see MATSIM-202 for an example).
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	 public boolean createVehicleContainer(){
		if ( this.vehicles != null ) return false;

		if ( !this.config.scenario().isUseVehicles() ) {
			log.info( "creating vehicles container while switch in config set to false. File will not be loaded automatically." );
		}
		this.vehicles = VehicleUtils.createVehiclesContainer();
		return true;
	}

	/**
	 * Creates a household container and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useHouseholds
	 * config switch to true.
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	public boolean createHouseholdsContainer(){
		if ( this.households != null ) return false;

		if ( !this.config.scenario().isUseHouseholds() ) {
			log.info( "creating households container while switch in config set to false. File will not be loaded automatically." );
		}

		this.households = new HouseholdsImpl();
		return true;
	}

    /**
	 * Creates a transit schedule and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useTransit
	 * config switch to true (see MATSIM-220 for an example).
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	 public boolean createTransitSchedule() {
		 if ( this.transitSchedule != null ) return false;

		if ( !this.config.scenario().isUseTransit() ) {
			log.info( "creating transit schedule while switch in config set to false. File will not be loaded automatically." );
		}

		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		return true;
	}
	
	@Override
	public ActivityFacilities getActivityFacilities() {
		return this.facilities;
	}

	@Override
	public Network getNetwork() {
		return this.network;
	}

	@Override
	public Population getPopulation() {
		return this.population;
	}

	@Override
	public Coord createCoord(final double d, final double e) {
		return new CoordImpl( d, e ) ;
	}

	@Override
	public Config getConfig() {
		return this.config;
	}

	public void setNetwork(Network network) {
		testForLocked();
		this.network = network;
	}

	public void setPopulation(Population population) {
		testForLocked();
		this.population = population;
	}

	@Override
	public Households getHouseholds() {
		if ( this.households == null ) {
			if ( this.config.scenario().isUseHouseholds() ) {
				this.createHouseholdsContainer();
			}
			else {
				// throwing an exception should be the right approach,
				// but it requires some testing (there may be places in the code
				// which are happy with getting a null pointer, and would then
				// not work anymore)
				// throw new IllegalStateException(
				log.warn(
						"no households, and households not activated from config. You must first call the create method of ScenarioImpl." );
			}
		}

		return this.households;
	}

	@Override
	public Vehicles getVehicles() {
		if ( this.vehicles == null ) {
			if ( this.config.scenario().isUseVehicles() ) {
				this.createVehicleContainer();
			}
			else {
				// throwing an exception should be the right approach,
				// but it requires some testing (there may be places in the code
				// which are happy with getting a null pointer, and would then
				// not work anymore)
				// throw new IllegalStateException(
				log.warn(
						"no vehicles container, and vehicles not activated from config. You must first call the create method of ScenarioImpl." );
			}
		}

		return this.vehicles;
	}

    @Override
	public TransitSchedule getTransitSchedule() {
		if ( this.transitSchedule == null ) {
			if ( this.config.scenario().isUseTransit() ) {
				this.createTransitSchedule();
			}
			else {
				// throwing an exception should be the right approach,
				// but it requires some testing (there may be places in the code
				// which are happy with getting a null pointer, and would then
				// not work anymore)
				// throw new IllegalStateException(
				log.warn(
						"no transit schedule, and transit not activated from config. You must first call the create method of ScenarioImpl." );
			}
		}

		return this.transitSchedule;
	}

	@Override
	public void addScenarioElement(
			final String name,
			final Object o) {
		// Once the "removal" operation is locked, you cannot add under the same name. kai, sep'14
		if ( o == null ) throw new NullPointerException( name );
		final Object former = elements.put( name , o );
		if ( former != null ) {
			throw new IllegalStateException( former+" is already associated with name "+name+" when adding "+o );
		}
	}

	@Override
	public Object removeScenarioElement(final String name) {
		testForLocked();
		return elements.remove( name );
	}

	@Override
	public Object getScenarioElement(final String name) {
		return elements.get( name );
	}
	
	public void setLocked() {
		this.locked = true ;
	}
	
	private void testForLocked() {
		if ( locked ) {
			throw new RuntimeException( "ScenarioImpl is locked; too late to do this.  See comments in code.") ;
			/* The decision is roughly as follows:
			 * - It is ok to set network, population, etc. in the Scenario during initial demand generation.
			 * - It is NOT ok to do this once the controler is running.
			 * - But we do not want to make a defensive copy of the whole thing at controler startup.
			 * - We also want to be able to plug alternative Scenario implementations into the controler.
			 * - But then the controler only gets the "Scenario" not the "ScenarioImpl", so it does not have to worry about setNetwork and the lik
			 * since it does not exist in the published interface.
			 * kai, sep'14
			 */
		}
	}

}
