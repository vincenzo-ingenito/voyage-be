package it.voyage.ms.controller.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.CoordsDto;
import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.PointDTO;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.service.impl.FirebaseStorageService;
import it.voyage.ms.service.impl.TravelService;

@RestController
@RequestMapping("/api/travels")
public class TravelCtl {

	@Autowired
	private TravelRepository travelRepo;

	@Autowired
	private TravelService travelService;


	@Value("${google.places.api.key}")
	private String googleApiKey;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private FirebaseStorageService storageService; 

 
	
	@PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> saveTravel(@RequestPart("travelData") TravelDTO travelData, 
                                        @RequestPart("files") List<MultipartFile> files, 
                                        @AuthenticationPrincipal FirebaseToken userFirebase) {

        // 1. Inizializzazione e conversione
        TravelEty travel = convertToDocument(travelData);  
        String userId = userFirebase.getUid();

        // Generazione ID se assente
        String idTravel = travel.getId();
        if (StringUtils.isBlank(idTravel)) {
            idTravel = UUID.randomUUID().toString();
            travel.setId(idTravel);
        }
        travel.setUserId(userId);
        // NOTA: il DTO 'travelData' ha passato gli indici temporanei all'Entity 'travel'

        List<String> uploadedFileIds;

        try {
            // 2. 🌟 ESECUZIONE DELLA LOGICA DI UPLOAD E INDICAZIONE
            // (Il metodo helper legge i dati dal DTO annidato in travel, carica i file, e aggiorna gli indici)
            uploadedFileIds = processAndUploadAttachments(travel, files);
            
            // 3. IMPOSTA LA LISTA FINALE DEGLI ID DEI FILE
            travel.setAllFileIds(uploadedFileIds); 

        } catch (IOException e) {
            // Gestione del fallimento nel caricamento
            System.err.println("Errore I/O durante il caricamento file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore I/O durante il caricamento di uno o più file.");
        } catch (Exception e) {
            System.err.println("Errore generico durante l'upload: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore generico durante l'elaborazione dei file: " + e.getMessage());
        }

        // 4. Salva l'entità aggiornata
        TravelEty savedTravel = travelRepo.save(travel);
        return ResponseEntity.ok(savedTravel);
    }
	
