package com.url.shortner.agent.v2.api;
import com.url.shortner.agent.v2.service.WorkflowV2Service; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.security.Principal; import java.util.Map;
@RestController @RequestMapping("/api/v2/workflows") @RequiredArgsConstructor public class WorkflowV2Controller {
 private final WorkflowV2Service service;
 @PostMapping public ResponseEntity<Map<String,Object>> create(@Valid @RequestBody CreateWorkflowV2Request r,Principal p){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r,p.getName()));}
 @GetMapping("/metrics") public Map<String,Object> metrics(Principal p){return service.metrics(p.getName());}
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable String id,Principal p){return service.get(id,p.getName());}
 @PostMapping("/{id}/approve") public Map<String,Object> approve(@PathVariable String id,@RequestBody(required=false) Map<String,String> body,Principal p){return service.approve(id,p.getName(),body==null?"":body.getOrDefault("comment",""));}
 @PostMapping("/{id}/reject") public Map<String,Object> reject(@PathVariable String id,@RequestBody(required=false) Map<String,String> body,Principal p){return service.reject(id,p.getName(),body==null?"":body.getOrDefault("comment",""));}
 @PostMapping("/{id}/clarifications") public Map<String,Object> clarify(@PathVariable String id,@Valid @RequestBody DecisionV2Request r,Principal p){return service.clarify(id,p.getName(),r);}
 @PostMapping("/{id}/replans") public Map<String,Object> replan(@PathVariable String id,@Valid @RequestBody DecisionV2Request r,Principal p){return service.replan(id,p.getName(),r);}
}
