import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import UploadForm from '../components/upload/UploadForm';
import usePageTitle from '../hooks/usePageTitle';

const Upload = () => {
  const { isAuthenticated, user } = useAuth();
  usePageTitle('Upload');

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

  if (user && user.emailVerified === false) {
    return (
      <div className="upload-page">
        <div className="upload-auth-prompt">
          <div className="prompt-icon">📧</div>
          <h2>Verify Your Email</h2>
          <p>You have limited access until your email is verified. Verify to unlock video uploads.</p>
          <Link to="/verify" className="btn btn-primary">
            Verify Email
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
