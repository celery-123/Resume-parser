package com.example.model;

import lombok.Data;
import java.util.List;

@Data
public class MatchResult {
    private ParsedResume resume;
    private List<JobMatch> jobMatches;
    private String analysis;
    private String algorithmUsed;
    private long processingTimeMs;
}