package it.voyage.ms.mapper;

import org.springframework.stereotype.Component;

import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.repository.entity.FriendRelationshipEty;

@Component
public class FriendrelationshipMapper {

	public FriendRelationshipDto mapToFriendRelationshipDto(FriendRelationshipEty relationship, String requesterName, String requesterAvatar){
		FriendRelationshipDto dto = new FriendRelationshipDto();
		dto.setCreatedAt(relationship.getCreatedAt());
		dto.setReceiverId(relationship.getReceiverId());
		dto.setRequesterId(relationship.getRequesterId());
		dto.setName(requesterName);
		dto.setAvatar(requesterAvatar);
		return dto;
	}
}
