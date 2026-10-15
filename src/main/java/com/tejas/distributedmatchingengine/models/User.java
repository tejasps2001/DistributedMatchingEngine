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

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe executed while another order may be
    // matched simultaneously. In that case, this method may be called
    // simultaneously leading to race conditions. Therefore, better to have
    // this synchronized.
    public synchronized void creditAmount(BigDecimal credit) {
        balance = balance.add(credit);
    }

    // Return true for success, false for negative balance
    public synchronized boolean debitAmount(BigDecimal debit) {
        // Negative balance; Do not update balance
        if(balance.compareTo(debit) < 0) {
            return false;
        }
        else {
            balance = balance.subtract(debit);
            return true;
        }
    }
}
