#!/bin/bash
# test-pets-api.sh

BASE_URL="http://localhost:8080"
TOKEN=""
PET_ID=""

# Colores para mejor visualización
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== TEST API MASCOTAS PETHUNT ===${NC}\n"

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
echo "Token: $TOKEN"
echo ""

# Test 1: Crear una mascota
echo -e "${BLUE}TEST 1: Creando nueva mascota...${NC}"
PET_RESPONSE=$(curl -s -X POST "$BASE_URL/pets" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Max",
    "speciesId": "dog",
    "breedId": "golden_retriever",
    "birthDate": "2019-05-15",
    "gender": "MALE",
    "weight": 25.5,
    "bio": "Un perro muy amigable y juguetón"
  }')

# Extraer el ID de la mascota creada
if command -v jq &> /dev/null; then
    PET_ID=$(echo $PET_RESPONSE | jq -r '.id')
    echo "Respuesta completa:"
    echo $PET_RESPONSE | jq
else
    # Fallback si jq no está disponible
    PET_ID=$(echo $PET_RESPONSE | grep -o '"id":"[^"]*"' | sed 's/"id":"//g' | sed 's/"//g')
    echo "Respuesta completa:"
    echo $PET_RESPONSE
fi

if [ -z "$PET_ID" ] || [ "$PET_ID" == "null" ]; then
    echo -e "${RED}Error al crear mascota${NC}"
    echo "Respuesta del servidor: $PET_RESPONSE"
    exit 1
fi

echo -e "${GREEN}Mascota creada correctamente${NC}"
echo "ID de mascota: $PET_ID"
echo ""

# Test 2: Obtener todas las mascotas del usuario
echo -e "${BLUE}TEST 2: Obteniendo mascotas del usuario...${NC}"
PETS_RESPONSE=$(curl -s -X GET "$BASE_URL/pets" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $PETS_RESPONSE | jq
else
    echo $PETS_RESPONSE
fi
echo ""

# Test 3: Obtener detalles de una mascota específica
echo -e "${BLUE}TEST 3: Obteniendo detalles de la mascota $PET_ID...${NC}"
PET_DETAILS=$(curl -s -X GET "$BASE_URL/pets/$PET_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $PET_DETAILS | jq
else
    echo $PET_DETAILS
fi
echo ""

# Test 4: Actualizar una mascota
echo -e "${BLUE}TEST 4: Actualizando mascota...${NC}"
UPDATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/pets/$PET_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Max",
    "speciesId": "dog",
    "breedId": "golden_retriever",
    "birthDate": "2019-05-15",
    "gender": "MALE",
    "weight": 27.3,
    "bio": "Un perro muy amigable, juguetón y ahora más grande"
  }')

if command -v jq &> /dev/null; then
    echo $UPDATE_RESPONSE | jq
else
    echo $UPDATE_RESPONSE
fi
echo ""

# Test 5: Actualizar la imagen de perfil
echo -e "${BLUE}TEST 5: Actualizando imagen de perfil...${NC}"
IMAGE_RESPONSE=$(curl -s -X POST "$BASE_URL/pets/$PET_ID/profile-image" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "imageUrl": "https://example.com/images/dog.jpg"
  }')

if command -v jq &> /dev/null; then
    echo $IMAGE_RESPONSE | jq
else
    echo $IMAGE_RESPONSE
fi
echo ""

# Test 6: Obtener el número de mascotas
echo -e "${BLUE}TEST 6: Obteniendo número de mascotas...${NC}"
COUNT_RESPONSE=$(curl -s -X GET "$BASE_URL/pets/count" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $COUNT_RESPONSE | jq
else
    echo $COUNT_RESPONSE
fi
echo ""

# Test 7: Eliminar una mascota
echo -e "${BLUE}TEST 7: Eliminando mascota...${NC}"
DELETE_RESPONSE=$(curl -s -X DELETE "$BASE_URL/pets/$PET_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "%{http_code}")

HTTP_CODE=${DELETE_RESPONSE: -3}
BODY=${DELETE_RESPONSE:0:${#DELETE_RESPONSE}-3}

echo "Código HTTP: $HTTP_CODE"
if [ "$HTTP_CODE" == "204" ]; then
    echo -e "${GREEN}Mascota eliminada correctamente${NC}"
else
    echo -e "${RED}Error al eliminar mascota${NC}"
    echo "Respuesta del servidor: $BODY"
fi
echo ""

echo -e "${GREEN}Pruebas completadas${NC}"