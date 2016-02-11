package io.vigilante.site.http.model.adapters;


import io.vigilante.IncidentProtos;
import io.vigilante.site.http.model.Incidents;
import io.vigilante.site.http.model.Services;
import io.vigilante.site.http.model.Teams;

import java.util.stream.Collectors;

public class IncidentsAdapters {
    public static Incidents fromProto(IncidentProtos.Incidents incidents) {
        return Incidents.builder()
            .incident(incidents.getIncidentList()
                .stream()
                .map(IncidentsAdapters::fromProto)
                .collect(Collectors.toList()))
            .build();
    }

    public static Incidents.Incident fromProto(IncidentProtos.Incident incident) {
       return Incidents.Incident.builder()
           .id(incident.getId())
           .team(Teams.Team.builder()
               .id(incident.getTeam().getId())
               .name(incident.getTeam().getName())
               .build())
           .service(Services.Service.builder()
               .id(incident.getService().getId())
               .name(incident.getService().getName())
               .build())
           .key(incident.getKey())
           .start(incident.getStart())
           .description(incident.getDescription())
           .state(Incidents.Incident.State.valueOf(incident.getState().toString()))
           .build();
    }


    public static IncidentProtos.Incidents fromPojo(Incidents incidents) {
        return IncidentProtos.Incidents.newBuilder()
            .addAllIncident(incidents.getIncident()
                .stream()
                .map(IncidentsAdapters::fromPojo)
                .collect(Collectors.toList()))
            .build();
    }

    public static IncidentProtos.Incident fromPojo(Incidents.Incident incident) {
        IncidentProtos.Incident.Builder incidentBuilder =  IncidentProtos.Incident.newBuilder();

        if (incident.getId() != null) {
            incidentBuilder.setId(incident.getId());
        }

        if (incident.getTeam() != null) {
            incidentBuilder.setTeam(TeamsAdapters.fromPojo(incident.getTeam()));
        }

        if (incident.getKey() != null) {
            incidentBuilder.setKey(incident.getKey());
        }

        if (incident.getStart() != null) {
            incidentBuilder.setStart(incident.getStart());
        }

        if (incident.getDescription() != null) {
            incidentBuilder.setDescription(incident.getDescription());
        }

        if (incident.getTeam() != null) {
            incidentBuilder.setTeam(TeamsAdapters.fromPojo(incident.getTeam()));
        }

        if (incident.getService() != null) {
            incidentBuilder.setService(ServicesAdapters.fromPojo(incident.getService()));
        }

        return incidentBuilder.build();
    }
}
