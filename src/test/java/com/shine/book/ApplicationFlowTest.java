package com.shine.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ApplicationFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private static final String PASSWORD = "example-fixture-password";
    private static final String ACCOUNT = "/api/account";
    private static final String CONTACTS = "/api/contacts";

    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Object data) throws Exception {
        return request.header("X-Contact-Book","1").contentType("application/json").content(json.writeValueAsBytes(data));
    }
    private MockHttpSession register(String identifier) throws Exception {
        var result = mvc.perform(body(post(ACCOUNT + "/register"), Map.of("name","Test user","identifier",identifier,"password",PASSWORD)))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
    private Map<String,Object> form(String first) {
        return Map.of("firstName",first,"lastName","Iqbal","title","Engineer",
                "emails",List.of(Map.of("label","Work","value","person@example.test")),
                "phones",List.of(Map.of("label","Home","value","+92 300 1234567")));
    }

    @Test void managesContactsAndProtectsOtherAccounts() throws Exception {
        var owner = register("owner@example.test");
        var other = register("+92 300 9999999");
        var created = mvc.perform(body(post(CONTACTS).session(owner),form("Amina"))).andExpect(status().isCreated()).andReturn();
        String url = CONTACTS + "/" + json.readTree(created.getResponse().getContentAsByteArray()).get("id").asLong();
        mvc.perform(get(url).session(owner)).andExpect(jsonPath("$.phones[0].label").value("Home"));
        mvc.perform(get(CONTACTS).session(owner).param("q","iQb")).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(CONTACTS).session(other)).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get(url).session(other)).andExpect(status().isNotFound());
        mvc.perform(body(put(url).session(other),form("Changed"))).andExpect(status().isNotFound());
        mvc.perform(delete(url).session(other).header("X-Contact-Book","1")).andExpect(status().isNotFound());
        mvc.perform(body(put(url).session(owner),form("Nadia"))).andExpect(jsonPath("$.firstName").value("Nadia"));
        mvc.perform(body(post(CONTACTS).session(owner),form(" "))).andExpect(status().isBadRequest());
        mvc.perform(delete(url).session(owner).header("X-Contact-Book","1")).andExpect(status().isNoContent());
        mvc.perform(get(url).session(owner)).andExpect(status().isNotFound());
    }

    @Test void changesCredentialsAndInvalidatesOlderSessions() throws Exception {
        var first = register("AUTH@example.test");
        var credentials = Map.of("identifier","auth@example.test","password",PASSWORD);
        var login = mvc.perform(body(post(ACCOUNT+"/login"),credentials)).andExpect(status().isOk()).andReturn();
        var second = (MockHttpSession) login.getRequest().getSession(false);
        var change = mvc.perform(body(post(ACCOUNT+"/password").session(second),
                Map.of("currentPassword",PASSWORD,"newPassword","replacement-fixture"))).andExpect(status().isNoContent()).andReturn();
        var updated = (MockHttpSession) change.getRequest().getSession(false);
        assertNotEquals(second.getId(), updated.getId());
        mvc.perform(get(ACCOUNT).session(first)).andExpect(status().isUnauthorized());
        mvc.perform(body(post(ACCOUNT+"/login"),credentials)).andExpect(status().isUnauthorized());
        mvc.perform(body(post(ACCOUNT+"/login"), Map.of("identifier","auth@example.test","password","replacement-fixture")))
                .andExpect(status().isOk());
        mvc.perform(post(ACCOUNT+"/logout").session(updated).header("X-Contact-Book","1")).andExpect(status().isNoContent());
        assertTrue(updated.isInvalid());
    }

    @Test void validatesRegistrationAndBlocksCrossSiteWrites() throws Exception {
        mvc.perform(get(CONTACTS)).andExpect(status().isUnauthorized());
        mvc.perform(post(ACCOUNT+"/register")).andExpect(status().isForbidden());
        register("0300-1234567");
        var duplicate = Map.of("name","Second","identifier","03001234567","password",PASSWORD);
        mvc.perform(body(post(ACCOUNT+"/register"),duplicate)).andExpect(status().isConflict());
        mvc.perform(body(post(ACCOUNT+"/login"),Map.of("identifier","0300 1234567","password",PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(body(post(ACCOUNT+"/register"),Map.of("name","Second","identifier","abc","password",PASSWORD)))
                .andExpect(status().isBadRequest());
    }
}
