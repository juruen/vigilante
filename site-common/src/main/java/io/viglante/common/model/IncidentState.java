package io.viglante.common.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum IncidentState {
    TRIGGERED,
    RESOLVED,
    ACKNOWLEDGED;

    private static Map<String, IncidentState> namesMap = new HashMap<String, IncidentState>(3);

    static {
        namesMap.put("triggered", TRIGGERED);
        namesMap.put("resolved", RESOLVED);
        namesMap.put("acknowledged", ACKNOWLEDGED);
    }

    @JsonCreator
    public static IncidentState forValue(String value) {
        return namesMap.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
        for (Map.Entry<String, IncidentState> entry : namesMap.entrySet()) {
            if (entry.getValue() == this)
                return entry.getKey();
        }

        return null; // or fail
    }
}