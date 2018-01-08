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

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import net.radai.clusterbench.control.api.ControllerGrpc;

import java.io.IOException;

public class ControllerServer implements AutoCloseable {
    
    private final ControllerGrpc.ControllerImplBase controllerImpl;
    private Server server;

    public ControllerServer(ControllerGrpc.ControllerImplBase controllerImpl) {
        this(controllerImpl, true);
    }

    public ControllerServer(ControllerGrpc.ControllerImplBase controllerImpl, boolean start) {
        this.controllerImpl = controllerImpl;
        if (start) {
            start();
        }
    }

    public void start() {
        try {
            server = NettyServerBuilder
                    .forPort(6666)
                    .addService(controllerImpl)
                    .build()
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("unable to start", e);
        }
    }

    @Override
    public void close() throws Exception {
        server.shutdown();
        server.awaitTermination();
    }
}
