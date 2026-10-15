package com.tejas.distributedmatchingengine.repositories;

import com.tejas.distributedmatchingengine.models.User;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.SortedMap;
import java.util.TreeMap;

@Repository
public class UserRepository {
    private final SortedMap<String, User> users;

    // Hard-coded users for milestone 1
    public UserRepository() {
        users = new TreeMap<>();
        User newUser1 = new User(BigDecimal.valueOf(1000.00));
        users.put(newUser1.getUserID(), newUser1);
        User newUser2 = new User(BigDecimal.valueOf(1000.00));
        users.put(newUser2.getUserID(), newUser2);
    }

    public String addUser(BigDecimal balance) {
        User newUser1 = new User(BigDecimal.valueOf(1000.00));
        users.put(newUser1.getUserID(), newUser1);
        return newUser1.getUserID();
    }
}
