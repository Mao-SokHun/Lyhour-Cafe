package com.example.demo.Repositories;

import com.example.demo.Models.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findBySettingKey(String settingKey);
}
