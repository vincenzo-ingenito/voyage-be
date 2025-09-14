package it.voyage.ms.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for friend requests.
 * Used to receive the receiver's ID from the frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDto {
    private String receiverId;
}