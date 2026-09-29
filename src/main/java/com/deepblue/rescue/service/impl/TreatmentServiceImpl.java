package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;
    private final SpecialistRepository specialistRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(AnimalRepository animalRepository,
                                SpecialistRepository specialistRepository,
                                TreatmentRepository treatmentRepository,
                                TreatmentMapper mapper) {
        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }

    @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
        return treatmentRepository
                .findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TreatmentResponse register(CreateTreatmentRequest request) {

        // 1. Animal (Regla 1)
        Animal animal = animalRepository
                .findByAnimalCode(request.animalCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found: " + request.animalCode()));

        // 2. Especialista (Regla 2)
        Specialist specialist = specialistRepository
                .findByProfessionalCode(request.specialistCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialist not found: " + request.specialistCode()));

        // 3. Especialista activo (Regla 3)
        if (!specialist.isActive()) {
            throw new BusinessRuleException(
                    "Specialist " + request.specialistCode() + " is not active.");
        }

        // 4. Caso del animal (Animal es el dueño de la relación con RescueCase)
        RescueCase rescueCase = animal.getRescueCase();

        // 5. Estado del caso (Regla 4)
        RescueStatus status = rescueCase.getStatus();
        if (status == RescueStatus.RELEASED || status == RescueStatus.CLOSED) {
            throw new BusinessRuleException(
                    "Cannot register treatment because the case is " + status + ".");
        }

        // 6. Fecha (Regla 5)
        if (request.performedAt() == null
                || request.performedAt().toLocalDate().isBefore(rescueCase.getRescueDate())) {
            throw new BusinessRuleException(
                    "Treatment date cannot be before the rescue date ("
                            + rescueCase.getRescueDate() + ").");
        }

        // 7. Crear
        Treatment treatment = new Treatment(
                animal,
                specialist,
                request.performedAt(),
                request.type(),
                request.description());

        // 8. Guardar
        Treatment saved = treatmentRepository.save(treatment);

        // 9. DTO
        return mapper.toResponse(saved);
    }
}
