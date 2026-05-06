package it.voyage.ms.controller.impl;

import it.voyage.ms.dto.response.UserSuggestionDTO;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IFriendshipService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller di debug per testare i suggerimenti AI.
 * RIMUOVERE IN PRODUZIONE!
 */
@RestController
@RequestMapping("/api/debug")
@AllArgsConstructor
@Slf4j
public class DebugSuggestionsController {

    private final IFriendshipService friendshipService;
    private final UserRepository userRepository;

    @GetMapping("/ai-users-count")
    public ResponseEntity<Map<String, Object>> getAiUsersCount() {
        long totalAiUsers = userRepository.findAll().stream()
            .filter(u -> u.isAiUser())
            .count();
        
        long publicAiUsers = userRepository.findAll().stream()
            .filter(u -> u.isAiUser() && !u.isPrivate())
            .count();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalAiUsers", totalAiUsers);
        response.put("publicAiUsers", publicAiUsers);
        response.put("message", "Utenti AI nel database");
        
        log.info("Debug: Total AI users: {}, Public AI users: {}", totalAiUsers, publicAiUsers);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-suggestions")
    public ResponseEntity<Map<String, Object>> testSuggestions(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("Debug: Testing suggestions for user: {}", user.getUserId());
        
        List<UserSuggestionDTO> suggestions = friendshipService.getFriendSuggestions(user.getUserId(), limit);
        
        long aiSuggestionsCount = suggestions.stream()
            .filter(s -> s.getIsAiUser() != null && s.getIsAiUser())
            .count();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalSuggestions", suggestions.size());
        response.put("aiSuggestions", aiSuggestionsCount);
        response.put("suggestions", suggestions);
        
        log.info("Debug: Returning {} suggestions ({} AI users)", suggestions.size(), aiSuggestionsCount);
        
        return ResponseEntity.ok(response);
    }
}