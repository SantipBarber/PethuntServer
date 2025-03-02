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
  <a href="#roadmap">Roadmap</a> •
  <a href="#licencia">Licencia</a>
</p>

## Visión General

PetHunt es una plataforma social centrada en mascotas que permite a los propietarios conectar entre sí, acceder a información detallada sobre diferentes especies y razas, y compartir contenido relacionado con sus mascotas. El proyecto se está desarrollando de manera incremental, comenzando con esta aplicación backend en Ktor.

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

### Backend
- **Lenguaje**: [Kotlin](https://kotlinlang.org/)
- **Framework**: [Ktor](https://ktor.io/)
- **Base de Datos**:
  - [PostgreSQL](https://www.postgresql.org/) (datos relacionales)
  - [MongoDB](https://www.mongodb.com/) (contenido y datos de especies)
  - [Redis](https://redis.io/) (caché y sesiones)
- **Almacenamiento**: [Firebase Storage](https://firebase.google.com/products/storage) (multimedia)
- **Seguridad**: JWT, BCrypt
- **Inyección de Dependencias**: [Koin](https://insert-koin.io/)

### Herramientas de Desarrollo
- **Build System**: [Gradle](https://gradle.org/) con Kotlin DSL
- **IDE Recomendado**: [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- **Testing**: JUnit, Ktor Test
- **CI/CD**: GitHub Actions

## Configuración

### Prerrequisitos
- JDK 17 o superior
- PostgreSQL
- MongoDB
- Redis
- Cuenta Firebase (para almacenamiento)

### Instalación y Ejecución

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/usuario/pethunt.git
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
       issuer = "pethunt.com"
       audience = "pethunt-audience"
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

## Estructura del Proyecto

```
com.pethunt.server
├── Application.kt                # Punto de entrada de la aplicación
├── config/                       # Configuraciones
│   ├── DatabaseConfig.kt         # Configuración de bases de datos
│   ├── SecurityConfig.kt         # Configuración de JWT y seguridad
│   ├── FirebaseConfig.kt         # Configuración de Firebase
│   └── SerializationConfig.kt    # Configuración de serialización JSON
├── models/                       # Modelos de datos
│   ├── User.kt                   # Modelo de usuario
│   ├── Pet.kt                    # Modelo de mascota
│   ├── Species.kt                # Modelo de especie
│   ├── Breed.kt                  # Modelo de raza
│   ├── BlogArticle.kt            # Modelo de artículo de blog
│   └── dto/                      # Data Transfer Objects
│       ├── AuthDTO.kt            # DTOs para autenticación
│       ├── UserDTO.kt            # DTOs para usuarios
│       └── PetDTO.kt             # DTOs para mascotas
├── repositories/                 # Acceso a datos
│   ├── UserRepository.kt         # Repositorio de usuarios (PostgreSQL)
│   ├── PetRepository.kt          # Repositorio de mascotas (PostgreSQL)
│   ├── SpeciesRepository.kt      # Repositorio de especies (MongoDB)
│   ├── BreedRepository.kt        # Repositorio de razas (MongoDB)
│   └── BlogRepository.kt         # Repositorio de blog (MongoDB)
├── services/                     # Lógica de negocio
│   ├── AuthService.kt            # Servicio de autenticación
│   ├── UserService.kt            # Servicio de usuarios
│   ├── PetService.kt             # Servicio de mascotas
│   ├── SpeciesService.kt         # Servicio de especies
│   ├── BlogService.kt            # Servicio de blog
│   └── StorageService.kt         # Servicio de almacenamiento (Firebase)
├── routes/                       # Endpoints de la API
│   ├── AuthRoutes.kt             # Rutas de autenticación
│   ├── UserRoutes.kt             # Rutas de usuarios
│   ├── PetRoutes.kt              # Rutas de mascotas
│   ├── SpeciesRoutes.kt          # Rutas de especies
│   └── BlogRoutes.kt             # Rutas de blog
├── utils/                        # Utilidades
│   ├── JwtUtils.kt               # Utilidades para JWT
│   ├── ValidationUtils.kt        # Utilidades para validación
│   ├── PasswordUtils.kt          # Utilidades para contraseñas
│   └── GeoUtils.kt               # Utilidades para geolocalización
└── di/                           # Inyección de dependencias
    ├── ApplicationModule.kt      # Módulo principal de Koin
    ├── DatabaseModule.kt         # Módulo de conexiones de BD
    ├── RepositoryModule.kt       # Módulo de repositorios
    └── ServiceModule.kt          # Módulo de servicios
```

## Roadmap

### Fase 1: Preparación y Diseño (Completado)
- Definición de requisitos técnicos
- Selección de tecnologías
- Diseño de arquitectura
- Documentación inicial

### Fase 2: Desarrollo del Core (En Progreso)
- Implementación del backend con Ktor
- Configuración de bases de datos
- Sistema básico de autenticación
- Implementación del repositorio de razas/especies
- CRUD de usuarios y mascotas

### Fase 3: Características Sociales
- Sistema de perfiles de usuario y mascotas
- Sistema de blog y comentarios
- Sistema de notificaciones
- WebSockets para chat en tiempo real
- Funcionalidades de geolocalización

### Fase 4: Expansión Móvil
- Desarrollo de apps con Kotlin Multiplatform
- Adaptación de la API para móviles
- Sincronización multiplataforma
- Características específicas para móviles

## Documentación de API

La documentación detallada de la API estará disponible en `/api/docs` una vez que el servidor esté en funcionamiento. Incluye todos los endpoints, parámetros requeridos, formatos de respuesta y ejemplos.

## Testing

Para ejecutar los tests:

```bash
./gradlew :server:test
```

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
- GitHub - [SpBarber](https://github.com/santipbarber)
- Web - En construcción!!!