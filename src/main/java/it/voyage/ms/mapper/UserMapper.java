package it.voyage.ms.mapper;

import org.springframework.stereotype.Component;

import it.voyage.ms.dto.response.UserSearchResult;
import it.voyage.ms.enums.FriendRelationshipStatusEnum;
import it.voyage.ms.repository.entity.UserEty;

@Component
public class UserMapper {

	public UserSearchResult mapToSearchResult(UserEty user, FriendRelationshipStatusEnum status) {
        return new UserSearchResult(user.getId(), user.getName(), user.getAvatar(), status);
    }
}
