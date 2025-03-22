#!/bin/bash
# test-search-advanced.sh

BASE_URL="http://localhost:8080"
TOKEN=""

# Colores para mejor visualización
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== TEST BÚSQUEDA AVANZADA PETHUNT ===${NC}\n"

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

# Prueba 1: Búsqueda básica de especies
echo -e "${BLUE}TEST 1: Búsqueda básica de especies por texto 'perro'${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?query=perro" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Total resultados: $(echo $RESPONSE | jq '.pagination.total')"
    echo "Paginación: $(echo $RESPONSE | jq '.pagination')"
    echo "Primeros 2 resultados:"
    echo $RESPONSE | jq '.items[0:2]'
else
    echo $RESPONSE
fi
echo ""

# Prueba 2: Búsqueda avanzada de especies por múltiples criterios
echo -e "${BLUE}TEST 2: Búsqueda avanzada de especies con múltiples filtros${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?type=DOG&size=MEDIUM&temperament=Leal&temperament=Amigable&language=es" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Total resultados: $(echo $RESPONSE | jq '.pagination.total')"
    echo "Paginación: $(echo $RESPONSE | jq '.pagination')"
    echo "Primeros 2 resultados:"
    echo $RESPONSE | jq '.items[0:2]'
else
    echo $RESPONSE
fi
echo ""

# Prueba 3: Búsqueda básica de razas
echo -e "${BLUE}TEST 3: Búsqueda básica de razas por texto 'golden'${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/breeds?query=golden" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Total resultados: $(echo $RESPONSE | jq '.pagination.total')"
    echo "Paginación: $(echo $RESPONSE | jq '.pagination')"
    echo "Primeros 2 resultados:"
    echo $RESPONSE | jq '.items[0:2]'
else
    echo $RESPONSE
fi
echo ""

# Prueba 4: Búsqueda avanzada de razas por múltiples criterios
echo -e "${BLUE}TEST 4: Búsqueda avanzada de razas con múltiples filtros${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/breeds?size=LARGE&temperaments=Amigable&temperaments=Inteligente&colors=Dorado&minWeight=20&maxWeight=35" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Total resultados: $(echo $RESPONSE | jq '.pagination.total')"
    echo "Paginación: $(echo $RESPONSE | jq '.pagination')"
    echo "Primeros 2 resultados:"
    echo $RESPONSE | jq '.items[0:2]'
else
    echo $RESPONSE
fi
echo ""

# Prueba 5: Búsqueda de razas por especie
echo -e "${BLUE}TEST 5: Búsqueda de razas por especie${NC}"
SPECIES_RESPONSE=$(curl -s -X GET "$BASE_URL/species?type=DOG&limit=1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

# Extraer el ID de la primera especie (si existe)
if command -v jq &> /dev/null; then
    SPECIES_ID=$(echo $SPECIES_RESPONSE | jq -r '.items[0].id // ""')
else
    SPECIES_ID=$(echo $SPECIES_RESPONSE | grep -o '"id":"[^"]*"' | head -1 | sed 's/"id":"//g' | sed 's/"//g')
fi

if [ -n "$SPECIES_ID" ]; then
    RESPONSE=$(curl -s -X GET "$BASE_URL/breeds?speciesId=$SPECIES_ID" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json")

    if command -v jq &> /dev/null; then
        echo "Total resultados: $(echo $RESPONSE | jq '.pagination.total')"
        echo "Paginación: $(echo $RESPONSE | jq '.pagination')"
        echo "Primeros 2 resultados:"
        echo $RESPONSE | jq '.items[0:2]'
    else
        echo $RESPONSE
    fi
else
    echo -e "${RED}No se encontraron especies para probar la búsqueda por especie${NC}"
fi
echo ""

# Prueba 6: Paginación
echo -e "${BLUE}TEST 6: Prueba de paginación en especies${NC}"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?page=1&pageSize=5" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Página 1 - Paginación: $(echo $RESPONSE | jq '.pagination')"
    echo "Número de resultados: $(echo $RESPONSE | jq '.items | length')"
else
    echo $RESPONSE
fi

echo -e "\nSegunda página:"
RESPONSE=$(curl -s -X GET "$BASE_URL/species?page=2&pageSize=5" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo "Página 2 - Paginación: $(echo $RESPONSE | jq '.pagination')"
    echo "Número de resultados: $(echo $RESPONSE | jq '.items | length')"
else
    echo $RESPONSE
fi
echo ""

echo -e "${GREEN}Pruebas de búsqueda avanzada completadas${NC}"