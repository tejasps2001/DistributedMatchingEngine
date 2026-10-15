package com.tejas.distributedmatchingengine.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@ToString
public class User {
    private String userID;

    @Setter(AccessLevel.NONE)
    private BigDecimal balance;

    public User(BigDecimal balance) {
        this.userID = UUID.randomUUID().toString();
        this.balance = balance;
    }
}
