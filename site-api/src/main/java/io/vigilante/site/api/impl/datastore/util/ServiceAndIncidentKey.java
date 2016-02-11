package io.vigilante.site.api.impl.datastore.util;

import io.vigilante.ServiceProtos.Service;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceAndIncidentKey {
	private final Service service;
	private final String IncidentKey;
}
