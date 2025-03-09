#!/bin/bash

BASE_URL="http://localhost:8080"
TOKEN=""

# Test 1: Registrar un nuevo usuario
echo "Registrando nuevo usuario..."
RESPONSE=$(curl -s -X POST "$BASE_URL/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "testuser",
    "fullName": "Test User",
    "city": "Test City",
    "region": "Test Region",
    "country": "Test Country"
  }')
echo $RESPONSE
echo ""

# Test 2: Iniciar sesión
echo "Iniciando sesión..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/users/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')
echo $LOGIN_RESPONSE

# Extraer el token usando herramientas más robustas (jq si está disponible)
if command -v jq &> /dev/null; then
    TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
else
    # Fallback si jq no está disponible (menos confiable)
    TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"//g' | sed 's/"//g')
fi

echo "Token: $TOKEN"
echo ""

# Test 3: Obtener perfil de usuario
echo "Obteniendo perfil de usuario..."
curl -s -X GET "$BASE_URL/users/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
echo ""