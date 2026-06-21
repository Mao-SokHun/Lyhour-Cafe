package com.example.demo.Service;

import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final ReportExportService reportExportService;
    private final OrderRepository orderRepository;

    @Value("${app.backup.enabled:true}")
    private boolean enabled;

    @Value("${app.backup.directory:./backups}")
    private String backupDirectory;

    public BackupService(ReportExportService reportExportService, OrderRepository orderRepository) {
        this.reportExportService = reportExportService;
        this.orderRepository = orderRepository;
    }

    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}")
    public void scheduledBackup() {
        if (!enabled) return;
        try {
            runBackup();
        } catch (IOException e) {
            log.error("Scheduled backup failed", e);
        }
    }

    public Path runBackup() throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path dir = Path.of(backupDirectory);
        Files.createDirectories(dir);
        Path sales = dir.resolve("sales-" + stamp + ".csv");
        Path summary = dir.resolve("summary-" + stamp + ".csv");
        Files.write(sales, reportExportService.exportSalesCsv());
        Files.write(summary, reportExportService.exportSummaryCsv());
        log.info("Backup saved to {} ({} orders)", dir.toAbsolutePath(), orderRepository.count());
        return dir;
    }
}
