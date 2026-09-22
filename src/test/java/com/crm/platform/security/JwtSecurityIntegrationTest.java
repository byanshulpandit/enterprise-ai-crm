package com.crm.platform.security;

import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.security.dto.LoginRequest;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private User testAdmin;
    private User testMarketer;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        if (jdbcTemplate != null) {
            jdbcTemplate.update("DELETE FROM campaigns");
            jdbcTemplate.update("DELETE FROM segments");
        }
        customerRepository.deleteAll();
        userRepository.deleteAll();

        testAdmin = new User("admin_e2e", "admin.e2e@crm.internal", passwordEncoder.encode("AdminPass123!"), RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        testAdmin = userRepository.save(testAdmin);

        testMarketer = new User("marketer_e2e", "marketer.e2e@crm.internal", passwordEncoder.encode("MarketerPass123!"), RoleEnum.ROLE_MARKETER, Boolean.TRUE);
        testMarketer = userRepository.save(testMarketer);

        Customer customer = new Customer();
        customer.setFirstName("E2E");
        customer.setLastName("Customer");
        customer.setEmail("e2e.customer@example.com");
        customer.setCity("Chicago");
        customer.setCountry("USA");
        customer.setTotalSpend(new BigDecimal("250.00"));
        customer.setVisitCount(1);
        customer.addTag("VIP");
        testCustomer = customerRepository.save(customer);
    }

    // =========================================================================
    // AUDIT 1: TRUE END-TO-END JWT FLOW
    // =========================================================================

    @Test
    @DisplayName("Audit 1.1: Valid real JWT authenticates end-to-end through the entire filter chain to controller")
    void testValidRealJwtSucceeds() throws Exception {
        String token = jwtTokenProvider.generateToken(testMarketer);

        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("e2e.customer@example.com")));
    }

    @Test
    @DisplayName("Audit 1.2: Missing Authorization header on protected endpoint returns 401 via AuthenticationEntryPoint")
    void testMissingTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.message", is("Authentication required. A valid Bearer token must be provided.")));
    }

    @Test
    @DisplayName("Audit 1.3: Malformed JWT on protected endpoint returns 401 via AuthenticationEntryPoint")
    void testMalformedJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer totally.malformed.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Audit 1.4: Expired JWT on protected endpoint returns 401 via AuthenticationEntryPoint")
    void testExpiredJwtReturns401() throws Exception {
        Instant past = Instant.now().minusSeconds(7200);
        Instant pastExpiry = Instant.now().minusSeconds(3600);
        String expiredToken = jwtTokenProvider.generateToken(testMarketer, past, pastExpiry);

        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Audit 1.5: Wrong-signature JWT on protected endpoint returns 401 via AuthenticationEntryPoint")
    void testWrongSignatureJwtReturns401() throws Exception {
        SecretKey rogueKey = Keys.hmacShaKeyFor("different-super-secret-key-at-least-32-bytes-long!".getBytes(StandardCharsets.UTF_8));
        String forgedToken = Jwts.builder()
                .subject(testMarketer.getUsername())
                .issuer(jwtProperties.getIssuer())
                .claim("uid", testMarketer.getId())
                .claim("role", "ROLE_ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(rogueKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Audit 1.6: Wrong-issuer JWT on protected endpoint returns 401 via AuthenticationEntryPoint")
    void testWrongIssuerJwtReturns401() throws Exception {
        SecretKey validKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        String wrongIssuerToken = Jwts.builder()
                .subject(testMarketer.getUsername())
                .issuer("untrusted-foreign-issuer-2026")
                .claim("uid", testMarketer.getId())
                .claim("role", "ROLE_MARKETER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(validKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongIssuerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Audit 1.7: Valid JWT for user that does not exist in DB returns 401")
    void testNonExistentDbUserReturns401() throws Exception {
        User ghost = new User("ghost_user", "ghost@crm.internal", "hash", RoleEnum.ROLE_MARKETER);
        ghost.setId(9999L);
        String ghostToken = jwtTokenProvider.generateToken(ghost);

        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ghostToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    // =========================================================================
    // AUDIT 2: LIVE DB ROLE AUTHORITY (ROLE PROMOTION & DEMOTION)
    // =========================================================================

    @Test
    @DisplayName("Audit 2.1: DB role promotion: existing JWT immediately gains elevated permissions on next request")
    void testLiveDbRolePromotionTakesEffectImmediately() throws Exception {
        // 1. Issue JWT while user is ROLE_MARKETER
        String jwtToken = jwtTokenProvider.generateToken(testMarketer);

        // 2. MARKETER can read customers
        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // 3. MARKETER cannot delete customer (403 Forbidden via AccessDeniedHandler)
        mockMvc.perform(delete("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));

        // 4. Update the user's role in MySQL to ROLE_ADMIN
        testMarketer.setRole(RoleEnum.ROLE_ADMIN);
        userRepository.save(testMarketer);

        // 5. Reuse the SAME existing JWT token — without re-login!
        // The token still contains "role": "ROLE_MARKETER" in its claims, but DB role wins!
        mockMvc.perform(delete("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent()); // 204 Deleted!
    }

    @Test
    @DisplayName("Audit 2.2: DB role demotion: existing JWT immediately loses admin permissions on next request")
    void testLiveDbRoleDemotionTakesEffectImmediately() throws Exception {
        // 1. Issue JWT while user is ROLE_ADMIN
        String adminToken = jwtTokenProvider.generateToken(testAdmin);

        // 2. ADMIN can access user management
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 3. Demote user in DB to ROLE_MARKETER
        testAdmin.setRole(RoleEnum.ROLE_MARKETER);
        userRepository.save(testAdmin);

        // 4. Reuse the SAME existing JWT token
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    // =========================================================================
    // AUDIT 3: ACTIVE / INACTIVE USER
    // =========================================================================

    @Test
    @DisplayName("Audit 3: Deactivating user in DB immediately blocks next request with 401")
    void testDeactivatedUserImmediatelyBlocked() throws Exception {
        // 1. Issue JWT while user is active
        String jwtToken = jwtTokenProvider.generateToken(testMarketer);

        // 2. Active user succeeds
        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // 3. Deactivate user in DB
        testMarketer.setIsActive(false);
        userRepository.save(testMarketer);

        // 4. Next request with the SAME valid token returns 401 Unauthorized
        mockMvc.perform(get("/api/v1/customers/" + testCustomer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    // =========================================================================
    // AUDIT 9: PUBLIC LOGIN WITH GARBAGE AUTHORIZATION HEADER
    // =========================================================================

    @Test
    @DisplayName("Audit 9: Invalid Authorization header does not break public login endpoint")
    void testInvalidAuthHeaderDoesNotBreakLogin() throws Exception {
        LoginRequest request = new LoginRequest("admin_e2e", "AdminPass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer corrupted.garbage.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.username", is("admin_e2e")));
    }
}
