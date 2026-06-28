import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import CommentCard from './CommentCard';
import CommentForm from './CommentForm';
import {
  getComments,
  getCommentCount,
  createComment,
} from '../../services/api/comment.service';
import { Link } from 'react-router-dom';

const CommentSection = ({ videoId }) => {
  const { isAuthenticated } = useAuth();
  const [comments, setComments] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchComments = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getComments(videoId);
      setComments(data.comments || []);
      setTotalCount(data.totalCount || 0);
    } catch {
      setError('Failed to load comments');
    } finally {
      setLoading(false);
    }
  }, [videoId]);

  const fetchCount = useCallback(async () => {
    try {
      const count = await getCommentCount(videoId);
      setTotalCount(count);
    } catch {
      // silent
    }
  }, [videoId]);

  useEffect(() => {
    fetchComments();
    fetchCount();
  }, [fetchComments, fetchCount]);

  const handleNewComment = async (content) => {
    await createComment(videoId, content, null);
    await fetchComments();
    await fetchCount();
  };

  return (
    <div className="comment-section">
      <div className="comment-section-header">
        <h3 className="comment-section-title">
          {totalCount} {totalCount === 1 ? 'Comment' : 'Comments'}
        </h3>
      </div>

      {isAuthenticated ? (
        <div className="comment-section-form">
          <CommentForm
            onSubmit={handleNewComment}
            placeholder="Add a comment..."
            submitLabel="Comment"
          />
        </div>
      ) : (
        <div className="comment-login-prompt">
          <Link to="/login">Sign in</Link> to leave a comment
        </div>
      )}

      {loading ? (
        <div className="loading-state" style={{ padding: '40px 0' }}>
          <div className="spinner" />
        </div>
      ) : error ? (
        <div className="message error" style={{ marginTop: 16 }}>{error}</div>
      ) : comments.length === 0 ? (
        <div className="comment-empty">
          <div className="comment-empty-icon">💬</div>
          <p>No comments yet. Be the first to share your thoughts!</p>
        </div>
      ) : (
        <div className="comment-list" style={{ marginTop: 16 }}>
          {comments.map((comment) => (
            <CommentCard
              key={comment.commentId}
              comment={comment}
              videoId={videoId}
              onCommentUpdated={fetchComments}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default CommentSection;
