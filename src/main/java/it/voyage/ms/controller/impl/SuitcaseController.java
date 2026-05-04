package it.voyage.ms.controller.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.ISuitcaseController;
import it.voyage.ms.dto.CreateSuitcaseRequest;
import it.voyage.ms.dto.SuitcaseDTO;
import it.voyage.ms.dto.SuitcaseItemDTO;
import it.voyage.ms.dto.UserCustomItemDTO;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.impl.SuitcaseService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class SuitcaseController implements ISuitcaseController {

	@Autowired
    private SuitcaseService suitcaseService;

	@Override
    public ResponseEntity<SuitcaseDTO> createSuitcase(CustomUserDetails user, CreateSuitcaseRequest request) {
        log.info("POST /api/suitcases - Creating suitcase");
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.createSuitcase(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(suitcase);
    }

	@Override
    public ResponseEntity<List<SuitcaseDTO>> getUserSuitcases(
            CustomUserDetails user,
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

	@Override
	public ResponseEntity<SuitcaseDTO> getSuitcaseById(
            CustomUserDetails user,
            Long suitcaseId) {
        log.info("GET /api/suitcases/{} - Fetching suitcase by id", suitcaseId);
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.getSuitcaseById(userId, suitcaseId);
        return ResponseEntity.ok(suitcase);
    }

	@Override
    public ResponseEntity<SuitcaseDTO> updateSuitcase(
            CustomUserDetails user,
            Long suitcaseId,
            CreateSuitcaseRequest request) {
        log.info("PUT /api/suitcases/{} - Updating suitcase", suitcaseId);
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.updateSuitcase(userId, suitcaseId, request);
        return ResponseEntity.ok(suitcase);
    }

	@Override
    public ResponseEntity<Void> deleteSuitcase(
            CustomUserDetails user,
            Long suitcaseId) {
        log.info("DELETE /api/suitcases/{} - Deleting suitcase", suitcaseId);
        String userId = user.getUserId();
        suitcaseService.deleteSuitcase(userId, suitcaseId);
        return ResponseEntity.noContent().build();
    }

	@Override
	public ResponseEntity<SuitcaseDTO> associateSuitcaseToTravel(
            CustomUserDetails user,
            Long suitcaseId,
            Long travelId) {
        log.info("POST /api/suitcases/{}/associate-travel/{} - Associating suitcase to travel", suitcaseId, travelId);
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.associateSuitcaseToTravel(userId, suitcaseId, travelId);
        return ResponseEntity.ok(suitcase);
    }

    @Override
    public ResponseEntity<SuitcaseDTO> disassociateSuitcaseFromTravel(
            CustomUserDetails user,
            Long suitcaseId) {
        log.info("POST /api/suitcases/{}/disassociate-travel - Disassociating suitcase from travel", suitcaseId);
        String userId = user.getUserId();
        SuitcaseDTO suitcase = suitcaseService.disassociateSuitcaseFromTravel(userId, suitcaseId);
        return ResponseEntity.ok(suitcase);
    }

    @Override
    public ResponseEntity<SuitcaseItemDTO> updateItemCheckedStatus(
            CustomUserDetails user,
            Long suitcaseId,
            Long itemId,
            Map<String, Boolean> payload) {
        log.info("PATCH /api/suitcases/{}/items/{}/check - Updating item checked status", suitcaseId, itemId);
        String userId = user.getUserId();
        boolean isChecked = payload.getOrDefault("isChecked", false);
        SuitcaseItemDTO item = suitcaseService.updateItemCheckedStatus(userId, suitcaseId, itemId, isChecked);
        return ResponseEntity.ok(item);
    }

    @Override
    public ResponseEntity<List<SuitcaseDTO>> getSuitcasesByTravelId(CustomUserDetails user, Long travelId) {
        log.info("GET /api/suitcases/by-travel/{} - Fetching suitcases for travel", travelId);
        String userId = user.getUserId();
        List<SuitcaseDTO> suitcases = suitcaseService.getSuitcasesByTravelId(userId, travelId);
        return ResponseEntity.ok(suitcases);
    }

    @Override
    public ResponseEntity<List<UserCustomItemDTO>> getUserCustomItems(CustomUserDetails user) {
        log.info("GET /api/suitcases/custom-items - Fetching custom items");
        String userId = user.getUserId();
        List<UserCustomItemDTO> customItems = suitcaseService.getUserCustomItems(userId);
        return ResponseEntity.ok(customItems);
    }
}
