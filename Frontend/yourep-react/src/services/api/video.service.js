import api from './axios.config';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export const getAllVideos = async () => {
  const response = await api.get('/get-all-video');
  return response.data;
};

export const getVideo = async (videoId) => {
  const response = await api.get(`/getVideo/${videoId}`);
  return response.data;
};

export const getTranscodeProgress = async (videoId) => {
  const response = await api.get(`/videos/${videoId}/transcode-progress`);
  return response.data;
};

export const uploadVideo = (formData, onProgress) => {
  return new Promise((resolve, reject) => {
    const token = localStorage.getItem('authToken');
    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable && onProgress) {
        onProgress({
          loaded: e.loaded,
          total: e.total,
          percentage: Math.round((e.loaded / e.total) * 100),
        });
      }
    });

    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(xhr.response);
      } else if (xhr.status === 401 || xhr.status === 403) {
        localStorage.clear();
        window.location.href = '/login';
        reject(new Error('Unauthorized'));
      } else {
        reject(new Error(`Upload failed: ${xhr.status}`));
      }
    });

    xhr.addEventListener('error', () => reject(new Error('Network error')));
    xhr.addEventListener('abort', () => reject(new Error('Upload cancelled')));

    xhr.open('POST', `${API_BASE_URL}/posting-video`);
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }
    xhr.setRequestHeader('Idempotency-Key', crypto.randomUUID());
    xhr.send(formData);
  });
};

export const updateVideo = async (videoId, title, description) => {
  const response = await api.put(`/update-video/${videoId}`, { title, description });
  return response.data;
};

export const deleteVideo = async (videoId) => {
  const response = await api.delete(`/delete-video/${videoId}`);
  return response.data;
};

export const getStreamUrl = (videoId) => {
  return `${API_BASE_URL}/videos/${videoId}/stream`;
};

export const getThumbnailUrl = (video) => {
  if (video.thumbnailUrl) {
    return `${API_BASE_URL}${video.thumbnailUrl}`;
  }
  return null;
};
