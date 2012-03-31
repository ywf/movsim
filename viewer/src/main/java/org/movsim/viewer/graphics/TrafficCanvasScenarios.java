/*
 * Copyright (C) 2010, 2011, 2012 by Arne Kesting, Martin Treiber, Ralph Germ, Martin Budden
 * <movsim.org@gmail.com>
 * -----------------------------------------------------------------------------------------
 * 
 * This file is part of
 * 
 * MovSim - the multi-model open-source vehicular-traffic simulator.
 * 
 * MovSim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MovSim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MovSim. If not, see <http://www.gnu.org/licenses/>
 * or <http://www.movsim.org>.
 * 
 * -----------------------------------------------------------------------------------------
 */

package org.movsim.viewer.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;

import org.movsim.simulator.Simulator;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.vehicles.VehicleGenerator;

/**
 * Traffic Canvas subclass that setups up the actual road network and traffic simulation scenarios.
 * 
 */
public class TrafficCanvasScenarios extends TrafficCanvas {

    static final long serialVersionUID = 1L;

    public static enum Scenario {
        NONE, ONRAMPFILE, STARTSTOPFILE, CLOVERLEAFFILE, OFFRAMPFILE, LANECLOSINGFILE, TRAFFICLIGHTFILE,
        SPEEDLIMITFILE, RINGROADONELANEFILE, RINGROADTWOLANESFILE, FLOWCONSERVINGBOTTLENECK,
        RAMPMETERING, ROUTING,
        VASALOPPET
    }

    private Scenario scenario = Scenario.NONE;

    // speed boost to get vehicles onto network quickly
    private boolean isInitialSpeedUp;
    private double speedupEndTime;
    private int sleepTimeSave;

    public TrafficCanvasScenarios(Simulator simulator) {
        super(simulator);

        simulationRunnable.addUpdateStatusCallback(this);
        setStatusControlCallbacks(statusControlCallbacks);

        final TrafficCanvasMouseWheelListener mousewheel = new TrafficCanvasMouseWheelListener(this);
        addMouseWheelListener(mousewheel);
    }

    public void setMessageStrings(String popupString, String popupStringExitEndRoad, String trafficInflowString,
            String perturbationRampingFinishedString, String perturbationAppliedString) {
        setMessageStrings(popupString, popupStringExitEndRoad);
    }

    @Override
    public void start() {
        assert scenario != Scenario.NONE;
        super.start();
    }

    @Override
    public void updateStatus(double simulationTime) {
        if (isInitialSpeedUp && simulationTime > speedupEndTime) {
            isInitialSpeedUp = false;
            setSleepTime(sleepTimeSave);
        }
    }

    /**
     * After the vehicles have been drawn, update any simulation , such as density diagrams and traffic lights.
     */
    @Override
    protected void drawAfterVehiclesMoved(Graphics2D g, double simulationTime, long iterationCount) {

    }

    /**
     * Pause the animation.
     */
    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void reset() {
        super.reset();
        isInitialSpeedUp = false;
        vehicleToHighlightId = -1;
        initGraphicSettings();
        forceRepaintBackground();
    }

    /**
     * Returns the current traffic scenario.
     * @return the current traffic scenario
     */
    Scenario scenario() {
        return scenario;
    }

