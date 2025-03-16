#!/bin/bash
# test-species-breeds-api.sh

BASE_URL="http://localhost:8080"
TOKEN=""
SPECIES_ID=""
BREED_ID=""

# Colores para mejor visualización
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== TEST API ESPECIES Y RAZAS PETHUNT ===${NC}\n"

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

# Test 1: Crear una nueva especie
echo -e "${BLUE}TEST 1: Creando nueva especie...${NC}"
SPECIES_RESPONSE=$(curl -s -X POST "$BASE_URL/species" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DOG",
    "scientificName": "Canis lupus familiaris",
    "commonNames": [
      {"language": "es", "text": "Perro"},
      {"language": "en", "text": "Dog"}
    ],
    "descriptions": [
      {"language": "es", "text": "El perro es un mamífero carnívoro de la familia de los cánidos"},
      {"language": "en", "text": "The dog is a domesticated carnivoran mammal of the family Canidae"}
    ],
    "characteristics": {
      "size": "MEDIUM",
      "lifespanMin": 8,
      "lifespanMax": 15,
      "temperament": ["Leal", "Amigable", "Protector"]
    },
    "careInfo": {
      "exerciseNeeds": 4,
      "groomingNeeds": 3,
      "trainingDifficulty": 2,
      "dietaryRequirements": ["Proteínas", "Vitaminas", "Minerales"]
    }
  }')

# Extraer el ID de la especie creada
if command -v jq &> /dev/null; then
    SPECIES_ID=$(echo $SPECIES_RESPONSE | jq -r '._id.$oid')
    echo "Respuesta completa:"
    echo $SPECIES_RESPONSE | jq
else
    # Fallback si jq no está disponible
    SPECIES_ID=$(echo $SPECIES_RESPONSE | grep -o '"_id":\{"$oid":"[^"]*"' | sed 's/"_id":\{"$oid":"//g' | sed 's/"//g')
    echo "Respuesta completa:"
    echo $SPECIES_RESPONSE
fi

if [ -z "$SPECIES_ID" ] || [ "$SPECIES_ID" == "null" ]; then
    echo -e "${RED}Error al crear especie${NC}"
    echo "Respuesta del servidor: $SPECIES_RESPONSE"
else
    echo -e "${GREEN}Especie creada correctamente${NC}"
    echo "ID de especie: $SPECIES_ID"
fi
echo ""

