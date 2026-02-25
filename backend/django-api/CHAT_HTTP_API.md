# Chat HTTP API Documentation

This document describes the HTTP REST API endpoints for the chat system. All endpoints require JWT authentication.

## Authentication

All endpoints require a valid JWT token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

## Base URL
```
http://localhost:8000/api/chats/
```

---

## Endpoints

### 1. List All Conversations

Get all conversations for the authenticated user, sorted by most recent message.

**Endpoint:** `GET /api/chats/conversations/`

**Response:**
```json
[
  {
    "id": 1,
    "other_user": {
      "id": 2,
      "name": "John Doe",
      "email": "john@example.com"
    },
    "last_message": {
      "id": 5,
      "conversation": 1,
      "sender": 2,
      "sender_id": 2,
      "sender_name": "John Doe",
      "body": "Hello! How are you?",
      "created_at": "2026-02-25T10:30:00Z",
      "is_read": false
    },
    "unread_count": 3,
    "created_at": "2026-02-20T08:00:00Z"
  }
]
```

---

### 2. Get Conversation Details

Get a specific conversation with all messages. Automatically marks unread messages as read.

**Endpoint:** `GET /api/chats/conversations/<conversation_id>/`

**Response:**
```json
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
      "sender_id": 2,
      "sender_name": "John Doe",
      "body": "Hi there!",
      "created_at": "2026-02-24T10:30:00Z",
      "is_read": true
    },
    {
      "id": 2,
      "conversation": 1,
      "sender": 1,
      "sender_id": 1,
      "sender_name": "Jane Smith",
      "body": "Hello!",
      "created_at": "2026-02-24T10:31:00Z",
      "is_read": true
    }
  ],
  "created_at": "2026-02-20T08:00:00Z"
}
```

**Error Responses:**
- `403 Forbidden`: User is not part of this conversation
- `404 Not Found`: Conversation does not exist

---

### 3. Create or Get Conversation

Create a new conversation with another user, or return existing conversation if one already exists.

**Endpoint:** `POST /api/chats/conversations/create/`

**Request Body:**
```json
{
  "user_id": 2
}
```

**Response (New Conversation):**
```json
{
  "id": 1,
  "created": true,
  "other_user": {
    "id": 2,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "created_at": "2026-02-25T10:30:00Z"
}
```

**Response (Existing Conversation):**
```json
{
  "id": 1,
  "created": false,
  "other_user": {
    "id": 2,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "created_at": "2026-02-20T08:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request`: Missing user_id or attempting to create conversation with self
- `404 Not Found`: Other user does not exist

---

### 4. Get Messages (Paginated)

Get messages from a conversation with pagination support.

**Endpoint:** `GET /api/chats/conversations/<conversation_id>/messages/`

**Query Parameters:**
- `page` (optional): Page number (default: 1)
- `page_size` (optional): Number of messages per page (default: 20, max: 100)

**Example:** `GET /api/chats/conversations/1/messages/?page=2&page_size=10`

**Response:**
```json
{
  "count": 45,
  "next": "http://localhost:8000/api/chats/conversations/1/messages/?page=3",
  "previous": "http://localhost:8000/api/chats/conversations/1/messages/?page=1",
  "results": [
    {
      "id": 20,
      "conversation": 1,
      "sender": 2,
      "sender_id": 2,
      "sender_name": "John Doe",
      "body": "This is message 20",
      "created_at": "2026-02-25T10:30:00Z",
      "is_read": true
    }
  ]
}
```

**Note:** Messages are ordered by most recent first (descending order).

**Error Responses:**
- `403 Forbidden`: User is not part of this conversation
- `404 Not Found`: Conversation does not exist

---

### 5. Send Message

Send a new message in a conversation via HTTP.

**Endpoint:** `POST /api/chats/conversations/<conversation_id>/messages/`

**Request Body:**
```json
{
  "body": "Hello, this is my message!"
}
```

Or alternatively:
```json
{
  "message": "Hello, this is my message!"
}
```

**Response:**
```json
{
  "id": 25,
  "conversation": 1,
  "sender": 1,
  "sender_id": 1,
  "sender_name": "Jane Smith",
  "body": "Hello, this is my message!",
  "created_at": "2026-02-25T10:35:00Z",
  "is_read": false
}
```

**Error Responses:**
- `400 Bad Request`: Empty message body
- `403 Forbidden`: User is not part of this conversation
- `404 Not Found`: Conversation does not exist

---

### 6. Mark Messages as Read

Manually mark all unread messages in a conversation as read.

**Endpoint:** `POST /api/chats/conversations/<conversation_id>/mark-read/`

