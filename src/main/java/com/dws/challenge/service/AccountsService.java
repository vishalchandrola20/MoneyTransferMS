package com.dws.challenge.service;

import com.dws.challenge.constants.Constants;
import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transaction;
import com.dws.challenge.exception.ServerBusyException;
import com.dws.challenge.repository.AccountsRepository;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository) {
    this.accountsRepository = accountsRepository;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transferAmount (Transaction transaction) {

    Account accountTo = this.accountsRepository.getAccount(transaction.getAccountToId());
    Account accountFrom = this.accountsRepository.getAccount(transaction.getAccountFromId());

    transactWithThreadSafety(accountTo,accountFrom,transaction.getAmount());

    this.notificationService.notifyAboutTransfer(getAccount(transaction.getAccountFromId()), String.format(Constants.DEBIT_NOTIFICATION,transaction.getAmount()));
    this.notificationService.notifyAboutTransfer(getAccount(transaction.getAccountToId()), String.format(Constants.CREDIT_NOTIFICATION,transaction.getAmount()));
  }

  private void transactWithThreadSafety (Account accountTo, Account accountFrom, BigDecimal amount) {
    try {
      if (accountFrom.getLock().tryLock(3000, TimeUnit.MILLISECONDS)) {
        try {
            if (accountTo.getLock().tryLock(3000, TimeUnit.MILLISECONDS)) {
                this.accountsRepository.withdrawAmount(accountFrom,amount);
                this.accountsRepository.depositAmount(accountTo,amount);
                log.info("Successfully Transferred::"+Thread.currentThread().getName());
                log.info("Account Id :: {} , updated balance:: {}", accountFrom.getAccountId(),accountFrom.getBalance());
                log.info("Account Id :: {} , updated balance:: {}", accountTo.getAccountId(),accountTo.getBalance());
            } else {
              throw new ServerBusyException(Constants.SERVER_BUSY);
          }
        } finally {
          accountTo.getLock().unlock();
        }
      } else {
        throw new ServerBusyException(Constants.SERVER_BUSY);
      }
    } catch (InterruptedException e) {
      log.error("Exception occurred",e);
      throw new ServerBusyException(Constants.SERVER_BUSY,e);
    } finally {
      accountFrom.getLock().unlock();
    }


  }
}
