package com.jobportal.service;

import com.jobportal.entity.Job;
import com.jobportal.entity.Role;
import com.jobportal.entity.User;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobSeekerService jobSeekerService;

    @Autowired
    private RecruiterService recruiterService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Cleanup applications before deleting job
        applicationRepository.deleteByJob(job);
        jobRepository.delete(job);
    }

    public User changeUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        if (role == Role.ROLE_RECRUITER) {
            user.setVerificationStatus(com.jobportal.entity.VerificationStatus.PENDING);
        } else {
            user.setVerificationStatus(com.jobportal.entity.VerificationStatus.APPROVED);
        }
        return userRepository.save(user);
    }

    public List<User> getPendingRecruiters() {
        return userRepository.findByRoleAndVerificationStatus(Role.ROLE_RECRUITER,
                com.jobportal.entity.VerificationStatus.PENDING);
    }

    public User updateRecruiterStatus(Long id, com.jobportal.entity.VerificationStatus status, String message) {
        User recruiter = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        if (recruiter.getRole() != Role.ROLE_RECRUITER) {
            throw new RuntimeException("User is not a recruiter");
        }
        recruiter.setVerificationStatus(status);
        if (message != null && !message.trim().isEmpty()) {
            recruiter.setStatusMessage(message);
        } else {
            if (status == com.jobportal.entity.VerificationStatus.APPROVED) {
                recruiter.setStatusMessage("Your account has been approved by Admin! You can now post jobs.");
            } else if (status == com.jobportal.entity.VerificationStatus.REJECTED) {
                recruiter.setStatusMessage("Your account verification failed. Please contact support.");
            }
        }
        return userRepository.save(recruiter);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new RuntimeException("Cannot delete an administrator account");
        }

        if (user.getRole() == Role.ROLE_RECRUITER) {
            recruiterService.deleteAccount(user.getEmail());
        } else if (user.getRole() == Role.ROLE_JOB_SEEKER) {
            jobSeekerService.deleteAccount(user.getEmail());
        } else {
            userRepository.delete(user);
        }
    }
}