**Request Body:** (empty)

**Response:**
```json
{
  "message": "Messages marked as read",
  "count": 5
}
```

**Note:** The `count` field indicates how many messages were marked as read.

**Error Responses:**
- `403 Forbidden`: User is not part of this conversation
- `404 Not Found`: Conversation does not exist

---

### 7. Get Unread Message Count

Get total number of unread messages across all conversations.

**Endpoint:** `GET /api/chats/unread-count/`

**Response:**
```json
{
  "unread_count": 12
}
```

---

## Usage Examples

### JavaScript (Fetch API)

```javascript
const token = 'your-jwt-token';
const API_BASE = 'http://localhost:8000/api/chats';

// List conversations
async function getConversations() {
  const response = await fetch(`${API_BASE}/conversations/`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
}

// Send a message
async function sendMessage(conversationId, messageText) {
  const response = await fetch(`${API_BASE}/conversations/${conversationId}/messages/`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      body: messageText
    })
  });
  return await response.json();
}

// Get messages with pagination
async function getMessages(conversationId, page = 1) {
  const response = await fetch(
    `${API_BASE}/conversations/${conversationId}/messages/?page=${page}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return await response.json();
}

// Create conversation
async function createConversation(userId) {
  const response = await fetch(`${API_BASE}/conversations/create/`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      user_id: userId
    })
  });
  return await response.json();
}
```

### Python (Requests)

```python
import requests

token = 'your-jwt-token'
API_BASE = 'http://localhost:8000/api/chats'
headers = {
    'Authorization': f'Bearer {token}',
    'Content-Type': 'application/json'
}

# List conversations
response = requests.get(f'{API_BASE}/conversations/', headers=headers)
conversations = response.json()

# Send message
response = requests.post(
    f'{API_BASE}/conversations/1/messages/',
    headers=headers,
    json={'body': 'Hello!'}
)
message = response.json()

# Get messages with pagination
response = requests.get(
    f'{API_BASE}/conversations/1/messages/?page=1&page_size=20',
    headers=headers
)
messages = response.json()

# Create conversation
response = requests.post(
    f'{API_BASE}/conversations/create/',
    headers=headers,
    json={'user_id': 2}
)
conversation = response.json()
```

### cURL

```bash
# Get conversations
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8000/api/chats/conversations/

# Send message
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"body": "Hello!"}' \
  http://localhost:8000/api/chats/conversations/1/messages/

# Get messages (page 1, 20 per page)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  "http://localhost:8000/api/chats/conversations/1/messages/?page=1&page_size=20"

# Create conversation
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"user_id": 2}' \
  http://localhost:8000/api/chats/conversations/create/

# Get unread count
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8000/api/chats/unread-count/
```

---

## Common Workflow

### Starting a Conversation

1. **Create/Get Conversation**
   ```
   POST /api/chats/conversations/create/
   { "user_id": 2 }
   ```

2. **Send First Message**
   ```
   POST /api/chats/conversations/1/messages/
   { "body": "Hi there!" }
   ```

### Viewing Conversations

1. **List All Conversations**
   ```
   GET /api/chats/conversations/
   ```

2. **Open Specific Conversation**
   ```
   GET /api/chats/conversations/1/
   ```
   (This automatically marks messages as read)

### Sending and Receiving Messages

1. **Send Message**
   ```
   POST /api/chats/conversations/1/messages/
   { "body": "Your message here" }
   ```

2. **Poll for New Messages** (every few seconds)
   ```
   GET /api/chats/conversations/1/messages/?page=1
   ```

3. **Check Unread Count** (for notification badge)
   ```
   GET /api/chats/unread-count/
   ```

---

## Features

✅ **HTTP-only**: No WebSocket required
✅ **Pagination**: Efficient message loading
✅ **Read Receipts**: Automatic and manual marking
✅ **Unread Counts**: Per conversation and total
✅ **Message History**: Full conversation history
✅ **User Verification**: Security checks on all endpoints
✅ **RESTful Design**: Standard HTTP methods

---

## Notes

- All timestamps are in ISO 8601 format (UTC)
- Messages are automatically marked as read when viewing conversation details
- Pagination defaults to 20 messages per page
- Maximum page size is 100 messages
- Conversations are sorted by most recent message
- Messages within a conversation are ordered chronologically (oldest first for detail view, newest first for paginated view)

---

## Error Codes

- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Missing or invalid JWT token
- `403 Forbidden`: User not authorized for this conversation
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

---

## Support

For issues or questions, please refer to your API documentation or contact the development team.
