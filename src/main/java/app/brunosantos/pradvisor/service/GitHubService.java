package app.brunosantos.pradvisor.service;

import app.brunosantos.pradvisor.model.FileChange;
import app.brunosantos.pradvisor.model.PullRequest;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

@Service
public class GitHubService {
    
    private final GitHub gitHub;
    
    public GitHubService(@Value("${github.token}") String token) throws IOException {
        this.gitHub = new GitHubBuilder().withOAuthToken(token).build();
    }
    
    public PullRequest getPullRequestDetails(String repoFullName, int prNumber) throws IOException {
        GHRepository repository = gitHub.getRepository(repoFullName);
        GHPullRequest ghPullRequest = repository.getPullRequest(prNumber);
        
        return PullRequest.builder()
                .id(ghPullRequest.getNumber())
                .title(ghPullRequest.getTitle())
                .description(ghPullRequest.getBody())
                .authorName(ghPullRequest.getUser().getLogin())
                .baseBranch(ghPullRequest.getBase().getRef())
                .headBranch(ghPullRequest.getHead().getRef())
                .fileChanges(getFileChanges(ghPullRequest))
                .build();
    }
    
    private List<FileChange> getFileChanges(GHPullRequest pullRequest) throws IOException {
        List<FileChange> fileChanges = new ArrayList<>();
        
        for (GHPullRequestFileDetail file : pullRequest.listFiles()) {
            GHRepository repo = pullRequest.getRepository();
            String baseContent = "";
            String headContent = "";
            
            // Get file content before changes if it's not a new file
            if (!"added".equals(file.getStatus())) {
                try {
                    GHContent baseFileContent = repo.getFileContent(
                            file.getFilename(), 
                            pullRequest.getBase().getSha());
                    baseContent = new String(Base64.getDecoder().decode(baseFileContent.getContent()));
                } catch (Exception e) {
                    // File might not exist in base
                }
            }
            
            // Get current file content if it's not deleted
            if (!"removed".equals(file.getStatus())) {
                try {
                    GHContent headFileContent = repo.getFileContent(
                            file.getFilename(), 
                            pullRequest.getHead().getSha());
                    headContent = new String(Base64.getDecoder().decode(headFileContent.getContent()));
                } catch (Exception e) {
                    // File might have issues
                }
            }
            
            fileChanges.add(FileChange.builder()
                    .filename(file.getFilename())
                    .status(file.getStatus())
                    .additions(file.getAdditions())
                    .deletions(file.getDeletions())
                    .patch(file.getPatch())
                    .baseContent(baseContent)
                    .headContent(headContent)
                    .build());
        }
        
        return fileChanges;
    }
}