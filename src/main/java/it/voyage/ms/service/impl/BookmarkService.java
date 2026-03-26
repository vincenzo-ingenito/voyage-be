package it.voyage.ms.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
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

		TravelEty travel = travelRepository.findById(travelId).orElseThrow(() -> new NotFoundException("Viaggio non trovato"));

		if (travel.getUser()!=null && travel.getUser().getId().equals(userId)) {
			throw new ConflictException("Non puoi salvare il tuo stesso viaggio");
		}

		BookmarkEty bookmark = new BookmarkEty(userId, travelId, travel.getUser().getId());
		try {
			bookmark = bookmarkRepository.save(bookmark);
			log.info("Bookmark creato con successo: {}", bookmark.getId());
			return buildBookmarkDTO(bookmark, travel);

		} catch (DataIntegrityViolationException e) {
			log.error("Tentativo di creare bookmark duplicato per userId={} e travelId={}", userId, travelId);
			throw new ConflictException("Viaggio già salvato nei segnalibri");
		}
	}

	@Override
	@Transactional
	public void removeBookmark(String userId, Long travelId) {
		log.info("Rimozione bookmark per userId={} e travelId={}", userId, travelId);

		int deleted = bookmarkRepository.deleteByUserIdAndTravelId(userId, travelId);

		if (deleted == 0) {
			throw new NotFoundException("Segnalibro non trovato");
		}

		log.info("Bookmark rimosso con successo");
	}

	@Override
	public boolean isBookmarked(String userId, Long travelId) {
		return bookmarkRepository.existsByUserIdAndTravelId(userId, travelId);
	}

	// Service
	@Override
	@Transactional(readOnly = true)
	public List<BookmarkDTO> getUserBookmarks(String userId) {
		log.info("Recupero bookmarks per userId={}", userId);

		return bookmarkRepository.findByUserIdWithTravel(userId).stream()
				.filter(b -> b.getTravel() != null)
				.map(b -> buildBookmarkDTO(b, b.getTravel()))
				.collect(Collectors.toList());
	}



	private BookmarkDTO buildBookmarkDTO(BookmarkEty bookmark, TravelEty travel) {
		BookmarkDTO dto = new BookmarkDTO();
		dto.setBookmarkId(bookmark.getId().toString());
		dto.setTravelId(travel.getId().toString());
		dto.setTravelName(travel.getTravelName());
		dto.setBookmarkedAt(bookmark.getCreatedAt());

		if (travel.getDateFrom() != null) {
			dto.setDateFrom(travel.getDateFrom().toString());
		}
		if (travel.getDateTo() != null) {
			dto.setDateTo(travel.getDateTo().toString());
		}

		dto.setOwnerId(travel.getUser().getId());

		if (travel.getUser() != null) {
			dto.setOwnerName(travel.getUser().getName());
		}

		return dto;
	}

	@Scheduled(cron = "0 0 2 * * ?") 
	@Transactional
	public void cleanupOrphanedBookmarks() {
		log.info("Pulizia bookmark orfani iniziata");
		int deleted = bookmarkRepository.deleteOrphanedBookmarks();
		log.info("Eliminati {} bookmark orfani", deleted);
	}
}