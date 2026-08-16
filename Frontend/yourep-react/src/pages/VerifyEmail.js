import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { sendVerificationCode, verifyEmail } from '../services/api/auth.service';
import usePageTitle from '../hooks/usePageTitle';

const VerifyEmail = () => {
  const { user, isAuthenticated, refreshVerified } = useAuth();
  const location = useLocation();
  const initialEmail = location.state?.email || user?.email || '';
  usePageTitle('Verify Email');

  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [verified, setVerified] = useState(false);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  if (verified || (user && user.emailVerified)) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-header">
            <div className="auth-logo">▶ YouRep</div>
            <p>Email verified!</p>
          </div>
          <div className="message success">Your email has been verified. You now have full access.</div>
          <Link to="/" className="btn btn-primary btn-full">Continue to Home</Link>
        </div>
      </div>
    );
  }

  const handleResend = async () => {
    if (!email.trim()) {
      setError('Please enter your email');
      return;
    }
    setError('');
    setMessage('');
    try {
      setLoading(true);
      await sendVerificationCode(email.trim());
      setMessage('Verification code sent! Check your inbox (and spam / junk folder).');
      setCooldown(60);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send verification code');
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      setLoading(true);
      await verifyEmail(email.trim(), code.trim());
      if (isAuthenticated) {
        refreshVerified();
      }
      setVerified(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Verification failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-logo">▶ YouRep</div>
          <p>Verify your email</p>
        </div>

        {isAuthenticated && (
          <div className="message info">
            You're signed in, but you have <strong>limited access</strong>: you can browse, but upload,
            comment, and like stay locked until your email is verified.
          </div>
        )}

        <div className="message info">
          <strong>Heads up:</strong> unverified accounts are deleted 3 days after signup. Verify your email
          to keep your account and unlock full access.
        </div>

        <div className="message info">
          <strong>Didn't get the code?</strong> Check your <strong>spam / junk</strong> folder too — the email
          can sometimes land there.
        </div>

        <form onSubmit={handleVerify} className="auth-form">
          <div className="form-group">
            <label htmlFor="verifyEmail">Email</label>
            <input
              id="verifyEmail"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email"
              autoComplete="email"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="verifyCode">6-digit code</label>
            <input
              id="verifyCode"
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="Enter the code from your email"
              inputMode="numeric"
              maxLength={6}
              disabled={loading}
            />
          </div>

          {error && <div className="message error">{error}</div>}
          {message && <div className="message success">{message}</div>}

          <button type="submit" className="btn btn-primary btn-full" disabled={loading || code.length !== 6}>
            {loading ? 'Verifying...' : 'Verify Email'}
          </button>
        </form>

        <div className="auth-footer">
          {cooldown > 0 && (
            <p className="resend-timer">
              <span className="resend-timer-icon">&#9203;</span>
              You can request another code in{' '}
              <strong>{Math.floor(cooldown / 60)}:{(cooldown % 60).toString().padStart(2, '0')}</strong>
            </p>
          )}
          <button
            type="button"
            onClick={handleResend}
            className="btn btn-secondary btn-full"
            disabled={loading || cooldown > 0}
          >
            Resend code
          </button>
          {isAuthenticated ? (
            <p>
              Skip for now — <Link to="/">continue to home</Link> with limited access
            </p>
          ) : (
            <p>
              <Link to="/login">Back to sign in</Link>
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default VerifyEmail;
