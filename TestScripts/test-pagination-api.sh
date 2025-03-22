#!/bin/bash
# test-pagination.sh

BASE_URL="http://localhost:8080"
TOKEN=""

# Colores para mejor visualización
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== TEST PAGINACIÓN PETHUNT ===${NC}\n"

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

# Test 1: Paginación básica de especies - Página 1 con tamaño 2
echo -e "${BLUE}TEST 1: Paginación básica - Especies (página 1, tamaño 2)${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?page=1&pageSize=2" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Información de paginación:"
    echo $RESPONSE | jq '.pagination'
    echo "Enlaces de paginación:"
    echo $RESPONSE | jq '.links'
    echo "Cantidad de elementos en esta página:"
    echo $RESPONSE | jq '.items | length'
else
    echo $RESPONSE
fi
echo ""

# Test 2: Paginación básica de especies - Página 2 con tamaño 2
echo -e "${BLUE}TEST 2: Paginación básica - Especies (página 2, tamaño 2)${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?page=2&pageSize=2" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Información de paginación:"
    echo $RESPONSE | jq '.pagination'
    echo "Enlaces de paginación:"
    echo $RESPONSE | jq '.links'
    echo "Cantidad de elementos en esta página:"
    echo $RESPONSE | jq '.items | length'
else
    echo $RESPONSE
fi
echo ""

# Test 3: Paginación con tamaño personalizado
echo -e "${BLUE}TEST 3: Paginación con tamaño personalizado - Especies (tamaño 5)${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?pageSize=5" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Información de paginación:"
    echo $RESPONSE | jq '.pagination'
    echo "Cantidad de elementos:"
    echo $RESPONSE | jq '.items | length'
else
    echo $RESPONSE
fi
echo ""

# Test 4: Paginación con tamaño excesivo (debe limitar a 100)
echo -e "${BLUE}TEST 4: Paginación con tamaño excesivo (500, debe limitar a 100)${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?pageSize=500" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Tamaño de página en respuesta (debe ser 100 o menor):"
    echo $RESPONSE | jq '.pagination.pageSize'
else
    echo $RESPONSE
fi
echo ""

# Test 5: Paginación en búsqueda
echo -e "${BLUE}TEST 5: Paginación en búsqueda - Especies (búsqueda \"perro\", página 1, tamaño 3)${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?query=perro&page=1&pageSize=3" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Información de paginación:"
    echo $RESPONSE | jq '.pagination'
    echo "Enlaces de paginación:"
    echo $RESPONSE | jq '.links'
else
    echo $RESPONSE
fi
echo ""

# Test 6: Paginación básica de razas
echo -e "${BLUE}TEST 6: Paginación básica - Razas (página 1, tamaño 2)${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/breeds?page=1&pageSize=2" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Información de paginación:"
    echo $RESPONSE | jq '.pagination'
    echo "Enlaces de paginación:"
    echo $RESPONSE | jq '.links'
    echo "Cantidad de elementos en esta página:"
    echo $RESPONSE | jq '.items | length'
else
    echo $RESPONSE
fi
echo ""

# Test 7: Navegación por enlaces - Siguiente página
echo -e "${BLUE}TEST 7: Navegación por enlaces - Siguiente página${NC}"
# Primero obtenemos el enlace a la siguiente página
if command -v jq &> /dev/null; then
    NEXT_LINK=$(echo $RESPONSE | jq -r '.links.next // ""')

    if [ -n "$NEXT_LINK" ] && [ "$NEXT_LINK" != "null" ]; then
        echo "Navegando a: $NEXT_LINK"
        NEXT_RESPONSE=$(curl -s -X GET "$NEXT_LINK" \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json")

        echo "Información de paginación de la siguiente página:"
        echo $NEXT_RESPONSE | jq '.pagination'
    else
        echo -e "${RED}No hay enlace a siguiente página disponible${NC}"
    fi
else
    echo -e "${RED}Se requiere jq para esta prueba${NC}"
fi
echo ""

# Test 8: Página inválida
echo -e "${BLUE}TEST 8: Página inválida (página -1)${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?page=-1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Información de paginación (debería usar página 1 por defecto):"
    echo $RESPONSE | jq '.pagination'
else
    echo $RESPONSE
fi
echo ""

echo -e "${GREEN}Pruebas de paginación completadas${NC}"