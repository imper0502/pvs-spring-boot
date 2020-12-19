package pvs.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pvs.app.dto.GithubCommitDTO;
import pvs.app.dto.GithubIssueDTO;
import pvs.app.service.GithubCommitService;
import pvs.app.service.GithubApiService;
import pvs.app.service.GithubIssueService;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class GithubApiController {

    static final Logger logger = LogManager.getLogger(GithubApiController.class.getName());

    private final GithubApiService githubApiService;
    private final GithubCommitService githubCommitService;
    private final GithubIssueService githubIssueService;

    public GithubApiController(GithubApiService githubApiService, GithubCommitService githubCommitService, GithubIssueService githubIssueService) {
        this.githubApiService = githubApiService;
        this.githubCommitService = githubCommitService;
        this.githubIssueService = githubIssueService;
    }

    @PostMapping("/issues/{repoOwner}/{repoName}")
    public ResponseEntity<String> postIssues(@PathVariable("repoOwner") String repoOwner, @PathVariable("repoName") String repoName) throws IOException {
        JsonNode responseJson = githubApiService.getIssues(repoOwner, repoName);

        logger.debug("responseJson ========= ");
        logger.debug(responseJson);

        if(responseJson != null) {
            responseJson.forEach(entity -> {
                GithubIssueDTO githubIssueDTO = new GithubIssueDTO();
                githubIssueDTO.setRepoOwner(repoOwner);
                githubIssueDTO.setRepoName(repoName);
                githubIssueDTO.setCreatedAt(entity.get("createdAt"));
                githubIssueDTO.setClosedAt(entity.get("closedAt"));
                githubIssueService.save(githubIssueDTO);
            });

            return ResponseEntity.status(HttpStatus.OK)
                    .body("Saving issues to database complete");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Can't find repository");
    }

    @PostMapping("/commits/{repoOwner}/{repoName}")
    public ResponseEntity<String> postCommits(@PathVariable("repoOwner") String repoOwner, @PathVariable("repoName") String repoName) throws IOException {
        Date lastUpdate;
        GithubCommitDTO githubCommitDTO = githubCommitService.getLastCommit(repoOwner, repoName);
        logger.debug(githubCommitDTO.getRepoName());
        if (null == githubCommitDTO) {
            logger.debug("Noraaaaaaaaaaaaaaaaaaaaaaaaaa");
            Calendar calendar = Calendar.getInstance();
            calendar.set(1970, Calendar.JANUARY, 1);
            lastUpdate = calendar.getTime();
        } else {
            logger.debug("Oliviaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            lastUpdate = githubCommitDTO.getCommittedDate();
        }

        logger.debug(lastUpdate);

        try {
            githubApiService.getCommitsFromGithub(repoOwner, repoName, lastUpdate);
            return ResponseEntity.status(HttpStatus.OK).body("");
        } catch (InterruptedException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }
    }

    @GetMapping("/commits/{repoOwner}/{repoName}")
    public ResponseEntity<String> getCommits(@PathVariable("repoOwner") String repoOwner, @PathVariable("repoName") String repoName) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();

        List<GithubCommitDTO> githubCommitDTOs = githubCommitService.getAllCommits(repoOwner, repoName);

        String githubCommitDTOsJson = objectMapper.writeValueAsString(githubCommitDTOs);
        logger.debug(githubCommitDTOsJson);

        return ResponseEntity.status(HttpStatus.OK)
                .body(githubCommitDTOsJson);
    }

    @GetMapping("/issues/{repoOwner}/{repoName}")
    public ResponseEntity<String> getIssues(@PathVariable("repoOwner") String repoOwner, @PathVariable("repoName") String repoName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        List<GithubIssueDTO> githubIssueDTOs = githubIssueService.getAllIssues(repoOwner, repoName);

        String githubIssueDTOsJson = objectMapper.writeValueAsString(githubIssueDTOs);
        logger.debug(githubIssueDTOsJson);

        return ResponseEntity.status(HttpStatus.OK)
                .body(githubIssueDTOsJson);
    }
}
