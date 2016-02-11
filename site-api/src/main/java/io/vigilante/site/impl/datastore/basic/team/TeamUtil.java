package io.vigilante.site.impl.datastore.basic.team;

import io.vigilante.TeamProtos.Team;
import io.vigilante.UserProtos.User;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.spotify.asyncdatastoreclient.Entity;
import com.spotify.asyncdatastoreclient.Value;

@Slf4j
public class TeamUtil {

	public static Team buildBasicTeam(@NonNull final Entity teamEntity) {
		List<User> users = new ArrayList<>();

		try {
			for (Value id : teamEntity.getList(Constants.USERS)) {
				users.add(User.newBuilder().setId(id.getInteger()).build());
			}
		} catch (IllegalArgumentException e) { // FIXME I Dunno why?!
			log.error("failed to fetch users for team");
		}

		return Team.newBuilder()
				.setId(teamEntity.getKey().getId())
				.setName(teamEntity.getString(Constants.NAME))
				.addAllUsers(users)
				.build();
	}
}