# Test 2: Obtener todas las especies
echo -e "${BLUE}TEST 2: Obteniendo todas las especies...${NC}"
SPECIES_LIST=$(curl -s -X GET "$BASE_URL/species" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $SPECIES_LIST | jq
else
    echo $SPECIES_LIST
fi
echo ""

# Test 3: Obtener detalles de una especie específica
echo -e "${BLUE}TEST 3: Obteniendo detalles de la especie $SPECIES_ID...${NC}"
SPECIES_DETAILS=$(curl -s -X GET "$BASE_URL/species/$SPECIES_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $SPECIES_DETAILS | jq
else
    echo $SPECIES_DETAILS
fi
echo ""

# Test 4: Buscar especies por tipo
echo -e "${BLUE}TEST 4: Buscando especies por tipo 'DOG'...${NC}"
SEARCH_RESPONSE=$(curl -s -X GET "$BASE_URL/species/search?type=DOG" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $SEARCH_RESPONSE | jq
else
    echo $SEARCH_RESPONSE
fi
echo ""

# Test 5: Crear una nueva raza
echo -e "${BLUE}TEST 5: Creando nueva raza...${NC}"
BREED_RESPONSE=$(curl -s -X POST "$BASE_URL/breeds" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "speciesId": "'$SPECIES_ID'",
    "names": [
      {"language": "es", "text": "Golden Retriever"},
      {"language": "en", "text": "Golden Retriever"}
    ],
    "descriptions": [
      {"language": "es", "text": "El Golden Retriever es una raza de perro que se desarrolló en Escocia"},
      {"language": "en", "text": "The Golden Retriever is a breed of dog developed in Scotland"}
    ],
    "characteristics": {
      "size": "LARGE",
      "weightRange": {
        "min": 25,
        "max": 34,
        "unit": "kg"
      },
      "heightRange": {
        "min": 55,
        "max": 61,
        "unit": "cm"
      },
      "coatTypes": ["Largo", "Denso", "Impermeable"],
      "colors": ["Dorado", "Crema"],
      "temperament": ["Inteligente", "Amable", "Confiable"]
    }
  }')

# Extraer el ID de la raza creada
if command -v jq &> /dev/null; then
    BREED_ID=$(echo $BREED_RESPONSE | jq -r '._id.$oid')
    echo "Respuesta completa:"
    echo $BREED_RESPONSE | jq
else
    # Fallback si jq no está disponible
    BREED_ID=$(echo $BREED_RESPONSE | grep -o '"_id":\{"$oid":"[^"]*"' | sed 's/"_id":\{"$oid":"//g' | sed 's/"//g')
    echo "Respuesta completa:"
    echo $BREED_RESPONSE
fi

if [ -z "$BREED_ID" ] || [ "$BREED_ID" == "null" ]; then
    echo -e "${RED}Error al crear raza${NC}"
    echo "Respuesta del servidor: $BREED_RESPONSE"
else
    echo -e "${GREEN}Raza creada correctamente${NC}"
    echo "ID de raza: $BREED_ID"
fi
echo ""

# Test 6: Obtener todas las razas
echo -e "${BLUE}TEST 6: Obteniendo todas las razas...${NC}"
BREED_LIST=$(curl -s -X GET "$BASE_URL/breeds" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $BREED_LIST | jq
else
    echo $BREED_LIST
fi
echo ""

# Test 7: Obtener detalles de una raza específica
echo -e "${BLUE}TEST 7: Obteniendo detalles de la raza $BREED_ID...${NC}"
BREED_DETAILS=$(curl -s -X GET "$BASE_URL/breeds/$BREED_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $BREED_DETAILS | jq
else
    echo $BREED_DETAILS
fi
echo ""

# Test 8: Buscar razas por características (tamaño)
echo -e "${BLUE}TEST 8: Buscando razas por tamaño 'LARGE'...${NC}"
SEARCH_BREEDS=$(curl -s -X GET "$BASE_URL/breeds/search?size=LARGE" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $SEARCH_BREEDS | jq
else
    echo $SEARCH_BREEDS
fi
echo ""

# Test 9: Obtener razas por especie
echo -e "${BLUE}TEST 9: Obteniendo razas para la especie $SPECIES_ID...${NC}"
BREEDS_BY_SPECIES=$(curl -s -X GET "$BASE_URL/species/$SPECIES_ID/breeds" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $BREEDS_BY_SPECIES | jq
else
    echo $BREEDS_BY_SPECIES
fi
echo ""

# Test 10: Actualizar una especie
echo -e "${BLUE}TEST 10: Actualizando especie...${NC}"
UPDATE_SPECIES=$(curl -s -X PUT "$BASE_URL/species/$SPECIES_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DOG",
    "scientificName": "Canis lupus familiaris",
    "commonNames": [
      {"language": "es", "text": "Perro doméstico"},
      {"language": "en", "text": "Domestic dog"}
    ],
    "descriptions": [
      {"language": "es", "text": "El perro es un mamífero carnívoro de la familia de los cánidos, actualizado"},
      {"language": "en", "text": "The dog is a domesticated carnivoran mammal of the family Canidae, updated"}
    ],
    "characteristics": {
      "size": "MEDIUM",
      "lifespanMin": 8,
      "lifespanMax": 16,
      "temperament": ["Leal", "Amigable", "Protector", "Inteligente"]
    }
  }')

if command -v jq &> /dev/null; then
    echo $UPDATE_SPECIES | jq
else
    echo $UPDATE_SPECIES
fi
echo ""

# Test 11: Actualizar una raza
echo -e "${BLUE}TEST 11: Actualizando raza...${NC}"
UPDATE_BREED=$(curl -s -X PUT "$BASE_URL/breeds/$BREED_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "speciesId": "'$SPECIES_ID'",
    "names": [
      {"language": "es", "text": "Golden Retriever"},
      {"language": "en", "text": "Golden Retriever"}
    ],
    "descriptions": [
      {"language": "es", "text": "El Golden Retriever es una raza de perro que se desarrolló en Escocia, actualizado"},
      {"language": "en", "text": "The Golden Retriever is a breed of dog developed in Scotland, updated"}
    ],
    "characteristics": {
      "size": "LARGE",
      "weightRange": {
        "min": 25,
        "max": 35,
        "unit": "kg"
      },
      "heightRange": {
        "min": 55,
        "max": 62,
        "unit": "cm"
      },
      "coatTypes": ["Largo", "Denso", "Impermeable", "Doble capa"],
      "colors": ["Dorado", "Crema", "Dorado oscuro"],
      "temperament": ["Inteligente", "Amable", "Confiable", "Juguetón"]
    }
  }')

if command -v jq &> /dev/null; then
    echo $UPDATE_BREED | jq
else
    echo $UPDATE_BREED
fi
echo ""

# Test 12: Buscar especies por texto
echo -e "${BLUE}TEST 12: Buscando especies por texto 'doméstico'...${NC}"
SEARCH_SPECIES_TEXT=$(curl -s -X GET "$BASE_URL/species/search?query=doméstico" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $SEARCH_SPECIES_TEXT | jq
else
    echo $SEARCH_SPECIES_TEXT
fi
echo ""

# Test 13: Buscar razas por texto
echo -e "${BLUE}TEST 13: Buscando razas por texto 'Golden'...${NC}"
SEARCH_BREEDS_TEXT=$(curl -s -X GET "$BASE_URL/breeds/search?query=Golden" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if command -v jq &> /dev/null; then
    echo $SEARCH_BREEDS_TEXT | jq
else
    echo $SEARCH_BREEDS_TEXT
fi
echo ""

# Test 14: Eliminar una raza
echo -e "${BLUE}TEST 14: Eliminando raza...${NC}"
DELETE_BREED=$(curl -s -X DELETE "$BASE_URL/breeds/$BREED_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "%{http_code}")

HTTP_CODE=${DELETE_BREED: -3}
BODY=${DELETE_BREED:0:${#DELETE_BREED}-3}

echo "Código HTTP: $HTTP_CODE"
if [ "$HTTP_CODE" == "204" ]; then
    echo -e "${GREEN}Raza eliminada correctamente${NC}"
else
    echo -e "${RED}Error al eliminar raza${NC}"
    echo "Respuesta del servidor: $BODY"
fi
echo ""

# Test 15: Eliminar una especie
echo -e "${BLUE}TEST 15: Eliminando especie...${NC}"
DELETE_SPECIES=$(curl -s -X DELETE "$BASE_URL/species/$SPECIES_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "%{http_code}")

HTTP_CODE=${DELETE_SPECIES: -3}
BODY=${DELETE_SPECIES:0:${#DELETE_SPECIES}-3}

echo "Código HTTP: $HTTP_CODE"
if [ "$HTTP_CODE" == "204" ]; then
    echo -e "${GREEN}Especie eliminada correctamente${NC}"
else
    echo -e "${RED}Error al eliminar especie${NC}"
    echo "Respuesta del servidor: $BODY"
fi
echo ""

echo -e "${GREEN}Pruebas completadas${NC}"
