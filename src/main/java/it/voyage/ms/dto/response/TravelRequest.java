package it.voyage.ms.dto.response;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class TravelRequest {

    private TravelDTO travelData; 
    
    private List<MultipartFile> files;
}
