package app.brunosantos.pradvisor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChange {
    private String filename;
    private String status; // added, modified, removed
    private int additions;
    private int deletions;
    private String patch;
    private String baseContent;
    private String headContent;
}