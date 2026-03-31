package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.domain.model.SystemSetting;
import com.pharmaCx.dms.domain.repository.SystemSettingRepository;
import com.pharmaCx.dms.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Manages global system settings (scope=GLOBAL).
 * Only SYSTEM_ADMIN can write; authenticated users can read.
 */
@RestController
@RequestMapping("/api/v1/system-settings")
public class SystemSettingController {

    private final SystemSettingRepository settingRepo;
    private final CurrentUserService currentUserService;

    public SystemSettingController(SystemSettingRepository settingRepo,
                                   CurrentUserService currentUserService) {
        this.settingRepo = settingRepo;
        this.currentUserService = currentUserService;
    }

    /** Get global system settings. Creates defaults if none exist. */
    @GetMapping
    public ResponseEntity<SystemSetting> getGlobal() {
        SystemSetting setting = settingRepo.findByScopeAndScopeIdIsNull("GLOBAL")
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setScope("GLOBAL");
                    s.setSettings(new SystemSetting.SettingValues());
                    return settingRepo.save(s);
                });
        return ResponseEntity.ok(setting);
    }

    /** Update global system settings. */
    @PutMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<SystemSetting> updateGlobal(@RequestBody SystemSetting.SettingValues values) {
        SystemSetting setting = settingRepo.findByScopeAndScopeIdIsNull("GLOBAL")
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setScope("GLOBAL");
                    return s;
                });
        setting.setSettings(values);
        setting.setUpdatedBy(currentUserService.getCurrentUserId());
        setting.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(settingRepo.save(setting));
    }
}
