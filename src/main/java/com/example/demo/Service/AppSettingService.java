package com.example.demo.Service;

import com.example.demo.Models.AppSetting;
import com.example.demo.Repositories.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppSettingService {

    private final AppSettingRepository appSettingRepository;

    public AppSettingService(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    public List<AppSetting> findAll() { return appSettingRepository.findAll(); }

    public String get(String key, String defaultValue) {
        return appSettingRepository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .orElse(defaultValue);
    }

    @Transactional
    public void save(String key, String value) {
        AppSetting setting = appSettingRepository.findBySettingKey(key).orElse(new AppSetting());
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        appSettingRepository.save(setting);
    }
}
