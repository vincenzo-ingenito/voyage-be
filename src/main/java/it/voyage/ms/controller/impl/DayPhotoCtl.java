package it.voyage.ms.controller.impl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.voyage.ms.service.IDayPhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import it.voyage.ms.security.user.CustomUserDetails;

@Slf4j
@RestController
@RequestMapping("/api/travels")
@RequiredArgsConstructor
@Tag(name = "Day Photo Management", description = "APIs for managing single photo memory per day")
public class DayPhotoCtl {

    private final IDayPhotoService dayPhotoService;

    @PostMapping(value = "/{travelId}/days/{dayNumber}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add or replace photo memory for a specific day")
    public ResponseEntity<String> addPhotoToDay(
            @PathVariable Long travelId,
            @PathVariable Integer dayNumber,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            log.info("Adding photo to travel {} day {}", travelId, dayNumber);
            String photoUrl = dayPhotoService.addOrReplacePhotoToDay(travelId, dayNumber, file, userDetails.getUserId());
            return ResponseEntity.ok(photoUrl);
        } catch (Exception e) {
            log.error("Error adding photo to day: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading photo: " + e.getMessage());
        }
    }

    @DeleteMapping("/{travelId}/days/{dayNumber}/photo")
    @Operation(summary = "Remove photo memory from a specific day")
    public ResponseEntity<Void> removePhotoFromDay(
            @PathVariable Long travelId,
            @PathVariable Integer dayNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            log.info("Removing photo from travel {} day {}", travelId, dayNumber);
            dayPhotoService.removePhotoFromDay(travelId, dayNumber, userDetails.getUserId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error removing photo from day: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}