    /**
     * Sets up the given traffic scenario.
     * 
     * @param scenario
     */
    public void setupTrafficScenario(Scenario scenario) {

        reset();

        if (this.scenario == scenario) {
            return; // TODO proper restart
        }
        final String path;
        switch (scenario) {
        case ONRAMPFILE: // TODO rg path
            path = ".." + File.separator + "sim" + File.separator + "buildingBlocks" + File.separator;
            simulator.loadScenarioFromXml("onramp", path);
            break;
        case OFFRAMPFILE:
            path = ".." + File.separator + "sim" + File.separator + "buildingBlocks" + File.separator;
            simulator.loadScenarioFromXml("offramp", path);
            break;
        case STARTSTOPFILE:
            path = ".." + File.separator + "sim" + File.separator + "bookScenarioStartStop" + File.separator;
            simulator.loadScenarioFromXml("startStop_IDM", path);
            break;
        case CLOVERLEAFFILE:
            path = ".." + File.separator + "sim" + File.separator + "buildingBlocks" + File.separator;
            simulator.loadScenarioFromXml("cloverleaf", path);
            break;
        case LANECLOSINGFILE:
            path = ".." + File.separator + "sim" + File.separator + "buildingBlocks" + File.separator;
            simulator.loadScenarioFromXml("laneclosure", path);
            break;
        case TRAFFICLIGHTFILE:
            path = ".." + File.separator + "sim" + File.separator + "buildingBlocks" + File.separator;
            simulator.loadScenarioFromXml("trafficlight", path);
            break;
        case SPEEDLIMITFILE:
            path = ".." + File.separator + "sim" + File.separator + "buildingBlocks" + File.separator;
            simulator.loadScenarioFromXml("speedlimit", path);
            break;
        case RINGROADONELANEFILE:
            path = ".." + File.separator + "sim" + File.separator + "buildingBlocks" + File.separator;
            simulator.loadScenarioFromXml("ringroad_1lane", path);
            initGraphicSettings();
            break;
        case RINGROADTWOLANESFILE:
            path = ".." + File.separator + "sim" + File.separator + "buildingBlocks" + File.separator;
            simulator.loadScenarioFromXml("ringroad_2lanes", path);
            break;
        case FLOWCONSERVINGBOTTLENECK:
            path = ".." + File.separator + "sim" + File.separator + "buildingBlocks" + File.separator;
            simulator.loadScenarioFromXml("flow_conserving_bottleneck", path);
            break;
        case RAMPMETERING:
            path = ".." + File.separator + "sim" + File.separator + "games" + File.separator;
            simulator.loadScenarioFromXml("ramp_metering_v1", path);
            vehicleColorMode = TrafficCanvas.VehicleColorMode.EXIT_COLOR;
            break;
        case ROUTING:
            path = ".." + File.separator + "sim" + File.separator + "games" + File.separator;
            simulator.loadScenarioFromXml("routing_v2", path);
            vehicleColorMode = TrafficCanvas.VehicleColorMode.EXIT_COLOR;
            break;
        case VASALOPPET:
            path = ".." + File.separator + "sim" + File.separator + "examples" + File.separator;
            simulator.loadScenarioFromXml("vasa_CCS", path);
            break;
        default:
            // nothing to do
        }
        initGraphicSettings();
        forceRepaintBackground();
        this.scenario = scenario;
    }

    private void initGraphicSettings() {
        setProperties(loadProperties());
        initGraphicConfigFieldsFromProperties();
        resetScaleAndOffset();

        for (final RoadSegment segment : roadNetwork) {
            segment.roadMapping().setRoadColor(roadColor);
        }

        // TODO more than one vehicle generator is possible. list must be generated from VEHICLE input
        final VehicleGenerator vehicleGenerator = simulator.getVehicleGenerator();
        if (vehicleGenerator != null) {
            for (String vehicleTypeLabel : vehicleGenerator.prototypes().keySet()) {
                int r = (int) (Math.random() * 256);
                int g = (int) (Math.random() * 256);
                int b = (int) (Math.random() * 256);
                Color color = new Color(r, g, b);

                // final float hue = random.nextFloat();
                // final float saturation = 0.9f;// 1.0 for brilliant, 0.0 for dull
                // final float luminance = 1.0f; // 1.0 for brighter, 0.0 for black
                // Color color = Color.getHSBColor(hue, saturation, luminance);

                System.out.println("set color for vehicle label=" + vehicleTypeLabel); //$NON-NLS-1$
                labelColors.put(vehicleTypeLabel, color);
            }
        }
    }
}
