package com.example.bank.repository;

import com.example.bank.entity.Account;
import com.example.bank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUser(User user);
}
