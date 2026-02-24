# Real-Time Chat System Documentation

## Overview
This is a real-time chat system built with Django Channels and WebSockets. It allows users to send direct messages to each other with real-time delivery (no page refresh needed).

## Architecture
- **Backend**: Django with Django Channels
- **WebSocket Protocol**: For real-time messaging
- **Authentication**: JWT tokens
- **Message Storage**: PostgreSQL database
- **Channel Layer**: Redis (for production) or In-Memory (for development)

## Setup Instructions

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Install and Run Redis (Required for Production)
For Windows:
- Download Redis from: https://github.com/microsoftarchive/redis/releases
- Run: `redis-server`

Alternatively, use Docker:
```bash
docker run -d -p 6379:6379 redis:alpine
```

For development without Redis, uncomment the InMemoryChannelLayer in `settings.py`.

### 3. Environment Variables
Add to your `.env` file:
```
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 4. Run Migrations
```bash
python manage.py makemigrations
python manage.py migrate
```

### 5. Run the Server
Use Daphne (ASGI server) instead of runserver for WebSocket support:
```bash
daphne -b 0.0.0.0 -p 8000 django_api.asgi:application
```

Or for development, Django's runserver also works with Channels:
```bash
python manage.py runserver
```

## API Endpoints

### REST API Endpoints

#### 1. List Conversations
```
GET /api/chats/conversations/
Authorization: Bearer <token>

Response:
[
  {
    "id": 1,
    "other_user": {
      "id": 2,
      "name": "John Doe",
      "email": "john@example.com"
    },
    "last_message": {
      "body": "Hello!",
      "created_at": "2026-02-24T10:30:00Z",
      "sender_id": 2,
      "is_read": false
    },
    "unread_count": 3,
    "created_at": "2026-02-20T08:00:00Z"
  }
]
```

#### 2. Get Conversation Details
```
GET /api/chats/conversations/<conversation_id>/
Authorization: Bearer <token>

