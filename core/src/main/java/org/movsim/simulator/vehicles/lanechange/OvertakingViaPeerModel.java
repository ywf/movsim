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

package org.movsim.simulator.vehicles.lanechange;

import java.util.Iterator;

import org.movsim.autogen.OvertakingViaPeer;
import org.movsim.simulator.roadnetwork.LaneSegment;
import org.movsim.simulator.roadnetwork.Lanes;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.simulator.vehicles.lanechange.LaneChangeModel.LaneChangeDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

public class OvertakingViaPeerModel {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(OvertakingViaPeerModel.class);

    private static final double ROAD_MAX_LOOK_AHEAD_DISTANCE = 1000;
    private static final double MIN_DISTANCE_BETWEEN_VEHICLES_IN_SAME_DIRECTION = 300;

    private double minTargetGap = 100;
    private double maxGapBehindLeader = 200;
    private double safetyTimeGapParameter = 2; // could be taken from IDM family but no access

    private double critFactorTTC = 4; // 6
    private double magicFactorReduceFreeAcc = 4;

    private final LaneChangeModel lcModel;
    private final OvertakingViaPeer parameter;

    OvertakingViaPeerModel(LaneChangeModel laneChangeModel, OvertakingViaPeer parameter) {
        this.lcModel = Preconditions.checkNotNull(laneChangeModel);
        this.parameter = Preconditions.checkNotNull(parameter);
    }

    LaneChangeDecision finishOvertaking(Vehicle me, LaneSegment newLaneSegment) {
        // evaluate situation on the right lane
        if (lcModel.isSafeLaneChange(me, newLaneSegment)) {
            return LaneChangeDecision.MANDATORY_TO_RIGHT;
        }
        return LaneChangeDecision.NONE;
    }

    LaneChangeDecision makeDecisionForOvertaking(Vehicle me, RoadSegment roadSegment) {
        assert roadSegment.hasPeer();
        LaneChangeDecision lcDecision = LaneChangeDecision.NONE;
        RoadSegment peerRoadSegment = roadSegment.getPeerRoadSegment();

        double remainingDistanceOnRoadSegment = roadSegment.roadLength() - me.getFrontPosition();
        if (roadIsSuitedForOvertaking(remainingDistanceOnRoadSegment, roadSegment)
                && overtakingLaneIsFreeInDrivingDirection(me, roadSegment)
                && noOvertakingManeuverFromPeer(me, roadSegment.getPeerRoadSegment())) {
            lcDecision = makeDecision(me, roadSegment);
        }
        return lcDecision;
    }

    /**
     * Checks if the road is sufficiently homogeneous for considering an overtaking maneuver. The check is limited to
     * {@code ROAD_MAX_LOOK_AHEAD_DISTANCE} and the next {@link RoadSegment}.
     * 
     * @param remainingDistanceOnRoadSegment
     * @param roadSegment
     * @return true if the road is sufficiently homogeneous for an overtaking maneuver
     */
    private static boolean roadIsSuitedForOvertaking(double remainingDistanceOnRoadSegment, RoadSegment roadSegment) {
        if (Iterables.size(roadSegment.trafficLights()) > 0
                || Iterables.size(roadSegment.getPeerRoadSegment().trafficLights()) > 0) {
            return false;
        }

        if (remainingDistanceOnRoadSegment < ROAD_MAX_LOOK_AHEAD_DISTANCE) {
            if (roadSegment.sizeSinkRoadSegments() > 1 || roadSegment.getPeerRoadSegment().sizeSourceRoadSegments() > 1) {
                return false;
            }
            RoadSegment sinkRoadSegment = roadSegment.sinkRoadSegment(Lanes.MOST_INNER_LANE);
            if (sinkRoadSegment == null) {
                return false;
            }
            if (remainingDistanceOnRoadSegment + sinkRoadSegment.roadLength() < ROAD_MAX_LOOK_AHEAD_DISTANCE) {
                return false;
            }
            if (roadSegment.laneCount() != sinkRoadSegment.laneCount()) {
                return false;
            }
            if (Iterables.size(sinkRoadSegment.trafficLights()) > 0) {
                return false;
            }

            RoadSegment sourceRoadSegmentPeer = roadSegment.getPeerRoadSegment().sourceRoadSegment(
                    Lanes.MOST_INNER_LANE);
            if (roadSegment.getPeerRoadSegment().laneCount() != sourceRoadSegmentPeer.laneCount()) {
                return false;
            }
            if (Iterables.size(sourceRoadSegmentPeer.trafficLights()) > 0) {
                return false;
            }
        }

        return true;
    }

