// Test client for exercising the WebSocket chat server with 5 built-in users.

const testUsers = [
    { id: 1, name: 'Test User 1', email: 'test1@example.com', password: 'password1' },
    { id: 2, name: 'Test User 2', email: 'test2@example.com', password: 'password2' },
    { id: 3, name: 'Test User 3', email: 'test3@example.com', password: 'password3' },
    { id: 4, name: 'Test User 4', email: 'test4@example.com', password: 'password4' },
    { id: 5, name: 'Test User 5', email: 'test5@example.com', password: 'password5' }
];

// Per-user state (token, websocket, status)
const userState = {};

// Initialize state objects
testUsers.forEach(user => {
    userState[user.id] = {
        token: null,
        websocket: null,
        statusEl: null
    };
});

function setStatus(userId, text, type) {
    const state = userState[userId];
    if (!state || !state.statusEl) {
        return;
    }
    state.statusEl.textContent = text;
    state.statusEl.className = 'status-pill ' + (type || '');
}

async function registerUser(userId) {
    const user = testUsers.find(u => u.id === userId);
    if (!user) return;

    setStatus(userId, 'Registering...', 'info');

    try {
        const response = await fetch('/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email: user.email,
                password: user.password
            })
        });

        const data = await response.json().catch(() => ({}));

        if (response.ok) {
            setStatus(userId, 'Registered', 'success');
        } else {
            setStatus(userId, data.error || 'Registration failed', 'error');
        }
    } catch (e) {
        console.error('Register error for user', userId, e);
        setStatus(userId, 'Error registering user', 'error');
    }
}

async function loginUser(userId) {
    const user = testUsers.find(u => u.id === userId);
    if (!user) return;

    setStatus(userId, 'Logging in...', 'info');

    try {
        // Login to set cookie and get userId
        const loginResponse = await fetch('/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email: user.email,
                password: user.password
            })
        });

        const loginData = await loginResponse.json().catch(() => ({}));

        if (!loginResponse.ok) {
            setStatus(userId, loginData.error || 'Login failed', 'error');
            return;
        }

        setStatus(userId, 'Logged in', 'success');
    } catch (e) {
        console.error('Login error for user', userId, e);
        setStatus(userId, 'Error logging in user', 'error');
    }
}

function getWebSocketUrl(token) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    const port = '8080'; // WebSocket server port
    return `${protocol}//${host}:${port}/websocket`;
}

function connectWebSocket(userId) {
    const state = userState[userId];
    if (!state) {
        setStatus(userId, 'No user id. Login first.', 'error');
        return;
    }

    // Close existing connection if present
    if (state.websocket) {
        try {
            state.websocket.close();
        } catch (e) {
            console.warn('Error closing previous WebSocket for user', userId, e);
        }
        state.websocket = null;
    }

    const url = getWebSocketUrl(state.token);
    console.log('[TestClient] Creating WebSocket for user', userId, 'readyState before:', state.websocket ? state.websocket.readyState : 'none');
    setStatus(userId, 'Connecting WebSocket...', 'info');

    try {
        const ws = new WebSocket(url);
        state.websocket = ws;

        ws.onopen = () => {
            console.log('[TestClient] WebSocket onopen for user', userId, 'readyState:', ws.readyState);
            setStatus(userId, 'WebSocket connected', 'success');
        };

        ws.onclose = (event) => {
            console.log('[TestClient] WebSocket onclose for user', userId, 'code:', event.code, 'reason:', event.reason, 'wasClean:', event.wasClean);
            console.log('[TestClient] WebSocket readyState at close for user', userId, ws.readyState);
            setStatus(userId, 'WebSocket closed', 'info');
        };

        ws.onerror = (error) => {
            console.error('[TestClient] WebSocket onerror for user', userId, 'error:', error, 'readyState:', ws.readyState);
            setStatus(userId, 'WebSocket error', 'error');
        };

        ws.onmessage = (event) => {
            console.log('[TestClient] WebSocket onmessage for user', userId, 'data:', event.data);

            // For the 5th user, append received messages to the textarea field
            if (userId === 5) {
                const textarea = document.getElementById('user5Messages');
                if (textarea) {
                    const ts = new Date().toLocaleTimeString();
                    textarea.value += `[${ts}] ${event.data}\n`;
                    textarea.scrollTop = textarea.scrollHeight;
                }
            }
        };
    } catch (e) {
        console.error('Failed to create WebSocket for user', userId, e);
        setStatus(userId, 'Error connecting WebSocket', 'error');
    }
}

