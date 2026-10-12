package com.enterprise.payment.user;

import com.enterprise.payment.common.constant.HeaderConstants;
import com.enterprise.payment.user.dto.*;
import com.enterprise.payment.user.entity.Account;
import com.enterprise.payment.user.entity.enums.AccountStatus;
import com.enterprise.payment.user.entity.enums.AccountType;
import com.enterprise.payment.user.entity.enums.AddressType;
import com.enterprise.payment.user.entity.enums.KycStatus;
import com.enterprise.payment.user.repository.AccountRepository;
import com.enterprise.payment.user.repository.UserAddressRepository;
import com.enterprise.payment.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @BeforeEach
    void setUp() {
        userAddressRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create user profile with automatically provisioned WALLET account")
    void shouldCreateUserProfileWithDefaultWalletAccount() throws Exception {
        UUID authUserId = UUID.randomUUID();
        CreateUserRequest request = CreateUserRequest.builder()
                .authUserId(authUserId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@enterprise.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.CORRELATION_ID, "corr-create-user-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.correlationId").value("corr-create-user-1"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john.doe@enterprise.com"))
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.accounts[0].accountType").value("WALLET"))
                .andExpect(jsonPath("$.data.accounts[0].balance").value(0))
                .andExpect(jsonPath("$.data.accounts[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accounts[0].accountNumber").isNotEmpty())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String accountNumber = root.path("data").path("accounts").get(0).path("accountNumber").asText();
        assertThat(accountNumber).startsWith("10");
        assertThat(accountNumber).hasSize(12);
    }

    @Test
    @DisplayName("Should reject user profile creation with duplicate email (409 Conflict)")
    void shouldRejectDuplicateEmail() throws Exception {
        CreateUserRequest req1 = CreateUserRequest.builder()
                .authUserId(UUID.randomUUID())
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@enterprise.com")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        CreateUserRequest req2 = CreateUserRequest.builder()
                .authUserId(UUID.randomUUID())
                .firstName("Alice2")
                .lastName("Smith2")
                .email("alice@enterprise.com")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("Should reject user profile creation with duplicate authUserId (409 Conflict)")
    void shouldRejectDuplicateAuthUserId() throws Exception {
        UUID sharedAuthId = UUID.randomUUID();

        CreateUserRequest req1 = CreateUserRequest.builder()
                .authUserId(sharedAuthId)
                .firstName("Bob")
                .lastName("Taylor")
                .email("bob1@enterprise.com")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        CreateUserRequest req2 = CreateUserRequest.builder()
                .authUserId(sharedAuthId)
                .firstName("Bob2")
                .lastName("Taylor2")
                .email("bob2@enterprise.com")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("Should retrieve user by ID and by authUserId")
    void shouldGetUserByIdAndByAuthUserId() throws Exception {
        UUID authUserId = UUID.randomUUID();
        CreateUserRequest request = CreateUserRequest.builder()
                .authUserId(authUserId)
                .firstName("Carol")
                .lastName("White")
                .email("carol@enterprise.com")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = root.path("data").path("id").asText();

        // 1. Get by ID
        mockMvc.perform(get("/api/v1/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("carol@enterprise.com"));

        // 2. Get by authUserId
        mockMvc.perform(get("/api/v1/users/by-auth/" + authUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.email").value("carol@enterprise.com"));
    }

    @Test
    @DisplayName("Should update user profile")
    void shouldUpdateUserProfile() throws Exception {
        CreateUserRequest createReq = CreateUserRequest.builder()
                .authUserId(UUID.randomUUID())
                .firstName("David")
                .lastName("OldName")
                .email("david@enterprise.com")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        UpdateUserRequest updateReq = UpdateUserRequest.builder()
                .firstName("David")
                .lastName("NewName")
                .phoneNumber("+9876543210")
                .dateOfBirth(LocalDate.of(1985, 5, 20))
                .build();

        mockMvc.perform(put("/api/v1/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastName").value("NewName"))
                .andExpect(jsonPath("$.data.phoneNumber").value("+9876543210"));
    }

    @Test
    @DisplayName("Should soft delete user and suspend active accounts")
    void shouldSoftDeleteUserAndSuspendAccounts() throws Exception {
        CreateUserRequest createReq = CreateUserRequest.builder()
                .authUserId(UUID.randomUUID())
                .firstName("Eve")
                .lastName("Adams")
                .email("eve@enterprise.com")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = root.path("data").path("id").asText();
        String accountId = root.path("data").path("accounts").get(0).path("id").asText();

        // Delete user
        mockMvc.perform(delete("/api/v1/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // User lookup should now return 404
        mockMvc.perform(get("/api/v1/users/" + userId))
                .andExpect(status().isNotFound());

        // Account should now be SUSPENDED
        mockMvc.perform(get("/api/v1/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("Should enforce KYC state machine transitions")
    void shouldEnforceKycStateMachine() throws Exception {
        CreateUserRequest createReq = CreateUserRequest.builder()
                .authUserId(UUID.randomUUID())
                .firstName("Frank")
                .lastName("Miller")
                .email("frank@enterprise.com")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 1. PENDING -> VERIFIED (allowed)
        UpdateKycRequest verifyReq = UpdateKycRequest.builder()
                .kycStatus(KycStatus.VERIFIED)
                .remarks("Passport approved")
                .build();

        mockMvc.perform(patch("/api/v1/users/" + userId + "/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("VERIFIED"));

        // 2. VERIFIED -> PENDING (disallowed -> 400 Bad Request)
        UpdateKycRequest revertReq = UpdateKycRequest.builder()
                .kycStatus(KycStatus.PENDING)
                .build();

        mockMvc.perform(patch("/api/v1/users/" + userId + "/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revertReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_KYC_TRANSITION"));
    }

    @Test
    @DisplayName("Should create additional SAVINGS account for user")
    void shouldCreateAdditionalSavingsAccount() throws Exception {
        CreateUserRequest createReq = CreateUserRequest.builder()
                .authUserId(UUID.randomUUID())
                .firstName("Grace")
                .lastName("Hopper")
                .email("grace@enterprise.com")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        CreateAccountRequest savingsReq = CreateAccountRequest.builder()
                .accountType(AccountType.SAVINGS)
                .currency("EUR")
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savingsReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.data.currency").value("EUR"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accountNumber").isNotEmpty());

        // Get all accounts for user -> 2 accounts
        mockMvc.perform(get("/api/v1/users/" + userId + "/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("Should manage account status transitions and validate closure rules")
    void shouldManageAccountStatusTransitions() throws Exception {
        CreateUserRequest createReq = CreateUserRequest.builder()
                .authUserId(UUID.randomUUID())
                .firstName("Hank")
                .lastName("Pym")
                .email("hank@enterprise.com")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("accounts").get(0).path("id").asText();

        // 1. ACTIVE -> SUSPENDED
        mockMvc.perform(patch("/api/v1/accounts/" + accountId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAccountStatusRequest(AccountStatus.SUSPENDED, "Suspicious activity"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // 2. SUSPENDED -> ACTIVE
        mockMvc.perform(patch("/api/v1/accounts/" + accountId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAccountStatusRequest(AccountStatus.ACTIVE, "Reactivated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 3. Set balance > 0 in DB and attempt to CLOSE -> 400 Bad Request
        Account account = accountRepository.findById(UUID.fromString(accountId)).orElseThrow();
        account.setBalance(new BigDecimal("150.0000"));
        accountRepository.save(account);

        mockMvc.perform(patch("/api/v1/accounts/" + accountId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAccountStatusRequest(AccountStatus.CLOSED, "Closing"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NON_ZERO_BALANCE"));

        // 4. Set balance back to 0 and CLOSE -> 200 OK
        Account accountToZero = accountRepository.findById(UUID.fromString(accountId)).orElseThrow();
        accountToZero.setBalance(BigDecimal.ZERO);
        accountRepository.save(accountToZero);

        mockMvc.perform(patch("/api/v1/accounts/" + accountId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAccountStatusRequest(AccountStatus.CLOSED, "Zero balance close"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // 5. Attempt to reopen CLOSED account -> 400 Bad Request
        mockMvc.perform(patch("/api/v1/accounts/" + accountId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAccountStatusRequest(AccountStatus.ACTIVE, "Reopening"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_ALREADY_CLOSED"));
    }

    @Test
    @DisplayName("Should add address and handle primary address reassignment")
    void shouldAddAddressAndManagePrimaryFlag() throws Exception {
        CreateUserRequest createReq = CreateUserRequest.builder()
                .authUserId(UUID.randomUUID())
                .firstName("Ivy")
                .lastName("Poison")
                .email("ivy@enterprise.com")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 1. Add first address with primary = true
        CreateAddressRequest addr1 = CreateAddressRequest.builder()
                .addressType(AddressType.HOME)
                .street("100 Main St")
                .city("New York")
                .state("NY")
                .country("US")
                .postalCode("10001")
                .primary(true)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addr1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primary").value(true));

        // 2. Add second address with primary = true
        CreateAddressRequest addr2 = CreateAddressRequest.builder()
                .addressType(AddressType.BILLING)
                .street("200 Wall St")
                .city("New York")
                .state("NY")
                .country("US")
                .postalCode("10005")
                .primary(true)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addr2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primary").value(true));

        // 3. Verify addresses list: second is primary, first is no longer primary
        MvcResult listResult = mockMvc.perform(get("/api/v1/users/" + userId + "/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();

        JsonNode listNode = objectMapper.readTree(listResult.getResponse().getContentAsString()).path("data");
        for (JsonNode addr : listNode) {
            if ("100 Main St".equals(addr.path("street").asText())) {
                assertThat(addr.path("primary").asBoolean()).isFalse();
            } else if ("200 Wall St".equals(addr.path("street").asText())) {
                assertThat(addr.path("primary").asBoolean()).isTrue();
            }
        }
    }
}
