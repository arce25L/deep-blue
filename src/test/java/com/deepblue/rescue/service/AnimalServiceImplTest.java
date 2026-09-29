package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @ParameterizedTest
    @EnumSource(RescueStatus.class)
    void canReceiveTreatmentOnlyWhenEvaluatedOrInRehabilitation(RescueStatus status) {
        RescueCase rescueCase = mock(RescueCase.class);
        when(rescueCase.getStatus()).thenReturn(status);
        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));

        boolean expected = status == RescueStatus.UNDER_EVALUATION
                || status == RescueStatus.IN_REHABILITATION;

        assertThat(service.canReceiveTreatment("AN-001")).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        when(repository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.canReceiveTreatment("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindByCode() {
        Animal animal = mock(Animal.class);
        AnimalResponse response = new AnimalResponse(1L, "AN-001", "Green Sea Turtle",
                "Chelonia mydas", "FEMALE", "RES-001", RescueStatus.IN_REHABILITATION);
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(mapper.toResponse(animal)).thenReturn(response);

        assertThat(service.findByCode("AN-001")).isEqualTo(response);
    }

    @Test
    void shouldFindAnimalsInRehabilitation() {
        Animal animal = mock(Animal.class);
        AnimalResponse response = new AnimalResponse(1L, "AN-001", "Green Sea Turtle",
                "Chelonia mydas", "FEMALE", "RES-001", RescueStatus.IN_REHABILITATION);
        when(repository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(animal));
        when(mapper.toResponse(animal)).thenReturn(response);

        assertThat(service.findAnimalsInRehabilitation()).containsExactly(response);
    }
}
