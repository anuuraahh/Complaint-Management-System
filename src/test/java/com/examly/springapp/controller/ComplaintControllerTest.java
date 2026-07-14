package com.examly.springapp.controller;

import com.examly.springapp.model.Complaint;
import com.examly.springapp.model.ComplaintPriority;
import com.examly.springapp.model.ComplaintStatus;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.ComplaintRepository;
import com.examly.springapp.repository.ComplaintUpdateRepository;
import com.examly.springapp.repository.NotificationRepository;
import com.examly.springapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintRepository repository;

    @Autowired
    private ComplaintUpdateRepository complaintUpdateRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        complaintUpdateRepository.deleteAll();
        repository.deleteAll();
        notificationRepository.deleteAll();
        userRepository.deleteAll();
        testUser = userRepository.save(User.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .passwordHash("$2a$10$dummyhashfortest000000000000000000000000000000000000000")
                .role(Role.CITIZEN)
                .build());
        adminUser = userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@example.com")
                .passwordHash("$2a$10$dummyhashfortest000000000000000000000000000000000000000")
                .role(Role.ADMIN)
                .build());
    }

    private Complaint savedComplaint(String title, String description, String category,
                                     String status, String priority) {
        return repository.save(Complaint.builder()
                .title(title)
                .description(description)
                .category(category)
                .complainant(testUser)
                .status(ComplaintStatus.valueOf(status))
                .priority(ComplaintPriority.valueOf(priority))
                .submittedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = "CITIZEN")
    void testCreateComplaint() throws Exception {
        String body = "{\"title\":\"Broken Street Light\","
                + "\"description\":\"The street light at the corner of Main St. and 5th Ave. has been out for a week.\","
                + "\"category\":\"INFRASTRUCTURE\"}";

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Broken Street Light")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.priority", is("MEDIUM")));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = "CITIZEN")
    void testCreateComplaintValidationFail() throws Exception {
        String body = "{\"title\":\"\",\"description\":\"Short\",\"category\":\"INVALID\"}";

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(emptyOrNullString())));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = "CITIZEN")
    void testGetAllComplaints() throws Exception {
        savedComplaint("Complaint 1", "Description one with more than 10 chars.", "SERVICE", "PENDING", "HIGH");
        savedComplaint("Complaint 2", "Description two with more than 10 chars.", "PERSONNEL", "RESOLVED", "LOW");

        mockMvc.perform(get("/api/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = "CITIZEN")
    void testGetComplaintByIdSuccessAndNotFound() throws Exception {
        Complaint saved = savedComplaint("Test Complaint", "Description more than 10 chars.", "SERVICE", "IN_PROGRESS", "MEDIUM");

        mockMvc.perform(get("/api/complaints/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Test Complaint")));

        mockMvc.perform(get("/api/complaints/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = "CITIZEN")
    void testGetComplaintsByStatus() throws Exception {
        savedComplaint("Complaint 1", "Description one for status filter.", "INFRASTRUCTURE", "PENDING", "MEDIUM");
        savedComplaint("Complaint 2", "Description two for status filter.", "SERVICE", "RESOLVED", "LOW");

        mockMvc.perform(get("/api/complaints/status/RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/complaints/status/WRONGSTATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid status")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testUpdateComplaintStatusWithoutOptionalFields() throws Exception {
        Complaint saved = savedComplaint("Status Update Test", "Testing status update without comments or assignee", "SERVICE", "PENDING", "MEDIUM");

        String updateJson = "{\"status\":\"IN_PROGRESS\"}";

        mockMvc.perform(put("/api/complaints/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.assignedTo").doesNotExist())
                .andExpect(jsonPath("$.resolutionComments").doesNotExist());
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = "CITIZEN")
    void testCreateComplaintWithMinBoundaryDescription() throws Exception {
        String body = "{\"title\":\"Streetlight\",\"description\":\"1234567890\",\"category\":\"SERVICE\"}";

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description", is("1234567890")));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = "CITIZEN")
    void testGetComplaintAfterDelete() throws Exception {
        Complaint saved = savedComplaint("Delete Me", "Will be deleted longer", "PERSONNEL", "PENDING", "LOW");
        repository.deleteById(saved.getId());

        mockMvc.perform(get("/api/complaints/" + saved.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testUpdateComplaintStatus() throws Exception {
        Complaint saved = savedComplaint("Resolve This", "Needs fixing and proper attention.", "INFRASTRUCTURE", "IN_PROGRESS", "HIGH");

        String updateJson = "{\"status\":\"RESOLVED\","
                + "\"assignedTo\":\"Admin User\","
                + "\"resolutionComments\":\"Fixed the issue successfully.\"}";

        mockMvc.perform(put("/api/complaints/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")))
                .andExpect(jsonPath("$.assignedTo", is("Admin User")))
                .andExpect(jsonPath("$.resolutionComments", is("Fixed the issue successfully.")))
                .andExpect(jsonPath("$.resolvedDate", notNullValue()));

        mockMvc.perform(put("/api/complaints/99999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));

        mockMvc.perform(put("/api/complaints/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid status")));
    }
}
