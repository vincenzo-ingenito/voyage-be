package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Friend {
    private String id;
    private String name;
//    private String bio;
    private String avatar;  
 
}