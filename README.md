# PetHunt

<p align="center">
  <img src="assets/circular_logo.png" alt="PetHunt Logo" width="300" height="300"/>
</p>

<p align="center">
  Una plataforma social centrada en mascotas que conecta propietarios y comparte información sobre especies y razas.
</p>

<p align="center">
  <a href="#visión-general">Visión General</a> •
  <a href="#características-principales">Características Principales</a> •
  <a href="#tecnologías">Tecnologías</a> •
  <a href="#configuración">Configuración</a> •
  <a href="#estructura-del-proyecto">Estructura del Proyecto</a> •
  <a href="#api">API</a> •
  <a href="#roadmap">Roadmap</a> •
  <a href="#contribuir">Contribuir</a> •
  <a href="#licencia">Licencia</a>
</p>

## Visión General

PetHunt es una plataforma social centrada en mascotas que permite a los propietarios conectar entre sí, acceder a información detallada sobre diferentes especies y razas, y compartir contenido relacionado con sus mascotas. El proyecto se está desarrollando de manera incremental, siguiendo un plan de implementación detallado con un backend en Ktor y un frontend planificado en Astro.js.

## Características Principales

### Repositorio de Mascotas
- Base de datos comprensiva de razas y especies
- Categorización por tipo de animal (perros, gatos, aves, etc.)
- Subcategorización específica por características
- Sistema de búsqueda avanzada con filtros
- Información detallada de cada raza/especie
- Top 10 de animales por categoría basado en votaciones de la comunidad

### Sistema de Blog
- Artículos relacionados con el mundo animal
- Contenido científico y educativo
- Sistema de etiquetado y categorización
- Búsqueda y filtrado de artículos
- Integración con el repositorio de mascotas

### Funcionalidades de Comunidad
- Sistema de registro y perfiles de usuario
- Registro de mascotas propias con fotos de perfil
- Marcado de razas/especies favoritas
- Sistema de comentarios en artículos
- Búsqueda de usuarios con mascotas similares
- Sistema de geolocalización aproximada para conexiones locales
- Protección de privacidad y ubicación exacta
- Sistema de mensajería entre usuarios
- Valoraciones y rankings de mascotas

## Tecnologías

