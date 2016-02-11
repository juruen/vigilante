package io.vigilante.site.http.resources.util;

import io.vigilante.site.api.PaginationOptions;

public class PaginationUtil {
    private static final long DEFAULT_LENGTH = 10;
    private static final String DEFAULT_DIRECTION = PaginationOptions.Direction.DESCENDING.toString();

    public static PaginationOptions toPaginationOptions(String start,
                                                        String direction,
                                                        Long length) {

        PaginationOptions.Direction dir;

        if (direction == null ){
            dir = PaginationOptions.Direction.DESCENDING;
        } else if  (direction.equalsIgnoreCase("forward")) {
            dir = PaginationOptions.Direction.ASCENDING;
        } else if (direction.equalsIgnoreCase("backward")) {
            dir = PaginationOptions.Direction.DESCENDING;
        } else {
            throw new IllegalArgumentException("Direction parameter is invalid");
        }

        return PaginationOptions.builder()
            .fromId(start)
            .direction(dir)
            .limit(length != null ? length : DEFAULT_LENGTH)
            .build();
    }
}
