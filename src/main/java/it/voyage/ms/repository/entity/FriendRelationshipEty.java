package it.voyage.ms.repository.entity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document("friend_relationships")
@Data
public class FriendRelationshipEty {

    @Id
    private String id;
    private String requesterId;
    private String receiverId;
    private String status;
    private Date createdAt;
 
}