function sendTestMessage(userId) {
    const state = userState[userId];
    if (!state || !state.websocket || state.websocket.readyState !== WebSocket.OPEN) {
        setStatus(userId, 'WebSocket not connected', 'error');
        return;
    }

    const user = testUsers.find(u => u.id === userId);
    const payload = {
        type: 'sendMessage',
        message: `Test message from ${user ? user.name : 'User ' + userId} at ${new Date().toLocaleTimeString()}`
    };

    try {
        state.websocket.send(JSON.stringify(payload));
        setStatus(userId, 'Test message sent', 'success');
    } catch (e) {
        console.error('Error sending message for user', userId, e);
        setStatus(userId, 'Error sending message', 'error');
    }
}

function buildTestUsersTable() {
    const tbody = document.getElementById('testUsersTableBody');
    if (!tbody) {
        console.error('testUsersTableBody element not found');
        return;
    }

    tbody.innerHTML = '';

    testUsers.forEach(user => {
        const tr = document.createElement('tr');

        const idTd = document.createElement('td');
        idTd.textContent = user.id.toString();

        const emailTd = document.createElement('td');
        emailTd.textContent = user.email;

        const passwordTd = document.createElement('td');
        passwordTd.textContent = user.password;

        const registerTd = document.createElement('td');
        const registerBtn = document.createElement('button');
        registerBtn.textContent = 'Register';
        registerBtn.className = 'btn btn-secondary';
        registerBtn.addEventListener('click', () => registerUser(user.id));
        registerTd.appendChild(registerBtn);

        const loginTd = document.createElement('td');
        const loginBtn = document.createElement('button');
        loginBtn.textContent = 'Login';
        loginBtn.className = 'btn btn-primary';
        loginBtn.addEventListener('click', () => loginUser(user.id));
        loginTd.appendChild(loginBtn);

        const connectTd = document.createElement('td');
        const connectBtn = document.createElement('button');
        connectBtn.textContent = 'Connect';
        connectBtn.className = 'btn btn-primary';
        connectBtn.addEventListener('click', () => connectWebSocket(user.id));
        connectTd.appendChild(connectBtn);

        const sendTd = document.createElement('td');
        const sendBtn = document.createElement('button');
        sendBtn.textContent = 'Send';
        sendBtn.className = 'btn btn-primary';
        sendBtn.addEventListener('click', () => sendTestMessage(user.id));
        sendTd.appendChild(sendBtn);

        const statusTd = document.createElement('td');
        const statusSpan = document.createElement('span');
        statusSpan.className = 'status-pill info';
        statusSpan.textContent = 'Idle';
        statusTd.appendChild(statusSpan);
        userState[user.id].statusEl = statusSpan;

        tr.appendChild(idTd);
        tr.appendChild(emailTd);
        tr.appendChild(passwordTd);
        tr.appendChild(registerTd);
        tr.appendChild(loginTd);
        tr.appendChild(connectTd);
        tr.appendChild(sendTd);
        tr.appendChild(statusTd);

        tbody.appendChild(tr);
    });
}

// Basic styles for status pill, reusing existing CSS classes where possible
function injectTestClientStyles() {
    const style = document.createElement('style');
    style.textContent = `
        .table-wrapper {
            overflow-x: auto;
        }
        .game-table {
            width: 100%;
            border-collapse: collapse;
        }
        .game-table th,
        .game-table td {
            padding: 8px 10px;
            border-bottom: 1px solid #e5e7eb;
            text-align: left;
            font-size: 14px;
        }
        .game-table th {
            background-color: #f9fafb;
            font-weight: 600;
        }
        .status-pill {
            display: inline-flex;
            align-items: center;
            padding: 2px 8px;
            border-radius: 9999px;
            font-size: 12px;
            border: 1px solid transparent;
        }
        .status-pill.info {
            background-color: #eff6ff;
            color: #1d4ed8;
            border-color: #bfdbfe;
        }
        .status-pill.success {
            background-color: #ecfdf3;
            color: #166534;
            border-color: #bbf7d0;
        }
        .status-pill.error {
            background-color: #fef2f2;
            color: #b91c1c;
            border-color: #fecaca;
        }
    `;
    document.head.appendChild(style);
}

document.addEventListener('DOMContentLoaded', () => {
    injectTestClientStyles();
    buildTestUsersTable();
});


