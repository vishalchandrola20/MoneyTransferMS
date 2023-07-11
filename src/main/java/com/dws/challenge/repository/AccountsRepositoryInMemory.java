package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundsException;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
        if (previousAccount != null) {
            throw new DuplicateAccountIdException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
    }

    @Override
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public void clearAccounts() {
        accounts.clear();
    }

    @Override
    public synchronized void depositAmount (Account toAccount, BigDecimal amount) {
        toAccount.setBalance(toAccount.getBalance().add(amount));

    }

    @Override
    public synchronized void withdrawAmount (Account fromAccount, BigDecimal amount) {
        if (fromAccount.getBalance().compareTo(amount) == -1) {
            throw new InsufficientFundsException(
                "Insufficient funds:: transfer amount " + amount + " is greater than available balance" + fromAccount.getBalance());
        }
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
    }

}
