const API_BASE_URL = 'http://localhost:8080/api';

// Load videos on page load
document.addEventListener('DOMContentLoaded', () => {
    loadVideos();
    setupUploadForm();
});

// Setup upload form
function setupUploadForm() {
    const form = document.getElementById('uploadForm');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await uploadVideo();
    });
}

// Upload video
async function uploadVideo() {
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
                } else {
                    reject(new Error(`Upload failed: ${xhr.status}`));
                }
            });
            xhr.addEventListener('error', () => reject(new Error('Network error')));
        });

        xhr.open('POST', `${API_BASE_URL}/posting-video`);
        xhr.send(formData);

        await uploadPromise;

        showMessage('Video uploaded successfully! 🎉', 'success');
        document.getElementById('uploadForm').reset();
        setTimeout(() => loadVideos(), 1000);

    } catch (error) {
        console.error('Upload error:', error);
        showMessage('Upload failed: ' + error.message, 'error');
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