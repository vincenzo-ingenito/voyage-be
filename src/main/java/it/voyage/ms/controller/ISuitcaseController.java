package it.voyage.ms.controller;

import java.util.List;
import java.util.Map;

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

@RestController
@RequestMapping("/api/suitcases")
public interface ISuitcaseController {


    @PostMapping
    public ResponseEntity<SuitcaseDTO> createSuitcase(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CreateSuitcaseRequest request);

    @GetMapping
    public ResponseEntity<List<SuitcaseDTO>> getUserSuitcases(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Boolean unassigned);

    @GetMapping("/{suitcaseId}")
    public ResponseEntity<SuitcaseDTO> getSuitcaseById(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId);

    @PutMapping("/{suitcaseId}")
    public ResponseEntity<SuitcaseDTO> updateSuitcase(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId,
            @RequestBody CreateSuitcaseRequest request);

    @DeleteMapping("/{suitcaseId}")
    public ResponseEntity<Void> deleteSuitcase(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId);

    @PostMapping("/{suitcaseId}/associate-travel/{travelId}")
    public ResponseEntity<SuitcaseDTO> associateSuitcaseToTravel(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId,
            @PathVariable Long travelId);

    @PostMapping("/{suitcaseId}/disassociate-travel")
    public ResponseEntity<SuitcaseDTO> disassociateSuitcaseFromTravel(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId);

    @PatchMapping("/{suitcaseId}/items/{itemId}/check")
    public ResponseEntity<SuitcaseItemDTO> updateItemCheckedStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long suitcaseId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Boolean> payload);

    @GetMapping("/by-travel/{travelId}")
    public ResponseEntity<List<SuitcaseDTO>> getSuitcasesByTravelId(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long travelId);

    @GetMapping("/custom-items")
    public ResponseEntity<List<UserCustomItemDTO>> getUserCustomItems(
            @AuthenticationPrincipal CustomUserDetails user);
}
