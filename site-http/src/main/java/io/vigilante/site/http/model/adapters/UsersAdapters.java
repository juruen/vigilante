package io.vigilante.site.http.model.adapters;


import io.vigilante.UserProtos;
import io.vigilante.site.http.model.Users;

import java.util.List;
import java.util.stream.Collectors;

public class UsersAdapters {
    public static Users.User fromProto(UserProtos.User user) {
       return Users.User.builder()
           .id(user.getId())
           .email(user.getEmail())
           .name(user.getName())
           .timeZone(user.getTimeZone())
           .notifications(buildNotifications(user))
           .build();
    }

    public static Users fromProto(UserProtos.Users users) {
        return Users.builder()
            .users(users.getUsersList().stream().map(user -> fromProto(user)).collect(Collectors.toList()))
            .build();
    }

    public static UserProtos.Users fromPojo(Users users) {
        return UserProtos.Users.newBuilder()
            .addAllUsers(users.getUsers().stream().map(u -> fromPojo(u)).collect(Collectors.toList()))
            .build();
    }

    public static UserProtos.User fromPojo(Users.User user) {
        UserProtos.User.Builder userBuilder =  UserProtos.User.newBuilder();

        if (user.getEmail() != null) {
            userBuilder.setEmail(user.getEmail());
        }

        if (user.getId() != null) {
            userBuilder.setId(user.getId());
        }

        if (user.getName() != null) {
            userBuilder.setName(user.getName());
        }

        if (user.getTimeZone() != null) {
            userBuilder.setTimeZone(user.getTimeZone());
        }

        if (user.getNotifications() != null) {
            userBuilder.addAllNotifications(buildNotifications(user.getNotifications()));
        }

        return userBuilder.build();
    }

    private static List<UserProtos.User.NotificationRule> buildNotifications(List<Users.User.NotificationRule> notifications) {
        return notifications.stream()
            .map(n -> buildNotificationRule(n))
            .collect(Collectors.toList());
    }

    private static UserProtos.User.NotificationRule buildNotificationRule(Users.User.NotificationRule n) {
        UserProtos.User.NotificationRule.Builder ruleBuilder = UserProtos.User.NotificationRule.newBuilder();

        if (n.getStart() != null){
            ruleBuilder.setStart(n.getStart());
        }

        if (n.getContactAddress() != null){
            ruleBuilder.setContactAddress(n.getContactAddress());
        }

        if (n.getContactAlias() != null){
            ruleBuilder.setContactAlias(n.getContactAlias());
        }

        if (n.getContactDetails() != null){
            ruleBuilder.setContactDetails(n.getContactDetails());
        }

        if (n.getType() != null) {
            ruleBuilder.setType(UserProtos.User.NotificationType.valueOf(n.getType().toString()));
        }

        return ruleBuilder.build();
    }

    private static List<Users.User.NotificationRule> buildNotifications(UserProtos.User user) {
        return user.getNotificationsList()
            .stream()
            .map(n -> Users.User.NotificationRule.builder()
                .contactAddress(n.getContactAddress())
                .contactAlias(n.getContactAlias())
                .contactDetails(n.getContactDetails())
                .start(n.getStart())
                .type(Users.User.NotificationRule.NotificationType.valueOf(n.getType().toString()))
                .build())
            .collect(Collectors.toList());
    }

}
