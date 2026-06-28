import api from './axios.config';

export const toggleLike = async (videoId) => {
  const response = await api.post(`/likes/${videoId}`);
  return response.data;
};

export const checkLiked = async (videoId) => {
  const response = await api.get(`/likes/${videoId}/check`);
  return response.data;
};

export const getMyLikedVideos = async () => {
  const response = await api.get('/likes/my-likes');
  return response.data;
};

export const getLikeCount = async (videoId) => {
  const response = await api.get(`/likes/${videoId}/count`);
  return response.data;
};