    private static boolean overtakingLaneIsFreeInDrivingDirection(Vehicle me, RoadSegment roadSegment) {
        Iterator<Vehicle> overtakingVehicleIterator = roadSegment.overtakingVehicles();
        while (overtakingVehicleIterator.hasNext()) {
            Vehicle overtakingVehicle = overtakingVehicleIterator.next();
            if (Math.abs(overtakingVehicle.getFrontPosition() - me.getFrontPosition()) < MIN_DISTANCE_BETWEEN_VEHICLES_IN_SAME_DIRECTION) {
                return false;
            }
        }
        double remainingDistanceOnRoadSegment = roadSegment.roadLength() - me.getFrontPosition();
        if (remainingDistanceOnRoadSegment < MIN_DISTANCE_BETWEEN_VEHICLES_IN_SAME_DIRECTION) {
            RoadSegment sinkRoadSegment = roadSegment.sinkRoadSegment(Lanes.MOST_INNER_LANE);
            if (sinkRoadSegment == null) {
                return false;
            }
            overtakingVehicleIterator = sinkRoadSegment.overtakingVehicles();
            while (overtakingVehicleIterator.hasNext()) {
                Vehicle overtakingVehicle = overtakingVehicleIterator.next();
                if (Math.abs(overtakingVehicle.getFrontPosition() + roadSegment.roadLength() - me.getFrontPosition()) < MIN_DISTANCE_BETWEEN_VEHICLES_IN_SAME_DIRECTION) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean noOvertakingManeuverFromPeer(Vehicle me, RoadSegment peerRoadSegment) {
        double mePositionOnPeer = peerRoadSegment.roadLength() - me.getRearPosition();
        Iterator<Vehicle> overtakingVehicleIterator = peerRoadSegment.overtakingVehicles();
        while (overtakingVehicleIterator.hasNext()) {
            Vehicle overtakingVehicleFromPeer = overtakingVehicleIterator.next();
            // allow overtaking if vehicle in counterdirection has already passed subject vehicle
            if (overtakingVehicleFromPeer.getRearPosition() <= mePositionOnPeer) {
                return false;
            }
        }
        // !! check for maneuvers on next roadSegment is a strong assumption in the evaluation of possible maneuvers
        if (mePositionOnPeer < ROAD_MAX_LOOK_AHEAD_DISTANCE) {
            // check also next peer roadSegment for overtaking vehicles
            assert peerRoadSegment.sizeSourceRoadSegments() == 1;
            RoadSegment sourceRoadSegmentPeer = peerRoadSegment.sourceRoadSegment(Lanes.MOST_INNER_LANE);
            if (sourceRoadSegmentPeer.overtakingVehicles().hasNext()) {
                return false;
            }
        }
        return true;
    }

    private LaneChangeDecision makeDecision(Vehicle me, RoadSegment roadSegment) {
        assert me.lane() == Lanes.MOST_INNER_LANE;

        RoadSegment peerRoadSegment = roadSegment.getPeerRoadSegment();
        double mePositionOnPeer = peerRoadSegment.roadLength() - me.getFrontPosition();
        Vehicle vehicleOnPeer = peerRoadSegment.rearVehicle(Lanes.MOST_INNER_LANE, mePositionOnPeer);
        LOG.debug("check for rear vehicle on peer at position={}, see vehicle={}", mePositionOnPeer,
                (vehicleOnPeer != null ? vehicleOnPeer : "null"));

        double distanceToVehicleOnPeer = (vehicleOnPeer == null) ? 10000 : calcDistance(me, peerRoadSegment,
                vehicleOnPeer);
        LOG.debug("=== consider vehicle in other direction of travel: distance={}", distanceToVehicleOnPeer);
        if (LOG.isDebugEnabled() && vehicleOnPeer != null) {
            LOG.debug("net distance from me={}, netDistancefromOther={}", me.getNetDistance(vehicleOnPeer),
                    vehicleOnPeer.getNetDistance(me));
            LOG.debug("roadSegmentId={}, vehiclePos={}, vehicleOnPeerPos=" + vehicleOnPeer.getFrontPosition()
                    + ", vehPositionOnPeer=" + mePositionOnPeer, roadSegment.userId(), me.getFrontPosition());
        }

        LaneChangeDecision decision = LaneChangeDecision.NONE;
        if (distanceToVehicleOnPeer > 0) {
            Vehicle frontVehicleInLane = roadSegment.frontVehicleOnLane(me);
            if (frontVehicleInLane != null && !frontVehicleInLane.inProcessOfLaneChange()
                    && frontVehicleInLane.type() == Vehicle.Type.VEHICLE) {
                double brutDistanceToFrontVehicleInLane = me.getBrutDistance(frontVehicleInLane);
                LOG.debug("brutDistance={}, frontVehicle={}", brutDistanceToFrontVehicleInLane, frontVehicleInLane);

                Vehicle secondFrontVehicleInLane = roadSegment.laneSegment(frontVehicleInLane.lane()).roadSegment()
                        .frontVehicleOnLane(frontVehicleInLane);
                double spaceOnTargetLane = (secondFrontVehicleInLane != null) ? frontVehicleInLane
                        .getNetDistance(secondFrontVehicleInLane) : 100000 /* infinite gap */;
                LOG.debug("space on targetlane={}", spaceOnTargetLane);

                if (me.getLongitudinalModel().getDesiredSpeed() > frontVehicleInLane.getLongitudinalModel()
                        .getDesiredSpeed()
                        && me.getBrutDistance(frontVehicleInLane) < maxGapBehindLeader
                        && spaceOnTargetLane > minTargetGap) {

                    double spaceToFrontVeh = brutDistanceToFrontVehicleInLane
                            + me.getLongitudinalModel().getMinimumGap();
                    // free model acceleration: large distance, dv=0
                    double accConst = me.getLongitudinalModel().calcAccSimple(10000, me.getSpeed(), 0);
                    accConst /= magicFactorReduceFreeAcc;

                    // time needed when accelerating constantly
                    double timeManeuver = Math.sqrt(2 * spaceToFrontVeh / accConst);
                    double safetyMargin = critFactorTTC * me.getSpeed() * safetyTimeGapParameter;
                    double speedVehicleOnPeer = vehicleOnPeer == null ? 0 : vehicleOnPeer.getSpeed();
                    double neededDist = timeManeuver * (me.getSpeed() + speedVehicleOnPeer) + spaceToFrontVeh
                            + safetyMargin;
                    if (distanceToVehicleOnPeer > neededDist) {
                        decision = LaneChangeDecision.OVERTAKE_VIA_PEER;
                    }
                }
            }
        }
        return decision;
    }

    private static double calcDistance(Vehicle subjectVehicle, RoadSegment peerRoadSegment, Vehicle vehicleOnPeerRoad) {
        double vehiclePositionOnPeer = peerRoadSegment.roadLength() - subjectVehicle.getFrontPosition();
        return vehiclePositionOnPeer - vehicleOnPeerRoad.getFrontPosition();
    }
}
