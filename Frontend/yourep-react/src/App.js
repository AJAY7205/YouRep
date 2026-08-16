import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Header from './components/layout/Header';
import ErrorBoundary from './components/common/ErrorBoundary';
import ProtectedRoute from './components/auth/ProtectedRoute';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Upload from './pages/Upload';
import VideoPlayer from './pages/VideoPlayer';
import VerifyEmail from './pages/VerifyEmail';
import './App.css';

function App() {
  return (
    <div className="app">
      <Header />
      <main className="main-content">
        <ErrorBoundary>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/verify" element={<VerifyEmail />} />
            <Route path="/watch/:id" element={<VideoPlayer />} />
            <Route
              path="/upload"
              element={
                <ProtectedRoute>
                  <Upload />
                </ProtectedRoute>
              }
            />
          </Routes>
        </ErrorBoundary>
      </main>
    </div>
  );
}

export default App;
