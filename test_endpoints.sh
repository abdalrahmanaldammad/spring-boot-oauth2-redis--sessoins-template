#!/bin/bash

# Testing Spring Boot Auth Endpoints
BASE_URL="http://localhost:8080/api"

echo "================================================"
echo "Testing Spring Boot Authentication Endpoints"
echo "================================================"
echo ""

# Check if server is running
echo "1. Checking if server is running..."
response=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/auth/status)
if [ "$response" == "200" ]; then
    echo "✓ Server is running"
else
    echo "✗ Server is not responding (HTTP $response)"
    echo "Please start the Spring Boot application first"
    exit 1
fi

# Test auth status (unauthenticated)
echo ""
echo "2. Testing auth status (unauthenticated)..."
curl -s -X GET $BASE_URL/auth/status | jq '.'

# Test user registration
echo ""
echo "3. Testing user registration..."
echo "   Registering testuser@example.com..."
register_response=$(curl -s -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "testuser@example.com",
    "password": "Test123!@#",
    "firstName": "Test",
    "lastName": "User"
  }')
echo "$register_response" | jq '.'

# Extract success status
success=$(echo "$register_response" | jq -r '.success')
if [ "$success" == "true" ]; then
    echo "✓ Registration successful"
else
    echo "✗ Registration failed"
fi

# Test login
echo ""
echo "4. Testing login..."
login_response=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "username": "testuser",
    "password": "Test123!@#"
  }')
echo "$login_response" | jq '.'

# Extract session ID if available
session_id=$(echo "$login_response" | jq -r '.sessionId')
if [ "$session_id" != "null" ]; then
    echo "✓ Login successful - Session ID: $session_id"
else
    echo "✗ Login failed"
fi

# Test authenticated status
echo ""
echo "5. Testing auth status (authenticated)..."
curl -s -X GET $BASE_URL/auth/me \
  -b cookies.txt | jq '.'

# Test session info
echo ""
echo "6. Testing session info..."
curl -s -X GET $BASE_URL/auth/session/info \
  -b cookies.txt | jq '.'

# Test active sessions
echo ""
echo "7. Testing active sessions..."
curl -s -X GET $BASE_URL/auth/sessions/active \
  -b cookies.txt | jq '.'

# Test accessing protected endpoint (user role)
echo ""
echo "8. Testing protected user endpoint..."
curl -s -X GET $BASE_URL/user/profile \
  -b cookies.txt \
  -o /dev/null -w "HTTP Status: %{http_code}\n"

# Test accessing admin endpoint (should fail)
echo ""
echo "9. Testing admin endpoint (should fail with 403)..."
curl -s -X GET $BASE_URL/admin/users \
  -b cookies.txt \
  -o /dev/null -w "HTTP Status: %{http_code}\n"

# Test logout
echo ""
echo "10. Testing logout..."
logout_response=$(curl -s -X POST $BASE_URL/auth/logout \
  -b cookies.txt)
echo "$logout_response" | jq '.'

# Test auth status after logout
echo ""
echo "11. Testing auth status after logout..."
curl -s -X GET $BASE_URL/auth/me \
  -b cookies.txt | jq '.'

# Clean up
rm -f cookies.txt

echo ""
echo "================================================"
echo "Testing Complete"
echo "================================================"
