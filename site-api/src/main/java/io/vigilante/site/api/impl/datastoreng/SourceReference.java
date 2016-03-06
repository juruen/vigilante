package io.vigilante.site.api.impl.datastoreng;

import com.spotify.asyncdatastoreclient.Entity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SourceReference {
    public enum Type {
        ADD,
        UPDATE
    }

    private final Type type;
    private final EntityDescriptor descriptor;
    private final String id;
    private final String name;
    private final Entity entity;
}
