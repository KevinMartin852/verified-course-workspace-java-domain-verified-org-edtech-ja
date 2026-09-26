package edu.example;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class CourseJoinServer {
    private final CourseWorkspace workspace;
    public CourseJoinServer(CourseWorkspace workspace) { this.workspace = workspace; }
    public static void main(String[] args) { SpringApplication.run(CourseJoinServer.class, args); }

    @PostMapping("/workspaces/join")
    public CourseWorkspace.WorkspaceJoin join(@RequestBody CourseWorkspace.Enrollment enrollment) {
        return workspace.join(enrollment);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(InfraiGateway.ApiError.class)
    public ResponseEntity<Map<String, String>> rejected(InfraiGateway.ApiError e) {
        HttpStatus status = e.status >= 400 && e.status < 500
            ? HttpStatus.valueOf(e.status) : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", e.code, "detail", e.getMessage()));
    }
}
