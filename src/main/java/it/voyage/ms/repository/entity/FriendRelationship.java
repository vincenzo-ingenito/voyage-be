package it.voyage.ms.repository.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.util.Date;

@Document("friend_relationships")
@Data
public class FriendRelationship {

    @Id
    private String id;
    private String requesterId;
    private String receiverId;
    private String status;
    private Date createdAt;
 
}