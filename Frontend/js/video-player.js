const API_BASE_URL = 'http://localhost:8080/api';
let currentVideoId = null;
let hasLiked = false;

// Get video ID from URL
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    currentVideoId = urlParams.get('id');
    
    if (currentVideoId) {
        loadVideo(currentVideoId);
        setupEventListeners();
    } else {
        alert('No video ID provided');
        window.location.href = 'index.html';
    }
});

// Setup event listeners
function setupEventListeners() {
    document.getElementById('likeBtn').addEventListener('click', toggleLike);
    document.getElementById('editBtn').addEventListener('click', showEditForm);
    document.getElementById('deleteBtn').addEventListener('click', deleteVideo);
    document.getElementById('updateForm').addEventListener('submit', updateVideo);
}

// Load video details
async function loadVideo(videoId) {
    try {
        const response = await fetch(`${API_BASE_URL}/getVideo/${videoId}`);
        const data = await response.json();
        const video = data.content[0];

        // Set video source
        document.getElementById('videoSource').src = `${API_BASE_URL}/videos/${videoId}/stream`;
        document.getElementById('videoPlayer').load();

        // Set video info
        document.getElementById('videoTitle').textContent = video.title;
        document.getElementById('videoDescription').textContent = video.description || 'No description';
        document.getElementById('viewCount').textContent = `👁️ ${video.viewCount} views`;
        document.getElementById('likeCount').textContent = video.likeCount;
        document.getElementById('uploadDate').textContent = formatDate(video.createdAt);
        document.getElementById('videoStatus').textContent = video.videoStatus;

        // Set edit form values
        document.getElementById('editTitle').value = video.title;
        document.getElementById('editDescription').value = video.description || '';

    } catch (error) {
        console.error('Error loading video:', error);
        alert('Failed to load video');
    }
}

// Toggle like
async function toggleLike() {
    try {
        const endpoint = hasLiked ? 'decrement-likes' : 'increment-likes';
        const response = await fetch(`${API_BASE_URL}/video-analyitcs/${currentVideoId}/${endpoint}`, {
            method: 'POST'
        });
        
        if (response.ok) {
            const data = await response.json();
            const analytics = data.content[0];
            document.getElementById('likeCount').textContent = analytics.likeCount;
            hasLiked = !hasLiked;
            
            const likeBtn = document.getElementById('likeBtn');
            likeBtn.innerHTML = hasLiked ? `👎 <span id="likeCount">${analytics.likeCount}</span>` : `👍 <span id="likeCount">${analytics.likeCount}</span>`;
        }
    } catch (error) {
        console.error('Error toggling like:', error);
    }
}

// Show edit form
function showEditForm() {
    const editForm = document.getElementById('editForm');
    editForm.style.display = editForm.style.display === 'none' ? 'block' : 'none';
}

// Cancel edit
function cancelEdit() {
    document.getElementById('editForm').style.display = 'none';
}

// Update video
async function updateVideo(e) {
    e.preventDefault();
    
    const title = document.getElementById('editTitle').value;
    const description = document.getElementById('editDescription').value;

    try {
        const response = await fetch(`${API_BASE_URL}/update-video/${currentVideoId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                title: title,
                description: description
            })
        });

        if (response.ok) {
            alert('Video updated successfully!');
            loadVideo(currentVideoId);
            cancelEdit();
        } else {
            alert('Failed to update video');
        }
    } catch (error) {
        console.error('Error updating video:', error);
        alert('Error updating video');
    }
}

// Delete video
async function deleteVideo() {
    if (!confirm('Are you sure you want to delete this video?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/delete-video/${currentVideoId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('Video deleted successfully!');
            window.location.href = 'index.html';
        } else {
            alert('Failed to delete video');
        }
    } catch (error) {
        console.error('Error deleting video:', error);
        alert('Error deleting video');
    }
}

// Utility function
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
    });
}