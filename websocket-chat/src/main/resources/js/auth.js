const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const loginBtn = document.getElementById('loginBtn');
const registerBtn = document.getElementById('registerBtn');
const messageDiv = document.getElementById('message');
const authForm = document.getElementById('authForm');

function showMessage(text, type) {
    messageDiv.textContent = text;
    messageDiv.className = `message ${type}`;
    messageDiv.style.display = 'block';
    
    // Auto-hide success messages after 3 seconds
    if (type === 'success') {
        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 3000);
    }
}

function hideMessage() {
    messageDiv.style.display = 'none';
}

function setLoading(isLoading) {
    loginBtn.disabled = isLoading;
    registerBtn.disabled = isLoading;
    emailInput.disabled = isLoading;
    passwordInput.disabled = isLoading;
}

async function login() {
    const email = emailInput.value.trim();
    const password = passwordInput.value;

    if (!email || !password) {
        showMessage('Please fill in all fields', 'error');
        return;
    }

    setLoading(true);
    hideMessage();

    try {
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (response.ok) {
            // Store token in localStorage
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('userId', data.userId);
            
            showMessage('Login successful! You are now logged in.', 'success');
            
            // Optionally redirect to a chat page if it exists
            // For now, just show success message
            // setTimeout(() => {
            //     window.location.href = '/chat.html';
            // }, 2000);
        } else {
            showMessage(data.error || 'Login failed. Please check your credentials.', 'error');
            setLoading(false);
        }
    } catch (error) {
        console.error('Login error:', error);
        showMessage('An error occurred. Please try again.', 'error');
        setLoading(false);
    }
}

async function register() {
    const email = emailInput.value.trim();
    const password = passwordInput.value;

    if (!email || !password) {
        showMessage('Please fill in all fields', 'error');
        return;
    }

    if (password.length < 6) {
        showMessage('Password must be at least 6 characters long', 'error');
        return;
    }

    setLoading(true);
    hideMessage();

    try {
        const response = await fetch('/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (response.ok) {
            showMessage('Registration successful! You can now login.', 'success');
            setLoading(false);
            // Clear password field
            passwordInput.value = '';
        } else {
            showMessage(data.error || 'Registration failed. Email may already be in use.', 'error');
            setLoading(false);
        }
    } catch (error) {
        console.error('Registration error:', error);
        showMessage('An error occurred. Please try again.', 'error');
        setLoading(false);
    }
}

// Event listeners
loginBtn.addEventListener('click', login);
registerBtn.addEventListener('click', register);

authForm.addEventListener('submit', (e) => {
    e.preventDefault();
    login();
});

// Allow Enter key to submit form (login)
passwordInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        e.preventDefault();
        login();
    }
});

