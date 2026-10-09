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
    "CLINIC_DENTIST_PASSWORD=fictional-dentist-test",
    "CLINIC_RECEPTIONIST_PASSWORD=fictional-receptionist-test"
})
@AutoConfigureMockMvc
class LoginTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcTemplate jdbcTemplate;

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
    void incorrectCredentialsShowAnErrorAndCsrfIsRequired() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "dentist")
            .param("password", "wrong-password")).andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login?error")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Incorrect username or password")));
        mvc.perform(post("/login").param("username", "dentist")
            .param("password", "fictional-dentist-test")).andExpect(status().isForbidden());
    }
}
