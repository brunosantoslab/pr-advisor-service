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
public class PullRequest {
    private int id;
    private String title;
    private String description;
    private String authorName;
    private String baseBranch;
    private String headBranch;
    private List<FileChange> fileChanges;
}