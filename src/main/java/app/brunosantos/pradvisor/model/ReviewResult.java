package app.brunosantos.pradvisor.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult {
    private String summary;
    private List<CodeSuggestion> suggestions;
    private int securityIssuesCount;
    private int bestPracticeIssuesCount;
    private int performanceIssuesCount;
}