Response:
{
  "id": 1,
  "other_user": {
    "id": 2,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "messages": [
    {
      "id": 1,
      "conversation": 1,
      "sender": 2,
      "sender_name": "John Doe",
      "body": "Hello!",
      "created_at": "2026-02-24T10:30:00Z",
      "is_read": true
    }
  ],
  "created_at": "2026-02-20T08:00:00Z"
}
```

#### 3. Create or Get Conversation
```
POST /api/chats/conversations/create/
Authorization: Bearer <token>
Content-Type: application/json

{
  "user_id": 2
}

Response:
{
  "id": 1,
  "other_user": {
    "id": 2,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "created": true,
  "created_at": "2026-02-24T10:30:00Z"
}
```

#### 4. Get Unread Count
```
GET /api/chats/unread-count/
Authorization: Bearer <token>

Response:
{
  "unread_count": 5
}
```

### WebSocket Endpoint

#### Connect to Conversation
```
ws://localhost:8000/ws/chat/<conversation_id>/?token=<jwt_token>
```

**Authentication**: Pass JWT token as query parameter

**Connection Events**:
- On connect: User joins the conversation room
- All unread messages are marked as read
- User can send and receive messages in real-time

**Send Message**:
```json
{
  "message": "Hello, how are you?"
}
```

**Receive Message**:
```json
{
  "type": "message",
  "message": {
    "id": 1,
    "conversation": 1,
    "sender": 2,
    "sender_name": "John Doe",
    "body": "Hello, how are you?",
    "created_at": "2026-02-24T10:30:00Z",
    "is_read": false
  }
}
```

**Error Response**:
```json
{
  "error": "Message body cannot be empty"
}
```

## Client-Side Integration Example

### JavaScript WebSocket Client

```javascript
// Get JWT token from your authentication system
const token = 'your-jwt-token';
const conversationId = 1;

// Connect to WebSocket
const chatSocket = new WebSocket(
    `ws://localhost:8000/ws/chat/${conversationId}/?token=${token}`
);

// Connection opened
chatSocket.onopen = function(e) {
    console.log('Connected to chat');
};

// Listen for messages
chatSocket.onmessage = function(e) {
    const data = JSON.parse(e.data);
    
    if (data.error) {
        console.error('Error:', data.error);
        return;
    }
    
    if (data.type === 'message') {
        const message = data.message;
        // Display message in your UI
        console.log(`${message.sender_name}: ${message.body}`);
    }
};

// Connection closed
chatSocket.onclose = function(e) {
    console.log('Disconnected from chat');
};

// Send a message
function sendMessage(messageText) {
    chatSocket.send(JSON.stringify({
        'message': messageText
    }));
}

// Example: Send a message
sendMessage('Hello, World!');
```

### React Example (using hooks)

```javascript
import { useEffect, useState, useRef } from 'react';

function ChatComponent({ conversationId, token }) {
    const [messages, setMessages] = useState([]);
    const [inputMessage, setInputMessage] = useState('');
    const ws = useRef(null);

    useEffect(() => {
        // Connect to WebSocket
        ws.current = new WebSocket(
            `ws://localhost:8000/ws/chat/${conversationId}/?token=${token}`
        );

        ws.current.onmessage = (e) => {
            const data = JSON.parse(e.data);
            if (data.type === 'message') {
                setMessages(prev => [...prev, data.message]);
            }
        };

        // Cleanup on unmount
        return () => {
            ws.current.close();
        };
    }, [conversationId, token]);

    const sendMessage = () => {
        if (inputMessage.trim()) {
            ws.current.send(JSON.stringify({
                message: inputMessage
            }));
            setInputMessage('');
        }
    };

    return (
        <div>
            <div className="messages">
                {messages.map(msg => (
                    <div key={msg.id}>
                        <strong>{msg.sender_name}:</strong> {msg.body}
                    </div>
                ))}
            </div>
            <input
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
            />
            <button onClick={sendMessage}>Send</button>
        </div>
    );
}
```

## Features

### Implemented
✅ Real-time messaging with WebSockets
✅ JWT authentication for WebSocket connections
✅ Automatic message persistence in database
✅ Read receipts (is_read status)
✅ Unread message count
✅ Conversation creation and listing
✅ User verification (only conversation participants can access)
✅ Message history retrieval

### Security Features
- JWT token authentication for WebSocket connections
- User verification for conversation access
- CORS configuration
- Allowed hosts origin validation

## Database Models

### Conversation
- `user1`: ForeignKey to User
- `user2`: ForeignKey to User
- `created_at`: DateTime
- Unique constraint on (user1, user2) pair

### Message
- `conversation`: ForeignKey to Conversation
- `sender`: ForeignKey to User
- `body`: TextField
- `created_at`: DateTime
- `is_read`: Boolean
- Index on (conversation, -created_at)

## Troubleshooting

### WebSocket connection fails
1. Ensure Redis is running (if using Redis channel layer)
2. Check that Daphne or Django server is running with ASGI support
3. Verify JWT token is valid and not expired
4. Check CORS settings in `settings.py`

### Messages not appearing in real-time
1. Verify WebSocket connection is established
2. Check browser console for JavaScript errors
3. Ensure channel layer is configured correctly
4. Verify Redis connection if using Redis

### Authentication errors
1. Check JWT token format: `?token=<your-token>`
2. Verify token is not expired
3. Ensure user exists in database
4. Check SIMPLE_JWT settings in `settings.py`

## Production Deployment

### Requirements
- Redis server (required for production)
- Daphne ASGI server
- Nginx or similar for reverse proxy
- SSL/TLS certificate for WSS (secure WebSocket)

### Nginx Configuration Example
```nginx
upstream django {
    server 127.0.0.1:8000;
}

server {
    listen 80;
    server_name your-domain.com;

    location /ws/ {
        proxy_pass http://django;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        proxy_pass http://django;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Testing

### Test WebSocket Connection
You can test WebSocket connection using browser console:
```javascript
const ws = new WebSocket('ws://localhost:8000/ws/chat/1/?token=YOUR_TOKEN');
ws.onmessage = (e) => console.log(e.data);
ws.send(JSON.stringify({message: 'Test message'}));
```

### Test With Postman
Postman supports WebSocket connections. Use the WebSocket request type and connect to:
```
ws://localhost:8000/ws/chat/1/?token=YOUR_TOKEN
```

## Support
For issues or questions, please contact the development team or create an issue in the repository.
