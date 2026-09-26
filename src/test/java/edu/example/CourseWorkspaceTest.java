package edu.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class CourseWorkspaceTest {
    @Test void employeeMustBelongToVerifiedCompanyDomainBeforeAnyApiCall() {
        assertDoesNotThrow(() -> CourseWorkspace.requireCompanyEmail("academy.example", "teacher@academy.example"));
        assertThrows(IllegalArgumentException.class,
            () -> CourseWorkspace.requireCompanyEmail("academy.example", "teacher@other.example"));
        assertThrows(IllegalArgumentException.class,
            () -> CourseWorkspace.requireCompanyEmail("academy.example", "teacher@academy.example.attacker.test"));
    }
}
