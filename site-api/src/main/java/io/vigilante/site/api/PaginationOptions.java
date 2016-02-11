package io.vigilante.site.api;

import lombok.Data;
import lombok.NonNull;
import lombok.Builder;

@Builder
@Data
public class PaginationOptions {
	public enum Direction {
		ASCENDING,
		DESCENDING
	}

	final private String fromId;
	final private long limit;

	@NonNull
	final private Direction direction;
}