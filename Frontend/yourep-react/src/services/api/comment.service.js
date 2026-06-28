import api from './axios.config';

export const getComments = async (videoId) => {
  const response = await api.get(`/comments/${videoId}`);
  return response.data;
};

export const getCommentCount = async (videoId) => {
  const response = await api.get(`/comments/${videoId}/count`);
  return response.data;
};

export const createComment = async (videoId, content, parentId) => {
  const body = parentId ? { content, parentId } : { content };
  const response = await api.post(`/comments/${videoId}`, body);
  return response.data;
};

export const replyToComment = async (videoId, parentId, content) => {
  const response = await api.post(`/comments/${videoId}/reply/${parentId}`, { content });
  return response.data;
};

export const toggleCommentLike = async (commentId) => {
  const response = await api.post(`/comments/${commentId}/like`);
  return response.data;
};

export const deleteComment = async (commentId) => {
  const response = await api.delete(`/comments/${commentId}`);
  return response.data;
};
