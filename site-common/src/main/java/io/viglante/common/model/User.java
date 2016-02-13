package io.viglante.common.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class User {
    private final Optional<Long> id;
    private final String name;
    private final String email;
    private final String timeZone;
    private final List<NotificationRule> notifications;

    @JsonCreator
    public User(@JsonProperty("id") Optional<Long> id, @JsonProperty("name") String name,
                @JsonProperty("email") String email, @JsonProperty("timeZone") String timeZone,
                @JsonProperty("notifications") List<NotificationRule> notifications) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.timeZone = timeZone;
        this.notifications = notifications;
    }

    @Data
    @Builder
    public static class NotificationRule {
        public enum NotificationType {
            EMAIL,
            SMS,
            PHONE,
            PUSH,
            SLACK
        }

        private final long start;
        private final NotificationType type;
        private final String contactAddress;
        private final Optional<String> contactDetails;
        private final String contactAlias;

        public NotificationRule(@JsonProperty("start") long start,
                                @JsonProperty("type") NotificationType type,
                                @JsonProperty("contactAddress") String contactAddress,
                                @JsonProperty("contactDetails") Optional<String> contactDetails,
                                @JsonProperty("contactAlias") String contactAlias) {
            this.start = start;
            this.type = type;
            this.contactAddress = contactAddress;
            this.contactDetails = contactDetails;
            this.contactAlias = contactAlias;
        }
    }
}
