package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RescueCenterRepository extends JpaRepository<RescueCenter, Long> {

    // Query Method: buscar un centro mediante su código
    Optional<RescueCenter> findByCode(String code);
}
