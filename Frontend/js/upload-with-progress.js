const API_BASE_URL = 'http://localhost:8080/api';
const WS_URL = 'http://localhost:8080/ws';

let stompClient = null;
let uploadStartTime = null;
let lastUploadedBytes = 0;
let lastUpdateTime = null;
let currentUploadId = null;
let uploadCancelled = false;

// Connect to WebSocket
function connectWebSocket(uploadId) {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('✅ Connected to WebSocket:', frame);
        
        // Subscribe to upload progress updates
        stompClient.subscribe('/topic/upload-progress/' + uploadId, function(message) {
            const progress = JSON.parse(message.body);
            updateProgressFromServer(progress);
        });
    }, function(error) {
        console.error('❌ WebSocket connection error:', error);
    });
}

function disconnectWebSocket() {
    if (stompClient !== null) {
        stompClient.disconnect();
        console.log('Disconnected from WebSocket');
    }
}

// Generate UUID
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// Upload video with real-time progress
function uploadVideoWithRealTimeProgress(file, thumbnailFile, metadata) {
    const authToken = localStorage.getItem('authToken');
    
    if (!authToken) {
        alert('Please login to upload videos');
        window.location.href = 'login.html';
        return;
    }

    currentUploadId = generateUUID();
    uploadCancelled = false;
    
    // Show progress UI
    document.getElementById('upload-form-container').style.display = 'none';
    document.getElementById('upload-progress-container').style.display = 'block';
    document.getElementById('file-name').textContent = file.name;
    
    uploadStartTime = Date.now();
    lastUpdateTime = Date.now();
    lastUploadedBytes = 0;
    
    // Connect to WebSocket for real-time server updates
    connectWebSocket(currentUploadId);
    
    // Prepare form data
    const formData = new FormData();
    formData.append('file', file);
    
    if (thumbnailFile) {
        formData.append('thumbnail', thumbnailFile);
    }
    
    formData.append('metadata', new Blob([JSON.stringify({
        title: metadata.title,
        description: metadata.description
    })], { type: 'application/json' }));
    
    // Upload with XMLHttpRequest for client-side progress tracking
    const xhr = new XMLHttpRequest();
    
    // Track upload progress (client-side)
    xhr.upload.addEventListener('progress', function(e) {
        if (e.lengthComputable && !uploadCancelled) {
            const percentage = Math.round((e.loaded / e.total) * 100);
            updateProgressUILocal(e.loaded, e.total, percentage);
        }
    });
    
    xhr.addEventListener('load', function() {
        if (xhr.status === 201 || xhr.status === 200) {
            console.log('✅ Upload complete!');
            showUploadSuccess();
            disconnectWebSocket();
        } else {
            alert('Upload failed: ' + xhr.statusText);
            resetUploadUI();
        }
    });
    
    xhr.addEventListener('error', function() {
        alert('Upload error! Please check your connection.');
        resetUploadUI();
        disconnectWebSocket();
    });
    
    xhr.addEventListener('abort', function() {
        console.log('Upload cancelled');
        resetUploadUI();
        disconnectWebSocket();
    });
    
    xhr.open('POST', `${API_BASE_URL}/posting-video?uploadId=${currentUploadId}`);
    xhr.setRequestHeader('Authorization', `Bearer ${authToken}`);
    xhr.send(formData);
    
    // Store xhr globally for cancellation
    window.currentXHR = xhr;
}

// Update progress UI from client-side tracking
function updateProgressUILocal(uploadedBytes, totalBytes, percentage) {
    // Update percentage
    document.getElementById('percentage-text').textContent = percentage + '%';
    document.getElementById('progress-bar').style.width = percentage + '%';
    
    // Calculate upload speed
    const now = Date.now();
    const timeDiff = (now - lastUpdateTime) / 1000; // seconds
    
    if (timeDiff > 0) {
        const bytesDiff = uploadedBytes - lastUploadedBytes;
        const speedBps = bytesDiff / timeDiff;
        const speedMBps = (speedBps / 1024 / 1024).toFixed(2);
        
        document.getElementById('upload-speed').textContent = speedMBps + ' MB/s';
        
        // Calculate time remaining
        const remainingBytes = totalBytes - uploadedBytes;
        const remainingSeconds = remainingBytes / speedBps;
        
        if (isFinite(remainingSeconds) && remainingSeconds > 0) {
            const minutes = Math.floor(remainingSeconds / 60);
            const seconds = Math.floor(remainingSeconds % 60);
            document.getElementById('time-remaining').textContent = 
                `${minutes}m ${seconds}s`;
        }
    }
    
    // Update size
    const uploadedMB = (uploadedBytes / 1024 / 1024).toFixed(2);
    const totalMB = (totalBytes / 1024 / 1024).toFixed(2);
    document.getElementById('upload-size').textContent = `${uploadedMB} MB / ${totalMB} MB`;
    
    lastUploadedBytes = uploadedBytes;
    lastUpdateTime = now;
}

// Update progress from server-side WebSocket
function updateProgressFromServer(progress) {
    console.log('📊 Server progress:', progress);
    
    if (progress.status === 'COMPLETED') {
        document.getElementById('percentage-text').textContent = '100%';
        document.getElementById('progress-bar').style.width = '100%';
    } else if (progress.status === 'FAILED') {
        alert('Upload failed: ' + (progress.error || 'Unknown error'));
        resetUploadUI();
    }
}

// Show upload success
function showUploadSuccess() {
    document.getElementById('upload-progress-container').style.display = 'none';
    document.getElementById('upload-success-container').style.display = 'block';
}

// Reset upload UI
function resetUploadUI() {
    document.getElementById('upload-progress-container').style.display = 'none';
    document.getElementById('upload-form-container').style.display = 'block';
    document.getElementById('upload-form').reset();
}

// Cancel upload
function cancelUpload() {
    if (confirm('Are you sure you want to cancel this upload?')) {
        uploadCancelled = true;
        if (window.currentXHR) {
            window.currentXHR.abort();
        }
        disconnectWebSocket();
        resetUploadUI();
    }
}

// Form submission
document.getElementById('upload-form').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const videoFile = document.getElementById('video-file').files[0];
    const thumbnailFile = document.getElementById('thumbnail-file').files[0];
    const title = document.getElementById('title').value;
    const description = document.getElementById('description').value;
    
    if (!videoFile) {
        alert('Please select a video file');
        return;
    }
    
    // Check file size (500MB limit)
    const maxSize = 500 * 1024 * 1024; // 500MB
    if (videoFile.size > maxSize) {
        alert('File size exceeds 500MB limit!');
        return;
    }
    
    uploadVideoWithRealTimeProgress(videoFile, thumbnailFile, { title, description });
});

// Handle page unload
window.addEventListener('beforeunload', function(e) {
    if (document.getElementById('upload-progress-container').style.display === 'block') {
        e.preventDefault();
        e.returnValue = 'Upload in progress. Are you sure you want to leave?';
        return e.returnValue;
    }
});