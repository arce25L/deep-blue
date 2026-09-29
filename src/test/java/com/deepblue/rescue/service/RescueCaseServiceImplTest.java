package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    private RescueCaseResponse response(RescueStatus status) {
        return new RescueCaseResponse(1L, "RES-001", LocalDate.of(2026, 8, 20),
                "Santa Marta", status, "CTR-01", "AN-001");
    }

    // TEST 1
    @Test
    void shouldFindRescueCaseByCode() {
        RescueCase rescueCase = org.mockito.Mockito.mock(RescueCase.class);
        RescueCaseResponse response = response(RescueStatus.ADMITTED);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(response);
        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    // TEST 2
    @Test
    void shouldThrowWhenRescueCaseDoesNotExist() {
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RES-999");

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindByStatus() {
        RescueCase rescueCase = org.mockito.Mockito.mock(RescueCase.class);
        RescueCaseResponse response = response(RescueStatus.IN_REHABILITATION);

        when(repository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        assertThat(service.findByStatus(RescueStatus.IN_REHABILITATION))
                .containsExactly(response);
    }

    // TEST 3
    @Test
    void shouldChangeStatusWhenTransitionIsValid() {
        RescueCase rescueCase = org.mockito.Mockito.mock(RescueCase.class);
        RescueCaseResponse response = response(RescueStatus.UNDER_EVALUATION);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(rescueCase.getStatus()).thenReturn(RescueStatus.ADMITTED);
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.changeStatus("RES-001",
                new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION));

        assertThat(result).isEqualTo(response);
        verify(rescueCase).setStatus(RescueStatus.UNDER_EVALUATION);
        verify(repository).save(rescueCase);
    }

    // TEST 4
    @Test
    void shouldRejectInvalidTransitionAndNeverSave() {
        RescueCase rescueCase = org.mockito.Mockito.mock(RescueCase.class);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(rescueCase.getStatus()).thenReturn(RescueStatus.ADMITTED);

        assertThatThrownBy(() -> service.changeStatus("RES-001",
                new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE)))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectTransitionOutOfReleased() {
        RescueCase rescueCase = org.mockito.Mockito.mock(RescueCase.class);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(rescueCase.getStatus()).thenReturn(RescueStatus.RELEASED);

        assertThatThrownBy(() -> service.changeStatus("RES-001",
                new ChangeRescueStatusRequest(RescueStatus.IN_REHABILITATION)))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenChangingStatusOfMissingCase() {
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus("RES-999",
                new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }
}
