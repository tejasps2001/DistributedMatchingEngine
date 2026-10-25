package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.exceptions.UserNotFoundException;
import com.tejas.distributedmatchingengine.models.UserWallet;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

@Repository
public class UserRepository {
    private final SortedMap<String, UserWallet> users;

    // Few hard-coded users for milestone 1 so that I don't need
    // to create them while trialing
    public UserRepository() {
        users = new TreeMap<>();
        UserWallet newUserWallet1 = new UserWallet(BigDecimal.valueOf(1000.00));
        users.put(newUserWallet1.getUserID(), newUserWallet1);
        UserWallet newUserWallet2 = new UserWallet(BigDecimal.valueOf(1000.00));
        users.put(newUserWallet2.getUserID(), newUserWallet2);
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
    public Optional<UserWallet> findUserByID(String userID) {
        UserWallet wallet = users.get(userID);
        return Optional.ofNullable(wallet);
    }
}
