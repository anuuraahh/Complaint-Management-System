package com.examly.springapp.repository;

import com.examly.springapp.model.Complaint;
import com.examly.springapp.model.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    List<Complaint> findByComplainantId(Long complainantId);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByAssignedEmployeeId(Long employeeId);

    List<Complaint> findByCategoryIgnoreCase(String category);

    @Query("SELECT c FROM Complaint c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.complainant.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Complaint> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT c FROM Complaint c WHERE c.assignedEmployee.id = :employeeId AND c.status = :status")
    List<Complaint> findByAssignedEmployeeIdAndStatus(
            @Param("employeeId") Long employeeId,
            @Param("status") ComplaintStatus status);

    @Query("SELECT CAST(c.submittedAt AS date) AS date, COUNT(c) AS count FROM Complaint c GROUP BY CAST(c.submittedAt AS date) ORDER BY CAST(c.submittedAt AS date) ASC")
    List<Map<String, Object>> countBySubmissionDate();
}
