const API_BASE_URL = 'http://localhost:8080/api';
let authToken = localStorage.getItem('authToken');
let currentUser = {
    username: localStorage.getItem('username'),
    roles: JSON.parse(localStorage.getItem('userRoles') || '[]')
};

// Load videos on page load
document.addEventListener('DOMContentLoaded', () => {
    updateUIBasedOnAuth();
    loadVideos();
    setupUploadForm();
    
    // Load liked videos if user is logged in
    if (authToken && currentUser.username) {
        loadLikedVideos();
    }
});

function updateUIBasedOnAuth() {
    const uploadSection = document.getElementById('uploadSection');
    const authButtons = document.getElementById('authButtons');
    const userInfo = document.getElementById('userInfo');
    const likedVideosSection = document.getElementById('likedVideosSection');

    if (authToken && currentUser.username) {
        // User is logged in
        if (authButtons) authButtons.style.display = 'none';
        if (userInfo) {
            userInfo.style.display = 'block';
            userInfo.innerHTML = `
                <span>Welcome, <strong>${currentUser.username}</strong></span>
                <button onclick="handleLogout()" class="btn-secondary">Logout</button>
            `;
        }
        
        // Show upload section only for USER or ADMIN
        if (uploadSection && (currentUser.roles.includes('USER') || currentUser.roles.includes('ADMIN'))) {
            uploadSection.style.display = 'block';
        } else if (uploadSection) {
            uploadSection.style.display = 'none';
        }
        
        // Show liked videos section for authenticated users
        if (likedVideosSection) {
            likedVideosSection.style.display = 'block';
        }
    } else {
        // Guest user
        if (authButtons) authButtons.style.display = 'block';
        if (userInfo) userInfo.style.display = 'none';
        if (uploadSection) uploadSection.style.display = 'none';
        if (likedVideosSection) likedVideosSection.style.display = 'none';
    }
}

function handleLogout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userRoles');
    
    authToken = null;
    currentUser = { username: null, roles: [] };
    
    window.location.reload();
}

// Setup upload form
function setupUploadForm() {
    const form = document.getElementById('uploadForm');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await uploadVideo();
        });
    }
}

// Upload video
async function uploadVideo() {
    if (!authToken) {
        showMessage('Please login to upload videos', 'error');
        setTimeout(() => {
            window.location.href = 'login.html';
        }, 2000);
        return;
    }

    const title = document.getElementById('title').value;
    const description = document.getElementById('description').value;
    const videoFile = document.getElementById('videoFile').files[0];
    const thumbnailFile = document.getElementById('thumbnailFile').files[0];

    if (!videoFile) {
        showMessage('Please select a video file', 'error');
        return;
    }

    // Create FormData
    const formData = new FormData();
    
    // Add metadata as JSON
    const metadata = {
        title: title,
        description: description || ''
    };
    formData.append('metadata', new Blob([JSON.stringify(metadata)], {
        type: 'application/json'
    }));
    
    formData.append('file', videoFile);
    
    if (thumbnailFile) {
        formData.append('thumbnail', thumbnailFile);
    }

    // Show progress
    const progressContainer = document.getElementById('uploadProgress');
    const progressFill = document.getElementById('progressFill');
    const progressText = document.getElementById('progressText');
    const uploadBtn = document.getElementById('uploadBtnText');
    
    progressContainer.style.display = 'block';
    uploadBtn.textContent = 'Uploading...';

    try {
        const xhr = new XMLHttpRequest();

        // Progress tracking
        xhr.upload.addEventListener('progress', (e) => {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                progressFill.style.width = percentComplete + '%';
                progressText.textContent = `Uploading... ${percentComplete}%`;
            }
        });

        // Promise wrapper for XMLHttpRequest
        const uploadPromise = new Promise((resolve, reject) => {
            xhr.addEventListener('load', () => {
                if (xhr.status >= 200 && xhr.status < 300) {
                    resolve(xhr.response);
                } else if (xhr.status === 401 || xhr.status === 403) {
                    reject(new Error('Unauthorized. Please login again.'));
                } else {
                    reject(new Error(`Upload failed: ${xhr.status}`));
                }
            });
            xhr.addEventListener('error', () => reject(new Error('Network error')));
        });

        xhr.open('POST', `${API_BASE_URL}/posting-video`);
        xhr.setRequestHeader('Authorization', `Bearer ${authToken}`);
        xhr.send(formData);

        await uploadPromise;

        showMessage('Video uploaded successfully! 🎉', 'success');
        document.getElementById('uploadForm').reset();
        setTimeout(() => loadVideos(), 1000);

    } catch (error) {
        console.error('Upload error:', error);
        if (error.message.includes('Unauthorized')) {
            showMessage('Session expired. Please login again.', 'error');
            setTimeout(() => {
                handleLogout();
                window.location.href = 'login.html';
            }, 2000);
        } else {
            showMessage('Upload failed: ' + error.message, 'error');
        }
    } finally {
        progressContainer.style.display = 'none';
        uploadBtn.textContent = 'Upload Video';
    }
}

