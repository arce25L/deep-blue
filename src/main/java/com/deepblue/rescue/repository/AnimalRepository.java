package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    // Consulta A: buscar animal por animalCode
    Optional<Animal> findByAnimalCode(String animalCode);

    // Consulta B: buscar animales cuyo commonName contenga determinado texto (ignore case)
    List<Animal> findByCommonNameContainingIgnoreCase(String text);

    // Paso 35: animales cuyo caso de rescate tenga determinado estado
    // Animal -> rescueCase -> status
    List<Animal> findByRescueCaseStatus(RescueStatus status);

    // Paso 36: animales pertenecientes a un centro determinado
    // Animal -> RescueCase -> RescueCenter -> code
    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);

    // Reto sin guía (Parte XIII):
    // Animales en cierto estado de rescueCase que hayan recibido al menos un
    // tratamiento realizado por un especialista con determinada experiencia.
    @Query("""
            select distinct a
            from Animal a
            join a.rescueCase rc
            join a.treatments t
            join t.specialist s
            join s.expertiseAreas e
            where rc.status = :status
              and lower(e.name) = lower(:expertiseName)
            """)
    List<Animal> findInRehabilitationTreatedBySpecialistWithExpertise(
            @Param("status") RescueStatus status,
            @Param("expertiseName") String expertiseName);
}