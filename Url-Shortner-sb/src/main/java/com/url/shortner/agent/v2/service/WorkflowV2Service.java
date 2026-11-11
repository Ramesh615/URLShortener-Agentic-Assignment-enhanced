package com.url.shortner.agent.v2.service;

import com.fasterxml.jackson.core.type.TypeReference; import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.shortner.agent.v2.agent.*; import com.url.shortner.agent.v2.api.*; import com.url.shortner.agent.v2.domain.*; import com.url.shortner.agent.v2.persistence.*;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.time.Instant; import java.util.*;

@Service public class WorkflowV2Service {
 private final WorkflowRunRepository runs; private final WorkflowTaskRepository tasks; private final WorkflowAuditRepository audit; private final AgentProvider agents; private final ObjectMapper mapper;
 private static final Map<String,List<String>> GRAPH=Map.ofEntries(
  Map.entry("requirements",List.of()), Map.entry("planning",List.of("requirements")), Map.entry("codebase",List.of("planning")),
  Map.entry("architecture",List.of("codebase")), Map.entry("implementation",List.of("architecture")),
  Map.entry("testing",List.of("implementation")), Map.entry("security",List.of("implementation")),
  Map.entry("documentation",List.of("implementation")), Map.entry("release",List.of("testing","security","documentation")));
 private static final Map<String,AgentType> TYPES=Map.of("requirements",AgentType.REQUIREMENTS,"planning",AgentType.PLANNER,"codebase",AgentType.CODEBASE_ANALYSIS,"architecture",AgentType.ARCHITECTURE,"implementation",AgentType.IMPLEMENTATION,"testing",AgentType.TESTING,"security",AgentType.SECURITY,"documentation",AgentType.DOCUMENTATION,"release",AgentType.RELEASE_READINESS);
 public WorkflowV2Service(WorkflowRunRepository r,WorkflowTaskRepository t,WorkflowAuditRepository a,AgentProvider p,ObjectMapper m){runs=r;tasks=t;audit=a;agents=p;mapper=m;}
 @Transactional public Map<String,Object> create(CreateWorkflowV2Request req,String owner){
  WorkflowRunEntity run=new WorkflowRunEntity(); run.id=UUID.randomUUID().toString();run.scenario=req.scenario().toUpperCase(Locale.ROOT);run.requirementText=req.requirement().trim();run.ownerId=owner;run.status=WorkflowStatus.PLANNING.name();run.createdAt=Instant.now();run.updatedAt=run.createdAt;runs.save(run);
  GRAPH.forEach((key,deps)->{WorkflowTaskEntity t=new WorkflowTaskEntity();t.id=UUID.randomUUID().toString();t.workflowId=run.id;t.taskKey=key;t.agentType=TYPES.get(key).name();t.dependenciesJson=write(deps);t.state=TaskStatus.PENDING.name();t.maxAttempts=3;tasks.save(t);});
  record(run.id,owner,"WORKFLOW_CREATED","Scenario="+run.scenario); executePreApproval(run); return snapshot(run.id,owner);
 }
 @Transactional public Map<String,Object> get(String id,String owner){return snapshot(id,owner);}
 @Transactional public Map<String,Object> approve(String id,String reviewer,String comment){WorkflowRunEntity r=owned(id,reviewer);if(!WorkflowStatus.WAITING_FOR_APPROVAL.name().equals(r.status))throw new IllegalStateException("Workflow is not awaiting approval");
  if(r.approvalHash==null||!r.approvalHash.equals(approvalScopeHash(r)))throw new IllegalStateException("Approval scope changed; re-approval required");
  record(id,reviewer,"IMPLEMENTATION_APPROVED",comment);r.status=WorkflowStatus.RUNNING.name();r.updatedAt=Instant.now();runs.save(r);executePostApproval(r);return snapshot(id,reviewer);}
 @Transactional public Map<String,Object> reject(String id,String reviewer,String comment){WorkflowRunEntity r=owned(id,reviewer);if(!WorkflowStatus.WAITING_FOR_APPROVAL.name().equals(r.status))throw new IllegalStateException("Workflow is not awaiting approval");r.status=WorkflowStatus.REJECTED.name();r.updatedAt=Instant.now();runs.save(r);record(id,reviewer,"APPROVAL_REJECTED",comment);return snapshot(id,reviewer);}
 @Transactional public Map<String,Object> replan(String id,String owner,DecisionV2Request req){WorkflowRunEntity r=owned(id,owner);if(Set.of("COMPLETED","REJECTED","CANCELLED").contains(r.status))throw new IllegalStateException("Terminal workflow cannot be replanned");
  r.requirementText=req.decision();r.requirementVersion++;r.status=WorkflowStatus.PLANNING.name();r.approvalHash=null;r.updatedAt=Instant.now();runs.save(r);
  // Requirement change invalidates every generated artifact in this initial safe policy. A future impact analyzer can narrow this set.
  for(WorkflowTaskEntity t:tasks.findByWorkflowIdOrderByTaskKey(id)){t.state=TaskStatus.PENDING.name();t.outputText=null;t.attempts=0;t.startedAt=null;t.completedAt=null;tasks.save(t);}record(id,owner,"REPLAN_INVALIDATED_ALL",Objects.toString(req.rationale(),""));executePreApproval(r);return snapshot(id,owner);}
 private void executePreApproval(WorkflowRunEntity r){
  try{
   runPreApprovalArtifacts(r);
   if("AMBIGUOUS".equals(r.scenario)){
    r.status=WorkflowStatus.WAITING_FOR_CLARIFICATION.name();
    record(r.id,"policy","CLARIFICATION_REQUIRED","Confirm expiry defaults, analytics dimensions and retention before implementation.");
   }else waitForApproval(r);
  }catch(RuntimeException ex){
   r.status=WorkflowStatus.SAFE_STOP.name();
   record(r.id,"policy","SAFE_STOP","Pre-approval task failure: "+safeAuditMessage(ex));
  }
  r.updatedAt=Instant.now();runs.save(r);
 }
 private void runPreApprovalArtifacts(WorkflowRunEntity r){
  runTask(r,"requirements");runTask(r,"planning");runTask(r,"codebase");runTask(r,"architecture");
 }

