package com.examly.springapp.service;

import com.examly.springapp.dto.AssignRequest;
import com.examly.springapp.dto.ComplaintRequest;
import com.examly.springapp.dto.ComplaintResponse;
import com.examly.springapp.dto.ComplaintUpdateRequest;
import com.examly.springapp.dto.ComplaintUpdateResponse;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Complaint;
import com.examly.springapp.model.ComplaintPriority;
import com.examly.springapp.model.ComplaintStatus;
import com.examly.springapp.model.ComplaintUpdate;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.StatusUpdateRequest;
import com.examly.springapp.model.UpdateVisibility;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.ComplaintRepository;
import com.examly.springapp.repository.ComplaintUpdateRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintUpdateRepository complaintUpdateRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ── FR2: Submit complaint ────────────────────────────────────────────────
    @Transactional
    public ComplaintResponse createComplaint(ComplaintRequest request, String submitterEmail) {
        User complainant = findUserByEmail(submitterEmail);

        ComplaintPriority priority = ComplaintPriority.MEDIUM;
        if (request.getPriority() != null && ComplaintPriority.isValid(request.getPriority())) {
            priority = ComplaintPriority.valueOf(request.getPriority().toUpperCase());
        }

        Complaint complaint = Complaint.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .status(ComplaintStatus.PENDING)
                .priority(priority)
                .complainant(complainant)
                .submittedAt(LocalDateTime.now())
                .build();

        return toResponse(complaintRepository.save(complaint));
    }

    // ── FR3.1: Admin — view all complaints ──────────────────────────────────
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAll().stream().map(this::toResponse).toList();
    }

    // ── FR6: Get complaint by ID (owner / assigned employee / admin) ─────────
    public ComplaintResponse getComplaintById(Long id, String requesterEmail) {
        Complaint complaint = findComplaintById(id);
        User requester = findUserByEmail(requesterEmail);
        assertCanView(complaint, requester);
        return toResponse(complaint);
    }

    // ── FR6.1: Citizen/Employee — view own submitted complaints ─────────────
    public List<ComplaintResponse> getMyComplaints(String requesterEmail) {
        User requester = findUserByEmail(requesterEmail);
        return complaintRepository.findByComplainantId(requester.getId())
                .stream().map(this::toResponse).toList();
    }

    // ── FR5.1: Employee — view assigned complaints ───────────────────────────
    public List<ComplaintResponse> getAssignedComplaints(String employeeEmail) {
        User employee = findUserByEmail(employeeEmail);
        return complaintRepository.findByAssignedEmployeeId(employee.getId())
                .stream().map(this::toResponse).toList();
    }

    // ── FR7: Filter by status ────────────────────────────────────────────────
    public List<ComplaintResponse> getByStatus(String statusStr) {
        if (!ComplaintStatus.isValid(statusStr)) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }
        return complaintRepository.findByStatus(ComplaintStatus.valueOf(statusStr.toUpperCase()))
                .stream().map(this::toResponse).toList();
    }

    // ── FR7: Filter by category ──────────────────────────────────────────────
    public List<ComplaintResponse> getByCategory(String category) {
        return complaintRepository.findByCategoryIgnoreCase(category)
                .stream().map(this::toResponse).toList();
    }

    // ── FR7.1: Search by keyword (title or complainant name) ─────────────────
    public List<ComplaintResponse> search(String keyword) {
        return complaintRepository.searchByKeyword(keyword)
                .stream().map(this::toResponse).toList();
    }

    // ── FR4: Admin assigns complaint to employee ─────────────────────────────
    @Transactional
    public ComplaintResponse assignComplaint(Long id, AssignRequest request, String adminEmail) {
        Complaint complaint = findComplaintById(id);
        User admin = findUserByEmail(adminEmail);

        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can assign complaints");
        }

        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + request.getEmployeeId()));

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException("Target user is not an EMPLOYEE");
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setAssignedEmployee(employee);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        Complaint saved = complaintRepository.save(complaint);

        // Audit trail entry (FR5.4)
        complaintUpdateRepository.save(ComplaintUpdate.builder()
                .complaint(saved)
                .user(admin)
                .comment("Complaint assigned to " + employee.getName())
                .statusChangeFrom(previousStatus.name())
                .statusChangeTo(ComplaintStatus.ASSIGNED.name())
                .visibility(UpdateVisibility.INTERNAL)
                .build());

        // FR9: notify assigned employee
        notificationService.createNotification(employee,
                "You have been assigned complaint #" + saved.getId() + ": " + saved.getTitle());

        return toResponse(saved);
    }

    // ── FR5.2 / status update endpoint ──────────────────────────────────────
    @Transactional
    public ComplaintResponse updateStatus(Long id, StatusUpdateRequest request, String requesterEmail) {
        if (!ComplaintStatus.isValid(request.getStatus())) {
            throw new IllegalArgumentException("Invalid status: " + request.getStatus());
        }

        Complaint complaint = findComplaintById(id);
        User requester = findUserByEmail(requesterEmail);

        boolean isAdmin = requester.getRole() == Role.ADMIN;
        boolean isAssignedEmployee = complaint.getAssignedEmployee() != null
                && complaint.getAssignedEmployee().getId().equals(requester.getId());

        if (!isAdmin && !isAssignedEmployee) {
            throw new AccessDeniedException("You do not have permission to update this complaint's status");
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        ComplaintStatus newStatus = ComplaintStatus.valueOf(request.getStatus().toUpperCase());
        complaint.setStatus(newStatus);

        if (request.getAssignedTo() != null) {
            complaint.setAssignedTo(request.getAssignedTo());
        }
        if (request.getResolutionComments() != null) {
            complaint.setResolutionComments(request.getResolutionComments());
        }
        if (newStatus == ComplaintStatus.RESOLVED || newStatus == ComplaintStatus.CLOSED) {
            complaint.setResolvedAt(LocalDateTime.now());
            complaint.setResolvedDate(LocalDateTime.now());
        }

        Complaint saved = complaintRepository.save(complaint);

        // Audit trail entry (FR5.4)
        if (!previousStatus.equals(newStatus)) {
            complaintUpdateRepository.save(ComplaintUpdate.builder()
                    .complaint(saved)
                    .user(requester)
                    .comment(request.getResolutionComments())
                    .statusChangeFrom(previousStatus.name())
                    .statusChangeTo(newStatus.name())
                    .visibility(UpdateVisibility.PUBLIC)
                    .build());

            // FR9: notify complainant of status change
            notificationService.createNotification(saved.getComplainant(),
                    "Your complaint #" + saved.getId() + " status changed to " + newStatus.name());
        }

        return toResponse(saved);
    }

    // ── FR3.3: Admin edits complaint details ─────────────────────────────────
    @Transactional
    public ComplaintResponse editComplaint(Long id, ComplaintRequest request, String adminEmail) {
        Complaint complaint = findComplaintById(id);
        User admin = findUserByEmail(adminEmail);

        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can edit complaint details");
        }

        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setCategory(request.getCategory());

        if (request.getPriority() != null && ComplaintPriority.isValid(request.getPriority())) {
            complaint.setPriority(ComplaintPriority.valueOf(request.getPriority().toUpperCase()));
        }

        return toResponse(complaintRepository.save(complaint));
    }

    // ── FR3.4: Admin deletes complaint ───────────────────────────────────────
    @Transactional
    public void deleteComplaint(Long id, String adminEmail) {
        Complaint complaint = findComplaintById(id);
        User admin = findUserByEmail(adminEmail);

        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can delete complaints");
        }

        complaintUpdateRepository.deleteAll(
                complaintUpdateRepository.findByComplaintIdOrderByTimestampAsc(id));
        complaintRepository.delete(complaint);
    }

    // ── FR5.3: Add comment/note to complaint ─────────────────────────────────
    @Transactional
    public ComplaintUpdateResponse addUpdate(Long complaintId,
                                             ComplaintUpdateRequest request,
                                             String requesterEmail) {
        Complaint complaint = findComplaintById(complaintId);
        User requester = findUserByEmail(requesterEmail);

        boolean isAdmin = requester.getRole() == Role.ADMIN;
        boolean isAssignedEmployee = complaint.getAssignedEmployee() != null
                && complaint.getAssignedEmployee().getId().equals(requester.getId());

        if (!isAdmin && !isAssignedEmployee) {
            throw new AccessDeniedException("You do not have permission to add updates to this complaint");
        }

        UpdateVisibility visibility = UpdateVisibility.PUBLIC;
        if (request.getVisibility() != null
                && request.getVisibility().equalsIgnoreCase("INTERNAL")) {
            visibility = UpdateVisibility.INTERNAL;
        }

        ComplaintUpdate update = ComplaintUpdate.builder()
                .complaint(complaint)
                .user(requester)
                .comment(request.getComment())
                .visibility(visibility)
                .build();

        return toUpdateResponse(complaintUpdateRepository.save(update));
    }

    // ── FR6.3: View audit trail for a complaint ──────────────────────────────
    public List<ComplaintUpdateResponse> getUpdates(Long complaintId,
                                                    String requesterEmail) {
        Complaint complaint = findComplaintById(complaintId);
        User requester = findUserByEmail(requesterEmail);
        assertCanView(complaint, requester);

        boolean isCitizen = requester.getRole() == Role.CITIZEN;

        return complaintUpdateRepository
                .findByComplaintIdOrderByTimestampAsc(complaintId)
                .stream()
                // FR6.4: hide INTERNAL comments from citizens
                .filter(u -> !isCitizen || u.getVisibility() == UpdateVisibility.PUBLIC)
                .map(this::toUpdateResponse)
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Complaint findComplaintById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
    }

    private void assertCanView(Complaint complaint, User requester) {
        boolean isOwner = complaint.getComplainant().getId().equals(requester.getId());
        boolean isAssignedEmployee = complaint.getAssignedEmployee() != null
                && complaint.getAssignedEmployee().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (!isOwner && !isAssignedEmployee && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this complaint");
        }
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .status(complaint.getStatus().name())
                .priority(complaint.getPriority() != null
                        ? complaint.getPriority().name() : ComplaintPriority.MEDIUM.name())
                .complainantName(complaint.getComplainant().getName())
                .complainantEmail(complaint.getComplainant().getEmail())
                .assignedEmployeeName(complaint.getAssignedEmployee() != null
                        ? complaint.getAssignedEmployee().getName() : null)
                .assignedTo(complaint.getAssignedTo())
                .resolutionComments(complaint.getResolutionComments())
                .submittedAt(complaint.getSubmittedAt())
                .resolvedAt(complaint.getResolvedAt())
                .resolvedDate(complaint.getResolvedDate())
                .build();
    }

    private ComplaintUpdateResponse toUpdateResponse(ComplaintUpdate u) {
        return ComplaintUpdateResponse.builder()
                .id(u.getId())
                .comment(u.getComment())
                .statusChangeFrom(u.getStatusChangeFrom())
                .statusChangeTo(u.getStatusChangeTo())
                .visibility(u.getVisibility().name())
                .updatedByName(u.getUser().getName())
                .timestamp(u.getTimestamp())
                .build();
    }
}
