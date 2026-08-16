import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import CommentForm from './CommentForm';
import { replyToComment, toggleCommentLike, deleteComment } from '../../services/api/comment.service';

const timeAgo = (dateStr) => {
  const now = new Date();
  const date = new Date(dateStr);
  const seconds = Math.floor((now - date) / 1000);

  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  const months = Math.floor(days / 30);
  if (months < 12) return `${months}mo ago`;
  return `${Math.floor(months / 12)}y ago`;
};

const CommentCard = ({ comment, videoId, onCommentUpdated, isReply = false }) => {
  const { isAuthenticated, user } = useAuth();
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [showReplies, setShowReplies] = useState(!isReply);
  const [liked, setLiked] = useState(comment.likedByCurrentUser);
  const [likeCount, setLikeCount] = useState(comment.likeCount);
  const [deleted, setDeleted] = useState(false);
  const [error, setError] = useState('');

  const isOwner = isAuthenticated && user?.username === comment.username;
  const isUnverified = isAuthenticated && user && user.emailVerified === false;

  const handleReply = async (content) => {
    await replyToComment(videoId, comment.commentId, content);
    setShowReplyForm(false);
    setShowReplies(true);
    if (onCommentUpdated) onCommentUpdated();
  };

  const handleLike = async () => {
    if (!isAuthenticated || isUnverified) return;
    try {
      await toggleCommentLike(comment.commentId);
      setLiked(!liked);
      setLikeCount((c) => (liked ? c - 1 : c + 1));
    } catch {
      setError('Failed to toggle like');
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Delete this comment?')) return;
    try {
      await deleteComment(comment.commentId);
      setDeleted(true);
    } catch {
      setError('Failed to delete comment');
    }
  };

  if (deleted) return null;

  return (
    <div className={`comment-card ${isReply ? 'comment-reply' : ''}`}>
      <div className="comment-avatar">
        {comment.username?.charAt(0).toUpperCase()}
      </div>
      <div className="comment-body">
        <div className="comment-header">
          <span className="comment-username">@{comment.username}</span>
          <span className="comment-time">{timeAgo(comment.createdAt)}</span>
          {comment.updatedAt !== comment.createdAt && (
            <span className="comment-edited">(edited)</span>
          )}
        </div>
        <div className="comment-content">{comment.content}</div>
        <div className="comment-actions">
          <button
            className={`comment-action-btn ${liked ? 'liked' : ''}`}
            onClick={handleLike}
            disabled={!isAuthenticated || isUnverified}
            title={isUnverified ? 'Verify your email to like comments' : ''}
          >
            {liked ? '👍' : '👍'} <span>{likeCount || ''}</span>
          </button>
          {isAuthenticated && (
            isUnverified ? (
              <Link to="/verify" className="comment-action-btn comment-action-verify">
                Verify email to reply
              </Link>
            ) : (
              <button
                className="comment-action-btn"
                onClick={() => setShowReplyForm(!showReplyForm)}
              >
                ↩ Reply
              </button>
            )
          )}
          {isOwner && (
            <button className="comment-action-btn comment-action-delete" onClick={handleDelete}>
              🗑️
            </button>
          )}
        </div>
        {error && <div className="message error" style={{ marginTop: 8 }}>{error}</div>}

        {showReplyForm && (
          <div style={{ marginTop: 12 }}>
            <CommentForm
              onSubmit={handleReply}
              placeholder={`Reply to @${comment.username}...`}
              submitLabel="Reply"
              onCancel={() => setShowReplyForm(false)}
              autoFocus
            />
          </div>
        )}

        {comment.replies && comment.replies.length > 0 && (
          <>
            {!showReplies ? (
              <button
                className="comment-show-replies"
                onClick={() => setShowReplies(true)}
              >
                ▶ {comment.replies.length} {comment.replies.length === 1 ? 'reply' : 'replies'}
              </button>
            ) : (
              <>
                <div className="comment-replies" style={{ marginTop: 12 }}>
                  {comment.replies.map((reply) => (
                    <CommentCard
                      key={reply.commentId}
                      comment={reply}
                      videoId={videoId}
                      onCommentUpdated={onCommentUpdated}
                      isReply
                    />
                  ))}
                </div>
                <button
                  className="comment-show-replies"
                  onClick={() => setShowReplies(false)}
                >
                  ▼ Hide replies
                </button>
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default CommentCard;
