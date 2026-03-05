package com.jobportal.service;

import com.jobportal.entity.Application;
import com.jobportal.entity.ApplicationStatus;
import com.jobportal.entity.Job;
import com.jobportal.entity.User;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.ResumeRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.entity.Resume;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecruiterService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    private User getRecruiter(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
    }

    public Job postJob(Job jobRequest, String email) {
        User recruiter = getRecruiter(email);
        if (recruiter.getVerificationStatus() != com.jobportal.entity.VerificationStatus.APPROVED) {
            throw new RuntimeException(
                    "Your account is not approved to post jobs. Current status: " + recruiter.getVerificationStatus());
        }
        jobRequest.setRecruiter(recruiter);
        jobRequest.setStatus(com.jobportal.entity.VerificationStatus.APPROVED); // Auto-approve new jobs as requested
        return jobRepository.save(jobRequest);
    }

    public Job editJob(Long jobId, Job jobRequest, String email) {
        User recruiter = getRecruiter(email);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to edit this job");
        }

        job.setTitle(jobRequest.getTitle());
        job.setDescription(jobRequest.getDescription());
        job.setSalary(jobRequest.getSalary());
        job.setLocation(jobRequest.getLocation());

        return jobRepository.save(job);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteJob(Long jobId, String email) {
        User recruiter = getRecruiter(email);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to delete this job");
        }

        // Delete associated applications first to satisfy foreign key constraint
        applicationRepository.deleteByJob(job);
        jobRepository.delete(job);
    }

    public List<Job> getMyJobs(String email) {
        User recruiter = getRecruiter(email);
        return jobRepository.findByRecruiter(recruiter);
    }

    public List<Application> getApplicantsForJob(Long jobId, String email) {
        User recruiter = getRecruiter(email);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to view applications for this job");
        }

        return applicationRepository.findByJob(job);
    }

    public Application updateApplicationStatus(Long applicationId, ApplicationStatus status, String email) {
        User recruiter = getRecruiter(email);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to update this application");
        }

        application.setStatus(status);
        return applicationRepository.save(application);
    }

    public Resume getResumeBySeekerId(Long seekerId) {
        User seeker = getSeekerById(seekerId);
        return resumeRepository.findByJobSeeker(seeker)
                .orElseThrow(() -> new RuntimeException("Resume not found for this user"));
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteAccount(String email) {
        User recruiter = getRecruiter(email);

        // 1. Get all jobs posted by this recruiter
        List<Job> myJobs = jobRepository.findByRecruiter(recruiter);

        // 2. For each job, delete its applications first
        for (Job job : myJobs) {
            applicationRepository.deleteByJob(job);
        }

        // 3. Delete all jobs by this recruiter
        jobRepository.deleteByRecruiter(recruiter);

        // 4. Finally delete the recruiter
        userRepository.delete(recruiter);
    }

    public User getSeekerById(Long seekerId) {
        return userRepository.findById(seekerId)
                .orElseThrow(() -> new RuntimeException("Seeker not found"));
    }

    public User getProfile(String email) {
        return getRecruiter(email);
    }
}
