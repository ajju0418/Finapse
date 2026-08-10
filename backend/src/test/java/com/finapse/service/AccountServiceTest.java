package com.finapse.service;

import com.finapse.dto.AccountCreateRequest;
import com.finapse.dto.AccountResponse;
import com.finapse.entity.Account;
import com.finapse.entity.User;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock UserService userService;
    @InjectMocks AccountService accountService;

    private User defaultUser;

    @BeforeEach
    void setUp() {
        defaultUser = new User();
        defaultUser.setId(UUID.randomUUID());
        defaultUser.setName("Local User");
    }

    @Test
    void create_persistsAccountWithCorrectFields() {
        when(userService.getDefaultUser()).thenReturn(defaultUser);
        AccountCreateRequest request = new AccountCreateRequest(
                "HDFC Savings", "HDFC Bank", "1234", "INR");

        Account saved = new Account();
        saved.setId(UUID.randomUUID());
        saved.setUser(defaultUser);
        saved.setName(request.name());
        saved.setInstitutionName(request.institutionName());
        saved.setLastFourDigits(request.lastFourDigits());
        saved.setCurrency("INR");

        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        AccountResponse response = accountService.create(request);

        assertThat(response.name()).isEqualTo("HDFC Savings");
        assertThat(response.institutionName()).isEqualTo("HDFC Bank");
        assertThat(response.lastFourDigits()).isEqualTo("1234");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void getAll_returnsAccountsForDefaultUser() {
        when(userService.getDefaultUser()).thenReturn(defaultUser);
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setUser(defaultUser);
        a.setName("SBI Savings");
        a.setCurrency("INR");

        when(accountRepository.findByUserIdOrderByCreatedAtDesc(defaultUser.getId()))
                .thenReturn(List.of(a));

        List<AccountResponse> result = accountService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("SBI Savings");
    }

    @Test
    void getById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivate_setsIsActiveFalse() {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUser(defaultUser);
        account.setName("Old Account");
        account.setCurrency("INR");
        account.setActive(true);

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        AccountResponse response = accountService.deactivate(account.getId());

        assertThat(response.isActive()).isFalse();
    }
}
