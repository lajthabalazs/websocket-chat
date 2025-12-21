const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const loginBtn = document.getElementById('loginBtn');
const registerBtn = document.getElementById('registerBtn');
const messageDiv = document.getElementById('message');
const authForm = document.getElementById('authForm');
const loginView = document.getElementById('loginView');
const loggedInView = document.getElementById('loggedInView');
const userEmailDisplay = document.getElementById('userEmailDisplay');
const userIdDisplay = document.getElementById('userIdDisplay');
const logoutBtn = document.getElementById('logoutBtn');

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
            // Token is now stored in HTTP-only cookie by the server
            // Store userId and email in localStorage for display purposes only
            localStorage.setItem('userId', data.userId);
            localStorage.setItem('userEmail', email); // Store email for display
            
            // Update UI to show logged-in state
            showLoggedInState(email, data.userId);
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

// Check authentication state on page load
async function checkAuthState() {
    try {
        // Check if user is authenticated by calling the /auth/me endpoint
        // This reads from the HTTP-only cookie server-side without exposing the token
        const response = await fetch('/auth/me', {
            method: 'GET',
            credentials: 'include' // Important: include cookies in request
        });
        
        if (response.ok) {
            const data = await response.json();
            const email = localStorage.getItem('userEmail') || 'User'; // Fallback if email not in localStorage
            showLoggedInState(email, data.userId);
        } else {
            // Not authenticated, clear any stale localStorage data
            localStorage.removeItem('userId');
            localStorage.removeItem('userEmail');
            showLoginState();
        }
    } catch (error) {
        console.error('Error checking auth state:', error);
        showLoginState();
    }
}

function showLoggedInState(email, userId) {
    // Hide login form
    loginView.style.display = 'none';
    
    // Show logged-in view
    loggedInView.style.display = 'block';
    
    // Update user info display
    userEmailDisplay.textContent = email;
    userIdDisplay.textContent = userId;
}

function showLoginState() {
    // Show login form
    loginView.style.display = 'block';
    
    // Hide logged-in view
    loggedInView.style.display = 'none';
    
    // Clear form
    emailInput.value = '';
    passwordInput.value = '';
    hideMessage();
}

async function logout() {
    try {
        // Call logout endpoint to clear HTTP-only cookie
        const response = await fetch('/auth/logout', {
            method: 'POST',
            credentials: 'include' // Important: include cookies in request
        });
        
        // Clear localStorage
        localStorage.removeItem('userId');
        localStorage.removeItem('userEmail');
        
        // Show login form
        showLoginState();
        
        if (response.ok) {
            showMessage('You have been logged out.', 'info');
        } else {
            showMessage('Logged out locally.', 'info');
        }
    } catch (error) {
        console.error('Logout error:', error);
        // Clear localStorage anyway
        localStorage.removeItem('userId');
        localStorage.removeItem('userEmail');
        showLoginState();
        showMessage('Logged out locally.', 'info');
    }
}

// Event listener for logout button
logoutBtn.addEventListener('click', logout);

// Check auth state when page loads
checkAuthState();

