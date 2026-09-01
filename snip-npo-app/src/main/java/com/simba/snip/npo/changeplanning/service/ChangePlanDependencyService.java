package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.ChangePlanException;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationDependencyEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ChangePlanDependencyService {

    public record DependencyEdge(UUID operationId, UUID dependsOnOperationId) {
    }

    public void validateGraph(
            UUID planId,
            List<NetworkChangePlanOperationEntity> operations,
            List<DependencyEdge> edges
    ) {
        Set<UUID> operationIds = new HashSet<>();
        for (NetworkChangePlanOperationEntity operation : operations) {
            operationIds.add(operation.getId());
        }
        for (DependencyEdge edge : edges) {
            if (edge.operationId().equals(edge.dependsOnOperationId())) {
                throw new ChangePlanException(ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID, "self dependency");
            }
            if (!operationIds.contains(edge.operationId()) || !operationIds.contains(edge.dependsOnOperationId())) {
                throw new ChangePlanException(ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID, "external reference");
            }
        }
        Set<String> seen = new HashSet<>();
        for (DependencyEdge edge : edges) {
            String key = edge.operationId() + "->" + edge.dependsOnOperationId();
            if (!seen.add(key)) {
                throw new ChangePlanException(ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID, "duplicate edge");
            }
        }
        if (hasCycle(edges)) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID, "cycle detected");
        }
    }

    public List<NetworkChangePlanOperationEntity> deterministicOrder(
            List<NetworkChangePlanOperationEntity> operations,
            List<DependencyEdge> edges
    ) {
        Map<UUID, NetworkChangePlanOperationEntity> byId = new HashMap<>();
        for (NetworkChangePlanOperationEntity operation : operations) {
            byId.put(operation.getId(), operation);
        }
        Map<UUID, Integer> inDegree = new HashMap<>();
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        for (NetworkChangePlanOperationEntity operation : operations) {
            inDegree.putIfAbsent(operation.getId(), 0);
            adjacency.putIfAbsent(operation.getId(), new ArrayList<>());
        }
        for (DependencyEdge edge : edges) {
            adjacency.get(edge.dependsOnOperationId()).add(edge.operationId());
            inDegree.merge(edge.operationId(), 1, Integer::sum);
        }
        List<UUID> queue = new ArrayList<>();
        for (NetworkChangePlanOperationEntity operation : operations) {
            if (inDegree.get(operation.getId()) == 0) {
                queue.add(operation.getId());
            }
        }
        queue.sort((a, b) -> Integer.compare(byId.get(a).getSequenceNumber(), byId.get(b).getSequenceNumber()));
        List<NetworkChangePlanOperationEntity> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            UUID current = queue.remove(0);
            ordered.add(byId.get(current));
            for (UUID next : adjacency.getOrDefault(current, List.of())) {
                int updated = inDegree.merge(next, -1, Integer::sum);
                if (updated == 0) {
                    queue.add(next);
                }
            }
            queue.sort((a, b) -> Integer.compare(byId.get(a).getSequenceNumber(), byId.get(b).getSequenceNumber()));
        }
        if (ordered.size() != operations.size()) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID, "cycle detected");
        }
        return ordered;
    }

    public List<NetworkChangePlanOperationDependencyEntity> toEntities(UUID planId, List<DependencyEdge> edges) {
        List<NetworkChangePlanOperationDependencyEntity> entities = new ArrayList<>();
        for (DependencyEdge edge : edges) {
            entities.add(NetworkChangePlanOperationDependencyEntity.create(
                    UUID.randomUUID(),
                    planId,
                    edge.operationId(),
                    edge.dependsOnOperationId()
            ));
        }
        return entities;
    }

    private boolean hasCycle(List<DependencyEdge> edges) {
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        Set<UUID> nodes = new HashSet<>();
        for (DependencyEdge edge : edges) {
            nodes.add(edge.operationId());
            nodes.add(edge.dependsOnOperationId());
            adjacency.computeIfAbsent(edge.dependsOnOperationId(), ignored -> new ArrayList<>()).add(edge.operationId());
        }
        Set<UUID> visiting = new HashSet<>();
        Set<UUID> visited = new HashSet<>();
        for (UUID node : nodes) {
            if (dfsCycle(node, adjacency, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsCycle(
            UUID node,
            Map<UUID, List<UUID>> adjacency,
            Set<UUID> visiting,
            Set<UUID> visited
    ) {
        if (visiting.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }
        visiting.add(node);
        for (UUID next : adjacency.getOrDefault(node, List.of())) {
            if (dfsCycle(next, adjacency, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }
}
