package com.url.shortner.agent;

import com.url.shortner.agent.api.ApprovalRequest;
import com.url.shortner.agent.api.CreateRunRequest;
import com.url.shortner.agent.api.DecisionRequest;
import com.url.shortner.agent.model.RunStatus;
import com.url.shortner.agent.model.TaskState;
import com.url.shortner.agent.service.AgenticWorkflowService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgenticWorkflowServiceTest {
    private final AgenticWorkflowService service = new AgenticWorkflowService();

    @Test
    void greenfieldRunWaitsForHumanApprovalThenCompletes() {
        var run = service.create(new CreateRunRequest("Build a URL shortener with click analytics", "GREENFIELD"));
        assertEquals(RunStatus.WAITING_FOR_APPROVAL, run.status());
        assertTrue(run.approvalRequired());
        assertEquals(TaskState.SUCCEEDED, run.tasks().get("design").state());
        assertEquals(TaskState.PENDING, run.tasks().get("implementation").state());

        var completed = service.approve(run.id(), "reviewer", new ApprovalRequest("Looks good"));
        assertEquals(RunStatus.COMPLETED, completed.status());
        assertEquals(TaskState.SUCCEEDED, completed.tasks().get("testing").state());
        assertEquals(TaskState.SUCCEEDED, completed.tasks().get("documentation").state());
        assertEquals(TaskState.SUCCEEDED, completed.tasks().get("release").state());
    }

    @Test
    void rejectionStopsDownstreamWork() {
        var run = service.create(new CreateRunRequest("Add expiration to short links", "BROWNFIELD"));
        var rejected = service.reject(run.id(), "reviewer", new ApprovalRequest("Risk not accepted"));
        assertEquals(RunStatus.REJECTED, rejected.status());
        assertEquals(TaskState.SKIPPED, rejected.tasks().get("implementation").state());
    }

    @Test
    void ambiguousRequirementWaitsForClarificationBeforeApproval() {
        var run = service.create(new CreateRunRequest("Shorten the supplied destination", "AMBIGUOUS"));
        assertTrue(run.decisions().containsKey("clarification"));
        assertEquals(RunStatus.WAITING_FOR_CLARIFICATION, run.status());
        var clarified = service.clarify(run.id(), new DecisionRequest("Default expiry is 90 days", "Product owner confirmed"));
        assertEquals(RunStatus.WAITING_FOR_APPROVAL, clarified.status());
    }

    @Test
    void cannotApproveCompletedRunTwice() {
        var run = service.create(new CreateRunRequest("Create short links", "GREENFIELD"));
        service.approve(run.id(), "reviewer", new ApprovalRequest("approved"));
        assertThrows(IllegalStateException.class,
                () -> service.approve(run.id(), "reviewer", new ApprovalRequest("approved again")));
    }

    @Test
    void replanInvalidatesOutputsAndReturnsToApprovalGate() {
        var run = service.create(new CreateRunRequest("Build short links", "GREENFIELD"));
        var replanned = service.replan(run.id(), new DecisionRequest("Add expiry", "New product requirement"));
        assertEquals(RunStatus.WAITING_FOR_APPROVAL, replanned.status());
        assertTrue(replanned.auditTrail().stream().anyMatch(e -> e.action().equals("REPLAN_REQUESTED")));
    }
}
