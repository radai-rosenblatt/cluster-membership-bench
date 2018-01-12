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

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import net.radai.clusterbench.control.api.ControllerGrpc;
import net.radai.clusterbench.control.api.RegistrationRequest;
import net.radai.clusterbench.control.api.RegistrationResponse;
import net.radai.clusterbench.util.TimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ControllerClient implements AutoCloseable {
    private final static Logger log = LogManager.getLogger(ControllerClient.class);
    private final Clock clock = Clock.systemUTC();
    private final ManagedChannel channel;
    private final ControllerGrpc.ControllerStub stub;
    private final int id;
    private final HeartbeatThread heartbeatThread;

    public ControllerClient(String serverHost, int serverPort) {
        channel = NettyChannelBuilder.forAddress(serverHost, serverPort).usePlaintext(true).build();
        stub = ControllerGrpc.newStub(channel);
        log.info("rpc stub created. registering");
        RegistrationResponse regResp = register();
        id = regResp.getAssignedNodeId();
        log.info("registration complete. assigned id {}", id);
        heartbeatThread = new HeartbeatThread(clock, stub, id);
        heartbeatThread.start();
    }
    
    private RegistrationResponse register() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RegistrationResponse> ref = new AtomicReference<>(null);
        long now = TimeUtils.toMicros(clock.instant());
        RegistrationRequest regReq = RegistrationRequest.newBuilder()
                .setTimestamp(now)
                .build();
        stub.register(regReq, new StreamObserver<>() {
            @Override
            public void onNext(RegistrationResponse value) {
                ref.set(value);
                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                log.error("unable to register", t);
            }

            @Override
            public void onCompleted() {
                //nop
            }
        });
        try {
            latch.await(30L, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {

        }
        RegistrationResponse response = ref.get();
        if (response == null) {
            throw new IllegalStateException("unable to register");
        }
        return response;
    }
    
    @Override
    public void close() throws Exception {
        heartbeatThread.die();
        channel.shutdown();
        channel.awaitTermination(30L, TimeUnit.SECONDS);
    }
}
