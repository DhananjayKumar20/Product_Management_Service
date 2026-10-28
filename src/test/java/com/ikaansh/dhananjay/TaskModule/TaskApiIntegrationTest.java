package com.ikaansh.dhananjay.TaskModule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full stack test: security filter chain + controllers + services + JPA on in-memory H2 (MySQL mode). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String email(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String signupAndGetToken(String email, String userType) throws Exception {
        String body = "{\"name\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"Password@123\",\"userType\":\"" + userType + "\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private String taskJson(String title, String status) {
        return "{\"title\":\"" + title + "\",\"description\":\"d\",\"status\":\"" + status
                + "\",\"dueDate\":\"" + LocalDate.now().plusDays(10) + "\"}";
    }

    private long createTask(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(authed(post("/api/v1/tasks"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson(title, "TODO")))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    // ---------------- auth ----------------

    @Test
    void signup_login_flow_andErrors() throws Exception {
        String email = email("auth");
        signupAndGetToken(email, "USER");

        // duplicate email -> 409
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"" + email + "\",\"password\":\"Password@123\",\"userType\":\"USER\"}"))
                .andExpect(status().isConflict());

        // login ok
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // wrong password -> 401
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        // invalid body -> 400 with field errors
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"1\",\"userType\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());

        // unknown role -> 400
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"" + email("role") + "\",\"password\":\"Password@123\",\"userType\":\"BOSS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withoutOrWithBadToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------- CRUD + double click ----------------

    @Test
    void task_crud_flow() throws Exception {
        String token = signupAndGetToken(email("crud"), "USER");
        long id = createTask(token, "First task");

        mockMvc.perform(authed(get("/api/v1/tasks/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("First task"))
                .andExpect(jsonPath("$.status").value("TODO"));

        mockMvc.perform(authed(put("/api/v1/tasks/" + id), token)
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson("Renamed", "DONE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.status").value("DONE"));

        // second click of the same update is harmless
        mockMvc.perform(authed(put("/api/v1/tasks/" + id), token)
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson("Renamed", "DONE")))
                .andExpect(status().isOk());

        mockMvc.perform(authed(delete("/api/v1/tasks/" + id), token)).andExpect(status().isOk());
        mockMvc.perform(authed(get("/api/v1/tasks/" + id), token)).andExpect(status().isNotFound());
        mockMvc.perform(authed(delete("/api/v1/tasks/" + id), token)).andExpect(status().isNotFound());
    }

    @Test
    void task_doubleClickCreate_secondRequestRejectedWith409() throws Exception {
        String token = signupAndGetToken(email("dbl"), "USER");

        mockMvc.perform(authed(post("/api/v1/tasks"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson("Same title", "TODO")))
                .andExpect(status().isCreated());
        mockMvc.perform(authed(post("/api/v1/tasks"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson("Same title", "TODO")))
                .andExpect(status().isConflict());
    }

    @Test
    void task_validationErrors_return400() throws Exception {
        String token = signupAndGetToken(email("val"), "USER");

        mockMvc.perform(authed(post("/api/v1/tasks"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"dueDate\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());

        mockMvc.perform(authed(post("/api/v1/tasks"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"status\":\"NOPE\",\"dueDate\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(authed(post("/api/v1/tasks"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"dueDate\":\"" + LocalDate.now().minusDays(1) + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(authed(get("/api/v1/tasks/abc"), token)).andExpect(status().isBadRequest());
    }

    // ---------------- RBAC ----------------

    @Test
    void rbac_userCannotTouchOthersTasks_adminCan() throws Exception {
        String userA = signupAndGetToken(email("a"), "USER");
        String userB = signupAndGetToken(email("b"), "USER");
        String admin = signupAndGetToken(email("admin"), "ADMIN");
        long taskId = createTask(userA, "A's task");

        mockMvc.perform(authed(get("/api/v1/tasks/" + taskId), userB)).andExpect(status().isForbidden());
        mockMvc.perform(authed(put("/api/v1/tasks/" + taskId), userB)
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson("Hacked", "DONE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(authed(delete("/api/v1/tasks/" + taskId), userB)).andExpect(status().isForbidden());

        mockMvc.perform(authed(get("/api/v1/tasks/" + taskId), admin)).andExpect(status().isOk());
        mockMvc.perform(authed(put("/api/v1/tasks/" + taskId), admin)
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson("By admin", "IN_PROGRESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("By admin"));
    }

    @Test
    void rbac_adminListEndpoint_isAdminOnly() throws Exception {
        String user = signupAndGetToken(email("u"), "USER");
        String admin = signupAndGetToken(email("adm"), "ADMIN");
        createTask(user, "Visible to admin");

        mockMvc.perform(authed(get("/api/v1/admin/tasks"), user)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(authed(get("/api/v1/admin/tasks"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ---------------- pagination + sorting ----------------

    @Test
    void list_isPaginatedSortedAndScopedToOwner() throws Exception {
        String user = signupAndGetToken(email("page"), "USER");
        String other = signupAndGetToken(email("page-other"), "USER");
        createTask(user, "Charlie");
        createTask(user, "Alpha");
        createTask(user, "Bravo");
        createTask(other, "Not mine");

        MvcResult result = mockMvc.perform(authed(get("/api/v1/tasks?page=0&size=2&sortBy=title&direction=asc"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        assertEquals("Alpha", content.get(0).get("title").asText());
        assertEquals("Bravo", content.get(1).get("title").asText());

        mockMvc.perform(authed(get("/api/v1/tasks?status=DONE"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(authed(get("/api/v1/tasks?sortBy=password"), user)).andExpect(status().isBadRequest());
        mockMvc.perform(authed(get("/api/v1/tasks?size=0"), user)).andExpect(status().isBadRequest());
        mockMvc.perform(authed(get("/api/v1/tasks?status=WRONG"), user)).andExpect(status().isBadRequest());
    }
}
