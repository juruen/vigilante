package io.vigilante.site.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticationResponse {
	private final long id;
	private final String session;
}
