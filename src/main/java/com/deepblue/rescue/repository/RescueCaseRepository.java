package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {

    // Consulta A: buscar un caso por caseCode
    Optional<RescueCase> findByCaseCode(String caseCode);

    // Consulta B: buscar todos los casos según status, ordenados por rescueDate ASC
    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    // Consulta C: buscar casos pertenecientes a un centro determinado, por rescueCenter.code
    List<RescueCase> findByRescueCenterCode(String code);

    // Paso 37: casos posteriores a determinada fecha, del más reciente al más antiguo
    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate date);

    // Reto sin guía: existe un caso con ese caseCode
    boolean existsByCaseCode(String caseCode);
}