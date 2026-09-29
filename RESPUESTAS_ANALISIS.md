# Respuestas — Parte XIII (Análisis)

## 58. Clasificar responsabilidades

| Necesidad | Capa |
|---|---|
| `SELECT` de RescueCase por código | **Repository** |
| Validar transición de status | **Service** |
| Convertir RescueCase a DTO | **Mapper** |
| Guardar Treatment | **Repository** (invocado desde el Service) |
| Verificar especialista activo | **Service** |
| Crear tabla treatments | **Flyway** |
| Controlar transacción | **Service** (`@Transactional`) |
| Representar información persistente | **Entity** |

## 59. ¿Por qué `specialist.isActive()` no va en `SpecialistRepository`?
Un Repository solo sabe *cómo acceder a los datos* (consultas, guardado). "Un especialista inactivo no puede tratar animales" es una **regla de negocio** que depende del contexto de la operación (registrar un tratamiento), no del acceso a datos. Además: el Repository no debe lanzar excepciones de dominio, la regla podría cambiar según el caso de uso (ej. consultar un especialista inactivo sí es válido) y mezclarla haría al Repository difícil de reutilizar y de probar. Esa orquestación pertenece al Service.

## 60. Transacciones
- `@Transactional(readOnly = true)`: `findByCode()`, `findByStatus()` (y `findByAnimalCode()`).
- Escritura (`@Transactional`): `registerTreatment()` (`register`) y `changeStatus()`, porque modifican o insertan datos y deben ser atómicas.

## 61. `orElseThrow(...)` vs `.get()`
`.get()` lanza `NoSuchElementException` sin contexto si el Optional está vacío: mensaje inútil, no es una excepción de dominio, no se puede traducir limpiamente a un 404 y oculta la intención. `orElseThrow` obliga a decidir qué pasa cuando no existe y lanza un error significativo (`ResourceNotFoundException("Rescue case not found: RES-999")`).

## 62. ¿Por qué no retornar la entidad `RescueCase` desde el Service?
- **Acoplamiento:** las capas superiores dependerían del modelo persistente y de JPA.
- **Lazy Loading:** al salir de la transacción, acceder a `animal` o `rescueCenter` lanza `LazyInitializationException`; además puede disparar consultas N+1.
- **Contrato entre capas:** el DTO define explícitamente qué se ofrece; la entidad ofrece todo.
- **Información expuesta:** una entidad puede filtrar campos internos o relaciones completas.
- **Evolución del modelo:** puedes cambiar tablas y entidades sin romper a los consumidores del DTO.
