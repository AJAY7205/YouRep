const API_BASE_URL = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    setupSignupForm();
});

function setupSignupForm() {
    const form = document.getElementById('signupForm');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await handleSignup();
    });
}

async function handleSignup() {
    const username = document.getElementById('username').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const signupBtn = document.getElementById('signupBtn');
    const signupBtnText = document.getElementById('signupBtnText');

    // Clear previous messages
    clearMessages();

    // Validate inputs
    if (!username || !email || !password || !confirmPassword) {
        showError('Please fill in all fields');
        return;
    }

    if (username.length < 3 || username.length > 20) {
        showError('Username must be between 3 and 20 characters');
        return;
    }

    if (password.length < 8) {
        showError('Password must be at least 8 characters long');
        return;
    }

    if (password !== confirmPassword) {
        showError('Passwords do not match');
        return;
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showError('Please enter a valid email address');
        return;
    }

    // Show loading state
    signupBtn.disabled = true;
    signupBtnText.textContent = 'Creating Account...';

    try {
        const response = await fetch(`${API_BASE_URL}/auth/signup`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                email: email,
                password: password,
                roles: ['user'] // Default role
            })
        });

        const data = await response.json();

        if (response.ok) {
            showSuccess('Account created successfully! Redirecting to login...');
            document.getElementById('signupForm').reset();
            
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2000);

        } else {
            showError(data.message || 'Signup failed. Please try again.');
        }

    } catch (error) {
        console.error('Signup error:', error);
        showError('Network error. Please try again later.');
    } finally {
        signupBtn.disabled = false;
        signupBtnText.textContent = 'Create Account';
    }
}

function showError(message) {
    const errorMessage = document.getElementById('errorMessage');
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
}

function showSuccess(message) {
    const successMessage = document.getElementById('successMessage');
    successMessage.textContent = message;
    successMessage.classList.add('show');
}

function clearMessages() {
    document.getElementById('errorMessage').classList.remove('show');
    document.getElementById('successMessage').classList.remove('show');
}