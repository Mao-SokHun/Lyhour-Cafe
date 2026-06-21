package com.example.demo.Service;

import com.example.demo.Models.*;
import com.example.demo.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeProfileRepository profileRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public EmployeeService(
            EmployeeProfileRepository profileRepository,
            AttendanceRepository attendanceRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.profileRepository = profileRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<EmployeeProfile> findAllProfiles() {
        return profileRepository.findAllByOrderByUser_UsernameAsc();
    }

    @Transactional
    public EmployeeProfile saveProfile(EmployeeProfile profile) {
        return profileRepository.save(profile);
    }

    public List<User> findStaffUsers() {
        return userRepository.findAll().stream()
                .filter(u -> !"CUSTOMER".equals(u.getRole()))
                .toList();
    }

    @Transactional
    public Attendance clockIn(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        attendanceRepository.findFirstByUserIdAndClockOutIsNullOrderByClockInDesc(userId)
                .ifPresent(a -> { throw new IllegalStateException("Already clocked in"); });
        Attendance a = new Attendance();
        a.setUser(user);
        a.setClockIn(LocalDateTime.now());
        auditLogService.log("CLOCK_IN", user.getUsername());
        return attendanceRepository.save(a);
    }

    @Transactional
    public Attendance clockOut(Long userId) {
        Attendance a = attendanceRepository.findFirstByUserIdAndClockOutIsNullOrderByClockInDesc(userId)
                .orElseThrow(() -> new IllegalStateException("Not clocked in"));
        a.setClockOut(LocalDateTime.now());
        auditLogService.log("CLOCK_OUT", a.getUser().getUsername());
        return attendanceRepository.save(a);
    }

    public List<Attendance> recentAttendance() {
        return attendanceRepository.findTop50ByOrderByClockInDesc();
    }
}
