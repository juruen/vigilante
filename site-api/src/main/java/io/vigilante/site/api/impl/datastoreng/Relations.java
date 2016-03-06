package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Relations {
    private final ImmutableMap<EntityDescriptor, List<EntityDescriptor>> relations;
    private final ImmutableMap<EntityDescriptor, List<EntityDescriptor>> reverseRelations;


    private Relations(Map<EntityDescriptor, List<EntityDescriptor>> relations) {
        this.relations = ImmutableMap.copyOf(relations);
        this.reverseRelations = ImmutableMap.copyOf(buildReverseRelations(relations));
    }

    public List<EntityDescriptor> getRelationsFor(EntityDescriptor kind) {
        return relations.getOrDefault(kind, ImmutableList.of());
    }

    public List<EntityDescriptor> getReverseRelationsFor(EntityDescriptor kind) {
        return reverseRelations.getOrDefault(kind, ImmutableList.of());
    }

    public static RelationsBuilder builder() {
        return new RelationsBuilder();
    }

    public static class RelationsBuilder {

        private final Map<EntityDescriptor, List<EntityDescriptor>> relations;
        RelationsBuilder() {
            this.relations = new HashMap<>();
        }

        public RelationsBuilder addRelation(EntityDescriptor from, EntityDescriptor to) {
            final List<EntityDescriptor> toList = relations.getOrDefault(from, new ArrayList<>());
            toList.add(to);
            relations.put(from, toList);

            return this;
        }

        public Relations build() {
            return new Relations(relations);
        }

    }

    private Map<EntityDescriptor, List<EntityDescriptor>> buildReverseRelations(
        Map<EntityDescriptor, List<EntityDescriptor>> relations
    ) {
        final Map<EntityDescriptor, List<EntityDescriptor>> reverse = new HashMap<>();

        relations
            .entrySet()
            .stream()
            .forEach(relation -> {
                relation
                    .getValue()
                    .stream()
                    .forEach(target -> {
                        reverse.putIfAbsent(target, new ArrayList<>());
                        reverse.get(target).add(relation.getKey());
                    });
            });

        return reverse;
    }
}
