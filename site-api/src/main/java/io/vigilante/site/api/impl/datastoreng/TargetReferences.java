package io.vigilante.site.api.impl.datastoreng;


import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TargetReferences {
    private final EntityDescriptor descriptor;
    private final List<String> ids;

    public TargetReferences(EntityDescriptor descriptor, String id){
        this(descriptor, ImmutableList.of(id));
    }
}
