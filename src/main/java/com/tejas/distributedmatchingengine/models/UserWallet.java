package com.tejas.distributedmatchingengine.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/*
 * Contains the user's balance and shares bought.
 */
@Getter
@Setter(AccessLevel.NONE)
@ToString
public class UserWallet {

    @Setter(AccessLevel.PUBLIC)
    private String userID;

    // Balance available for new BUYs
    private BigDecimal availableBalance;
    // Total value of the account. Balance available for new BUYs +
    // balance tied up to BUY orders
    private BigDecimal totalBalance;

    // Shares available for new SELLs
    private int availableShares;
    // Total number of shares in the account.
    // Shares available for new SELLs + shares tied up to SELL orders
    private int totalShares;

    public UserWallet(BigDecimal balance) {
        this.userID = UUID.randomUUID().toString();
        this.totalBalance = this.availableBalance = balance;
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe executed while another order may be
    // matched simultaneously. In that case, this method may be called
    // simultaneously leading to race conditions. Therefore, better to have
    // this synchronized.
    public synchronized void addToAvailableBalance(BigDecimal credit) {
        availableBalance = availableBalance.add(credit);
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe executed while another order may be
    // matched simultaneously. In that case, this method may be called
    // simultaneously leading to race conditions. Therefore, better to have
    // this synchronized.
    public synchronized void addToTotalBalance(BigDecimal credit) {
        totalBalance = totalBalance.add(credit);
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe executed while another order may be
    // matched simultaneously. In that case, this method may be called
    // simultaneously leading to race conditions. Therefore, better to have
    // this synchronized.
    // Return true for success, false for negative balance
    public synchronized boolean tryDeductFromAvailableBalance(BigDecimal debit) {
        // Negative balance; Do not update balance
        if(availableBalance.compareTo(debit) < 0) {
            return false;
        }
        else {
            availableBalance = availableBalance.subtract(debit);
            return true;
        }
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe executed while another order may be
    // matched simultaneously. In that case, this method may be called
    // simultaneously leading to race conditions. Therefore, better to have
    // this synchronized.
    // Return true for success, false for negative balance
    public synchronized boolean tryDeductFromTotalBalance(BigDecimal debit) {
        // Negative balance; Do not update balance
        if(totalBalance.compareTo(debit) < 0) {
            return false;
        }
        else {
            totalBalance = totalBalance.subtract(debit);
            return true;
        }
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe executed while another order may be
    // matched simultaneously. In that case, this method may be called
    // simultaneously leading to race conditions. Therefore, better to have
    // this synchronized.
    public synchronized void addToAvailableShares(int shares) {
        availableShares += shares;
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe executed while another order may be
    // matched simultaneously. In that case, this method may be called
    // simultaneously leading to race conditions. Therefore, better to have
    // this synchronized.
    public synchronized void addToTotalShares(int shares) {
        totalShares += shares;
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe executed while another order may be
    // matched simultaneously. In that case, this method may be called
    // simultaneously leading to race conditions. Therefore, better to have
    // this synchronized.
    // Return true for success, false for negative balance
    public synchronized boolean tryDeductFromAvailableShares(int shares) {
        // Negative number of shares; Do not update
        if(availableShares < shares) {
            return false;
        }
        else {
            availableShares -= shares;
            return true;
        }
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe executed while another order may be
    // matched simultaneously. In that case, this method may be called
    // simultaneously leading to race conditions. Therefore, better to have
    // this synchronized.
    // Return true for success, false for negative balance
    public synchronized boolean tryDeductFromTotalShares(int shares) {
        // Negative number of shares; Do not update
        if(totalShares < shares) {
            return false;
        }
        else {
            totalShares -= shares;
            return true;
        }
    }
}
