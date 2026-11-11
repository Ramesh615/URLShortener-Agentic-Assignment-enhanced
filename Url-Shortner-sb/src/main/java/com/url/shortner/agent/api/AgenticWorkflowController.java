package com.url.shortner.agent.api;

import com.url.shortner.agent.model.WorkflowRun;
import com.url.shortner.agent.service.AgenticWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.security.Principal;

@RestController
@RequestMapping("/api/agentic")
@RequiredArgsConstructor
public class AgenticWorkflowController {
    private final AgenticWorkflowService workflowService;

    @PostMapping("/runs")
    public ResponseEntity<WorkflowRun> create(@Valid @RequestBody CreateRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.create(request));
    }
    @GetMapping("/runs/{runId}")
    public WorkflowRun get(@PathVariable String runId) { return workflowService.get(runId); }
    @PostMapping("/runs/{runId}/approve")
    public WorkflowRun approve(@PathVariable String runId, @RequestBody ApprovalRequest request, Principal principal) {
        return workflowService.approve(runId, principal.getName(), request);
    }
    @PostMapping("/runs/{runId}/clarify")
    public WorkflowRun clarify(@PathVariable String runId, @Valid @RequestBody DecisionRequest request) {
        return workflowService.clarify(runId, request);
    }
    @PostMapping("/runs/{runId}/reject")
    public WorkflowRun reject(@PathVariable String runId, @RequestBody ApprovalRequest request, Principal principal) {
        return workflowService.reject(runId, principal.getName(), request);
    }
    @PostMapping("/runs/{runId}/replan")
    public WorkflowRun replan(@PathVariable String runId, @Valid @RequestBody DecisionRequest request) {
        return workflowService.replan(runId, request);
    }
    @GetMapping("/metrics")
    public Map<String,Object> metrics() { return workflowService.metrics(); }
}
