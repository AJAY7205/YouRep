import React, { useState, useRef, useEffect } from 'react';

const CommentForm = ({ onSubmit, placeholder, submitLabel, initialValue, onCancel, autoFocus }) => {
  const [text, setText] = useState(initialValue || '');
  const [submitting, setSubmitting] = useState(false);
  const inputRef = useRef(null);

  useEffect(() => {
    if (autoFocus && inputRef.current) {
      inputRef.current.focus();
    }
  }, [autoFocus]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed) return;

    setSubmitting(true);
    try {
      await onSubmit(trimmed);
      setText('');
    } catch {
      // error handled by parent
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="comment-form" onSubmit={handleSubmit}>
      <div className="comment-form-input-wrapper">
        <textarea
          ref={inputRef}
          className="comment-form-input"
          placeholder={placeholder || 'Write a comment...'}
          value={text}
          onChange={(e) => setText(e.target.value)}
          rows={1}
          onInput={(e) => {
            e.target.style.height = 'auto';
            e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px';
          }}
          disabled={submitting}
        />
      </div>
      <div className="comment-form-actions">
        {onCancel && (
          <button type="button" className="comment-btn comment-btn-cancel" onClick={onCancel}>
            Cancel
          </button>
        )}
        <button
          type="submit"
          className="comment-btn comment-btn-submit"
          disabled={!text.trim() || submitting}
        >
          {submitting ? 'Posting...' : submitLabel || 'Comment'}
        </button>
      </div>
    </form>
  );
};

export default CommentForm;
