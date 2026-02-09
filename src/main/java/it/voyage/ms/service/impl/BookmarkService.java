package it.voyage.ms.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.dto.response.BookmarkDTO;
import it.voyage.ms.exceptions.ConflictException;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.repository.entity.BookmarkEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.BookmarkRepository;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.service.IBookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementazione del servizio per la gestione dei segnalibri
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService implements IBookmarkService {
    
    private final BookmarkRepository bookmarkRepository;
    private final TravelRepository travelRepository;
    
    @Override
    @Transactional
    public BookmarkDTO addBookmark(String userId, Long travelId) {
        log.info("Aggiunta bookmark per userId={} e travelId={}", userId, travelId);
        
        // Verifica che il viaggio esista
        TravelEty travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new NotFoundException("Viaggio non trovato"));
        
        // Verifica che l'utente non stia salvando il proprio viaggio
        if (travel.getUser().getId().equals(userId)) {
            throw new ConflictException("Non puoi salvare il tuo stesso viaggio");
        }
        
        // Verifica che il bookmark non esista già
        if (bookmarkRepository.existsByUserIdAndTravelId(userId, travelId)) {
            throw new ConflictException("Viaggio già salvato nei segnalibri");
        }

        // Crea il bookmark
        BookmarkEty bookmark = new BookmarkEty(userId, travelId, travel.getUser().getId());
        bookmark = bookmarkRepository.save(bookmark);
        log.info("Bookmark creato con successo: {}", bookmark.getId());
        
        return buildBookmarkDTO(bookmark, travel);
    }
    
    @Override
    @Transactional
    public void removeBookmark(String userId, String travelId) {
        log.info("Rimozione bookmark per userId={} e travelId={}", userId, travelId);
        
        // Converti travelId in Long
        Long travelIdLong;
        try {
            travelIdLong = Long.parseLong(travelId);
        } catch (NumberFormatException e) {
            log.error("TravelId non valido: {}", travelId);
            throw new IllegalArgumentException("ID viaggio non valido");
        }
        
        // Verifica che il bookmark esista
        if (!bookmarkRepository.existsByUserIdAndTravelId(userId, travelIdLong)) {
            throw new NotFoundException("Segnalibro non trovato");
        }
        
        bookmarkRepository.deleteByUserIdAndTravelId(userId, travelIdLong);
        log.info("Bookmark rimosso con successo");
    }
    
    @Override
    public boolean isBookmarked(String userId, String travelId) {
        try {
            Long travelIdLong = Long.parseLong(travelId);
            return bookmarkRepository.existsByUserIdAndTravelId(userId, travelIdLong);
        } catch (NumberFormatException e) {
            log.error("TravelId non valido per verifica bookmark: {}", travelId);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookmarkDTO> getUserBookmarks(String userId) {
        log.info("Recupero bookmarks per userId={}", userId);
        
        List<BookmarkEty> bookmarks = bookmarkRepository.findByUserId(userId);
        
        return bookmarks.stream()
                .map(bookmark -> {
                    TravelEty travel = bookmark.getTravel();
                    
                    if (travel == null) {
                        // Se il viaggio non esiste più, elimina il bookmark orfano
                        log.warn("Viaggio {} non trovato, eliminazione bookmark orfano", bookmark.getTravelId());
                        bookmarkRepository.deleteById(bookmark.getId());
                        return null;
                    }
                    
                    // Inizializza la collezione points se necessario (ancora dentro la transazione)
                    if (travel.getItinerary() != null && !travel.getItinerary().isEmpty()) {
                        travel.getItinerary().forEach(day -> {
                            if (day.getPoints() != null) {
                                day.getPoints().size(); // Forza il caricamento lazy
                            }
                        });
                    }
                    
                    return buildBookmarkDTO(bookmark, travel);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteBookmarksByTravel(String travelId) {
        log.info("Eliminazione di tutti i bookmarks per travelId={}", travelId);
        try {
            Long travelIdLong = Long.parseLong(travelId);
            bookmarkRepository.deleteByTravelId(travelIdLong);
        } catch (NumberFormatException e) {
            log.error("TravelId non valido per eliminazione bookmarks: {}", travelId);
        }
    }
    
    /**
     * Costruisce un BookmarkDTO a partire da un BookmarkEty e un TravelEty
     */
    private BookmarkDTO buildBookmarkDTO(BookmarkEty bookmark, TravelEty travel) {
        BookmarkDTO dto = new BookmarkDTO();
        dto.setBookmarkId(bookmark.getId().toString());
        dto.setTravelId(travel.getId().toString());
        dto.setTravelName(travel.getTravelName());
        dto.setBookmarkedAt(bookmark.getCreatedAt());
        
        // Converti Date in String
        if (travel.getDateFrom() != null) {
            dto.setDateFrom(travel.getDateFrom().toString());
        }
        if (travel.getDateTo() != null) {
            dto.setDateTo(travel.getDateTo().toString());
        }
        
        dto.setOwnerId(travel.getUser().getId());
        
        // Estrai città e paese dal primo punto dell'itinerario
        if (travel.getItinerary() != null && !travel.getItinerary().isEmpty()) {
            var firstDay = travel.getItinerary().get(0);
            if (firstDay.getPoints() != null && !firstDay.getPoints().isEmpty()) {
                var firstPoint = firstDay.getPoints().get(0);
                dto.setCity(firstPoint.getCity());
                dto.setCountry(firstPoint.getCountry());
            }
            
            // Aggiungi l'immagine di copertina se presente
            if (firstDay.getMemoryImageUrl() != null) {
                dto.setCoverImageUrl(firstDay.getMemoryImageUrl());
            }
        }
        
        // Recupera informazioni sul proprietario del viaggio
        if (travel.getUser() != null) {
            dto.setOwnerName(travel.getUser().getName());
            dto.setOwnerAvatar(travel.getUser().getAvatar());
        }
        
        return dto;
    }
}