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

    // Return null if no user with userID present
    // Will be replaced by Spring Data's built-in methods later
    // TODO: Ensure "It's also possible that the map
    //  explicitly maps the key to null." doesn't happen or,
    //  don't return null for no user.
    //  NOTE: getOrDefault() method is used under the assumption that
    //  once created, the userID doesn't change and is not deleted
    //  since this method doesn't give synchronization guarantees.
    //  Most probably not needed after milestone 1.
    public UserWallet findUserByID(String userID) {
        return users.getOrDefault(userID, null);
    }
}
