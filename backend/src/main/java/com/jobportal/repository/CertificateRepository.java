package com.jobportal.repository;

import com.jobportal.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByJobSeekerId(Long jobSeekerId);
    void deleteByJobSeekerId(Long jobSeekerId);
}
