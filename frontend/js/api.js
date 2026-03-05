const API_BASE_URL = 'http://localhost:8080/api';

const api = {
    getHeaders: () => {
        const headers = { 'Content-Type': 'application/json' };
        const token = localStorage.getItem('token');
        if (token) headers['Authorization'] = `Bearer ${token}`;
        return headers;
    },

    async request(endpoint, options = {}) {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                ...options,
                headers: options.multipart ?
                    { 'Authorization': `Bearer ${localStorage.getItem('token')}` } :
                    { ...this.getHeaders(), ...options.headers }
            });

            const isJson = response.headers.get('content-type')?.includes('application/json');
            const data = isJson ? await response.json() : await response.text();

            if (!response.ok) {
                if (response.status === 401 && !endpoint.includes('/auth/login')) {
                    localStorage.clear();
                    window.location.href = 'index.html';
                }
                throw new Error((data && data.error) || (data && data.message) || response.statusText);
            }
            return data;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    },

    async download(endpoint) {
        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, {
                headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
            });

            if (!response.ok) throw new Error('Download failed');

            const blob = await response.blob();
            return URL.createObjectURL(blob);
        } catch (error) {
            console.error('Download Error:', error);
            throw error;
        }
    },

    auth: {
        login: (credentials) => api.request('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
        register: (userData) => api.request('/auth/register', { method: 'POST', body: JSON.stringify(userData) })
    },

    seeker: {
        getProfile: () => api.request('/seeker/profile'),
        searchJobs: (keyword = '') => api.request(`/seeker/jobs${keyword ? '?keyword=' + keyword : ''}`),
        applyJob: (jobId) => api.request(`/seeker/apply/${jobId}`, { method: 'POST' }),
        getApplications: () => api.request('/seeker/applications'),
        uploadResume: (formData) => api.request('/seeker/resume/upload', { method: 'POST', body: formData, multipart: true }),
        deleteResume: () => api.request('/seeker/resume', { method: 'DELETE' }),
        updateProfile: (profileData) => api.request('/seeker/profile', { method: 'PUT', body: JSON.stringify(profileData) }),
        uploadProfilePicture: (formData) => api.request('/seeker/profile/picture', { method: 'POST', body: formData, multipart: true }),
        getStats: () => api.request('/seeker/stats'),
        getOwnResume: () => api.request('/seeker/resume'),
        downloadResume: () => api.download('/seeker/resume/download'),
        getProfilePictureURL: () => api.download('/seeker/profile/picture'),
        deleteAccount: () => api.request('/seeker/account', { method: 'DELETE' })
    },

    recruiter: {
        getProfile: () => api.request('/recruiter/profile'),
        getJobs: () => api.request('/recruiter/jobs'),
        postJob: (jobData) => api.request('/recruiter/jobs', { method: 'POST', body: JSON.stringify(jobData) }),
        editJob: (id, jobData) => api.request(`/recruiter/jobs/${id}`, { method: 'PUT', body: JSON.stringify(jobData) }),
        deleteJob: (id) => api.request(`/recruiter/jobs/${id}`, { method: 'DELETE' }),
        getApplicants: (jobId) => api.request(`/recruiter/jobs/${jobId}/applications`),
        updateApplication: (appId, status) => api.request(`/recruiter/applications/${appId}/status?status=${status}`, { method: 'PUT' }),
        getResume: (seekerId) => api.request(`/recruiter/resume/${seekerId}`),
        downloadResume: (seekerId) => api.download(`/recruiter/resume/download/${seekerId}`),
        getSeekerProfilePictureURL: (seekerId) => api.download(`/recruiter/seeker-profile-pic/${seekerId}`),
        deleteAccount: () => api.request('/recruiter/account', { method: 'DELETE' })
    },

    admin: {
        getUsers: () => api.request('/admin/users'),
        deleteUser: (id) => api.request(`/admin/users/${id}`, { method: 'DELETE' }),
        getJobs: () => api.request('/admin/jobs'),
        deleteJob: (id) => api.request(`/admin/jobs/${id}`, { method: 'DELETE' }),
        changeRole: (userId, role) => api.request(`/admin/users/${userId}/role?role=${role}`, { method: 'PUT' }),
        getPendingRecruiters: () => api.request('/admin/recruiters/pending'),
        updateRecruiterStatus: (id, status, message = '') => api.request(`/admin/recruiters/${id}/status?status=${status}&message=${encodeURIComponent(message)}`, { method: 'PUT' }),
        getPendingJobs: () => api.request('/admin/jobs/pending'),
        updateJobStatus: (id, status) => api.request(`/admin/jobs/${id}/status?status=${status}`, { method: 'PUT' })
    }
};

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'fa-info-circle';
    if (type === 'success') icon = 'fa-check-circle';
    if (type === 'error') icon = 'fa-exclamation-circle';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> ${message}`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function logout() {
    localStorage.clear();
    window.location.href = 'index.html';
}

function redirectBasedOnRole(role) {
    if (role === 'ROLE_JOB_SEEKER') window.location.href = 'seeker_dashboard.html';
    else if (role === 'ROLE_RECRUITER') window.location.href = 'recruiter_dashboard.html';
    else if (role === 'ROLE_ADMIN') window.location.href = 'admin_dashboard.html';
}
