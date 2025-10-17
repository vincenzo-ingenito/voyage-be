package it.voyage.ms.dto.response;

import it.voyage.ms.enums.FriendRelationshipStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSearchResult {
    private String id;
    private String name;
    private String avatar;
    private FriendRelationshipStatusEnum status;

 
}