package it.voyage.ms.controller.impl;

import it.voyage.ms.controller.IBookmarkCtl;
import it.voyage.ms.dto.response.BookmarkDTO;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IBookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Implementazione del controller per la gestione dei segnalibri
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class BookmarkCtl implements IBookmarkCtl {
    
    private final IBookmarkService bookmarkService;
    
    @Override
    public ResponseEntity<BookmarkDTO> addBookmark(Long travelId, CustomUserDetails userDetails) {
        log.info("Richiesta di aggiunta bookmark per travelId={} da userId={}", travelId, userDetails.getUserId());
        BookmarkDTO bookmark = bookmarkService.addBookmark(userDetails.getUserId(), travelId);
        log.info("Bookmark aggiunto con successo");
        return ResponseEntity.ok(bookmark);
    }
    
    @Override
    public ResponseEntity<Void> removeBookmark(String travelId, CustomUserDetails userDetails) {
        log.info("Richiesta di rimozione bookmark per travelId={} da userId={}", travelId, userDetails.getUserId());
        bookmarkService.removeBookmark(userDetails.getUserId(), travelId);
        log.info("Bookmark rimosso con successo");
        return ResponseEntity.noContent().build();
    }
    
    @Override
    public ResponseEntity<List<BookmarkDTO>> getUserBookmarks(CustomUserDetails userDetails) {
        log.info("Richiesta lista bookmarks per userId={}", userDetails.getUserId());
        List<BookmarkDTO> bookmarks = bookmarkService.getUserBookmarks(userDetails.getUserId());
        log.info("Trovati {} bookmarks", bookmarks.size());
        return ResponseEntity.ok(bookmarks);
    }
    
    @Override
    public ResponseEntity<Map<String, Boolean>> isBookmarked(String travelId, CustomUserDetails userDetails) {
        log.info("Verifica bookmark per travelId={} da userId={}", travelId, userDetails.getUserId());
        boolean isBookmarked = bookmarkService.isBookmarked(userDetails.getUserId(), travelId);
        return ResponseEntity.ok(Map.of("isBookmarked", isBookmarked));
    }
}