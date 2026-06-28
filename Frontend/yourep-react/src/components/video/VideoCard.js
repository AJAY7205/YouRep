import React from 'react';
import { useNavigate } from 'react-router-dom';
import { getThumbnailUrl } from '../../services/api/video.service';

const formatDate = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  const diff = now - date;
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (days === 0) return 'Today';
  if (days === 1) return 'Yesterday';
  if (days < 7) return `${days} days ago`;
  if (days < 30) return `${Math.floor(days / 7)} weeks ago`;
  if (days < 365) return `${Math.floor(days / 30)} months ago`;
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
};

const VideoCard = ({ video }) => {
  const navigate = useNavigate();
  const thumbnailUrl = getThumbnailUrl(video);

  const handleClick = () => {
    navigate(`/watch/${video.videoId}`);
  };

  return (
    <div className="video-card" onClick={handleClick}>
      <div className="video-card-thumbnail">
        <img
          src={thumbnailUrl || 'https://via.placeholder.com/320x180?text=No+Thumbnail'}
          alt={video.title}
          onError={(e) => {
            e.target.src = 'https://via.placeholder.com/320x180?text=No+Thumbnail';
          }}
        />
        <div className="video-card-status">
          {video.videoStatus}
        </div>
      </div>
      <div className="video-card-body">
        <h3 className="video-card-title">{video.title}</h3>
        <p className="video-card-description">
          {video.description || 'No description'}
        </p>
        <div className="video-card-meta">
          <div className="video-card-stats">
            <span className="stat">
              👁 {video.viewCount ?? 0}
            </span>
            <span className="stat">
              👍 {video.likeCount ?? 0}
            </span>
          </div>
          <div className="video-card-info">
            <span className="video-card-user">{video.username || 'Unknown'}</span>
            <span className="video-card-date">{formatDate(video.createdAt)}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VideoCard;
