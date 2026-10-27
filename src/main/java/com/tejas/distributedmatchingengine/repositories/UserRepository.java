package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.UserWallet;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

@Repository
public class UserRepository {
    private final SortedMap<String, UserWallet> users;

    public UserRepository() {
        users = new TreeMap<>();
    }

    // Package-private constructor for testing purposes
    UserRepository(SortedMap<String, UserWallet> users) {
        this.users = users;
    }

    public String addUser(BigDecimal balance) {
        UserWallet newUserWallet1 = new UserWallet(balance);
        users.put(newUserWallet1.getUserID(), newUserWallet1);
        return newUserWallet1.getUserID();
    }

    // Will be replaced by Spring Data's built-in methods later
    // TODO: Ensure "It's also possible that the map
    //  explicitly maps the key to null." doesn't happen or,
    //  don't return null for no user.
    //  This changes with the introduction of Optional??
    public Optional<UserWallet> findUserByID(String userID) {
        UserWallet wallet = users.get(userID);
        return Optional.ofNullable(wallet);
    }
}
