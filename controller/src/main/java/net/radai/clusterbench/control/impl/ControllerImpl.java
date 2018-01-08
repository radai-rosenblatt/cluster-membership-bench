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
import net.radai.clusterbench.control.api.RegistrationRequest;
import net.radai.clusterbench.control.api.RegistrationResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class ControllerImpl extends ControllerGrpc.ControllerImplBase {
    
    private final Clock clock;
    private final AtomicReference<NodeTable> nodeTableRef = new AtomicReference<>();

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
        long utcMicros = now.getLong(ChronoField.INSTANT_SECONDS) * 1000000 + now.getLong(ChronoField.MICRO_OF_SECOND);
        RegistrationResponse response = RegistrationResponse.newBuilder()
                .setTimestamp(utcMicros)
                .setAssignedNodeId(newNode.getId())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
