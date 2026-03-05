package com.jobportal.repository;

import com.jobportal.entity.Job;
import com.jobportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRecruiter(User recruiter);

    @org.springframework.data.jpa.repository.Query("SELECT j FROM Job j WHERE j.status = com.jobportal.entity.VerificationStatus.APPROVED")
    List<Job> findAllApproved();

    @org.springframework.data.jpa.repository.Query("SELECT j FROM Job j WHERE j.status = com.jobportal.entity.VerificationStatus.APPROVED AND (LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Job> searchApprovedJobs(@org.springframework.data.repository.query.Param("keyword") String keyword);

    List<Job> findByStatus(com.jobportal.entity.VerificationStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByRecruiter(User recruiter);
}
