package com.examly.springapp.service;

import com.examly.springapp.dto.AttachmentResponse;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Attachment;
import com.examly.springapp.model.Complaint;
import com.examly.springapp.repository.AttachmentRepository;
import com.examly.springapp.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ComplaintRepository complaintRepository;

    @Transactional
    public AttachmentResponse upload(Long complaintId, MultipartFile file) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        // Store a reference URL; actual file storage (S3/disk) is infrastructure concern
        String fileUrl = "/uploads/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        Attachment attachment = Attachment.builder()
                .complaint(complaint)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .mimeType(file.getContentType())
                .build();

        return toResponse(attachmentRepository.save(attachment));
    }

    public List<AttachmentResponse> getByComplaint(Long complaintId) {
        if (!complaintRepository.existsById(complaintId)) {
            throw new ResourceNotFoundException("Complaint not found: " + complaintId);
        }
        return attachmentRepository.findByComplaintId(complaintId)
                .stream().map(this::toResponse).toList();
    }

    private AttachmentResponse toResponse(Attachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .fileName(a.getFileName())
                .fileUrl(a.getFileUrl())
                .mimeType(a.getMimeType())
                .build();
    }
}
