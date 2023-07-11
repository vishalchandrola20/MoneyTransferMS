package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transaction;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  void transferAmount_InsufficientFunds() {

    String id1 = "Id5" + System.currentTimeMillis();
    Account account = new Account(id1,new BigDecimal(100));
    this.accountsService.createAccount(account);

    String id2 = "Id6" + System.currentTimeMillis();
    Account account2 = new Account(id2,new BigDecimal(100));
    this.accountsService.createAccount(account2);

    Transaction transaction = new Transaction(id1,id2,new BigDecimal(200));
    try {
      this.accountsService.transferAmount(transaction);
      fail("Should have failed when transferring amount more than balance");
    } catch (InsufficientFundsException ex) {
      assertThat(ex.getMessage()).isEqualTo("Insufficient funds:: transfer amount " + 200 + " is greater than available balance" + 100);
    }


  }
  @Test
  void transferAmount_simulteneousTransferAccount() {

    String id1 = "Id11" + System.currentTimeMillis();
    Account account = new Account(id1,new BigDecimal(100));
    this.accountsService.createAccount(account);

    String id2 = "Id12" + System.currentTimeMillis();
    Account account2 = new Account(id2,new BigDecimal(100));
    this.accountsService.createAccount(account2);

    String id3 = "Id13" + System.currentTimeMillis();
    Account account3 = new Account(id3,new BigDecimal(100));
    this.accountsService.createAccount(account3);

    Transaction transaction = new Transaction(id1,id2,new BigDecimal(20));
    Transaction transaction2 = new Transaction(id2,id3,new BigDecimal(10));

    Runnable runnable = () -> {
      for (int i= 0;i<5;i++) {
        this.accountsService.transferAmount(transaction);
      }
    };

    Runnable runnable2 = () -> {
      for (int i= 0;i<5;i++) {
        this.accountsService.transferAmount(transaction2);
      }
    };

    new Thread(runnable).start();
    new Thread(runnable2).start();

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
}

