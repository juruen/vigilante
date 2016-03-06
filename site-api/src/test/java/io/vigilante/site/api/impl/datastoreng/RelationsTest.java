package io.vigilante.site.api.impl.datastoreng;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RelationsTest {


    @Test
    public void testGetRelations() throws Exception {
        final Relations relations = Relations
            .builder()
            .addRelation(EntityDescriptor.TEAM, EntityDescriptor.USER)
            .addRelation(EntityDescriptor.TEAM, EntityDescriptor.ESCALATION)
            .build();

        assertEquals(
            ImmutableList.of(EntityDescriptor.USER, EntityDescriptor.ESCALATION ),
            relations.getRelationsFor(EntityDescriptor.TEAM));


        assertEquals(
            ImmutableList.of(EntityDescriptor.TEAM),
            relations.getReverseRelationsFor(EntityDescriptor.USER));

        assertEquals(
            ImmutableList.of(EntityDescriptor.TEAM),
            relations.getReverseRelationsFor(EntityDescriptor.ESCALATION));
    }

}