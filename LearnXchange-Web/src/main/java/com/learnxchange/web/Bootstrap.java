package com.learnxchange.web;

import com.learnxchange.service.AuthService;
import com.learnxchange.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/** On startup: create tables/seed skills if missing, and create the first admin from env vars. */
@Component
public class Bootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initSchema();
        initAdmin();
    }

    private void initSchema() throws Exception {
        String sql = new String(new ClassPathResource("schema.sql").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.trim().startsWith("--")) cleaned.append(line).append('\n');
        }
        try (Connection c = DBConnection.get(); Statement st = c.createStatement()) {
            for (String stmt : cleaned.toString().split(";")) {
                String s = stmt.trim();
                if (s.isEmpty()) continue;
                String upper = s.toUpperCase();
                if (upper.startsWith("CREATE DATABASE") || upper.startsWith("USE ")) continue;
                st.execute(s);
            }
        }
        log.info("Database schema is ready.");
    }

    private void initAdmin() {
        String email = System.getenv("ADMIN_EMAIL");
        String password = System.getenv("ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD not set - no admin account will be created.");
            return;
        }
        boolean created = new AuthService().ensureAdmin(System.getenv("ADMIN_NAME"), email, password);
        if (created) log.info("Admin account created for {}", email);
    }
}
