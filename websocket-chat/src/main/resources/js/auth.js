const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const loginBtn = document.getElementById('loginBtn');
const registerBtn = document.getElementById('registerBtn');
const messageDiv = document.getElementById('message');
const loggedInMessageDiv = document.getElementById('loggedInMessage');
const authForm = document.getElementById('authForm');
const loginView = document.getElementById('loginView');
const loggedInView = document.getElementById('loggedInView');
const userEmailDisplay = document.getElementById('userEmailDisplay');
const userIdDisplay = document.getElementById('userIdDisplay');
const logoutBtn = document.getElementById('logoutBtn');
const gameSelect = document.getElementById('gameSelect');
const startNewGameBtn = document.getElementById('startNewGameBtn');
const joinGameBtn = document.getElementById('joinGameBtn');
const refreshGamesBtn = document.getElementById('refreshGamesBtn');

function showMessage(text, type) {
    const activeMessageDiv = loggedInView.style.display !== 'none' ? loggedInMessageDiv : messageDiv;
    activeMessageDiv.textContent = text;
    activeMessageDiv.className = `message ${type}`;
    activeMessageDiv.style.display = 'block';
    
    // Auto-hide success messages after 3 seconds
    if (type === 'success') {
        setTimeout(() => {
            activeMessageDiv.style.display = 'none';
        }, 3000);
    }
}

function hideMessage() {
    messageDiv.style.display = 'none';
    if (loggedInMessageDiv) {
        loggedInMessageDiv.style.display = 'none';
    }
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
            // Store userId in localStorage for game operations
            localStorage.setItem('userId', data.userId);
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
    
    // Load games list when user logs in
    loadGamesList();
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

// Game management functions
async function loadGamesList() {
    try {
        const response = await fetch('/games', {
            method: 'GET',
            credentials: 'include'
        });
        
        if (response.ok) {
            const games = await response.json();
            populateGameDropdown(games);
        } else {
            console.error('Failed to load games:', response.status);
            showMessage('Failed to load games list', 'error');
        }
    } catch (error) {
        console.error('Error loading games:', error);
        showMessage('Error loading games list', 'error');
    }
}

function populateGameDropdown(games) {
    // Clear existing options except the first placeholder
    gameSelect.innerHTML = '<option value="">-- No game selected --</option>';
    
    if (games && games.length > 0) {
        games.forEach(game => {
            const option = document.createElement('option');
            option.value = game.gameId;
            option.textContent = `${game.name} (${game.gameId})`;
            gameSelect.appendChild(option);
        });
    } else {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = '-- No games available --';
        option.disabled = true;
        gameSelect.appendChild(option);
    }
    
    // Update join button state
    updateJoinButtonState();
}

function updateJoinButtonState() {
    const selectedGameId = gameSelect.value;
    joinGameBtn.disabled = !selectedGameId || selectedGameId === '';
}

async function startNewGame() {
    const userId = localStorage.getItem('userId');
    if (!userId) {
        showMessage('User ID not found. Please log in again.', 'error');
        return;
    }
    
    // Prompt for game name
    const gameName = prompt('Enter a name for your new game:');
    if (gameName === null) {
        return; // User cancelled
    }
    
    setGameButtonsLoading(true);
    hideMessage();
    
    try {
        const response = await fetch('/games', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify({
                playerId: userId,
                gameParameters: {
                    name: gameName.trim() || 'New Game'
                }
            })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            showMessage(`Game "${gameName}" created successfully!`, 'success');
            // Refresh the games list to include the new game
            await loadGamesList();
            // Select the newly created game
            gameSelect.value = data.gameId;
            updateJoinButtonState();
        } else {
            showMessage(data.error || 'Failed to create game', 'error');
        }
    } catch (error) {
        console.error('Error creating game:', error);
        showMessage('An error occurred while creating the game', 'error');
    } finally {
        setGameButtonsLoading(false);
    }
}

async function joinSelectedGame() {
    const userId = localStorage.getItem('userId');
    if (!userId) {
        showMessage('User ID not found. Please log in again.', 'error');
        return;
    }
    
    const selectedGameId = gameSelect.value;
    if (!selectedGameId) {
        showMessage('Please select a game first', 'error');
        return;
    }
    
    setGameButtonsLoading(true);
    hideMessage();
    
    try {
        const response = await fetch('/games/join', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify({
                playerId: userId,
                gameId: selectedGameId
            })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            const gameName = gameSelect.options[gameSelect.selectedIndex].textContent;
            showMessage(`Successfully joined ${gameName}!`, 'success');
        } else {
            showMessage(data.error || 'Failed to join game', 'error');
        }
    } catch (error) {
        console.error('Error joining game:', error);
        showMessage('An error occurred while joining the game', 'error');
    } finally {
        setGameButtonsLoading(false);
    }
}

async function refreshGamesList() {
    setGameButtonsLoading(true);
    hideMessage();
    
    try {
        await loadGamesList();
        showMessage('Game list refreshed', 'success');
    } catch (error) {
        console.error('Error refreshing games:', error);
        showMessage('Failed to refresh game list', 'error');
    } finally {
        setGameButtonsLoading(false);
    }
}

function setGameButtonsLoading(isLoading) {
    startNewGameBtn.disabled = isLoading;
    joinGameBtn.disabled = isLoading || !gameSelect.value;
    refreshGamesBtn.disabled = isLoading;
    gameSelect.disabled = isLoading;
}

// Event listeners for game management
startNewGameBtn.addEventListener('click', startNewGame);
joinGameBtn.addEventListener('click', joinSelectedGame);
refreshGamesBtn.addEventListener('click', refreshGamesList);
gameSelect.addEventListener('change', updateJoinButtonState);

// Check auth state when page loads
checkAuthState();