	private List<String> processAndUploadAttachments(TravelEty travelEty, List<MultipartFile> files) throws IOException {

        List<String> allFileIds = new ArrayList<>();
        int fileCounter = 0; // Contatore globale per l'indice finale (0, 1, 2, ...)

        String userId = travelEty.getUserId();
        String travelId = travelEty.getId();

        List<DailyItineraryDTO> itinerary = travelEty.getItinerary();

        if (itinerary != null) {

            for (DailyItineraryDTO dayDto : itinerary) {

                // 1. Processa l'Immagine Ricordo del Giorno
                if (dayDto.getMemoryImageIndex() != null) {

                    int tempFileIndex = dayDto.getMemoryImageIndex();

                    if (tempFileIndex >= 0 && tempFileIndex < files.size()) {
                        MultipartFile fileToUpload = files.get(tempFileIndex);

                        // 🌟 CHIAMATA ALL'UPLOAD: Restituisce il PATH (ID) del file in Storage
                        String fileId = storageService.uploadFile(fileToUpload, userId, travelId, "day-memory"); 

                        // 🌟 IMPOSTA L'INDICE FINALE
                        dayDto.setMemoryImageIndex(fileCounter);

                        allFileIds.add(fileId);
                        fileCounter++;
                    }
                }

                // 2. Processa gli Allegati dei Punti
                if (dayDto.getPoints() != null) {
                    for (PointDTO pointDto : dayDto.getPoints()) {

                        if (pointDto.getAttachmentIndices() != null && !pointDto.getAttachmentIndices().isEmpty()) {

                            List<Integer> finalAttachmentIndices = new ArrayList<>();

                            for (Integer tempFileIndex : pointDto.getAttachmentIndices()) {

                                if (tempFileIndex >= 0 && tempFileIndex < files.size()) {
                                    MultipartFile fileToUpload = files.get(tempFileIndex);

                                    String fileId = storageService.uploadFile(fileToUpload, userId, travelId, "point-attachment"); 

                                    finalAttachmentIndices.add(fileCounter);

                                    allFileIds.add(fileId);
                                    fileCounter++;
                                }
                            }
                            // Sostituisce la lista di indici temporanei con quelli permanenti
                            pointDto.setAttachmentIndices(finalAttachmentIndices);
                        }
                    }
                }
            }
        }

        return allFileIds;
    }
//
//	private void processItineraryAttachments(List<DailyItineraryDTO> itinerary, List<MultipartFile> files, String storageBasePath, String userId,String travelId) throws Exception {
//
//		for (int dayIndex = 0; dayIndex < itinerary.size(); dayIndex++) {
//			DailyItineraryDTO day = itinerary.get(dayIndex);
//
//			// 1. Gestione Immagine Ricordo del Giorno
//			if (day.getMemoryImageIndex() != null) {
//				int fileIndex = day.getMemoryImageIndex();
//
//				if (fileIndex >= 0 && fileIndex < files.size()) {
//					MultipartFile dayMemoryFile = files.get(fileIndex);
//					//
//					//					// Crea un percorso unico per l'immagine ricordo
//					//					String path = String.format("%s/day_%d/memory_image_%s", 
//					//							storageBasePath, 
//					//							day.getDay(), 
//					//							dayMemoryFile.getOriginalFilename());
//
//					// Carica il file e ottiene l'URL
//					String fileUrl = storageService.uploadFile(dayMemoryFile,userId, travelId);
//
//					// AGGIORNAMENTO CRUCIALE: Salva l'URL nel DTO (o Entity) del giorno
//					day.setMemoryImageUrl(fileUrl);
//					day.setMemoryImageIndex(null); // Rimuovi l'indice dopo l'uso
//				}
//			}
//
//			// 2. Gestione Allegati dei Punti
//			if (day.getPoints() != null) {
//				for (int pointIndex = 0; pointIndex < day.getPoints().size(); pointIndex++) {
//					PointDTO point = day.getPoints().get(pointIndex);
//
//					if (point.getAttachmentIndices() != null) {
//						List<String> attachmentUrls = new ArrayList<>();
//
//						for (int fileIndex : point.getAttachmentIndices()) {
//							if (fileIndex >= 0 && fileIndex < files.size()) {
//								MultipartFile attachmentFile = files.get(fileIndex);
//
//								//								// Crea un percorso unico per l'allegato
//								//								String path = String.format("%s/day_%d/point_%d/attachment_%s", 
//								//										storageBasePath, 
//								//										day.getDay(), 
//								//										pointIndex + 1, // Usa indice leggibile
//								//										attachmentFile.getOriginalFilename());
//
//								// Carica il file e ottiene l'URL
//								String fileUrl = storageService.uploadFile(attachmentFile,userId, travelId);
//								attachmentUrls.add(fileUrl);
//							}
//						}
//
//						// AGGIORNAMENTO CRUCIALE: Salva la lista di URL nel DTO (o Entity) del punto
//						point.setAttachmentUrls(attachmentUrls); 
//						point.setAttachmentIndices(null); // Rimuovi gli indici dopo l'uso
//					}
//				}
//			}
//		}
//	}

