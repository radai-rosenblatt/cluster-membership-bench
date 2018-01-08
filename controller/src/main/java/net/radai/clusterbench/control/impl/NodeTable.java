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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class NodeTable {
    private final Map<Integer, NodeDescriptor> nodes;
    private final Instant created;

    public NodeTable(Map<Integer, NodeDescriptor> nodes, Instant created) {
        this.nodes = nodes;
        this.created = created;
    }
    
    public NodeTable add (NodeDescriptor newNode, Instant now) {
        Integer id = newNode.getId();
        if (nodes.containsKey(id)) {
            throw new IllegalArgumentException("already contains node " + id);
        }
        Map<Integer, NodeDescriptor> newMap = new HashMap<>(nodes);
        if (newMap.put(id, newNode) != null) {
            throw new IllegalStateException("this is a bug");
        }
        return new NodeTable(newMap, now);
    }
    
    public int nextKey() {
        int max = 0;
        for (Integer k : nodes.keySet()) {
            max = Math.max(max, k);
        }
        return max + 1;
    }
}
