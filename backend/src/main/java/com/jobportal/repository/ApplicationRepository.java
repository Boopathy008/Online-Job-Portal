package com.jobportal.repository;

import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJob(Job job);

    List<Application> findByJobSeeker(User jobSeeker);

    Optional<Application> findByJobAndJobSeeker(Job job, User jobSeeker);

    boolean existsByJobAndJobSeeker(Job job, User jobSeeker);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByJob(Job job);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByJobSeeker(User jobSeeker);
}
