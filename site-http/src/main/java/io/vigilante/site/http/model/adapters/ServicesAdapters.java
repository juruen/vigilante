package io.vigilante.site.http.model.adapters;


import io.vigilante.ServiceProtos;
import io.vigilante.site.http.model.Schedules;
import io.vigilante.site.http.model.Services;
import io.vigilante.site.http.model.Teams;

import java.util.stream.Collectors;

public class ServicesAdapters {
    public static Services fromProto(ServiceProtos.Services services) {
        return Services.builder()
            .services(services.getServicesList()
                .stream()
                .map(ServicesAdapters::fromProto)
                .collect(Collectors.toList()))
            .build();
    }

    public static Services.Service fromProto(ServiceProtos.Service service) {
        return Services.Service.builder()
            .id(service.getId())
            .name(service.getName())
            .key(service.getKey())
            .team(Teams.Team.builder().id(service.getTeam().getId()).build())
            .schedule(Schedules.Schedule.builder().id(service.getSchedule().getId()).build())
            .build();
    }

    public static ServiceProtos.Services fromPojo(Services services) {
        return ServiceProtos.Services.newBuilder()
            .addAllServices(services.getServices()
                .stream()
                .map(ServicesAdapters::fromPojo)
                .collect(Collectors.toList()))
            .build();
    }

    public static ServiceProtos.Service fromPojo(Services.Service service) {
        ServiceProtos.Service.Builder serviceBuilder =  ServiceProtos.Service.newBuilder();

        if (service.getId() != null) {
            serviceBuilder.setId(service.getId());
        }

        if (service.getName() != null) {
            serviceBuilder.setName(service.getName());
        }

        if (service.getKey() != null) {
            serviceBuilder.setKey(service.getKey());
        }

        if (service.getTeam() != null) {
            serviceBuilder.setTeam(TeamsAdapters.fromPojo(service.getTeam()));
        }

        if (service.getSchedule() != null) {
            serviceBuilder.setSchedule(SchedulesAdapters.fromPojo(service.getSchedule()));
        }

        return serviceBuilder.build();
    }

}
