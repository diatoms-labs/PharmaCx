package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.api.dto.QuizSubmissionDto;
import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.domain.model.QuizAnswer;
import com.pharmaCx.dms.domain.model.TrainingAssignment;
import com.pharmaCx.dms.domain.model.TrainingQuestion;
import com.pharmaCx.dms.service.DocumentWorkflowService;
import com.pharmaCx.dms.service.TrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/training")
public class TrainingController {

    private final TrainingService trainingService;
    private final DocumentWorkflowService documentService;

    public TrainingController(TrainingService trainingService, DocumentWorkflowService documentService) {
        this.trainingService = trainingService;
        this.documentService = documentService;
    }

    @GetMapping("/my-assignments")
    public ResponseEntity<List<TrainingAssignment>> getMyAssignments() {
        return ResponseEntity.ok(trainingService.getMyAssignments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingAssignment> getById(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.getAssignment(id));
    }

    @PostMapping("/{id}/start-reading")
    public ResponseEntity<TrainingAssignment> startReading(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.startReading(id));
    }

    @PostMapping("/{id}/complete-reading")
    public ResponseEntity<TrainingAssignment> completeReading(@PathVariable String id, @RequestBody Map<String, Object> body) {
        long duration = body.containsKey("durationSeconds") ? ((Number) body.get("durationSeconds")).longValue() : 0L;
        return ResponseEntity.ok(trainingService.completeReading(id, duration));
    }

    @PostMapping("/{id}/submit-quiz")
    public ResponseEntity<TrainingAssignment> submitQuiz(@PathVariable String id, @RequestBody QuizSubmissionDto dto) {
        TrainingAssignment assignment = trainingService.getAssignment(id);
        ControlledDocument doc = documentService.getDocument(assignment.getDocumentId());
        List<TrainingQuestion> questions = doc.getTrainingQuestions();

        List<QuizAnswer> answers = dto.getAnswers().stream().map(a -> {
            QuizAnswer qa = new QuizAnswer();
            qa.setQuestionId(a.getQuestionId());
            qa.setSelectedAnswerIndex(a.getSelectedAnswerIndex());
            return qa;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(trainingService.submitQuiz(id, answers, questions));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<TrainingAssignment> acknowledge(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(trainingService.acknowledge(id, body.get("signatureData")));
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<TrainingAssignment>> getByDocument(@PathVariable String documentId) {
        return ResponseEntity.ok(trainingService.getByDocument(documentId));
    }

    // ═══════ Manager endpoints ═══════

    /**
     * Assign training for a published document to specific users.
     * Only managers and above can use this endpoint.
     */
    @PostMapping("/assign")
    public ResponseEntity<List<TrainingAssignment>> assignTraining(@RequestBody Map<String, Object> body) {
        String documentId = (String) body.get("documentId");
        @SuppressWarnings("unchecked")
        List<String> traineeUserIds = (List<String>) body.get("traineeUserIds");
        int dueDays = body.containsKey("dueDays") ? ((Number) body.get("dueDays")).intValue() : 14;

        return ResponseEntity.ok(trainingService.assignTraining(documentId, traineeUserIds, dueDays));
    }

    /**
     * Re-assign a FAILED training assignment so the trainee gets another set of attempts.
     */
    @PostMapping("/{id}/reassign")
    public ResponseEntity<TrainingAssignment> reassignTraining(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.reassignTraining(id));
    }

    /**
     * Returns published documents available for training assignment
     * in the current user's department.
     */
    @GetMapping("/manageable")
    public ResponseEntity<List<ControlledDocument>> getManageableDocuments() {
        return ResponseEntity.ok(trainingService.getManageableDocuments());
    }

    /**
     * Returns all assignments managed (assigned by) the current user.
     */
    @GetMapping("/managed")
    public ResponseEntity<List<TrainingAssignment>> getManagedAssignments() {
        return ResponseEntity.ok(trainingService.getMyManagedAssignments());
    }
}
