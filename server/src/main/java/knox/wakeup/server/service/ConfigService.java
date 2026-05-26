package knox.wakeup.server.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    @Value("${wakeup.config-path:config/alarm-defaults.txt}")
    private String configPath;

    private int minOperand = 10;
    private int maxOperand = 999;
    private int operationCount = 2;

    @PostConstruct
    public void load() {
        try (InputStream in = openConfig()) {
            if (in == null) {
                log.warn("Config file not found at '{}'. Using defaults.", configPath);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int eq = trimmed.indexOf('=');
                    if (eq <= 0) continue;
                    String key = trimmed.substring(0, eq).trim();
                    String value = trimmed.substring(eq + 1).trim();
                    applyKey(key, value);
                }
            }
            log.info("Loaded config: min={}, max={}, ops={}", minOperand, maxOperand, operationCount);
        } catch (Exception e) {
            log.warn("Failed to read config '{}'. Using defaults. Reason: {}", configPath, e.getMessage());
        }
    }

    private InputStream openConfig() throws Exception {
        Path absolute = Paths.get(configPath);
        if (Files.exists(absolute)) {
            return Files.newInputStream(absolute);
        }
        ClassPathResource classpath = new ClassPathResource(configPath);
        if (classpath.exists()) {
            return classpath.getInputStream();
        }
        return null;
    }

    private void applyKey(String key, String value) {
        try {
            switch (key) {
                case "min-operand" -> minOperand = Integer.parseInt(value);
                case "max-operand" -> maxOperand = Integer.parseInt(value);
                case "operation-count" -> operationCount = Integer.parseInt(value);
                default -> log.debug("Unknown config key '{}'", key);
            }
        } catch (NumberFormatException nfe) {
            log.warn("Invalid number for '{}'='{}', keeping default.", key, value);
        }
    }

    public int getMinOperand() { return minOperand; }
    public int getMaxOperand() { return maxOperand; }
    public int getOperationCount() { return operationCount; }
}
