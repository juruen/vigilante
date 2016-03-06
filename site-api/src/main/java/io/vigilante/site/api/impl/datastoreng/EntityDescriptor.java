package io.vigilante.site.api.impl.datastoreng;


import io.vigilante.site.impl.datastore.basic.Constants;

public enum EntityDescriptor {
    USER(Constants.USER_KIND, "users", "user_names"),
    TEAM(Constants.TEAM_KIND, "team", "team_names"),
    SCHEDULE(Constants.SCHEDULE_KIND, "schedule", "schedule_names"),
    ESCALATION(Constants.ESCLATION_KIND, "escalation", "escalation_names"),
    SERVICE(Constants.SERVICE_KIND, "service", "service_names");

    private final String kind;
    private final String ids;
    private final String names;

    EntityDescriptor(String kind, String ids, String names) {
        this.kind = kind;
        this.ids = ids;
        this.names = names;
    }

    String kind() {
        return kind;
    }

    String ids() {
        return ids;
    }

    String names() {
        return names;
    }

    public static EntityDescriptor fromKind (String kind) {
        if (kind != null) {
            for (EntityDescriptor d : EntityDescriptor.values()) {
                if (kind.equals(d.kind())) {
                    return d;
                }
            }
        }
        return null;
    }
}
