package com.mod.hrss.service;

import com.mod.hrss.dto.request.LeaveApplyRequest;
import com.mod.hrss.dto.response.LeaveRequestDto;
import com.mod.hrss.entity.LeaveRequest;
import com.mod.hrss.entity.LeaveStatus;
import com.mod.hrss.entity.LeaveType;
import com.mod.hrss.entity.User;
import com.mod.hrss.exception.BusinessException;
import com.mod.hrss.mapper.LeaveRequestMapper;
import com.mod.hrss.repository.LeaveRequestRepository;
import com.mod.hrss.repository.UserRepository;
import com.mod.hrss.service.impl.LeaveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    private User employee;
    private LeaveApplyRequest applyRequest;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(1L);
        employee.setEmail("test@company.com");
        employee.setFirstName("Test");
        employee.setLastName("Employee");

        applyRequest = new LeaveApplyRequest();
        applyRequest.setLeaveType("CASUAL");
        applyRequest.setStartDate(LocalDate.now().plusDays(2));
        applyRequest.setEndDate(LocalDate.now().plusDays(5));
        applyRequest.setReason("Rest");
    }

    @Test
    void applyLeave_Success() {
        when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        
        LeaveRequest mockSavedRequest = new LeaveRequest();
        mockSavedRequest.setId(10L);
        mockSavedRequest.setEmployee(employee);
        mockSavedRequest.setLeaveType(LeaveType.CASUAL);
        mockSavedRequest.setStartDate(applyRequest.getStartDate());
        mockSavedRequest.setEndDate(applyRequest.getEndDate());
        mockSavedRequest.setApprovalStatus(LeaveStatus.PENDING);

        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(mockSavedRequest);
        
        LeaveRequestDto expectedDto = new LeaveRequestDto();
        expectedDto.setId(10L);
        expectedDto.setEmployeeId(1L);
        expectedDto.setEmployeeName("Test Employee");
        expectedDto.setLeaveType("CASUAL");
        expectedDto.setStartDate(applyRequest.getStartDate());
        expectedDto.setEndDate(applyRequest.getEndDate());
        expectedDto.setApprovalStatus("PENDING");

        when(leaveRequestMapper.toDto(mockSavedRequest)).thenReturn(expectedDto);

        LeaveRequestDto result = leaveService.applyLeave(applyRequest, employee.getEmail());

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("PENDING", result.getApprovalStatus());
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
    }

    @Test
    void applyLeave_InvalidDates_ThrowsBusinessException() {
        applyRequest.setStartDate(LocalDate.now().plusDays(5));
        applyRequest.setEndDate(LocalDate.now().plusDays(2));

        when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));

        assertThrows(BusinessException.class, () -> {
            leaveService.applyLeave(applyRequest, employee.getEmail());
        });

        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }
}
