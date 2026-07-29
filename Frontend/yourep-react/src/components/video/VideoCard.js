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

const FALLBACK_THUMBNAIL = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='320' height='180'%3E%3Crect fill='%23333' width='320' height='180'/%3E%3Ctext fill='%23999' font-family='sans-serif' font-size='16' text-anchor='middle' x='160' y='95'%3ENo Thumbnail%3C/text%3E%3C/svg%3E";

const VideoCard = ({ video }) => {
  const navigate = useNavigate();
  const thumbnailUrl = getThumbnailUrl(video);
  const [imgError, setImgError] = React.useState(false);

  const handleClick = () => {
    navigate(`/watch/${video.videoId}`);
  };

  return (
    <div className="video-card" onClick={handleClick}>
      <div className="video-card-thumbnail">
        <img
          src={imgError || !thumbnailUrl ? FALLBACK_THUMBNAIL : thumbnailUrl}
          alt={video.title}
          loading="lazy"
          onError={() => setImgError(true)}
        />
        {video.videoStatus !== 'UPLOADED' && (
          <div className="video-card-status">
            {video.videoStatus}
          </div>
        )}
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
