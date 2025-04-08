package app.brunosantos.pradvisor.controller;

import app.brunosantos.pradvisor.model.PullRequest;
import app.brunosantos.pradvisor.model.ReviewResult;
import app.brunosantos.pradvisor.service.ClaudeMcpService;
import app.brunosantos.pradvisor.service.GitHubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/pr-review")
public class PrAdvisorController {

    private final GitHubService gitHubService;
    private final ClaudeMcpService claudeMcpService;
    
    public PrAdvisorController(GitHubService gitHubService, ClaudeMcpService claudeMcpService) {
        this.gitHubService = gitHubService;
        this.claudeMcpService = claudeMcpService;
    }
    
    @GetMapping("/{repoFullName}/{prNumber}")
    public ResponseEntity<ReviewResult> reviewPullRequest(
            @PathVariable String repoFullName,
            @PathVariable int prNumber) throws IOException {

        PullRequest pullRequest = gitHubService.getPullRequestDetails(repoFullName, prNumber);

        ReviewResult reviewResult = claudeMcpService.analyzePullRequest(pullRequest);
        
        return ResponseEntity.ok(reviewResult);
    }
}