package com.example.bank.controller;

import com.example.bank.common.ApiResponse;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.UserException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/admin/logs")
public class LogController {

    private final String logFilePath;

    public LogController(@Value("${LOG_FILE_PATH:D:/Github/Bank/logs}") String logFilePath) {
        this.logFilePath = logFilePath;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getLogs(
            @RequestParam(defaultValue = "user-service") String serviceName,
            @RequestParam(defaultValue = "100") int lines,
            HttpServletRequest req) {

        Path filePath = Paths.get(logFilePath, serviceName + ".log");

        if (!Files.exists(filePath)) {
            throw new UserException(ErrorCode.VALIDATION_FAILED, "Log file not found: " + serviceName + ".log");
        }

        try (Stream<String> stream = Files.lines(filePath)) {
            List<String> tailLines = stream.collect(Collectors.toList());
            if (tailLines.size() > lines) {
                tailLines = tailLines.subList(tailLines.size() - lines, tailLines.size());
            }
            return ResponseEntity.ok(ApiResponse.success(tailLines, req.getHeader("X-Request-Id")));
        } catch (IOException e) {
            throw new UserException(ErrorCode.INTERNAL_ERROR, "Failed to read log file: " + e.getMessage());
        }
    }
}
