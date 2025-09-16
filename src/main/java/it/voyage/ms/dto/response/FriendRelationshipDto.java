package it.voyage.ms.dto.response;

import java.util.Date;

import lombok.Data;

@Data
public class FriendRelationshipDto {

	private String requesterId;
	private String receiverId;
	private String status;
	private Date createdAt;
	private String name;
	private String avatar;

}
