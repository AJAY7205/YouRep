const API_BASE_URL = 'http://localhost:8080/api';
let currentVideoId = null;
let authToken = localStorage.getItem('authToken');
let currentUser = {
    username: localStorage.getItem('username'),
    userId: localStorage.getItem('userId'),
    roles: JSON.parse(localStorage.getItem('userRoles') || '[]')
};

// Get video ID from URL
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    currentVideoId = urlParams.get('id');
    
    updateUIBasedOnAuth();
    
    if (currentVideoId) {
        loadVideo(currentVideoId);
        setupEventListeners();
        checkIfLiked();
    } else {
        alert('No video ID provided');
        window.location.href = 'index.html';
    }
});

function updateUIBasedOnAuth() {
    const editBtn = document.getElementById('editBtn');
    const deleteBtn = document.getElementById('deleteBtn');
    const likeBtn = document.getElementById('likeBtn');
    const userInfo = document.getElementById('playerUserInfo');

    if (authToken && currentUser.username) {
        // Show user info
        if (userInfo) {
            userInfo.innerHTML = `
                <span>Logged in as: <strong>${currentUser.username}</strong></span>
                <a href="index.html" class="btn-secondary" style="margin-left: 10px;">Home</a>
                <button onclick="handleLogout()" class="btn-secondary">Logout</button>
            `;
        }
    } else {
        // Guest user
        if (editBtn) editBtn.style.display = 'none';
        if (deleteBtn) deleteBtn.style.display = 'none';
        if (likeBtn) likeBtn.disabled = true;
        if (userInfo) {
            userInfo.innerHTML = `
                <span>Viewing as Guest</span>
                <a href="login.html" class="btn-primary" style="margin-left: 10px;">Login to interact</a>
            `;
        }
    }
}

function handleLogout() {
    localStorage.clear();
    window.location.href = 'login.html';
}

// Setup event listeners
function setupEventListeners() {
    const likeBtn = document.getElementById('likeBtn');
    const editBtn = document.getElementById('editBtn');
    const deleteBtn = document.getElementById('deleteBtn');
    const updateForm = document.getElementById('updateForm');

    if (likeBtn) likeBtn.addEventListener('click', toggleLike);
    if (editBtn) editBtn.addEventListener('click', showEditForm);
    if (deleteBtn) deleteBtn.addEventListener('click', deleteVideo);
    if (updateForm) updateForm.addEventListener('submit', updateVideo);
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
        document.getElementById('videoOwner').textContent = video.username || 'Unknown';

        // Set edit form values
        document.getElementById('editTitle').value = video.title;
        document.getElementById('editDescription').value = video.description || '';

        // Show/hide edit/delete buttons based on ownership
        checkVideoOwnership(video.username);

    } catch (error) {
        console.error('Error loading video:', error);
        alert('Failed to load video');
    }
}

function checkVideoOwnership(videoOwner) {
    const editBtn = document.getElementById('editBtn');
    const deleteBtn = document.getElementById('deleteBtn');
    
    if (!authToken) {
        if (editBtn) editBtn.style.display = 'none';
        if (deleteBtn) deleteBtn.style.display = 'none';
        return;
    }

    const isOwner = currentUser.username === videoOwner;
    const isAdmin = currentUser.roles.includes('ADMIN');

    if (isOwner || isAdmin) {
        if (editBtn) editBtn.style.display = 'inline-block';
        if (deleteBtn) deleteBtn.style.display = 'inline-block';
    } else {
        if (editBtn) editBtn.style.display = 'none';
        if (deleteBtn) deleteBtn.style.display = 'none';
    }
}

// Check if user has liked the video
async function checkIfLiked() {
    if (!authToken) return;

    try {
        const response = await fetch(`${API_BASE_URL}/likes/${currentVideoId}/check`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (response.ok) {
            const isLiked = await response.json();
            updateLikeButton(isLiked);
        }
    } catch (error) {
        console.error('Error checking like status:', error);
    }
}

// Toggle like
async function toggleLike() {
    if (!authToken) {
        alert('Please login to like videos');
        window.location.href = 'login.html';
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/likes/${currentVideoId}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (response.ok) {
            const data = await response.json();
            // Reload like count
            await loadLikeCount();
            await checkIfLiked();
        } else if (response.status === 401 || response.status === 403) {
            alert('Session expired. Please login again.');
            handleLogout();
        }
    } catch (error) {
        console.error('Error toggling like:', error);
    }
}

async function loadLikeCount() {
    try {
        const response = await fetch(`${API_BASE_URL}/likes/${currentVideoId}/count`);
        if (response.ok) {
            const count = await response.json();
            document.getElementById('likeCount').textContent = count;
        }
    } catch (error) {
        console.error('Error loading like count:', error);
    }
}

function updateLikeButton(isLiked) {
    const likeBtn = document.getElementById('likeBtn');
    if (isLiked) {
        likeBtn.innerHTML = `❤️ <span id="likeCount">${document.getElementById('likeCount').textContent}</span>`;
        likeBtn.classList.add('liked');
    } else {
        likeBtn.innerHTML = `🤍 <span id="likeCount">${document.getElementById('likeCount').textContent}</span>`;
        likeBtn.classList.remove('liked');
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
    
    if (!authToken) {
        alert('Please login to edit videos');
        return;
    }

    const title = document.getElementById('editTitle').value;
    const description = document.getElementById('editDescription').value;

    try {
        const response = await fetch(`${API_BASE_URL}/update-video/${currentVideoId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
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
        } else if (response.status === 401 || response.status === 403) {
            alert('You do not have permission to edit this video');
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
    if (!authToken) {
        alert('Please login to delete videos');
        return;
    }

    if (!confirm('Are you sure you want to delete this video?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/delete-video/${currentVideoId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (response.ok) {
            alert('Video deleted successfully!');
            window.location.href = 'index.html';
        } else if (response.status === 401 || response.status === 403) {
            alert('You do not have permission to delete this video');
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