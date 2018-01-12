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

import io.grpc.stub.StreamObserver;
import net.radai.clusterbench.control.api.ControllerGrpc;
import net.radai.clusterbench.control.api.ControllerHeartbeat;
import net.radai.clusterbench.control.api.NodeHeartbeat;
import net.radai.clusterbench.control.api.RegistrationRequest;
import net.radai.clusterbench.control.api.RegistrationResponse;
import net.radai.clusterbench.util.TimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ControllerImpl extends ControllerGrpc.ControllerImplBase {
    private final static Logger log = LogManager.getLogger(ControllerImpl.class);
    
    private final Clock clock;
    private final AtomicReference<NodeTable> nodeTableRef = new AtomicReference<>();
    private final ConcurrentHashMap<Integer, NodeStats> nodeStats = new ConcurrentHashMap<>();

    public ControllerImpl(Clock clock, NodeTable initialValue) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must be provided");
        }
        this.clock = clock;
        if (initialValue == null) {
            nodeTableRef.set(new NodeTable(Collections.emptyMap(), clock.instant()));
        } else {
            nodeTableRef.set(initialValue);
        }
    }

    @Override
    public void register(RegistrationRequest request, StreamObserver<RegistrationResponse> responseObserver) {
        log.info("register called");
        NodeDescriptor newNode;
        Instant received = clock.instant();
        Instant now = received;
        while (true) {
            NodeTable currentTable = nodeTableRef.get();
            newNode = new NodeDescriptor(currentTable.nextKey());
            NodeTable newTable = currentTable.add(newNode, now);
            if (nodeTableRef.compareAndSet(currentTable, newTable)) {
                break;
            }
            now = clock.instant();
        }
        long utcMicros = TimeUtils.toMicros(now);
        int assignedId = newNode.getId();
        RegistrationResponse response = RegistrationResponse.newBuilder()
                .setTimestamp(utcMicros)
                .setAssignedNodeId(assignedId)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        nodeStats.put(assignedId, new NodeStats(assignedId, TimeUtils.toMicros(received)));
    }

    @Override
    public StreamObserver<NodeHeartbeat> heartbeat(StreamObserver<ControllerHeartbeat> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(NodeHeartbeat value) {
                Instant received = clock.instant();
                long receivedUs = TimeUtils.toMicros(received);
                
                long timestamp = value.getTimestamp();
                int nodeId = value.getNodeId();
                long seq = value.getSeq();
                long inResponseToSeq = value.getInResponseToSeq();
                long inResponseToTs = value.getInResponseToTs();

                log.info("ping from {}", nodeId);
                
                NodeStats stats = ControllerImpl.this.nodeStats.get(nodeId);
                if (stats == null) {
                    responseObserver.onError(new IllegalArgumentException("unknown node " + nodeId));
                    return;
                }
                
                long nextSequence;
                synchronized (stats) {
                    long lastReceivedSeq = stats.getLastReceivedSeq();
                    long lastSentSeq = stats.getLastSentSeq();
                    if (lastReceivedSeq != seq - 1 || inResponseToSeq != lastSentSeq) {
                        //TODO - just clear sequences and business as usual ?
                        responseObserver.onError(new IllegalStateException("sequence mismatch"));
                        return;
                    }
                    nextSequence = lastSentSeq + 1;
                    long distance = 0;
                    long clockDiff = 0;
                    if (inResponseToTs > 0) {
                        distance = (receivedUs - inResponseToTs) / 2; //half the round trip
                        clockDiff = (timestamp + distance) - receivedUs;
                    }
                    
                    stats.setLastHeardFrom(receivedUs);
                    stats.setLastReceivedSeq(seq);
                    stats.setLastSentSeq(nextSequence);
                    stats.setNetworkDistance(distance);
                    stats.setClockDiff(clockDiff);
                }
                
                ControllerHeartbeat hb = ControllerHeartbeat.newBuilder()
                        .setTimestamp(TimeUtils.toMicros(received))
                        .setSeq(nextSequence)
                        .setInResponseToSeq(seq)
                        .setInResponseToTs(timestamp)
                        .build();
                
                responseObserver.onNext(hb);
            }

            @Override
            public void onError(Throwable t) {
                log.warn("call errored out", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
