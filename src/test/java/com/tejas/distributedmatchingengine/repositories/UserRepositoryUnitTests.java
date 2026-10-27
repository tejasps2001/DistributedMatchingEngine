package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.UserWallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserRepositoryUnitTests {
    private SortedMap<String, UserWallet> users;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        users = new TreeMap<>();
        userRepository = new UserRepository(users);
    }

    @Test
    @DisplayName("Test whether a new user is added.")
    public void userIsAdded() {
        String userID = userRepository.addUser(BigDecimal.valueOf(100.00));
        Optional<UserWallet> wallet = userRepository.findUserByID(userID);
        assertTrue(wallet.isPresent());
    }

    @Test
    @DisplayName("Test that user query returns the correct user's wallet if" +
            " present.")
    public void findCorrectUserWallet() {
        users.put("user1243", new UserWallet(BigDecimal.valueOf(100.00)));
        Optional<UserWallet> wallet = userRepository.findUserByID(
                "user1243");
        assertTrue(wallet.isPresent());
    }

    @Test
    @DisplayName("Test that user query returns empty Optional if userID not" +
            " present.")
    public void returnEmptyOptionalWalletIfUserAbsent() {
        Optional<UserWallet> wallet = userRepository.findUserByID(
                "user1243");
        assertTrue(wallet.isEmpty());
    }
}
