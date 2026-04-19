package com.bingeboxed.profiles;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileServiceTest {

    @Autowired
    private MockMvc mockMvc;

    private String getToken(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"email\":\"%s\",\"password\":\"password123\"}", email)));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"password\":\"password123\"}", email)))
                .andReturn().getResponse().getContentAsString();
        return response.split("\"token\":\"")[1]
                .replace("\"}", "").trim();
    }

    // FR-01: GET /api/profiles/me — authenticated
    @Test
    void getOwnProfile_authenticated_returns200() throws Exception {
        String token = getToken("profile1@test.com");
        mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").exists());
    }

    // FR-01: GET /api/profiles/me — no auth
    @Test
    void getOwnProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isUnauthorized());
    }

    // FR-02: GET /api/profiles/{id} — not found
    @Test
    void getProfile_notFound_returns404() throws Exception {
        String token = getToken("profile2@test.com");
        mockMvc.perform(get("/api/profiles/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // FR-02: GET /api/profiles/{id} — found
    @Test
    void getProfile_exists_returns200() throws Exception {
        String token = getToken("profile3@test.com");
        mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // FR-03: PUT /api/profiles/me — update own profile
    @Test
    void updateProfile_ownProfile_returns200() throws Exception {
        String token = getToken("profile4@test.com");
        mockMvc.perform(put("/api/profiles/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"displayName":"Test User","bio":"Hello world"}
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test User"));
    }

    // FR-03: PUT /api/profiles/me — update persists
    @Test
    void updateProfile_changesPersist() throws Exception {
        String token = getToken("profile5@test.com");
        mockMvc.perform(put("/api/profiles/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {"displayName":"Persistent Name","bio":"Persistent bio"}
            """));
        mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Persistent Name"));
    }

    // FR-04: GET /api/profiles/public/{id} — no auth required
    @Test
    void getPublicProfile_noAuth_returns200() throws Exception {
        getToken("public1@test.com");
        mockMvc.perform(get("/api/profiles/public/1"))
                .andExpect(status().isOk());
    }

    // FR-04: GET /api/profiles/public/{id} — not found
    @Test
    void getPublicProfile_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/profiles/public/99999"))
                .andExpect(status().isNotFound());
    }

    // FR-05: GET /profile — authenticated
    @Test
    void profilePage_authenticated_returns200() throws Exception {
        String token = getToken("page1@test.com");
        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(view().name("profiles/view"));
    }

    // FR-06: GET /profile/edit — authenticated
    @Test
    void editProfilePage_authenticated_returns200() throws Exception {
        String token = getToken("page2@test.com");
        mockMvc.perform(get("/profile/edit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(view().name("profiles/edit"));
    }
}