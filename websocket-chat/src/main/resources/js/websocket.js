// WebSocket connection management
let websocket = null;
let websocketReconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 3000; // 3 seconds

// DOM elements - get them when needed to ensure they exist
function getPlayerView() {
    return document.getElementById('playerView');
}

function getDisplayView() {
    return document.getElementById('displayView');
}

function getLoggedInView() {
    return document.getElementById('loggedInView');
}

const messageInput = document.getElementById('messageInput');
const sendMessageBtn = document.getElementById('sendMessageBtn');
const messagesContainer = document.getElementById('messagesContainer');
const backFromPlayerBtn = document.getElementById('backFromPlayerBtn');
const backFromDisplayBtn = document.getElementById('backFromDisplayBtn');

// Get WebSocket URL with authentication token
async function getWebSocketUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    const port = '8080'; // WebSocket server port
    
    // Fetch token from server (since cookie is HttpOnly, we can't read it directly)
    try {
        const response = await fetch('/auth/websocket-token', {
            method: 'GET',
            credentials: 'include'
        });
        
        if (response.ok) {
            const data = await response.json();
            const token = data.token;
            if (token) {
                return `${protocol}//${host}:${port}/websocket?token=${encodeURIComponent(token)}`;
            }
        }
    } catch (error) {
        console.error('Error fetching WebSocket token:', error);
    }
    
    // Fallback: try without token (might work if cookies are sent)
    return `${protocol}//${host}:${port}/websocket`;
}

// Connect to WebSocket
async function connectWebSocket() {
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        console.log('WebSocket already connected');
        return;
    }

    const url = await getWebSocketUrl();
    console.log('Connecting to WebSocket:', url.replace(/\?token=[^&]+/, '?token=***')); // Hide token in logs
    
    websocket = new WebSocket(url);

    websocket.onopen = function(event) {
        console.log('WebSocket connected');
        websocketReconnectAttempts = 0;
        
        // Request initial messages when connected
        const displayViewEl = getDisplayView();
        if (displayViewEl && displayViewEl.style.display !== 'none') {
            requestMessages();
        }
    };

    websocket.onmessage = function(event) {
        console.log('WebSocket message received:', event.data);
        handleWebSocketMessage(event.data);
    };

    websocket.onerror = function(error) {
        console.error('WebSocket error:', error);
    };

    websocket.onclose = function(event) {
        console.log('WebSocket closed:', event.code, event.reason);
        
        // Attempt to reconnect if we're still in a view that needs WebSocket
        const playerViewEl = getPlayerView();
        const displayViewEl = getDisplayView();
        if ((playerViewEl && playerViewEl.style.display !== 'none') || 
            (displayViewEl && displayViewEl.style.display !== 'none')) {
            if (websocketReconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                websocketReconnectAttempts++;
                console.log(`Attempting to reconnect (${websocketReconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})...`);
                setTimeout(connectWebSocket, RECONNECT_DELAY);
            } else {
                console.error('Max reconnection attempts reached');
            }
        }
    };
}

// Disconnect WebSocket
function disconnectWebSocket() {
    if (websocket) {
        websocket.close();
        websocket = null;
    }
}

// Send message through WebSocket
function sendWebSocketMessage(message) {
    if (!websocket || websocket.readyState !== WebSocket.OPEN) {
        console.error('WebSocket is not connected');
        return false;
    }

    try {
        websocket.send(JSON.stringify(message));
        return true;
    } catch (error) {
        console.error('Error sending WebSocket message:', error);
        return false;
    }
}

// Handle incoming WebSocket messages
function handleWebSocketMessage(data) {
    try {
        const message = JSON.parse(data);
        
        switch (message.type) {
            case 'getMessagesResponse':
                if (message.messages && Array.isArray(message.messages)) {
                    displayMessages(message.messages);
                }
                break;
                
            case 'messageReceivedNotification':
                addMessageToDisplay(message.screenName, message.message);
                break;
                
            case 'playerJoinedChatNotification':
                console.log('Player joined:', message.screenName);
                break;
                
            case 'playerLeftChatNotification':
                console.log('Player left:', message.screenName);
                break;
                
            default:
                console.log('Unknown message type:', message.type);
        }
    } catch (error) {
        console.error('Error parsing WebSocket message:', error);
    }
}

// Request messages from server
function requestMessages() {
    sendWebSocketMessage({ type: 'getMessages' });
}

// Send a chat message
function sendChatMessage(messageText) {
    if (!messageText || messageText.trim() === '') {
        return false;
    }
    
    return sendWebSocketMessage({
        type: 'sendMessage',
        message: messageText.trim()
    });
}

// Display messages in the display view
function displayMessages(messages) {
    messagesContainer.innerHTML = '';
    
    if (!messages || messages.length === 0) {
        messagesContainer.innerHTML = '<p class="empty-message">No messages yet. Messages will appear here.</p>';
        return;
    }
    
    messages.forEach(msg => {
        addMessageToDisplay(msg.screenName, msg.message);
    });
}

