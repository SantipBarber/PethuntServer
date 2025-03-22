#!/bin/bash
# test-images-api.sh

BASE_URL="http://localhost:8080"
TOKEN=""

# Colores para mejor visualización
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== TEST API IMÁGENES PETHUNT ===${NC}\n"

# Login para obtener token
echo -e "${BLUE}Iniciando sesión...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/users/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')

# Extraer el token
if command -v jq &> /dev/null; then
    TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
else
    # Fallback si jq no está disponible
    TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"//g' | sed 's/"//g')
fi

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo -e "${RED}Error al obtener token. Asegúrate de que el usuario exista y las credenciales sean correctas.${NC}"
    echo "Respuesta del servidor: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}Login exitoso${NC}"
echo ""

# Test 1: Subir imagen desde URL
echo -e "${BLUE}TEST 1: Subir imagen desde URL...${NC}"
UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/images/upload-url" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "imageUrl": "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png",
    "folder": "test"
  }')

echo "Respuesta:"
if command -v jq &> /dev/null; then
    echo $UPLOAD_RESPONSE | jq
    IMAGE_URL=$(echo $UPLOAD_RESPONSE | jq -r '.imageUrl')
else
    echo $UPLOAD_RESPONSE
    IMAGE_URL=$(echo $UPLOAD_RESPONSE | grep -o '"imageUrl":"[^"]*"' | sed 's/"imageUrl":"//g' | sed 's/"//g')
fi

if [ -z "$IMAGE_URL" ] || [ "$IMAGE_URL" == "null" ]; then
    echo -e "${RED}Error al subir imagen desde URL${NC}"
    echo "Respuesta del servidor: $UPLOAD_RESPONSE"
    exit 1
fi

echo -e "${GREEN}Imagen subida correctamente: $IMAGE_URL${NC}"
echo ""

# Test 2: Eliminar imagen
echo -e "${BLUE}TEST 2: Eliminar imagen...${NC}"
# Codificar URL para pasarla como parámetro
ENCODED_URL=$(echo $IMAGE_URL | jq -sRr @uri)
DELETE_RESPONSE=$(curl -s -X DELETE "$BASE_URL/images/$ENCODED_URL" \
  -H "Authorization: Bearer $TOKEN" \
  -w "%{http_code}")

HTTP_CODE=${DELETE_RESPONSE: -3}

echo "Código HTTP: $HTTP_CODE"
if [ "$HTTP_CODE" == "204" ]; then
    echo -e "${GREEN}Imagen eliminada correctamente${NC}"
else
    echo -e "${RED}Error al eliminar imagen${NC}"
    echo "Respuesta del servidor: ${DELETE_RESPONSE:0:${#DELETE_RESPONSE}-3}"
fi
echo ""

echo -e "${GREEN}Pruebas completadas${NC}"