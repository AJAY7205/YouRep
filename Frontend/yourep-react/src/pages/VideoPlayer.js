import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getVideo, getStreamUrl, updateVideo, deleteVideo } from '../services/api/video.service';
import { toggleLike, checkLiked, getLikeCount } from '../services/api/like.service';
import CommentSection from '../components/comment/CommentSection';

const VideoPlayer = () => {
  const { id } = useParams();
  const { isAuthenticated, user } = useAuth();
  const videoRef = useRef(null);
  const [video, setVideo] = useState(null);
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [buffering, setBuffering] = useState(false);
  const [videoError, setVideoError] = useState('');

  const fetchVideo = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getVideo(id);
      const v = response.content[0];
      setVideo(v);
      setLikeCount(v.likeCount || 0);
      setEditTitle(v.title);
      setEditDescription(v.description || '');
    } catch (err) {
      setError('Failed to load video');
    } finally {
      setLoading(false);
    }
  }, [id]);

  const fetchLikeStatus = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const isLiked = await checkLiked(id);
      setLiked(isLiked);
    } catch (err) {
      console.error('Error checking like:', err);
    }
  }, [id, isAuthenticated]);

  const fetchLikeCount = useCallback(async () => {
    try {
      const count = await getLikeCount(id);
      setLikeCount(count);
    } catch (err) {
      console.error('Error getting like count:', err);
    }
  }, [id]);

  useEffect(() => {
    fetchVideo();
    fetchLikeStatus();
    fetchLikeCount();
  }, [fetchVideo, fetchLikeStatus, fetchLikeCount]);

  const handleLike = async () => {
    if (!isAuthenticated) return;
    try {
      await toggleLike(id);
      setLiked(!liked);
      await fetchLikeCount();
    } catch (err) {
      console.error('Error toggling like:', err);
    }
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      await updateVideo(id, editTitle, editDescription);
      setEditing(false);
      await fetchVideo();
    } catch (err) {
      setError('Failed to update video');
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this video?')) return;
    try {
      await deleteVideo(id);
      window.location.href = '/';
    } catch (err) {
      setError('Failed to delete video');
    }
  };

  const retryVideo = () => {
    setVideoError('');
    setBuffering(false);
    if (videoRef.current) {
      videoRef.current.load();
    }
  };

  const isOwner = video && user && video.username === user.username;
  const isAdmin = user?.roles?.includes('ADMIN');
  const canModify = isOwner || isAdmin;

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="spinner" />
        <p>Loading video...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-state">
        <p>{error}</p>
        <Link to="/" className="btn btn-primary">Back to Home</Link>
      </div>
    );
  }

  if (!video) {
    return (
      <div className="error-state">
        <p>Video not found</p>
        <Link to="/" className="btn btn-primary">Back to Home</Link>
      </div>
    );
  }

  const streamUrl = getStreamUrl(video.videoId);

  return (
    <div className="player-page">
      <div className="player-container">
        <div className="video-player-wrapper">
          {videoError && (
            <div className="video-error-overlay">
              <p>⚠️ {videoError}</p>
              <button onClick={retryVideo} className="btn btn-primary">Retry</button>
            </div>
          )}
          {buffering && !videoError && (
            <div className="buffering-overlay">
              <div className="spinner" />
              <p>Buffering...</p>
            </div>
          )}
          <video
            ref={videoRef}
            className="video-player"
            controls
            autoPlay
            src={streamUrl}
            onError={(e) => {
              const code = e.target?.error?.code;
              console.warn(`Video error (code ${code}): MEDIA_ERR_${['ABORTED','NETWORK','DECODE','SRC_NOT_SUPPORTED'][code-1] || 'UNKNOWN'}`);
              setVideoError('Failed to load video stream. Check your connection.');
            }}
            onStalled={() => setBuffering(true)}
            onWaiting={() => setBuffering(true)}
            onCanPlay={() => { setBuffering(false); setVideoError(''); }}
            onPlaying={() => setBuffering(false)}
          >
            Your browser does not support video playback.
          </video>
        </div>

        <div className="video-info-section">
          <h1 className="video-title">{video.title}</h1>

          <div className="video-actions">
            <div className="video-stats">
              <span className="stat">👁 {video.viewCount ?? 0} views</span>
              <button
                className={`btn btn-like ${liked ? 'liked' : ''}`}
                onClick={handleLike}
                disabled={!isAuthenticated}
              >
                {liked ? '❤️' : '🤍'} {likeCount}
              </button>
            </div>

            {canModify && (
              <div className="video-mod-actions">
                <button onClick={() => setEditing(!editing)} className="btn btn-edit">
                  ✏️ Edit
                </button>
                <button onClick={handleDelete} className="btn btn-delete">
                  🗑️ Delete
                </button>
              </div>
            )}
          </div>

          <div className="video-meta">
            <span className="meta-label">Uploaded by:</span>
            <span className="meta-value">{video.username || 'Unknown'}</span>
            <span className="meta-separator">•</span>
            <span className="meta-label">Status:</span>
            <span className="meta-value">{video.videoStatus}</span>
            <span className="meta-separator">•</span>
            <span className="meta-label">{new Date(video.createdAt).toLocaleDateString('en-US', {
              year: 'numeric', month: 'long', day: 'numeric'
            })}</span>
          </div>

          <div className="video-description">
            <p>{video.description || 'No description provided.'}</p>
          </div>

          {editing && (
            <form onSubmit={handleUpdate} className="edit-form">
              <div className="form-group">
                <label htmlFor="editTitle">Title</label>
                <input
                  id="editTitle"
                  type="text"
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label htmlFor="editDescription">Description</label>
                <textarea
                  id="editDescription"
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  rows={3}
                />
              </div>
              <div className="edit-actions">
                <button type="submit" className="btn btn-primary">Save</button>
                <button type="button" onClick={() => setEditing(false)} className="btn btn-cancel">Cancel</button>
              </div>
            </form>
          )}
        </div>

        <CommentSection videoId={video.videoId} />

        <Link to="/" className="btn btn-back">
          ← Back to Home
        </Link>
      </div>
    </div>
  );
};

export default VideoPlayer;
