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

package net.radai.clusterbench.util;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class CloseableChannel extends ManagedChannel implements AutoCloseable {
    private final ManagedChannel delegate;
    private final long timeoutMillis;

    public CloseableChannel(ManagedChannel delegate) {
        this(delegate, Long.MAX_VALUE);
    }

    public CloseableChannel(ManagedChannel delegate, Duration timeout) {
        this(delegate, timeout.toMillis());
    }
    
    public CloseableChannel(ManagedChannel delegate, long timeout, TimeUnit unit) {
        this(delegate, unit.toMillis(timeout));
    }

    public CloseableChannel(ManagedChannel delegate, long timeoutMillis) {
        this.delegate = delegate;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void close() throws Exception {
        delegate.shutdown();
        delegate.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ManagedChannel shutdown() {
        return delegate.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public ManagedChannel shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public ConnectivityState getState(boolean requestConnection) {
        return delegate.getState(requestConnection);
    }

    @Override
    public void notifyWhenStateChanged(ConnectivityState source, Runnable callback) {
        delegate.notifyWhenStateChanged(source, callback);
    }

    @Override
    public void resetConnectBackoff() {
        delegate.resetConnectBackoff();
    }

    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
        return delegate.newCall(methodDescriptor, callOptions);
    }

    @Override
    public String authority() {
        return delegate.authority();
    }
}
