import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { getAllVideos } from '../services/api/video.service';
import { getMyLikedVideos } from '../services/api/like.service';
import VideoCard from '../components/video/VideoCard';
import usePageTitle from '../hooks/usePageTitle';

const Home = () => {
  const { isAuthenticated } = useAuth();
  usePageTitle('Home');
  const [videos, setVideos] = useState([]);
  const [likedVideos, setLikedVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchVideos = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getAllVideos();
      setVideos(response.content || []);
    } catch (err) {
      setError('Failed to load videos');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchLikedVideos = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const data = await getMyLikedVideos();
      setLikedVideos(data || []);
    } catch (err) {
      console.error('Error loading liked videos:', err);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchVideos();
    fetchLikedVideos();
  }, [fetchVideos, fetchLikedVideos]);

  return (
    <div className="home-page">
      {isAuthenticated && likedVideos.length > 0 && (
        <section className="section liked-section">
          <h2 className="section-title">
            ❤️ Liked Videos
          </h2>
          <div className="liked-scroll">
            {likedVideos.map((video) => (
              <div key={video.videoId} className="liked-card-wrapper">
                <VideoCard video={video} />
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="section">
        <h2 className="section-title">
          All Videos
        </h2>

        {loading ? (
          <div className="loading-state">
            <div className="spinner" />
            <p>Loading videos...</p>
          </div>
        ) : error ? (
          <div className="error-state">
            <p>{error}</p>
            <button onClick={fetchVideos} className="btn btn-retry">
              Retry
            </button>
          </div>
        ) : videos.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">🎬</div>
            <p>No videos uploaded yet.</p>
            {isAuthenticated && (
              <a href="/upload" className="btn btn-upload">
                Upload the first video
              </a>
            )}
          </div>
        ) : (
          <div className="video-grid">
            {videos.map((video) => (
              <VideoCard key={video.videoId} video={video} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

export default Home;