// Load all videos
async function loadVideos() {
    const videosList = document.getElementById('videosList');
    videosList.innerHTML = '<p class="loading">Loading videos...</p>';

    try {
        const response = await fetch(`${API_BASE_URL}/get-all-video`);
        const data = await response.json();

        if (!data.content || data.content.length === 0) {
            videosList.innerHTML = '<p class="loading">No videos uploaded yet.</p>';
            return;
        }

        videosList.innerHTML = '';
        data.content.forEach(video => {
            const card = createVideoCard(video);
            videosList.appendChild(card);
        });

    } catch (error) {
        console.error('Error loading videos:', error);
        videosList.innerHTML = '<p class="loading">Error loading videos. Please try again.</p>';
    }
}

// Load user's liked videos
async function loadLikedVideos() {
    const likedVideosList = document.getElementById('likedVideosList');
    
    if (!authToken) {
        likedVideosList.innerHTML = '<p class="loading">Please login to view your liked videos.</p>';
        return;
    }
    
    likedVideosList.innerHTML = '<p class="loading">Loading liked videos...</p>';

    try {
        const response = await fetch(`${API_BASE_URL}/likes/my-likes`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (response.status === 401 || response.status === 403) {
            likedVideosList.innerHTML = '<p class="loading">Session expired. Please login again.</p>';
            setTimeout(() => {
                handleLogout();
                window.location.href = 'login.html';
            }, 2000);
            return;
        }

        const likedVideos = await response.json();

        if (!likedVideos || likedVideos.length === 0) {
            likedVideosList.innerHTML = '<p class="loading">You haven\'t liked any videos yet. ❤️</p>';
            return;
        }

        likedVideosList.innerHTML = '';
        likedVideos.forEach(video => {
            const card = createVideoCard(video);
            likedVideosList.appendChild(card);
        });

    } catch (error) {
        console.error('Error loading liked videos:', error);
        likedVideosList.innerHTML = '<p class="loading">Error loading liked videos. Please try again.</p>';
    }
}

// Create video card
function createVideoCard(video) {
    const card = document.createElement('div');
    card.className = 'video-card';
    card.onclick = () => window.location.href = `video-player.html?id=${video.videoId}`;

    const thumbnailUrl = video.thumbnailUrl 
        ? `${API_BASE_URL}${video.thumbnailUrl}` 
        : 'https://via.placeholder.com/320x180?text=No+Thumbnail';

    card.innerHTML = `
        <img src="${thumbnailUrl}" alt="${video.title}" class="video-thumbnail" 
             onerror="this.src='https://via.placeholder.com/320x180?text=No+Thumbnail'">
        <div class="video-card-content">
            <h3>${escapeHtml(video.title)}</h3>
            <div class="video-card-stats">
                <span>👁️ ${video.viewCount}</span>
                <span>👍 ${video.likeCount}</span>
            </div>
            <div class="video-card-meta">
                <small>By: ${video.username || 'Unknown'}</small>
                <small>Uploaded: ${formatDate(video.createdAt)}</small>
            </div>
        </div>
    `;

    return card;
}

// Show message
function showMessage(message, type) {
    const messageDiv = document.getElementById('uploadMessage');
    messageDiv.textContent = message;
    messageDiv.className = `message ${type}`;
    setTimeout(() => {
        messageDiv.className = 'message';
    }, 5000);
}

// Utility functions
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric' 
    });
}