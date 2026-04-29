package it.voyage.ms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.dto.CreateSuitcaseRequest;
import it.voyage.ms.dto.SuitcaseDTO;
import it.voyage.ms.dto.SuitcaseItemDTO;
import it.voyage.ms.dto.UserCustomItemDTO;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.impl.SuitcaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/suitcases")
@RequiredArgsConstructor
@Slf4j
public class SuitcaseController {

    private final SuitcaseService suitcaseService;

    @PostMapping
    public ResponseEntity<SuitcaseDTO> createSuitcase(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CreateSuitcaseRequest request) {
        log.info("POST /api/suitcases - Creating suitcase");
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.createSuitcase(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(suitcase);
    }

    @GetMapping
    public ResponseEntity<List<SuitcaseDTO>> getUserSuitcases(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Boolean unassigned) {
        log.info("GET /api/suitcases - Fetching user suitcases");
        String userId = user.getUserId();
        
        List<SuitcaseDTO> suitcases;
        if (Boolean.TRUE.equals(unassigned)) {
            suitcases = suitcaseService.getUserUnassignedSuitcases(userId);
        } else {
            suitcases = suitcaseService.getUserSuitcases(userId);
        }
        
        return ResponseEntity.ok(suitcases);
    }

    @GetMapping("/{suitcaseId}")
    public ResponseEntity<SuitcaseDTO> getSuitcaseById(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId) {
        log.info("GET /api/suitcases/{} - Fetching suitcase by id", suitcaseId);
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.getSuitcaseById(userId, suitcaseId);
        return ResponseEntity.ok(suitcase);
    }

    @PutMapping("/{suitcaseId}")
    public ResponseEntity<SuitcaseDTO> updateSuitcase(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId,
            @RequestBody CreateSuitcaseRequest request) {
        log.info("PUT /api/suitcases/{} - Updating suitcase", suitcaseId);
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.updateSuitcase(userId, suitcaseId, request);
        return ResponseEntity.ok(suitcase);
    }

    @DeleteMapping("/{suitcaseId}")
    public ResponseEntity<Void> deleteSuitcase(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId) {
        log.info("DELETE /api/suitcases/{} - Deleting suitcase", suitcaseId);
        String userId = user.getUserId();
        suitcaseService.deleteSuitcase(userId, suitcaseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{suitcaseId}/associate-travel/{travelId}")
    public ResponseEntity<SuitcaseDTO> associateSuitcaseToTravel(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId,
            @PathVariable Long travelId) {
        log.info("POST /api/suitcases/{}/associate-travel/{} - Associating suitcase to travel", suitcaseId, travelId);
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.associateSuitcaseToTravel(userId, suitcaseId, travelId);
        return ResponseEntity.ok(suitcase);
    }

    @PostMapping("/{suitcaseId}/disassociate-travel")
    public ResponseEntity<SuitcaseDTO> disassociateSuitcaseFromTravel(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId) {
        log.info("POST /api/suitcases/{}/disassociate-travel - Disassociating suitcase from travel", suitcaseId);
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.disassociateSuitcaseFromTravel(userId, suitcaseId);
        return ResponseEntity.ok(suitcase);
    }

    @PatchMapping("/{suitcaseId}/items/{itemId}/check")
    public ResponseEntity<SuitcaseItemDTO> updateItemCheckedStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Boolean> payload) {
        log.info("PATCH /api/suitcases/{}/items/{}/check - Updating item checked status", suitcaseId, itemId);
        String userId = user.getUserId();
        boolean isChecked = payload.getOrDefault("isChecked", false);
        SuitcaseItemDTO item = suitcaseService.updateItemCheckedStatus(userId, suitcaseId, itemId, isChecked);
        return ResponseEntity.ok(item);
    }

    @GetMapping("/by-travel/{travelId}")
    public ResponseEntity<List<SuitcaseDTO>> getSuitcasesByTravelId(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long travelId) {
        log.info("GET /api/suitcases/by-travel/{} - Fetching suitcases for travel", travelId);
        String userId = user.getUserId();
        List<SuitcaseDTO> suitcases = suitcaseService.getSuitcasesByTravelId(userId, travelId);
        return ResponseEntity.ok(suitcases);
    }

    @GetMapping("/custom-items")
    public ResponseEntity<List<UserCustomItemDTO>> getUserCustomItems(
            @AuthenticationPrincipal CustomUserDetails user) {
        log.info("GET /api/suitcases/custom-items - Fetching custom items");
        String userId = user.getUserId();
        List<UserCustomItemDTO> customItems = suitcaseService.getUserCustomItems(userId);
        return ResponseEntity.ok(customItems);
    }
}
