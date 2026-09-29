package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;
    @Mock
    private SpecialistRepository specialistRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    private CreateTreatmentRequest request(LocalDateTime date, TreatmentType type) {
        return new CreateTreatmentRequest("AN-001", "SPEC-001", date, type,
                "Cleaning of left front flipper injury.");
    }

    private Animal animalWith(RescueCase rescueCase) {
        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);
        return animal;
    }

    private Specialist activeSpecialist(boolean active) {
        Specialist specialist = mock(Specialist.class);
        when(specialist.isActive()).thenReturn(active);
        return specialist;
    }

    // TEST 5
    @Test
    void shouldRegisterTreatment() {
        Specialist specialist = activeSpecialist(true);
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(RescueStatus.IN_REHABILITATION);
        when(rescueCase.getRescueDate()).thenReturn(LocalDate.of(2026, 8, 20));
        Animal animal = animalWith(rescueCase);

        TreatmentResponse response = new TreatmentResponse(1L, "AN-001", "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE, "desc");

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        TreatmentResponse result = service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE));

        assertThat(result).isEqualTo(response);
        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSpecialistDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(mock(Animal.class)));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 6
    @Test
    void shouldRejectInactiveSpecialist() {
        Animal animal = mock(Animal.class);
        Specialist specialist = activeSpecialist(false);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE)))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 7
    @Test
    void shouldRejectWhenCaseIsReleased() {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(RescueStatus.RELEASED);

        Animal animal = animalWith(rescueCase);
        Specialist specialist = activeSpecialist(true);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.OBSERVATION)))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentBeforeRescueDate() {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(RescueStatus.IN_REHABILITATION);
        when(rescueCase.getRescueDate()).thenReturn(LocalDate.of(2026, 8, 20));

        Animal animal = animalWith(rescueCase);
        Specialist specialist = activeSpecialist(true);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(
                request(LocalDateTime.of(2026, 8, 15, 9, 0), TreatmentType.WOUND_CARE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("before the rescue date");

        verify(treatmentRepository, never()).save(any());
    }
}
