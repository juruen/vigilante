package io.vigilante.site.http.model.adapters;


import io.vigilante.TeamProtos;
import io.vigilante.site.http.model.Teams;

import java.util.stream.Collectors;

public class TeamsAdapters {
    public static Teams.Team fromProto(TeamProtos.Team team) {
       return Teams.Team.builder()
           .id(team.getId())
           .name(team.getName())
           .users(team.getUsersList().stream().map(user -> UsersAdapters.fromProto(user)).collect(Collectors.toList()))
           .build();
    }

    public static Teams fromProto(TeamProtos.Teams teams) {
        return Teams.builder()
            .teams(teams.getTeamsList().stream().map(team -> fromProto(team)).collect(Collectors.toList()))
            .build();
    }

    public static TeamProtos.Teams fromPojo(Teams teams) {
        return TeamProtos.Teams.newBuilder()
            .addAllTeams(teams.getTeams().stream().map(team -> fromPojo(team)).collect(Collectors.toList()))
            .build();
    }

    public static TeamProtos.Team fromPojo(Teams.Team team) {
        TeamProtos.Team.Builder teamBuilder =  TeamProtos.Team.newBuilder();

        if (team.getId() != null) {
            teamBuilder.setId(team.getId());
        }

        if (team.getName() != null) {
            teamBuilder.setName(team.getName());
        }

        teamBuilder.addAllUsers(team.getUsers()
                .stream().map(user -> UsersAdapters.fromPojo(user))
                .collect(Collectors.toList()));

        return teamBuilder.build();
    }

}
