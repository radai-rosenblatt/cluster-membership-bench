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

package net.radai.clusterbench.control;

import io.grpc.stub.StreamObserver;
import net.radai.clusterbench.control.api.ControllerGrpc;
import net.radai.clusterbench.control.api.ControllerHeartbeat;
import net.radai.clusterbench.control.api.NodeHeartbeat;
import net.radai.clusterbench.util.TimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

public class HeartbeatThread extends Thread implements StreamObserver<ControllerHeartbeat> {
    private final static Logger log = LogManager.getLogger(HeartbeatThread.class);
    
    private final Clock clock;
    private final ControllerGrpc.ControllerStub stub;
    private final int nodeId;
    
    private volatile boolean alive = true;
    private final CountDownLatch deathLatch = new CountDownLatch(1);
    private volatile Throwable error;
    
    private StreamObserver<NodeHeartbeat> toServer; 

    public HeartbeatThread(Clock clock, ControllerGrpc.ControllerStub stub, int nodeId) {
        this.clock = clock;
        this.stub = stub;
        this.nodeId = nodeId;
        setDaemon(true);
        setName("control client heartbeat thread");
    }
    
    public void die() {
        alive = false;
        deathLatch.countDown();
    }
    
    @Override
    public void run() {
        log.info("initializing rpc stub");
        toServer = stub.heartbeat(this); //TODO - move to ctr?
        Instant now = clock.instant();
        NodeHeartbeat nodeHeartbeat = buildHeartbeat(null, now);
        log.info("sending 1st heartbeat");
        toServer.onNext(nodeHeartbeat);
        while (alive) {
            try {
                deathLatch.await();
            } catch (InterruptedException ignored) {
                log.warn("interrupted", ignored);
            }
        }
        log.info("terminating");
        toServer.onCompleted();
    }
    
    private NodeHeartbeat buildHeartbeat(ControllerHeartbeat inResponseTo, Instant now) {
        long seq;
        long inResponseToSeq;
        long inResponseToTs;
        if (inResponseTo != null) {
            seq = inResponseTo.getInResponseToSeq() + 1;
            inResponseToSeq = inResponseTo.getSeq();
            inResponseToTs = inResponseTo.getTimestamp();
        } else {
            seq = 1;
            inResponseToSeq = 0;
            inResponseToTs = 0;
        }
        NodeHeartbeat nodeHeartbeat = NodeHeartbeat.newBuilder()
                .setTimestamp(TimeUtils.toMicros(now))
                .setNodeId(nodeId)
                .setSeq(seq)
                .setInResponseToSeq(inResponseToSeq)
                .setInResponseToTs(inResponseToTs)
                .build();
        return nodeHeartbeat;
    }
    
    @Override
    public void onNext(ControllerHeartbeat value) {
        Instant now = clock.instant();
        log.info("got heartbeat from server. responding");
        NodeHeartbeat heartbeat = buildHeartbeat(value, now);
        if (alive) {
            toServer.onNext(heartbeat);
            log.info("sent heartbeat to server");
        }
    }

    @Override
    public void onError(Throwable t) {
        error = t;
        alive = false;
        deathLatch.countDown();
        log.error("from rpc stub", t);
    }

    @Override
    public void onCompleted() {
        log.info("completed");
        if (alive) { //depends on which party initiates
            toServer.onCompleted();
            alive = false;
        }
        deathLatch.countDown();
    }
}
