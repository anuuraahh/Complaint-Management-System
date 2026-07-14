package com.examly.springapp.controller;

import com.examly.springapp.dto.AssignRequest;
import com.examly.springapp.dto.AttachmentResponse;
import com.examly.springapp.dto.ComplaintRequest;
import com.examly.springapp.dto.ComplaintResponse;
import com.examly.springapp.dto.ComplaintUpdateRequest;
import com.examly.springapp.dto.ComplaintUpdateResponse;
import com.examly.springapp.model.StatusUpdateRequest;
import com.examly.springapp.service.AttachmentService;
import com.examly.springapp.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final AttachmentService attachmentService;

    // FR2: Submit new complaint
    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody ComplaintRequest request,
            Authentication authentication) {

        return new ResponseEntity<>(
                complaintService.createComplaint(request, authentication.getName()),
                HttpStatus.CREATED);
    }

    // FR3.1: Admin — get all complaints
    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // FR6: Get complaint by ID
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                complaintService.getComplaintById(id, authentication.getName()));
    }

    // FR6.1: Citizen/Employee — view own submitted complaints
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
            Authentication authentication) {

        return ResponseEntity.ok(
                complaintService.getMyComplaints(authentication.getName()));
    }

    // FR5.1: Employee — view assigned complaints
    @GetMapping("/assigned")
    public ResponseEntity<List<ComplaintResponse>> getAssignedComplaints(
            Authentication authentication) {

        return ResponseEntity.ok(
                complaintService.getAssignedComplaints(authentication.getName()));
    }

    // FR7: Filter by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ComplaintResponse>> getByStatus(
            @PathVariable String status) {

        return ResponseEntity.ok(complaintService.getByStatus(status));
    }

    // FR7: Filter by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ComplaintResponse>> getByCategory(
            @PathVariable String category) {

        return ResponseEntity.ok(complaintService.getByCategory(category));
    }

    // FR7.1: Search by keyword (title or complainant name)
    @GetMapping("/search")
    public ResponseEntity<List<ComplaintResponse>> search(
            @RequestParam String keyword) {

        return ResponseEntity.ok(complaintService.search(keyword));
    }

    // FR4: Admin assigns complaint to employee
    @PutMapping("/{id}/assign")
    public ResponseEntity<ComplaintResponse> assignComplaint(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                complaintService.assignComplaint(id, request, authentication.getName()));
    }

    // FR5.2: Update complaint status
    @PutMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                complaintService.updateStatus(id, request, authentication.getName()));
    }

    // FR3.3: Admin edits complaint details
    @PutMapping("/{id}")
    public ResponseEntity<ComplaintResponse> editComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                complaintService.editComplaint(id, request, authentication.getName()));
    }

    // FR3.4: Admin deletes complaint
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long id,
            Authentication authentication) {

        complaintService.deleteComplaint(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // FR5.3: Add comment/note to complaint
    @PostMapping("/{id}/updates")
    public ResponseEntity<ComplaintUpdateResponse> addUpdate(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintUpdateRequest request,
            Authentication authentication) {

        return new ResponseEntity<>(
                complaintService.addUpdate(id, request, authentication.getName()),
                HttpStatus.CREATED);
    }

    // FR6.3: View audit trail for a complaint
    @GetMapping("/{id}/updates")
    public ResponseEntity<List<ComplaintUpdateResponse>> getUpdates(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                complaintService.getUpdates(id, authentication.getName()));
    }

    // FR2.3: Upload attachment
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        return new ResponseEntity<>(attachmentService.upload(id, file), HttpStatus.CREATED);
    }

    // FR2.3: List attachments
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(@PathVariable Long id) {
        return ResponseEntity.ok(attachmentService.getByComplaint(id));
    }
}
