package app.brunosantos.pradvisor.service;

import app.brunosantos.pradvisor.model.CodeSuggestion;
import app.brunosantos.pradvisor.model.FileChange;
import app.brunosantos.pradvisor.model.PullRequest;
import app.brunosantos.pradvisor.model.ReviewResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.ArrayList;

@Service
public class ClaudeMcpService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String claudeDesktopUrl;

    public ClaudeMcpService(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        @Value("${claude.desktop.url}") String claudeDesktopUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.claudeDesktopUrl = claudeDesktopUrl;
    }

    public ReviewResult analyzePullRequest(PullRequest pullRequest) {
        try {
            // Build the prompt and MCP configuration for Claude
            var mcpRequest = createMcpRequest(pullRequest);

            // Send request to Claude Desktop
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            var request = new HttpEntity<>(mcpRequest, headers);

            var mcpResponse = restTemplate.postForObject(claudeDesktopUrl + "/mcp", request, String.class);

            // Process the response
            return parseReviewResponse(mcpResponse);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze PR with Claude", e);
        }
    }

    private ObjectNode createMcpRequest(PullRequest pullRequest) {
        var mcpRequest = objectMapper.createObjectNode();

        // Configure MCP with a custom tool for code analysis
        var mcpConfig = objectMapper.createObjectNode();
        var tools = objectMapper.createArrayNode();

        // Tool for PR analysis
        var prAnalysisTool = objectMapper.createObjectNode();
        prAnalysisTool.put("name", "pr_advisor");
        prAnalysisTool.put("description", "Analyzes Pull Requests and provides recommendations");

        // Parameters for the tool
        var parameters = objectMapper.createObjectNode();

        // PR details
        var prDetails = objectMapper.createObjectNode();
        prDetails.put("id", pullRequest.getId());
        prDetails.put("title", pullRequest.getTitle());
        prDetails.put("description", pullRequest.getDescription());
        prDetails.put("author", pullRequest.getAuthorName());
        prDetails.put("baseBranch", pullRequest.getBaseBranch());
        prDetails.put("headBranch", pullRequest.getHeadBranch());

        // Changed files
        var fileChanges = objectMapper.createArrayNode();
        for (var change : pullRequest.getFileChanges()) {
            var fileNode = objectMapper.createObjectNode();
            fileNode.put("filename", change.getFilename());
            fileNode.put("status", change.getStatus());
            fileNode.put("additions", change.getAdditions());
            fileNode.put("deletions", change.getDeletions());
            fileNode.put("patch", change.getPatch());
            fileNode.put("baseContent", change.getBaseContent());
            fileNode.put("headContent", change.getHeadContent());
            fileChanges.add(fileNode);
        }

        parameters.set("pr", prDetails);
        parameters.set("files", fileChanges);
        prAnalysisTool.set("parameters", parameters);

        tools.add(prAnalysisTool);
        mcpConfig.set("tools", tools);

        // Main prompt for Claude using Text Block
        String prompt = """
            You are a code review expert. Please analyze this Pull Request \
            and provide a concise summary, identify potential issues, and suggest improvements. \
            Focus on security issues, best practices, potential bugs, and optimization opportunities. \
            Organize your response in JSON format with the following sections: \
            1. summary: A general summary of the PR \
            2. suggestions: A list of specific suggestions with filename, lineNumber, suggestion, explanation, severity, and type \
            3. securityIssuesCount: The number of security issues found \
            4. bestPracticeIssuesCount: The number of best practice issues \
            5. performanceIssuesCount: The number of performance issues
            """;

        mcpRequest.put("prompt", prompt);
        mcpRequest.set("mcp", mcpConfig);

        return mcpRequest;
    }

    private ReviewResult parseReviewResponse(String mcpResponse) {
        try {
            var responseJson = objectMapper.readValue(mcpResponse, ObjectNode.class);

            // Extract fields from the response JSON
            var summary = responseJson.path("summary").asText();
            var securityIssuesCount = responseJson.path("securityIssuesCount").asInt();
            var bestPracticeIssuesCount = responseJson.path("bestPracticeIssuesCount").asInt();
            var performanceIssuesCount = responseJson.path("performanceIssuesCount").asInt();

            // Process suggestions using streams
            var suggestionsNode = (ArrayNode) responseJson.path("suggestions");
            var suggestions = new ArrayList<CodeSuggestion>(suggestionsNode.size());

            for (var i = 0; i < suggestionsNode.size(); i++) {
                var suggestionNode = (ObjectNode) suggestionsNode.get(i);

                var suggestion = CodeSuggestion.builder()
                    .filename(suggestionNode.path("filename").asText())
                    .lineNumber(suggestionNode.path("lineNumber").asInt())
                    .suggestion(suggestionNode.path("suggestion").asText())
                    .explanation(suggestionNode.path("explanation").asText())
                    .severity(suggestionNode.path("severity").asText())
                    .type(suggestionNode.path("type").asText())
                    .build();

                suggestions.add(suggestion);
            }

            return ReviewResult.builder()
                .summary(summary)
                .suggestions(suggestions)
                .securityIssuesCount(securityIssuesCount)
                .bestPracticeIssuesCount(bestPracticeIssuesCount)
                .performanceIssuesCount(performanceIssuesCount)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process Claude's response", e);
        }
    }
}