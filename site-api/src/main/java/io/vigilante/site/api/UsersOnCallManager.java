package io.vigilante.site.api;

import io.vigilante.UserOnCallProtos.UsersOnCall;

public interface UsersOnCallManager {

	UsersOnCall getUsersOnCallForService(long id);
}
