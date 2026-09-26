package edu.example;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CourseWorkspace {
    private final InfraiGateway api;
    public CourseWorkspace(InfraiGateway api) { this.api = api; }

    public record Enrollment(String domain, String email, String proofName, String proofValue,
                             String courseId, LocalDate learnerDeadline, String educatorReportEmail) {}
    public record WorkspaceJoin(String workspace, String member, String courseId,
                                LocalDate learnerDeadline, String educatorReportEmail, String state) {}

    public static void requireCompanyEmail(String domain, String email) {
        if (domain == null || email == null || !domain.matches("[A-Za-z0-9.-]+")
            || !email.toLowerCase().endsWith("@" + domain.toLowerCase())
            || email.indexOf('@') != email.lastIndexOf('@'))
            throw new IllegalArgumentException("Use an email address at the company domain");
    }

    public WorkspaceJoin join(Enrollment input) {
        requireCompanyEmail(input.domain(), input.email());
        if (input.proofName() == null || input.proofName().isBlank() || input.proofValue() == null
            || input.proofValue().isBlank() || input.courseId() == null || input.courseId().isBlank()
            || input.learnerDeadline() == null || input.educatorReportEmail() == null
            || input.educatorReportEmail().isBlank())
            throw new IllegalArgumentException("Proof, course, deadline and report recipient are required");

        JsonNode zone = api.call("POST", "/v1/dns/domain/add", Map.of("domain", input.domain()));
        String zoneId = zone.path("zone_id").asText();
        if (zoneId.isBlank()) throw new IllegalStateException("Domain response needs zone_id");
        // An upsert makes retrying the ownership record a repeatable write.
        api.call("PUT", "/v1/dns/record/upsert", Map.of("zone_id", zoneId,
            "record_type", "TXT", "name", input.proofName(), "content", input.proofValue()));
        api.call("POST", "/v1/dns/domain/verify", Map.of("domain", input.domain()));

        String enrollmentId = UUID.nameUUIDFromBytes((input.domain().toLowerCase() + ":"
            + input.email().toLowerCase()).getBytes(StandardCharsets.UTF_8)).toString();
        api.call("POST", "/v1/auth/user/create", Map.of("email", input.email(),
            "idempotency_key", enrollmentId, "metadata", Map.of("workspace", input.domain(),
                "courseId", input.courseId(), "learnerDeadline", input.learnerDeadline().toString(),
                "educatorReportEmail", input.educatorReportEmail())));
        return new WorkspaceJoin(input.domain(), input.email(), input.courseId(),
            input.learnerDeadline(), input.educatorReportEmail(), "joined");
    }
}
