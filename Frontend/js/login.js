const API_BASE_URL = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    // Check if already logged in
    const token = localStorage.getItem('authToken');
    if (token) {
        window.location.href = 'index.html';
        return;
    }

    setupLoginForm();
});

function setupLoginForm() {
    const form = document.getElementById('loginForm');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await handleLogin();
    });
}

async function handleLogin() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const loginBtn = document.getElementById('loginBtn');
    const loginBtnText = document.getElementById('loginBtnText');
    const errorMessage = document.getElementById('errorMessage');

    // Validate inputs
    if (!username || !password) {
        showError('Please fill in all fields');
        return;
    }

    // Show loading state
    loginBtn.disabled = true;
    loginBtnText.textContent = 'Signing in...';
    errorMessage.classList.remove('show');

    try {
        const response = await fetch(`${API_BASE_URL}/auth/signin`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });

        const data = await response.json();

        if (response.ok) {
            // Store authentication data
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('userId', data.id);
            localStorage.setItem('username', data.username);
            localStorage.setItem('userEmail', data.email);
            localStorage.setItem('userRoles', JSON.stringify(data.roles));

            // Show success and redirect
            showSuccess('Login successful! Redirecting...');
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 1000);

        } else {
            showError(data.message || 'Login failed. Please check your credentials.');
        }

    } catch (error) {
        console.error('Login error:', error);
        showError('Network error. Please try again later.');
    } finally {
        loginBtn.disabled = false;
        loginBtnText.textContent = 'Sign In';
    }
}

function showError(message) {
    const errorMessage = document.getElementById('errorMessage');
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
}

function showSuccess(message) {
    const errorMessage = document.getElementById('errorMessage');
    errorMessage.textContent = message;
    errorMessage.style.background = 'rgba(0, 255, 0, 0.1)';
    errorMessage.style.borderColor = 'var(--success)';
    errorMessage.style.color = 'var(--success)';
    errorMessage.classList.add('show');
}