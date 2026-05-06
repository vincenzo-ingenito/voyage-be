package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiUserStats {
    private long totalAiUsers;
    private long publicAiUsers;
    private long aiTravels;
}