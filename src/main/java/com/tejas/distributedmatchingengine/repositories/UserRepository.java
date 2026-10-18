package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.UserWallet;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
        UserWallet newUserWallet1 = new UserWallet(BigDecimal.valueOf(1000.00));
        users.put(newUserWallet1.getUserID(), newUserWallet1);
        return newUserWallet1.getUserID();
    }
}
