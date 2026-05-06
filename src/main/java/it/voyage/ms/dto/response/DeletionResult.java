package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeletionResult {
    private int totalUsers;
    private int successCount;
    private int errorCount;
    private List<String> errors;
}