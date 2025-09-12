package it.voyage.ms.repository.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.util.Date;

@Document(collection = "users")
@Data
public class UserEty {

    @Id
    private String id;
    private String displayName;
    private String email;
    private String photoURL;
    private Date createdAt;
    private Date lastLogin;

}