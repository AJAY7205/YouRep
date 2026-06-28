import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import UploadForm from '../components/upload/UploadForm';

const Upload = () => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return (
      <div className="upload-page">
        <div className="upload-auth-prompt">
          <div className="prompt-icon">🔒</div>
          <h2>Login Required</h2>
          <p>Please sign in to upload videos.</p>
          <Link to="/login" className="btn btn-primary">
            Sign In
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="upload-page">
      <UploadForm />
    </div>
  );
};

export default Upload;
