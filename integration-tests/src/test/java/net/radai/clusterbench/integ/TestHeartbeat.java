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

package net.radai.clusterbench.integ;

import net.radai.clusterbench.control.ControllerClient;
import net.radai.clusterbench.control.ControllerServer;
import net.radai.clusterbench.control.impl.ControllerImpl;
import org.junit.Test;

import java.time.Clock;

public class TestHeartbeat {
    
    @Test
    public void testWhatever() throws Exception {
        Clock clock = Clock.systemUTC();
        ControllerImpl controller = new ControllerImpl(clock, null);
        try (ControllerServer server = new ControllerServer(controller)) {
            ControllerClient client = new ControllerClient("127.0.0.1", 6666);
            Thread.sleep(500);
            client.close();
        }
    }
}
