package com.dentalclinic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.password=unused-test-password",
    "spring.flyway.enabled=false",
    "CLINIC_DENTIST_PASSWORD=fictional-dentist-test",
    "CLINIC_RECEPTIONIST_PASSWORD=fictional-receptionist-test"
})
@AutoConfigureMockMvc
class LoginTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcTemplate jdbcTemplate;
    @MockitoBean DashboardService dashboard;
    @MockitoBean PatientRepository patients;
    @MockitoBean AppointmentRepository appointments;
    @MockitoBean AppointmentService appointmentService;

    @org.junit.jupiter.api.BeforeEach
    void dashboardTotals() {
        org.mockito.Mockito.when(dashboard.totals(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new DashboardService.Totals(0, 0, java.math.BigDecimal.ZERO));
        org.mockito.Mockito.when(dashboard.appointments(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean()))
            .thenReturn(java.util.List.of());
        org.mockito.Mockito.when(dashboard.payments(org.mockito.ArgumentMatchers.any()))
            .thenReturn(java.util.List.of());
    }

    @Test
    void visitorsMustSignInAndLoginFormHasCsrfToken() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/index.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/css/clinic.css")).andExpect(status().isOk());
    }

    @Test
    void bothStaffRolesCanSignInAndSignOut() throws Exception {
        for (String user : new String[]{"dentist", "receptionist"}) {
            MockHttpSession session = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                    .param("username", user).param("password", "fictional-" + user + "-test"))
                .andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession(false);
            assertNotNull(session);
            mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Signed in as")))
                .andExpect(content().string(containsString(user)));
            mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
            mvc.perform(get("/")).andExpect(status().is3xxRedirection());
        }
    }

    @Test
    void dashboardLinksOpenProtectedEmptyDetailViews() throws Exception {
        mvc.perform(get("/").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("dentist").roles("DENTIST")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/appointments?date=")))
            .andExpect(content().string(containsString("arrivals=true")))
            .andExpect(content().string(containsString("/today/collections")));
        for (String path : new String[]{"appointments", "arrivals", "collections"}) {
            mvc.perform(get("/today/" + path)).andExpect(status().is3xxRedirection());
            mvc.perform(get("/today/" + path).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No records for today.")));
        }
    }

    @Test
    void patientPagesAreProtectedAndRegistrationValidatesAndRedirects() throws Exception {
        mvc.perform(get("/patients")).andExpect(status().is3xxRedirection());
        var staff = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("receptionist").roles("RECEPTIONIST");
        org.mockito.Mockito.when(patients.search("Synthetic")).thenReturn(java.util.List.of(new Patient(7, "Synthetic Patient", "9876543210", null, null)));
        mvc.perform(get("/patients").param("q", "Synthetic").with(staff)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Synthetic Patient")))
            .andExpect(content().string(containsString("/patients/new")))
            .andExpect(content().string(containsString("/patients/7")));
        mvc.perform(get("/patients/new").with(staff)).andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(post("/patients").with(staff).with(csrf()).param("fullName", " ").param("phone", "-------"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Please correct")));
        org.mockito.Mockito.verify(patients, org.mockito.Mockito.never()).create(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.when(patients.create(org.mockito.ArgumentMatchers.any())).thenReturn(7L);
        mvc.perform(post("/patients").with(staff).with(csrf()).param("fullName", "Synthetic Patient").param("phone", "9876543210"))
            .andExpect(redirectedUrl("/patients/7"));
        mvc.perform(post("/patients").with(staff).param("fullName", "Synthetic Patient").param("phone", "9876543210"))
            .andExpect(status().isForbidden());
        org.mockito.Mockito.when(patients.find(7)).thenReturn(java.util.Optional.of(new Patient(7, "Synthetic Patient", "9876543210", null, null)));
        mvc.perform(get("/patients/7").with(staff)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Synthetic Patient")));
        mvc.perform(get("/patients/9999").with(staff)).andExpect(status().isNotFound());
    }

    @Test
    void appointmentFormsAndDetailsWorkForStaff() throws Exception {
        var staff = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("receptionist").roles("RECEPTIONIST");
        var patient = new Patient(7, "Synthetic Patient", "9876543210", null, null);
        var start = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).plusDays(1).withSecond(0).withNano(0);
        var appointment = new Appointment(4, 7, "Synthetic Patient", start, 30, "Checkup", "SCHEDULED", null);
        org.mockito.Mockito.when(patients.search("")).thenReturn(java.util.List.of(patient));
        org.mockito.Mockito.when(appointments.find(4)).thenReturn(java.util.Optional.of(appointment));
        org.mockito.Mockito.when(appointments.onDate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(java.util.List.of(appointment));
        mvc.perform(get("/appointments")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/appointments").with(staff)).andExpect(status().isOk()).andExpect(content().string(containsString("Synthetic Patient")));
        mvc.perform(get("/appointments/new").with(staff)).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/appointments/4/edit").with(staff)).andExpect(status().isOk());
        mvc.perform(get("/appointments/4").with(staff)).andExpect(status().isOk()).andExpect(content().string(containsString("Mark arrived")));
        org.mockito.Mockito.when(appointmentService.book(org.mockito.ArgumentMatchers.any())).thenReturn(4L);
        mvc.perform(post("/appointments").with(staff).with(csrf()).param("patientId", "7").param("startsAt", start.toString()).param("durationMinutes", "30"))
            .andExpect(redirectedUrl("/appointments/4"));
        mvc.perform(post("/appointments").with(staff).with(csrf()).param("durationMinutes", "0"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Please correct")));
        mvc.perform(post("/appointments/4/status").with(staff).param("status", "ARRIVED")).andExpect(status().isForbidden());
        mvc.perform(post("/appointments/4/status").with(staff).with(csrf()).param("status", "ARRIVED")).andExpect(redirectedUrl("/appointments/4"));
        mvc.perform(get("/appointments/9999").with(staff)).andExpect(status().isNotFound());
    }

    @Test
    void patientSearchIsProtectedAndSlotConflictAppearsOnForm() throws Exception {
        var staff = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("receptionist").roles("RECEPTIONIST");
        mvc.perform(get("/patients/options").param("q", "Synthetic")).andExpect(status().is3xxRedirection());
        org.mockito.Mockito.when(patients.search("Synthetic")).thenReturn(java.util.List.of(new Patient(150, "Synthetic Patient", "9876543210", java.time.LocalDate.of(2000, 1, 1), "Private address")));
        mvc.perform(get("/patients/options").param("q", "Synthetic").with(staff))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(150))
            .andExpect(jsonPath("$[0].fullName").value("Synthetic Patient"))
            .andExpect(jsonPath("$[0].address").doesNotExist());
        mvc.perform(get("/appointments/new").with(staff)).andExpect(status().isOk())
            .andExpect(content().string(containsString("list=\"patientOptions\"")))
            .andExpect(content().string(containsString("/js/patient-picker.js")));
        org.mockito.Mockito.when(appointmentService.book(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new AppointmentService.SlotUnavailable("An appointment already exists during this date and time."));
        var start = java.time.LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        mvc.perform(post("/appointments").with(staff).with(csrf()).param("patientId", "150")
            .param("startsAt", start.toString()).param("durationMinutes", "30"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("appointmentForm", "startsAt"))
            .andExpect(content().string(containsString("An appointment already exists")));
    }

    @Test
    void incorrectCredentialsShowAnErrorAndCsrfIsRequired() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "dentist")
            .param("password", "wrong-password")).andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login?error")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Incorrect username or password")));
        mvc.perform(post("/login").param("username", "dentist")
            .param("password", "fictional-dentist-test")).andExpect(status().isForbidden());
    }
}

