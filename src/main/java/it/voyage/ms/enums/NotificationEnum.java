package it.voyage.ms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NotificationEnum {

    SEND_LIKE("LIKE", "Nuovo like!", "%s ha messo mi piace a %s"),
    GROUP_TRAVEL_INVITE("GROUP_TRAVEL_INVITE", "Nuovo invito viaggio!", "%s ti ha invitato a %s"),
    INVITE_ACCEPTED("INVITE_ACCEPTED", "Invito accettato!", "%s ha accettato l'invito a %s"),
    NEW_FOLLOWER("NEW_FOLLOWER", "Nuovo follower!", "%s ha iniziato a seguirti"),
    FRIEND_REQUEST("FRIEND_REQUEST", "Nuova richiesta di amicizia!", "%s ti ha inviato una richiesta di amicizia");

    private final String type;
    private final String title;
    private final String description;
}