// Add a single message to the display
function addMessageToDisplay(screenName, message) {
    // Remove empty message if present
    const emptyMessage = messagesContainer.querySelector('.empty-message');
    if (emptyMessage) {
        emptyMessage.remove();
    }
    
    const messageItem = document.createElement('div');
    messageItem.className = 'message-item';
    
    const senderDiv = document.createElement('div');
    senderDiv.className = 'message-sender';
    senderDiv.textContent = screenName || 'Anonymous';
    
    const textDiv = document.createElement('div');
    textDiv.className = 'message-text';
    textDiv.textContent = message;
    
    messageItem.appendChild(senderDiv);
    messageItem.appendChild(textDiv);
    
    messagesContainer.appendChild(messageItem);
    
    // Scroll to bottom
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// Show player view
function showPlayerView() {
    const loggedInViewEl = getLoggedInView();
    const displayViewEl = getDisplayView();
    const playerViewEl = getPlayerView();
    
    if (!playerViewEl) {
        console.error('Player view element not found');
        return;
    }
    
    if (loggedInViewEl) loggedInViewEl.style.display = 'none';
    if (displayViewEl) displayViewEl.style.display = 'none';
    playerViewEl.style.display = 'block';
    
    console.log('Switched to Player View');
    
    // Show game info if available
    const gameInfo = document.getElementById('playerGameInfo');
    const gameName = localStorage.getItem('joinedGameName');
    const gameId = localStorage.getItem('joinedGameId');
    if (gameInfo && (gameName || gameId)) {
        gameInfo.textContent = `Game: ${gameName || gameId} (Player Mode)`;
        gameInfo.style.display = 'block';
    }
    
    // Connect WebSocket if not already connected
    connectWebSocket();
    
    // Focus on message input
    setTimeout(() => {
        if (messageInput) {
            messageInput.focus();
        }
    }, 100);
}

// Show display view
function showDisplayView() {
    const loggedInViewEl = getLoggedInView();
    const playerViewEl = getPlayerView();
    const displayViewEl = getDisplayView();
    
    if (!displayViewEl) {
        console.error('Display view element not found');
        return;
    }
    
    if (loggedInViewEl) loggedInViewEl.style.display = 'none';
    if (playerViewEl) playerViewEl.style.display = 'none';
    displayViewEl.style.display = 'block';
    
    console.log('Switched to Display View');
    
    // Show game info if available
    const gameInfo = document.getElementById('displayGameInfo');
    const gameName = localStorage.getItem('joinedGameName');
    const gameId = localStorage.getItem('joinedGameId');
    if (gameInfo && (gameName || gameId)) {
        gameInfo.textContent = `Game: ${gameName || gameId} (Display Mode)`;
        gameInfo.style.display = 'block';
    }
    
    // Connect WebSocket if not already connected
    connectWebSocket();
    
    // Request messages when showing display view
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        requestMessages();
    } else {
        // If WebSocket is not ready, wait for it to connect
        const checkConnection = setInterval(() => {
            if (websocket && websocket.readyState === WebSocket.OPEN) {
                clearInterval(checkConnection);
                requestMessages();
            }
        }, 100);
        
        // Stop checking after 5 seconds
        setTimeout(() => clearInterval(checkConnection), 5000);
    }
}

// Show logged in view (go back)
function showLoggedInView() {
    const playerViewEl = getPlayerView();
    const displayViewEl = getDisplayView();
    const loggedInViewEl = getLoggedInView();
    
    if (playerViewEl) playerViewEl.style.display = 'none';
    if (displayViewEl) displayViewEl.style.display = 'none';
    if (loggedInViewEl) loggedInViewEl.style.display = 'block';
    
    // Don't disconnect WebSocket - keep it open in case user switches views again
}

// Event listeners
if (sendMessageBtn) {
    sendMessageBtn.addEventListener('click', () => {
        const message = messageInput.value;
        if (sendChatMessage(message)) {
            messageInput.value = '';
            messageInput.focus();
        }
    });
}

if (messageInput) {
    messageInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            const message = messageInput.value;
            if (sendChatMessage(message)) {
                messageInput.value = '';
            }
        }
    });
}

if (backFromPlayerBtn) {
    backFromPlayerBtn.addEventListener('click', showLoggedInView);
}

if (backFromDisplayBtn) {
    backFromDisplayBtn.addEventListener('click', showLoggedInView);
}

// Export functions for use in auth.js immediately
// This ensures they're available as soon as the script loads
window.showPlayerView = showPlayerView;
window.showDisplayView = showDisplayView;
window.disconnectWebSocket = disconnectWebSocket;

// Log that functions are available
console.log('WebSocket functions exported:', {
    showPlayerView: typeof window.showPlayerView,
    showDisplayView: typeof window.showDisplayView,
    disconnectWebSocket: typeof window.disconnectWebSocket
});

