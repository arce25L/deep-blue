# Métodos que deben existir en los repositories

```java
// RescueCaseRepository
Optional<RescueCase> findByCaseCode(String caseCode);
List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);
Optional<RescueCase> findByAnimalAnimalCode(String animalCode);   // NUEVO (RescueCase.animal.animalCode)

// AnimalRepository
Optional<Animal> findByAnimalCode(String animalCode);

// SpecialistRepository
Optional<Specialist> findByProfessionalCode(String professionalCode);

// TreatmentRepository
List<Treatment> findByAnimalAnimalCodeOrderByPerformedAtAsc(String animalCode);
```
