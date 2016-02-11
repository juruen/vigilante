package io.vigilante.site.http.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
public class Users {
    @JsonProperty
    private List<User> users;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        @JsonProperty
        private Long id;

        @JsonProperty
        private String name;

        @JsonProperty
        private String email;

        @JsonProperty
        private String timeZone;

        @JsonProperty
        private List<NotificationRule> notifications;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class NotificationRule {
            public enum NotificationType {
                EMAIL,
                SMS,
                PHONE,
                PUSH,
                SLACK
            }

            @JsonProperty /* FIXME Deprecated */
            private Long id;

            @JsonProperty
            private Long start;

            @JsonProperty
            private NotificationType type;

            @JsonProperty
            private String contactAddress;

            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            private String contactDetails;

            @JsonProperty
            private String contactAlias;
        }
    }
}
