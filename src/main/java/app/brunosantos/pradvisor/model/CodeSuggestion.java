package app.brunosantos.pradvisor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSuggestion {
    private String filename;
    private int lineNumber;
    private String suggestion;
    private String explanation;
    private String severity; // HIGH, MEDIUM, LOW
    private String type; // SECURITY, PERFORMANCE, BEST_PRACTICE, LOGIC
}