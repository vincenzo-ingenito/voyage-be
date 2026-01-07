package it.voyage.ms.service.impl;

import it.voyage.ms.dto.response.BookmarkDTO;
import it.voyage.ms.exceptions.ConflictException;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.repository.entity.BookmarkEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.BookmarkRepository;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.IBookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementazione del servizio per la gestione dei segnalibri
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService implements IBookmarkService {
    
    private final BookmarkRepository bookmarkRepository;
    private final TravelRepository travelRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public BookmarkDTO addBookmark(String userId, String travelId) {
        log.info("Aggiunta bookmark per userId={} e travelId={}", userId, travelId);
        
        // Verifica che il viaggio esista
        TravelEty travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new NotFoundException("Viaggio non trovato"));
        
        // Verifica che l'utente non stia cercando di salvare il proprio viaggio
        if (travel.getUserId().equals(userId)) {
            throw new ConflictException("Non puoi salvare i tuoi viaggi nei segnalibri");
        }
        
        // Verifica che il bookmark non esista già
        if (bookmarkRepository.existsByUserIdAndTravelId(userId, travelId)) {
            throw new ConflictException("Viaggio già salvato nei segnalibri");
        }
        
        // Crea e salva il bookmark
        BookmarkEty bookmark = new BookmarkEty(userId, travelId, travel.getUserId());
        bookmark = bookmarkRepository.save(bookmark);
        
        log.info("Bookmark creato con successo: {}", bookmark.getId());
        
        // Costruisci e ritorna il DTO
        return buildBookmarkDTO(bookmark, travel);
    }
    
    @Override
    @Transactional
    public void removeBookmark(String userId, String travelId) {
        log.info("Rimozione bookmark per userId={} e travelId={}", userId, travelId);
        
        // Verifica che il bookmark esista
        if (!bookmarkRepository.existsByUserIdAndTravelId(userId, travelId)) {
            throw new NotFoundException("Segnalibro non trovato");
        }
        
        bookmarkRepository.deleteByUserIdAndTravelId(userId, travelId);
        log.info("Bookmark rimosso con successo");
    }
    
    @Override
    public boolean isBookmarked(String userId, String travelId) {
        return bookmarkRepository.existsByUserIdAndTravelId(userId, travelId);
    }
    
    @Override
    public List<BookmarkDTO> getUserBookmarks(String userId) {
        log.info("Recupero bookmarks per userId={}", userId);
        
        List<BookmarkEty> bookmarks = bookmarkRepository.findByUserId(userId);
        
        return bookmarks.stream()
                .map(bookmark -> {
                    // Recupera il viaggio associato
                    TravelEty travel = travelRepository.findById(bookmark.getTravelId())
                            .orElse(null);
                    
                    if (travel == null) {
                        // Se il viaggio non esiste più, elimina il bookmark orfano
                        log.warn("Viaggio {} non trovato, eliminazione bookmark orfano", bookmark.getTravelId());
                        bookmarkRepository.deleteById(bookmark.getId());
                        return null;
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
        bookmarkRepository.deleteByTravelId(travelId);
    }
    
    /**
     * Costruisce un BookmarkDTO a partire da un BookmarkEty e un TravelEty
     */
    private BookmarkDTO buildBookmarkDTO(BookmarkEty bookmark, TravelEty travel) {
        BookmarkDTO dto = new BookmarkDTO();
        dto.setBookmarkId(bookmark.getId());
        dto.setTravelId(travel.getId());
        dto.setTravelName(travel.getTravelName());
        dto.setDateFrom(travel.getDateFrom());
        dto.setDateTo(travel.getDateTo());
        dto.setOwnerId(travel.getUserId());
        dto.setBookmarkedAt(bookmark.getCreatedAt());
        
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
        userRepository.findById(travel.getUserId()).ifPresent(owner -> {
            dto.setOwnerName(owner.getName());
            dto.setOwnerAvatar(owner.getAvatar());
        });
        
        return dto;
    }
}