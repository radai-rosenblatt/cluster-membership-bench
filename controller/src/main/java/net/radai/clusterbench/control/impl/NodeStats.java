/*
 *     Copyright (c) 2018 Radai Rosenblatt 
 *     This file is part of Cluster-Membership-Bench.
 *
 *     Cluster-Membership-Bench is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cluster-Membership-Bench is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cluster-Membership-Bench.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.radai.clusterbench.control.impl;

public class NodeStats {
    private final int nodeId;
    private long lastHeardFrom; //micros UTC
    private long lastReceivedSeq;
    private long lastSentSeq;
    private long networkDistance; //micros, one way trip
    private long clockDiff; //micros, relative to controller

    public NodeStats(int nodeId, long lastHeardFrom) {
        this.nodeId = nodeId;
        this.lastHeardFrom = lastHeardFrom;
        this.lastReceivedSeq = 0; 
        this.lastSentSeq = 0; 
        this.networkDistance = 0; 
        this.clockDiff = 0; 
    }

    public int getNodeId() {
        return nodeId;
    }

    public long getLastHeardFrom() {
        return lastHeardFrom;
    }

    public void setLastHeardFrom(long lastHeardFrom) {
        this.lastHeardFrom = lastHeardFrom;
    }

    public long getLastReceivedSeq() {
        return lastReceivedSeq;
    }

    public void setLastReceivedSeq(long lastReceivedSeq) {
        this.lastReceivedSeq = lastReceivedSeq;
    }

    public long getLastSentSeq() {
        return lastSentSeq;
    }

    public void setLastSentSeq(long lastSentSeq) {
        this.lastSentSeq = lastSentSeq;
    }

    public long getNetworkDistance() {
        return networkDistance;
    }

    public void setNetworkDistance(long networkDistance) {
        this.networkDistance = networkDistance;
    }

    public long getClockDiff() {
        return clockDiff;
    }

    public void setClockDiff(long clockDiff) {
        this.clockDiff = clockDiff;
    }
}
