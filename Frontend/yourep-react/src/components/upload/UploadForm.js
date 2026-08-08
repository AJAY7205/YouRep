import React, { useState, useRef } from 'react';
import { uploadVideo } from '../../services/api/video.service';

const MAX_FILE_SIZE = 500 * 1024 * 1024;

const UploadForm = () => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [videoFile, setVideoFile] = useState(null);
  const [thumbnailFile, setThumbnailFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState({ percentage: 0, loaded: 0, total: 0 });
  const [speed, setSpeed] = useState('');
  const [timeRemaining, setTimeRemaining] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const xhrRef = useRef(null);
  const lastLoadedRef = useRef(0);
  const lastTimeRef = useRef(0);

  const handleVideoChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > MAX_FILE_SIZE) {
        setError('File size exceeds 500MB limit');
        e.target.value = '';
        return;
      }
      setVideoFile(file);
      setError('');
    }
  };

  const handleThumbnailChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setThumbnailFile(file);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!title.trim()) {
      setError('Title is required');
      return;
    }

    if (!videoFile) {
      setError('Please select a video file');
      return;
    }

    const formData = new FormData();
    const metadata = { title: title.trim(), description: description.trim() };
    formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    formData.append('file', videoFile);
    if (thumbnailFile) {
      formData.append('thumbnail', thumbnailFile);
    }

    setUploading(true);
    setProgress({ percentage: 0, loaded: 0, total: videoFile.size });
    lastLoadedRef.current = 0;
    lastTimeRef.current = Date.now();
    setSpeed('');
    setTimeRemaining('');

    try {
      await uploadVideo(formData, (clientProgress) => {
        setProgress(clientProgress);

        const now = Date.now();
        const timeDiff = (now - lastTimeRef.current) / 1000;

        if (timeDiff > 0) {
          const bytesDiff = clientProgress.loaded - lastLoadedRef.current;
          const speedBps = bytesDiff / timeDiff;
          const speedMBps = (speedBps / (1024 * 1024)).toFixed(2);
          setSpeed(`${speedMBps} MB/s`);

          const remaining = clientProgress.total - clientProgress.loaded;
          const remainingSecs = remaining / speedBps;
          if (isFinite(remainingSecs) && remainingSecs > 0) {
            const mins = Math.floor(remainingSecs / 60);
            const secs = Math.floor(remainingSecs % 60);
            setTimeRemaining(`${mins}m ${secs}s`);
          }
        }

        lastLoadedRef.current = clientProgress.loaded;
        lastTimeRef.current = now;
      });

      setSuccess('Video uploaded! It is now being processed and will be available soon.');
      setUploading(false);
      setTitle('');
      setDescription('');
      setVideoFile(null);
      setThumbnailFile(null);
    } catch (err) {
      if (err.message === 'Upload cancelled') {
        setError('Upload cancelled');
      } else {
        setError(err.message || 'Upload failed');
      }
      setUploading(false);
    }
  };

  const handleCancel = () => {
    if (xhrRef.current) {
      xhrRef.current.abort();
    }
    setUploading(false);
    setError('Upload cancelled');
  };

  const formatSize = (bytes) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  return (
    <div className="upload-form-container">
      <h2 className="upload-title">Upload Video</h2>

      {!uploading ? (
        <form onSubmit={handleSubmit} className="upload-form">
          <div className="form-group">
            <label htmlFor="title">Title *</label>
            <input
              id="title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Enter video title"
              maxLength={200}
              disabled={uploading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Enter video description"
              rows={4}
              disabled={uploading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="videoFile">Video File * (max 500MB)</label>
            <input
              id="videoFile"
              type="file"
              accept="video/*"
              onChange={handleVideoChange}
              disabled={uploading}
              className="file-input"
            />
            {videoFile && (
              <span className="file-info">{videoFile.name} ({formatSize(videoFile.size)})</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="thumbnailFile">Thumbnail (optional)</label>
            <input
              id="thumbnailFile"
              type="file"
              accept="image/*"
              onChange={handleThumbnailChange}
              disabled={uploading}
              className="file-input"
            />
            {thumbnailFile && (
              <span className="file-info">{thumbnailFile.name}</span>
            )}
          </div>

          {error && <div className="message error">{error}</div>}
          {success && <div className="message success">{success}</div>}

          <button type="submit" className="btn btn-upload" disabled={uploading}>
            Upload Video
          </button>
        </form>
      ) : (
        <div className="upload-progress-container">
          <div className="progress-header">
            <span className="file-name">{videoFile?.name}</span>
          </div>

          <div className="progress-bar-container">
            <div
              className="progress-bar-fill"
              style={{ width: `${progress.percentage}%` }}
            />
          </div>

          <div className="progress-stats">
            <span className="progress-percentage">{progress.percentage}%</span>
            <span className="progress-size">
              {formatSize(progress.loaded)} / {formatSize(progress.total)}
            </span>
          </div>

          <div className="progress-details">
            {speed && <span className="progress-speed">Speed: {speed}</span>}
            {timeRemaining && (
              <span className="progress-time">Remaining: {timeRemaining}</span>
            )}
          </div>

          {error && <div className="message error">{error}</div>}
          {success && <div className="message success">{success}</div>}

          <button onClick={handleCancel} className="btn btn-cancel">
            Cancel
          </button>
        </div>
      )}
    </div>
  );
};

export default UploadForm;
