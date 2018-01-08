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

import io.grpc.netty.NettyChannelBuilder;
import net.radai.clusterbench.control.api.ControllerGrpc;
import net.radai.clusterbench.control.api.RegistrationRequest;
import net.radai.clusterbench.control.api.RegistrationResponse;
import net.radai.clusterbench.control.impl.ControllerImpl;
import net.radai.clusterbench.util.CloseableChannel;
import org.junit.Assert;
import org.junit.Test;

import java.time.Clock;

public class ControllerServerTest {
    
    @Test
    public void testRegistrationCall() throws Exception {
        Clock clock = Clock.systemUTC();
        ControllerImpl controller = new ControllerImpl(clock, null);
        try (ControllerServer server = new ControllerServer(controller);
             CloseableChannel channel = new CloseableChannel(NettyChannelBuilder
                     .forAddress("127.0.0.1", 6666)
                     .usePlaintext(true).build())
             ) {
            ControllerGrpc.ControllerBlockingStub client = ControllerGrpc.newBlockingStub(channel);
            RegistrationRequest req = RegistrationRequest
                    .newBuilder()
                    .setTimestamp(1L)
                    .build();
            RegistrationResponse response = client.register(req);
            Assert.assertNotNull(response);
            Assert.assertEquals(1L, response.getAssignedNodeId());
        }
    }
}