	public TravelEty convertToDocument(TravelDTO dto) {
	    TravelEty travel = new TravelEty();
	    
	    // Mappatura campi base
	    travel.setId(dto.getTravelId()); // Importante per l'update
	    travel.setTravelName(dto.getTravelName());
	    travel.setDateFrom(dto.getDateFrom());
	    travel.setDateTo(dto.getDateTo());

	    List<DailyItineraryDTO> itineraryDocuments = dto.getItinerary().stream()
	            .map(dayDTO -> {
	                // Mappiamo il DTO in ingresso (dayDTO) in un nuovo DTO (day) che sarà l'Entity annidata
	                DailyItineraryDTO day = new DailyItineraryDTO();
	                day.setDay(dayDTO.getDay());
	                day.setDate(dayDTO.getDate());
	                
	                // 🌟 CORREZIONE 1: Mappa l'indice dell'immagine ricordo (temporaneo)
	                day.setMemoryImageIndex(dayDTO.getMemoryImageIndex()); 

	                List<PointDTO> pointDocuments = dayDTO.getPoints().stream()
	                        .map(pointDTO -> {
	                            PointDTO point = new PointDTO();
	                            point.setName(pointDTO.getName());
	                            point.setType(pointDTO.getType());
	                            point.setDescription(pointDTO.getDescription());
	                            point.setCost(pointDTO.getCost());
	                            // Assumiamo che CoordsDto abbia un costruttore che accetta lat e lng
	                            point.setCoord(new CoordsDto(pointDTO.getCoord().getLat(),pointDTO.getCoord().getLng()));
	                            point.setCountry(pointDTO.getCountry());
	                            point.setRegion(pointDTO.getRegion());
	                            point.setCity(pointDTO.getCity());
	                            
	                            // 🌟 CORREZIONE 2: Mappa la lista di indici allegati (temporanea)
	                            point.setAttachmentIndices(pointDTO.getAttachmentIndices());
	                            
	                            // Lascia qui i campi URL e URLS a null, verranno risolti dal TravelService
	                            
	                            return point;
	                        }).collect(Collectors.toList());

	                day.setPoints(pointDocuments);
	                return day;
	            }).collect(Collectors.toList());

	    travel.setItinerary(itineraryDocuments);
	    return travel;
	}

//	@GetMapping("")
//	public ResponseEntity<List<TravelDTO>> getTravels(@AuthenticationPrincipal FirebaseToken userFirebase) {
//		List<TravelEty> travelEty = travelRepo.findByUserId(userFirebase.getUid());
//		List<TravelDTO> output = new ArrayList<>();
//		for(TravelEty t : travelEty) {
//			output.add(TravelDTO.convertToDTO(t));
//		}
//		return ResponseEntity.ok(output);
//	}
	
	@GetMapping("")
	public ResponseEntity<List<TravelDTO>> getTravels(@AuthenticationPrincipal FirebaseToken userFirebase) {
		List<TravelDTO> travels = travelService.getTravelsForUser(userFirebase.getUid());
		return ResponseEntity.ok(travels);
	}

	@DeleteMapping("/{travelId}")
	public ResponseEntity<String> deleteTravelById(@PathVariable String travelId, @AuthenticationPrincipal FirebaseToken userFirebase) {
		long deletedCount = travelRepo.deleteByIdAndUserId(travelId, userFirebase.getUid());
		return deletedCount > 0 ? ResponseEntity.ok("Viaggio eliminato con successo") : ResponseEntity.notFound().build();
	}


	private static final String PLACES_AUTOCOMPLETE_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
	private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json";


	/**
	 * Endpoint proxy per Google Places Autocomplete.
	 * @param input Il testo di ricerca dell'utente.
	 * @return La risposta JSON dell'API di Google.
	 */
	@GetMapping("/autocomplete")
	public ResponseEntity<String> autocomplete(@RequestParam String input) {
		if (input == null || input.length() < 3) {
			return ResponseEntity.badRequest().body("{\"error\": \"Input must be at least 3 characters\"}");
		}

		String url = String.format("%s?input=%s&key=%s&language=it", 
				PLACES_AUTOCOMPLETE_URL, input, googleApiKey);

		// Esegue la chiamata all'API di Google
		String response = restTemplate.getForObject(url, String.class);

		return ResponseEntity.ok(response);
	}


	@GetMapping("/details")
	public ResponseEntity<String> getPlaceDetails(@RequestParam String placeId) {
		if (placeId == null || placeId.isEmpty()) {
			return ResponseEntity.badRequest().body("{\"error\": \"Place ID is required\"}");
		}

		// MODIFICA CRUCIALE: Includi 'address_components'
		String fields = "geometry,types,address_components"; 

		String url = String.format("%s?place_id=%s&key=%s&fields=%s",
				PLACES_DETAILS_URL, placeId, googleApiKey, fields);

		// Esegue la chiamata all'API di Google
		String response = restTemplate.getForObject(url, String.class);

		return ResponseEntity.ok(response);
	}

	@PutMapping("/{travelId}")
	public ResponseEntity<TravelDTO> updateTravel(
			@PathVariable String travelId,
			@RequestBody TravelDTO travelDTO,
			@AuthenticationPrincipal FirebaseToken userFirebase) {
		System.out.println("stop");
		//		String uid = userDetails.getUsername(); // Ottiene l'UID dal token autenticato
		//
		//			// Chiama il servizio che gestisce l'autorizzazione e l'aggiornamento del DB
		TravelDTO updatedTravel = travelService.updateExistingTravel(userFirebase.getUid(), travelId, travelDTO);
		// Ritorna HTTP 200 OK con i dati aggiornati
		return ResponseEntity.ok(updatedTravel); 

	}


}
