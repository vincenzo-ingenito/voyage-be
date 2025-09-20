package it.voyage.ms.dto.response;

import it.voyage.ms.enums.FriendRelationshipStatusEnum;

public class UserSearchResult {
    private String id;
    private String name;
    private String avatar;
    private FriendRelationshipStatusEnum status;

    public UserSearchResult(String id, String name, String avatar, FriendRelationshipStatusEnum status) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.status = status;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public FriendRelationshipStatusEnum getStatus() {
        return status;
    }

    public void setStatus(FriendRelationshipStatusEnum status) {
        this.status = status;
    }
}