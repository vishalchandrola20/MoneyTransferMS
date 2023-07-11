package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transaction;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private NotificationService notificationService;


  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  void transferAmount() throws Exception {

    Mockito.doNothing().when(notificationService).notifyAboutTransfer(Mockito.any(),Mockito.any());

    String toAccountId = "Id1" + System.currentTimeMillis();
    String fromAccountId = "Id2" + System.currentTimeMillis();


    Account toAccount = new Account(toAccountId, new BigDecimal("20.45"));
    Account fromAccount = new Account(fromAccountId, new BigDecimal("40.45"));

    this.accountsService.createAccount(toAccount);
    this.accountsService.createAccount(fromAccount);

    Transaction transaction = new Transaction(fromAccountId,toAccountId,new BigDecimal("15"));

    this.mockMvc.perform(post("/v1/accounts/transfer",transaction).contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(transaction)))
        .andExpect(status().isOk());

    assertThat(toAccount.getBalance()).isEqualByComparingTo("35.45");
    assertThat(fromAccount.getBalance()).isEqualByComparingTo("25.45");
    Mockito.verify(notificationService,Mockito.times(2)).notifyAboutTransfer(Mockito.any(),Mockito.any());

  }

  @Test
  void transferAmountInsufficientFunds() throws Exception {
    String toAccountId = "Id1" + System.currentTimeMillis();
    String fromAccountId = "Id2" + System.currentTimeMillis();


    Account toAccount = new Account(toAccountId, new BigDecimal("40.45"));
    Account fromAccount = new Account(fromAccountId, new BigDecimal("20.45"));

    this.accountsService.createAccount(toAccount);
    this.accountsService.createAccount(fromAccount);

    Transaction transaction = new Transaction(fromAccountId,toAccountId,new BigDecimal("25"));

    this.mockMvc.perform(post("/v1/accounts/transfer",transaction).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(transaction)))
        .andExpect(status().isBadRequest());

  }

  @Test
  void transferNegativeAmount() throws Exception {
    String toAccountId = "Id20" + System.currentTimeMillis();
    String fromAccountId = "Id21" + System.currentTimeMillis();


    Account toAccount = new Account(toAccountId, new BigDecimal("40.45"));
    Account fromAccount = new Account(fromAccountId, new BigDecimal("20.45"));

    this.accountsService.createAccount(toAccount);
    this.accountsService.createAccount(fromAccount);

    Transaction transaction = new Transaction(fromAccountId,toAccountId,new BigDecimal("-25"));

    this.mockMvc.perform(post("/v1/accounts/transfer",transaction).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(transaction)))
        .andExpect(status().isBadRequest());

  }


}
