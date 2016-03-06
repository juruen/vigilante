package io.vigilante.site.api.impl.datastoreng;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReferenceDelta {
    private final List<TargetReferences> toLeave;
    private final List<TargetReferences> toAdd;
    private final List<TargetReferences> toRemove;
    private final List<TargetReferences> current;
 }
