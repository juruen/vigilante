package io.vigilante.site.impl.datastore.basic.team;

import static com.spotify.asyncdatastoreclient.QueryBuilder.asc;
import static com.spotify.asyncdatastoreclient.QueryBuilder.eq;
import io.vigilante.TeamProtos.Team;
import io.vigilante.UserProtos.User;
import io.vigilante.site.impl.datastore.basic.Constants;

import java.util.ArrayList;
import java.util.List;

import com.spotify.asyncdatastoreclient.Delete;
import com.spotify.asyncdatastoreclient.Insert;
import com.spotify.asyncdatastoreclient.Key;
import com.spotify.asyncdatastoreclient.KeyQuery;
import com.spotify.asyncdatastoreclient.Query;
import com.spotify.asyncdatastoreclient.QueryBuilder;
import com.spotify.asyncdatastoreclient.Update;

public class TeamQueries {

	public static Query getTeams() {
		return QueryBuilder.query()
				.kindOf(Constants.TEAM_KIND)
				.orderBy(asc(Constants.NAME));
	}

	public static Insert insertTeam(final Team team) {
		return QueryBuilder.insert(Constants.TEAM_KIND)
				.value(Constants.NAME, team.getName())
				.value(Constants.USERS, getUsers(team));
	}

	public static Update updateTeam(final long id, final Team team) {
		return QueryBuilder.update(Constants.TEAM_KIND, id)
				.value(Constants.NAME, team.getName())
				.value(Constants.USERS, getUsers(team));
	}

	public static Delete deleteTeam(final long id) {
		return QueryBuilder.delete(Constants.TEAM_KIND, id);
	}

	public static KeyQuery getTeam(final long id) {
		return QueryBuilder.query(Constants.TEAM_KIND, id);
	}

	public static Query getTeamsForUser(final long id) {
		return QueryBuilder.query().kindOf(Constants.TEAM_KIND).filterBy(eq(Constants.USERS, id));
	}

	public static KeyQuery getTeamKey(final long id) {
		return QueryBuilder.query(Key.builder(Constants.TEAM_KIND, id).build());
	}

	private static List<Object> getUsers(Team team) {
		List<Object> users = new ArrayList<Object>();

		for (User u : team.getUsersList()) {
			users.add(u.getId());
		}

		return users;
	}

}
