package it.voyage.ms.repository.entity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "users")
@Data
public class UserEty {

    @Id
    private String id;
    private String name;
    private String email;
    private String avatar;
    private Date createdAt;
    private Date lastLogin;

}