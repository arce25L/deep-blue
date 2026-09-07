# DeepBlue Rescue

## 1. Nombre del proyecto

**DeepBlue Rescue**

## 2. Descripción breve

Proyecto de persistencia para gestionar centros de rescate, casos de rescate,
animales, registros médicos, especialistas, especialidades y tratamientos.

Tecnologías principales: Java 21, Spring Boot, Spring Data JPA, Hibernate,
PostgreSQL, Flyway y Testcontainers.

## 3. Modelo de datos

Entidades implementadas:

- `CentroRescate`
- `CasoRescate`
- `Animal`
- `RegistroMedico`
- `Especialista`
- `Especialidad`
- `Tratamiento`

## 4. Relaciones

- `CentroRescate` 1:N `CasoRescate`
- `CasoRescate` 1:1 `Animal`
- `Animal` 1:1 `RegistroMedico`
- `Especialista` N:M `Especialidad`
- `Animal` 1:N `Tratamiento`
- `Especialista` 1:N `Tratamiento`

La relación N:M utiliza la tabla intermedia `specialist_expertise`.

## 5. Instrucciones para ejecutar

### Requisitos

- Java 21
- Maven
- Docker
- -PostgreSQL

Iniciar PostgreSQL:

```bash
docker compose up -d
```

Ejecutar la aplicación:

```bash
mvn spring-boot:run
```

La conexión puede configurarse con `DB_URL`, `DB_USER` y `DB_PASSWORD`.

## 6. Instrucciones para ejecutar tests

Los tests son de integración y utilizan PostgreSQL mediante Testcontainers.

Ejecutar:

```bash
mvn clean test
```

Es necesario tener Docker disponible para ejecutar los tests.

## 7. Flyway

Flyway se utiliza para crear y versionar el esquema de la base de datos.

Las migraciones se encuentran en:

```text
src/main/resources/db/migration/
```

Migraciones implementadas:

- `V1__create_schema.sql`: crea las tablas y restricciones.
- `V2__insert_expertise_catalog.sql`: inserta las especialidades iniciales.
- `V3__add_tracking_device_to_animal.sql`: agrega `tracking_device_code`.

Hibernate utiliza `ddl-auto: validate`, por lo que no modifica el esquema.

## 8. Testcontainers

Testcontainers permite ejecutar los tests sobre una instancia real de
PostgreSQL dentro de un contenedor independiente.

Se utiliza la imagen:

```text
postgres:18-alpine
```

La configuración utiliza `@Testcontainers`, `@Container` y
`@ServiceConnection`.

## 9. Query Methods implementados

- `findByCode`
- `findByCodecase`
- `findByEstadoOrderByFechaRescateAsc`
- `findByCentroRescateCodigo`
- `findByFechaRescateAfterOrderByFechaRescateDesc`
- `findByNombreComunContainingIgnoreCase`
- `findByCasoRescateEstado`
- `findByCasoRescateCentroRescateCodigo`
- `findByNombreIgnoreCase`
- `findByAnimalIdOrderByRealizadoEnAsc`

También se utilizan métodos heredados de `JpaRepository`, como
`saveAndFlush`, `findById`, `existsById` y `count`.

## 10. Consultas JPQL implementadas

- Especialistas activos por especialidad.
- Tratamientos realizados entre dos fechas.
- Tratamientos realizados en un centro de rescate.
- Tratamientos realizados por especialistas con una especialidad.
- Animales en un estado determinado tratados por especialistas con una
  especialidad determinada.

Las consultas utilizan `JOIN`, `DISTINCT`, `LOWER`, parámetros nombrados y
`ORDER BY` según las necesidades de cada consulta.