### Stack Tecnológico
- **Backend**:
    - **Lenguaje**: [Kotlin](https://kotlinlang.org/)
    - **Framework**: [Ktor](https://ktor.io/)
    - **Base de Datos**:
        - [PostgreSQL](https://www.postgresql.org/) (datos relacionales)
        - [MongoDB](https://www.mongodb.com/) (contenido y datos de especies)
        - [Redis](https://redis.io/) (caché y sesiones)
    - **Almacenamiento**: [Firebase Storage](https://firebase.google.com/products/storage) (multimedia)
    - **Seguridad**: JWT, BCrypt
    - **Inyección de Dependencias**: [Koin](https://insert-koin.io/)
- **Frontend** (en desarrollo):
    - [Astro.js](https://astro.build/) con [React](https://reactjs.org/) para componentes interactivos
- **Apps Móviles** (fase posterior):
    - [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)

### Herramientas de Desarrollo
- **Build System**: [Gradle](https://gradle.org/) con Kotlin DSL
- **IDE Recomendado**: [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- **Testing**: JUnit, Ktor Test, Mockk
- **CI/CD**: GitHub Actions

## Configuración

### Prerrequisitos
- JDK 17 o superior
- PostgreSQL 14+
- MongoDB 6.0+
- Redis 6+ (opcional, para caché y sesiones)
- Cuenta Firebase (para almacenamiento)

### Guías de instalación de bases de datos

#### macOS (con Homebrew)

```bash
# PostgreSQL
brew install postgresql
brew services start postgresql
createdb pethunt

# MongoDB
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb/brew/mongodb-community

# Redis
brew install redis
brew services start redis
```

#### Linux (Ubuntu/Debian)

```bash
# PostgreSQL
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo -u postgres createdb pethunt

# MongoDB
wget -qO - https://www.mongodb.org/static/pgp/server-6.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu $(lsb_release -cs)/mongodb-org/6.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-6.0.list
sudo apt update
sudo apt install mongodb-org
sudo systemctl start mongod

# Redis
sudo apt install redis-server
sudo systemctl start redis-server
```

### Instalación y Ejecución

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/santipbarber/pethunt.git
   cd pethunt
   ```

2. Configurar las bases de datos:
    - Crear una base de datos PostgreSQL llamada `pethunt`
    - Asegurarse que MongoDB esté funcionando en el puerto predeterminado
    - Asegurarse que Redis esté funcionando en el puerto predeterminado

3. Configurar el archivo `application.conf` en `src/main/resources`:
   ```hocon
   ktor {
       deployment {
           port = 8080
       }
       application {
           modules = [ com.pethunt.server.ApplicationKt.module ]
       }
   }
   
   database {
       postgres {
           host = "localhost"
           port = 5432
           database = "pethunt"
           user = "postgres"
           password = "postgres"
           auto_create = true  # Establecer a false en producción
       }
       mongodb {
           uri = "mongodb://localhost:27017/pethunt"
       }
       redis {
           host = "localhost"
           port = 6379
       }
   }
   
   jwt {
       secret = "tu-secreto-aqui"
       issuer = "http://0.0.0.0:8080/"
       audience = "http://0.0.0.0:8080/users"
       realm = "PetHunt API"
   }
   
   firebase {
       storageBucket = "tu-bucket-firebase.appspot.com"
   }
   ```

4. Ejecutar la aplicación:
   ```bash
   ./gradlew :server:run
   ```

5. La API estará disponible en `http://localhost:8080`

### Probar APIs con los scripts de prueba
El proyecto incluye varios scripts para probar las APIs:

```bash
# Prueba registro y login
./test-api.sh

# Prueba funcionalidades de especies y razas
./test-species-breeds-api.sh

# Prueba funcionalidades de mascotas
./test-pets-api.sh

# Prueba el sistema de búsqueda
./test-search-api.sh

# Prueba el sistema de paginación
./test-pagination-api.sh
```

## Estructura del Proyecto

```
com.pethunt.server
├── Application.kt                # Punto de entrada de la aplicación
├── config/                       # Configuraciones
│   ├── DatabaseFactory.kt        # Configuración PostgreSQL
│   ├── MongoFactory.kt           # Configuración MongoDB
│   ├── RedisFactory.kt           # Configuración Redis
│   └── SecurityConfig.kt         # Configuración JWT/seguridad
├── models/                       # Modelos de datos
│   ├── User.kt                   # Modelo de usuario
│   ├── Pet.kt                    # Modelo de mascota
│   ├── Species.kt                # Modelo de especie
│   ├── Breed.kt                  # Modelo de raza
│   └── serializers/              # Serializadores personalizados
│       └── ObjectIdSerializer.kt # Serialización para MongoDB
├── repositories/                 # Acceso a datos
│   ├── UserRepository.kt         # Repositorio PostgreSQL - Usuarios
│   ├── PetRepository.kt          # Repositorio PostgreSQL - Mascotas
│   ├── SpeciesRepository.kt      # Repositorio MongoDB - Especies
│   └── BreedRepository.kt        # Repositorio MongoDB - Razas
├── services/                     # Lógica de negocio
│   ├── UserService.kt            # Servicio usuarios/autenticación
│   ├── PetService.kt             # Servicio de mascotas
│   ├── SpeciesService.kt         # Servicio de especies
│   ├── BreedService.kt           # Servicio de razas
│   ├── CacheService.kt           # Servicio de caché
│   └── StorageService.kt         # Servicio Firebase Storage
├── routes/                       # Endpoints API REST
│   ├── UserRoutes.kt             # Rutas usuarios/auth
│   ├── PetRoutes.kt              # Rutas mascotas
│   ├── SpeciesRoutes.kt          # Rutas especies
│   ├── BreedRoutes.kt            # Rutas razas
│   ├── StatusRoutes.kt           # Rutas de estado
│   └── ImageRoutes.kt            # Rutas de imágenes
├── utils/                        # Utilidades
│   ├── PaginationUtils.kt        # Utilidades para paginación
│   └── ValidationUtils.kt        # Validaciones de datos
├── plugins/                      # Plugins Ktor
│   ├── Routing.kt                # Configuración de rutas
│   ├── Serialization.kt          # Serialización JSON
│   ├── Security.kt               # Configuración seguridad
│   ├── Monitoring.kt             # Logging y monitorización
│   └── Databases.kt              # Inicialización de bases de datos
└── di/                           # Inyección de dependencias
    └── appModule.kt              # Módulo principal de Koin
```

## API

La API RESTful de PetHunt está organizada en los siguientes recursos principales:

### Autenticación
- `POST /users/register` - Registro de usuario
- `POST /users/login` - Inicio de sesión (devuelve token JWT)

### Usuarios
- `GET /users/profile` - Obtener perfil de usuario actual
- `PUT /users/{id}` - Actualizar perfil de usuario
- `POST /users/change-password` - Cambiar contraseña

### Mascotas
- `GET /pets` - Listar mascotas del usuario actual
- `GET /pets/{id}` - Obtener mascota específica
- `POST /pets` - Crear nueva mascota
- `PUT /pets/{id}` - Actualizar mascota
- `DELETE /pets/{id}` - Eliminar mascota
- `POST /pets/{id}/profile-image` - Actualizar imagen de perfil

### Especies
- `GET /species` - Listar todas las especies (con filtros opcionales)
- `GET /species/{id}` - Obtener especie específica
- `POST /species` - Crear nueva especie (admin)
- `PUT /species/{id}` - Actualizar especie (admin)
- `DELETE /species/{id}` - Eliminar especie (admin)

### Razas
- `GET /breeds` - Listar todas las razas (con filtros opcionales)
- `GET /breeds/{id}` - Obtener raza específica
- `POST /breeds` - Crear nueva raza (admin)
- `PUT /breeds/{id}` - Actualizar raza (admin)
- `DELETE /breeds/{id}` - Eliminar raza (admin)

### Imágenes
- `POST /images/upload` - Subir imagen (multipart form)
- `POST /images/upload-url` - Subir imagen desde URL
- `DELETE /images/{url}` - Eliminar imagen

### Estado
- `GET /status` - Estado del sistema
- `GET /status/cache` - Métricas de caché
- `GET /status/storage` - Estado de Firebase Storage

## Roadmap

### Fase 1: Preparación y Diseño ✅
- Definición de requisitos técnicos
- Selección de tecnologías
- Diseño de arquitectura
- Documentación inicial

### Fase 2: Desarrollo del Core (En Progreso) 🔄
- Implementación del backend con Ktor
- Configuración de bases de datos
- Sistema básico de autenticación
- Implementación del repositorio de razas/especies
- CRUD de usuarios y mascotas
- Sistema de almacenamiento de imágenes

### Fase 3: Características Sociales (Próximamente) 📅
- Sistema de perfiles de usuario y mascotas
- Sistema de blog y comentarios
- Sistema de notificaciones
- WebSockets para chat en tiempo real
- Funcionalidades de geolocalización

### Fase 4: Expansión Frontend y Móvil (Futuro) 🔮
- Desarrollo del frontend con Astro.js
- Desarrollo de apps con Kotlin Multiplatform
- Adaptación de la API para móviles
- Sincronización multiplataforma
- Características específicas para móviles

## Testing

Para ejecutar los tests:

```bash
./gradlew :server:test
```

El proyecto incluye:
- Tests unitarios con JUnit y Mockk
- Tests de integración para repositorios
- Tests de servicios
- Scripts de prueba de API

## Contribuir

¡Las contribuciones son bienvenidas! En PetHunt, valoramos especialmente las contribuciones relacionadas con:

### Conocimiento sobre mascotas
- **Datos de especies**: Información científica y educativa sobre diferentes especies
- **Razas**: Características, cuidados y peculiaridades de razas específicas
- **Conexiones con APIs**: Integración con repositorios de información sobre animales
- **Verificación de datos**: Validación de información existente para garantizar precisión

### Desarrollo técnico
- **Mejoras en el backend**: Optimizaciones, nuevas funcionalidades
- **Testing**: Ampliación de la cobertura de pruebas
- **Documentación**: Mejoras en la documentación técnica y de usuario
- **Frontend**: Desarrollo del frontend con Astro.js

### Cómo contribuir
1. Haz un fork del repositorio
2. Crea una rama para tu contribución (`git checkout -b feature/amazing-feature`)
3. Realiza tus cambios y documéntalos adecuadamente
4. Asegúrate de que todos los tests pasan (`./gradlew test`)
5. Haz commit de tus cambios (`git commit -m 'Add some amazing feature'`)
6. Push a tu rama (`git push origin feature/amazing-feature`)
7. Abre un Pull Request

### Contribución de datos
Si quieres contribuir con información sobre especies o razas, por favor:
1. Consulta la estructura de datos en `docs/data-structure.md`
2. Prepara tus datos en el formato JSON especificado
3. Incluye referencias a fuentes confiables para verificación
4. Abre un Pull Request con tus datos en la carpeta `data/contributions/`

El equipo revisará y validará la información antes de incorporarla a la base de datos principal.

## Licencia

Este proyecto está licenciado bajo la licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

## Contacto

- Nombre - [Santiago Pérez Barber](mailto:santipbr@gmail.com)
- GitHub - [santipbarber](https://github.com/santipbarber)
- Web - En construcción!!!