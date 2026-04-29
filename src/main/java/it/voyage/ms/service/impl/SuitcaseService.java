package it.voyage.ms.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.dto.CreateSuitcaseRequest;
import it.voyage.ms.dto.SuitcaseDTO;
import it.voyage.ms.dto.SuitcaseItemDTO;
import it.voyage.ms.dto.UserCustomItemDTO;
import it.voyage.ms.repository.SuitcaseItemRepository;
import it.voyage.ms.repository.SuitcaseRepository;
import it.voyage.ms.repository.UserCustomItemRepository;
import it.voyage.ms.repository.entity.SuitcaseEty;
import it.voyage.ms.repository.entity.SuitcaseItemEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.UserCustomItemEty;
import it.voyage.ms.repository.impl.TravelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuitcaseService {

    private final SuitcaseRepository suitcaseRepository;
    private final SuitcaseItemRepository suitcaseItemRepository;
    private final TravelRepository travelRepository;
    private final UserCustomItemRepository userCustomItemRepository;
    
    // Lista degli oggetti preimpostati di default
    private static final List<String> DEFAULT_PRESET_ITEMS = List.of("Spazzolino", "Dentifricio", "Passaporto", "Caricabatterie");

    @Transactional
    public SuitcaseDTO createSuitcase(String userId, CreateSuitcaseRequest request) {
        log.info("Creating suitcase for user: {}, name: {}", userId, request.getName());

        SuitcaseEty suitcase = SuitcaseEty.builder().name(request.getName()).userId(userId).build();

        // Se specificato un travelId, associa la valigia al viaggio
        if (request.getTravelId() != null) {
            TravelEty travel = travelRepository.findById(request.getTravelId()).orElseThrow(() -> new RuntimeException("Travel not found with id: " + request.getTravelId()));
            
            // Verifica che il viaggio appartenga all'utente
            if (!travel.getUser().getId().equals(userId)) {
                throw new RuntimeException("User does not have access to this travel");
            }
            
            suitcase.setTravel(travel);
        }

        suitcase = suitcaseRepository.save(suitcase);

        // Aggiungi gli item se presenti
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (SuitcaseItemDTO itemDTO : request.getItems()) {
                SuitcaseItemEty item = SuitcaseItemEty.builder()
                        .name(itemDTO.getName())
                        .isChecked(itemDTO.getIsChecked() != null ? itemDTO.getIsChecked() : false)
                        .quantity(itemDTO.getQuantity())
                        .category(itemDTO.getCategory())
                        .suitcase(suitcase)
                        .build();
                suitcase.getItems().add(item);
                
                // Se l'oggetto non è tra quelli di default, salvalo come custom item dell'utente
                saveCustomItemIfNew(userId, itemDTO.getName(), itemDTO.getCategory());
            }
            suitcase = suitcaseRepository.save(suitcase);
        }

        log.info("Suitcase created successfully with id: {}", suitcase.getId());
        return convertToDTO(suitcase);
    }

    @Transactional(readOnly = true)
    public List<SuitcaseDTO> getUserSuitcases(String userId) {
        log.info("Fetching suitcases for user: {}", userId);
        List<SuitcaseEty> suitcases = suitcaseRepository.findByUserId(userId);
        return suitcases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SuitcaseDTO> getUserUnassignedSuitcases(String userId) {
        log.info("Fetching unassigned suitcases for user: {}", userId);
        List<SuitcaseEty> suitcases = suitcaseRepository.findByUserIdAndTravelIsNull(userId);
        return suitcases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SuitcaseDTO getSuitcaseById(String userId, Long suitcaseId) {
        log.info("Fetching suitcase with id: {} for user: {}", suitcaseId, userId);
        SuitcaseEty suitcase = suitcaseRepository.findByIdAndUserIdWithItems(suitcaseId, userId)
                .orElseThrow(() -> new RuntimeException("Suitcase not found with id: " + suitcaseId));
        return convertToDTO(suitcase);
    }

    @Transactional
    public SuitcaseDTO updateSuitcase(String userId, Long suitcaseId, CreateSuitcaseRequest request) {
        log.info("Updating suitcase with id: {} for user: {}", suitcaseId, userId);
        
        SuitcaseEty suitcase = suitcaseRepository.findByIdAndUserId(suitcaseId, userId)
                .orElseThrow(() -> new RuntimeException("Suitcase not found with id: " + suitcaseId));

        suitcase.setName(request.getName());

        // Aggiorna l'associazione al viaggio
        if (request.getTravelId() != null) {
            TravelEty travel = travelRepository.findById(request.getTravelId())
                    .orElseThrow(() -> new RuntimeException("Travel not found with id: " + request.getTravelId()));
            
            if (!travel.getUser().getId().equals(userId)) {
                throw new RuntimeException("User does not have access to this travel");
            }
            
            suitcase.setTravel(travel);
        } else {
            suitcase.setTravel(null);
        }

        // Rimuovi tutti gli item esistenti
        suitcase.getItems().clear();

        // Aggiungi i nuovi item
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (SuitcaseItemDTO itemDTO : request.getItems()) {
                SuitcaseItemEty item = SuitcaseItemEty.builder()
                        .name(itemDTO.getName())
                        .isChecked(itemDTO.getIsChecked() != null ? itemDTO.getIsChecked() : false)
                        .quantity(itemDTO.getQuantity())
                        .category(itemDTO.getCategory())
                        .suitcase(suitcase)
                        .build();
                suitcase.getItems().add(item);
                
                // Se l'oggetto non è tra quelli di default, salvalo come custom item dell'utente
                saveCustomItemIfNew(userId, itemDTO.getName(), itemDTO.getCategory());
            }
        }

        suitcase = suitcaseRepository.save(suitcase);
        log.info("Suitcase updated successfully with id: {}", suitcaseId);
        return convertToDTO(suitcase);
    }

    @Transactional
    public void deleteSuitcase(String userId, Long suitcaseId) {
        log.info("Deleting suitcase with id: {} for user: {}", suitcaseId, userId);
        
        SuitcaseEty suitcase = suitcaseRepository.findByIdAndUserId(suitcaseId, userId)
                .orElseThrow(() -> new RuntimeException("Suitcase not found with id: " + suitcaseId));

        suitcaseRepository.delete(suitcase);
        log.info("Suitcase deleted successfully with id: {}", suitcaseId);
    }

    @Transactional
    public SuitcaseDTO associateSuitcaseToTravel(String userId, Long suitcaseId, Long travelId) {
        log.info("Associating suitcase {} to travel {} for user: {}", suitcaseId, travelId, userId);
        
        SuitcaseEty suitcase = suitcaseRepository.findByIdAndUserId(suitcaseId, userId)
                .orElseThrow(() -> new RuntimeException("Suitcase not found with id: " + suitcaseId));

        TravelEty travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new RuntimeException("Travel not found with id: " + travelId));
        
        if (!travel.getUser().getId().equals(userId)) {
            throw new RuntimeException("User does not have access to this travel");
        }

        // Se esiste già una valigia associata a questo viaggio, dissociala prima
        List<SuitcaseEty> existingSuitcases = suitcaseRepository.findByUserIdAndTravelId(userId, travelId);
        for (SuitcaseEty existingSuitcase : existingSuitcases) {
            if (!existingSuitcase.getId().equals(suitcaseId)) {
                log.info("Disassociating existing suitcase {} from travel {}", existingSuitcase.getId(), travelId);
                existingSuitcase.setTravel(null);
                suitcaseRepository.save(existingSuitcase);
            }
        }

        suitcase.setTravel(travel);
        suitcase = suitcaseRepository.save(suitcase);
        
        log.info("Suitcase associated successfully to travel");
        return convertToDTO(suitcase);
    }

    @Transactional
    public SuitcaseDTO disassociateSuitcaseFromTravel(String userId, Long suitcaseId) {
        log.info("Disassociating suitcase {} from travel for user: {}", suitcaseId, userId);
        
        SuitcaseEty suitcase = suitcaseRepository.findByIdAndUserId(suitcaseId, userId)
                .orElseThrow(() -> new RuntimeException("Suitcase not found with id: " + suitcaseId));

        suitcase.setTravel(null);
        suitcase = suitcaseRepository.save(suitcase);
        
        log.info("Suitcase disassociated successfully from travel");
        return convertToDTO(suitcase);
    }

    @Transactional
    public SuitcaseItemDTO updateItemCheckedStatus(String userId, Long suitcaseId, Long itemId, boolean isChecked) {
        log.info("Updating item {} checked status to {} for suitcase {}", itemId, isChecked, suitcaseId);
        
        SuitcaseEty suitcase = suitcaseRepository.findByIdAndUserId(suitcaseId, userId)
                .orElseThrow(() -> new RuntimeException("Suitcase not found with id: " + suitcaseId));

        SuitcaseItemEty item = suitcase.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + itemId));

        item.setIsChecked(isChecked);
        suitcaseItemRepository.save(item);
        
        log.info("Item checked status updated successfully");
        return convertItemToDTO(item);
    }

    @Transactional(readOnly = true)
    public List<SuitcaseDTO> getSuitcasesByTravelId(String userId, Long travelId) {
        log.info("Fetching suitcases for travel {} and user: {}", travelId, userId);
        List<SuitcaseEty> suitcases = suitcaseRepository.findByUserIdAndTravelId(userId, travelId);
        return suitcases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Recupera gli oggetti personalizzati dell'utente
     */
    @Transactional(readOnly = true)
    public List<UserCustomItemDTO> getUserCustomItems(String userId) {
        log.info("Fetching custom items for user: {}", userId);
        List<UserCustomItemEty> customItems = userCustomItemRepository.findByUserId(userId);
        return customItems.stream()
                .map(this::convertCustomItemToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Salva un oggetto personalizzato se è nuovo (non è tra i default e non esiste già)
     */
    private void saveCustomItemIfNew(String userId, String itemName, String category) {
        // Ignora se è un oggetto di default
        if (DEFAULT_PRESET_ITEMS.contains(itemName)) {
            return;
        }
        
        // Verifica se esiste già
        if (!userCustomItemRepository.existsByUserIdAndName(userId, itemName)) {
            UserCustomItemEty customItem = UserCustomItemEty.builder()
                    .userId(userId)
                    .name(itemName)
                    .category(category != null ? category : "Altro")
                    .build();
            userCustomItemRepository.save(customItem);
            log.info("Saved new custom item '{}' for user: {}", itemName, userId);
        }
    }

    private SuitcaseDTO convertToDTO(SuitcaseEty entity) {
        return SuitcaseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .userId(entity.getUserId())
                .travelId(entity.getTravel() != null ? entity.getTravel().getId() : null)
                .travelName(entity.getTravel() != null ? entity.getTravel().getTravelName() : null)
                .items(entity.getItems().stream()
                        .map(this::convertItemToDTO)
                        .collect(Collectors.toList()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private SuitcaseItemDTO convertItemToDTO(SuitcaseItemEty entity) {
        return SuitcaseItemDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .isChecked(entity.getIsChecked())
                .quantity(entity.getQuantity())
                .category(entity.getCategory())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private UserCustomItemDTO convertCustomItemToDTO(UserCustomItemEty entity) {
        return UserCustomItemDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .category(entity.getCategory())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