 @Transactional public Map<String,Object> clarify(String id,String owner,DecisionV2Request req){
  WorkflowRunEntity r=owned(id,owner);
  if(!WorkflowStatus.WAITING_FOR_CLARIFICATION.name().equals(r.status))throw new IllegalStateException("Clarification is not pending");
  r.requirementText += "\nClarification: "+req.decision()+"\nRationale: "+Objects.toString(req.rationale(),"");
  r.requirementVersion++;
  record(id,owner,"CLARIFICATION_RECORDED",req.decision());
  // Clarification changes upstream context; regenerate every pre-approval artifact before hashing approval scope.
  for(String key:List.of("requirements","planning","codebase","architecture")){
   WorkflowTaskEntity task=tasks.findByWorkflowIdAndTaskKey(id,key).orElseThrow();
   task.state=TaskStatus.PENDING.name();task.outputText=null;task.attempts=0;task.startedAt=null;task.completedAt=null;tasks.save(task);
  }
  try{runPreApprovalArtifacts(r);waitForApproval(r);}
  catch(RuntimeException ex){r.status=WorkflowStatus.SAFE_STOP.name();record(id,"policy","SAFE_STOP","Clarification re-execution failed: "+safeAuditMessage(ex));}
  r.updatedAt=Instant.now();runs.save(r);return snapshot(id,owner);
 }
 private void waitForApproval(WorkflowRunEntity r){r.status=WorkflowStatus.WAITING_FOR_APPROVAL.name();r.approvalHash=approvalScopeHash(r);record(r.id,"policy","APPROVAL_GATE","Human approval required before code changes.");}
 private void executePostApproval(WorkflowRunEntity r){try{runTask(r,"implementation"); // independent validation/documentation branches; each persists its own output
  runTask(r,"testing");runTask(r,"security");runTask(r,"documentation");runTask(r,"release");r.status=WorkflowStatus.COMPLETED.name();record(r.id,"orchestrator","WORKFLOW_COMPLETED","All required validation branches succeeded; deployment was not performed.");
 }catch(RuntimeException ex){r.status=WorkflowStatus.SAFE_STOP.name();record(r.id,"policy","SAFE_STOP",safeAuditMessage(ex));}r.updatedAt=Instant.now();runs.save(r);}
 private void runTask(WorkflowRunEntity r,String key){
  WorkflowTaskEntity t=tasks.findByWorkflowIdAndTaskKey(r.id,key).orElseThrow();
  List<String> deps=read(t.dependenciesJson);
  Map<String,String> context=new LinkedHashMap<>();
  for(String dependency:deps){
   WorkflowTaskEntity upstream=tasks.findByWorkflowIdAndTaskKey(r.id,dependency).orElseThrow();
   if(!TaskStatus.SUCCEEDED.name().equals(upstream.state)) throw new IllegalStateException("Dependency not satisfied: "+dependency);
   context.put(dependency,upstream.outputText);
  }
  RuntimeException lastFailure=null;
  for(int attempt=1;attempt<=t.maxAttempts;attempt++){
   t.state=TaskStatus.RUNNING.name();t.startedAt=Instant.now();t.attempts++;tasks.save(t);
   record(r.id,"agent:"+key,"TASK_STARTED","Attempt="+t.attempts);
   try{
    AgentResult result=agents.invoke(TYPES.get(key),new AgentRequest(r.id,r.requirementText,r.requirementVersion,key,Map.copyOf(context)));
    if(result==null||result.content()==null||result.content().isBlank()) throw new IllegalStateException("Agent returned an empty result");
    if(result.needsHumanClarification()) throw new IllegalStateException("Agent requested clarification: "+Objects.toString(result.clarificationQuestion(),"unspecified"));
    t.outputText=result.content();t.state=TaskStatus.SUCCEEDED.name();t.completedAt=Instant.now();tasks.save(t);
    record(r.id,"agent:"+key,"TASK_SUCCEEDED","Output persisted after attempt "+t.attempts);return;
   }catch(RuntimeException ex){
    lastFailure=ex;t.state=attempt<t.maxAttempts?TaskStatus.RETRY_WAIT.name():TaskStatus.FAILED.name();tasks.save(t);
    record(r.id,"agent:"+key,"TASK_ATTEMPT_FAILED","Attempt "+t.attempts+" of "+t.maxAttempts+" failed: "+safeAuditMessage(ex));
   }
  }
  throw new IllegalStateException("Task "+key+" failed after "+t.attempts+" bounded attempts",lastFailure);
 }
 private String safeAuditMessage(RuntimeException ex){
  String message=Objects.toString(ex.getMessage(),"No error detail");
  return message.length()>500?message.substring(0,500):message;
 }
 private String approvalScopeHash(WorkflowRunEntity run){
  StringBuilder scope=new StringBuilder(run.requirementText).append('|').append(run.requirementVersion);
  for(String key:List.of("requirements","planning","codebase","architecture")){
   WorkflowTaskEntity task=tasks.findByWorkflowIdAndTaskKey(run.id,key).orElseThrow();
   scope.append('|').append(key).append(':').append(Objects.toString(task.outputText,""));
  }
  return hash(scope.toString());
 }
 @Transactional(readOnly=true) public Map<String,Object> metrics(String owner){
  long total=runs.countByOwnerId(owner);
  long completed=runs.countByOwnerIdAndStatus(owner,WorkflowStatus.COMPLETED.name());
  long failed=runs.countByOwnerIdAndStatus(owner,WorkflowStatus.SAFE_STOP.name());
  long rejected=runs.countByOwnerIdAndStatus(owner,WorkflowStatus.REJECTED.name());
  long active=total-completed-failed-rejected;
  double successRate=total==0?0.0:(completed*100.0/total);
  long retryEvents=audit.countEventsByOwnerAndType(owner,"TASK_ATTEMPT_FAILED");
  long safeStopEvents=audit.countEventsByOwnerAndType(owner,"SAFE_STOP");
  return Map.of("totalRuns",total,"completedRuns",completed,"safeStoppedRuns",failed,
    "rejectedRuns",rejected,"activeOrAwaitingRuns",active,"retryEvents",retryEvents,
    "safeStopEvents",safeStopEvents,"successRatePercent",Math.round(successRate*100.0)/100.0);
 }
 private Map<String,Object> snapshot(String id,String owner){WorkflowRunEntity r=owned(id,owner);List<Map<String,Object>> taskViews=tasks.findByWorkflowIdOrderByTaskKey(id).stream().map(t->Map.<String,Object>of("id",t.id,"key",t.taskKey,"agent",t.agentType,"dependencies",read(t.dependenciesJson),"state",t.state,"attempts",t.attempts,"output",Objects.toString(t.outputText,""))).toList();List<Map<String,Object>> events=audit.findByWorkflowIdOrderByOccurredAtAsc(id).stream().map(e->Map.<String,Object>of("actor",e.actor,"type",e.eventType,"details",Objects.toString(e.details,""),"at",e.occurredAt.toString())).toList();return Map.of("id",r.id,"scenario",r.scenario,"requirement",r.requirementText,"requirementVersion",r.requirementVersion,"status",r.status,"tasks",taskViews,"audit",events,"createdAt",r.createdAt.toString(),"updatedAt",r.updatedAt.toString());}
 private WorkflowRunEntity owned(String id,String owner){return runs.findByIdAndOwnerId(id,owner).orElseThrow(()->new NoSuchElementException("Workflow not found"));}
 private void record(String id,String actor,String type,String details){WorkflowAuditEntity e=new WorkflowAuditEntity();e.workflowId=id;e.actor=actor;e.eventType=type;e.details=details;e.occurredAt=Instant.now();audit.save(e);}
 private String write(Object o){try{return mapper.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
 private List<String> read(String s){try{return mapper.readValue(s,new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}}
 private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
