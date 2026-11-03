package com.tejas.distributedmatchingengine.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/*
 * Contains the user's balance and shares bought.
 * For milestone 1, only a symbol (e.g. BTC) and a legal tender
 * (e.g. INR) is required. Therefore, all the "shares" here
 * imply the symbol and all the "balance" imply the legal tender.
 */
@Getter
@Setter(AccessLevel.NONE)
public class UserWallet {

    private String userID;
    private List<Order> orderList;

    // Balance available for new BUY orders
    private BigDecimal availableBalance;
    // Total value of the account.
    // Balance available for new BUYs + balance tied up in old BUY orders
    private BigDecimal totalBalance;

    // Shares available for new SELL orders
    private int availableShares;
    // Total number of shares in the account.
    // Shares available for new SELLs + shares tied up in old SELL orders
    private int totalShares;

    public UserWallet(BigDecimal balance) {
        this.userID = UUID.randomUUID().toString();
        this.totalBalance = this.availableBalance = balance;
        this.availableShares = this.totalShares = 0;
        orderList = Collections.synchronizedList(new ArrayList<>());
    }

    public void addOrderToWallet(Order order) {
        orderList.add(order);
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe matched while another order (of a different
    // symbol) may be matched simultaneously. In this case, this method may be called
    // simultaneously leading to race conditions. Therefore, it is better to have
    // this synchronized.
    public synchronized void addToAvailableBalance(BigDecimal credit) {
        availableBalance = availableBalance.add(credit);
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe matched while another order (of a different
    // symbol) may be matched simultaneously. In this case, this method may be called
    // simultaneously leading to race conditions. Therefore, it is better to have
    // this synchronized.
    public synchronized void addToTotalBalance(BigDecimal credit) {
        totalBalance = totalBalance.add(credit);
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe matched while another order (of a different
    // symbol) may be matched simultaneously. In this case, this method may be called
    // simultaneously leading to race conditions. Therefore, it is better to have
    // this synchronized.
    // Return true for success, false for negative balance
    public synchronized boolean tryDeductFromAvailableBalance(BigDecimal debit) {
        // Negative balance; Do not update balance
        if (availableBalance.compareTo(debit) < 0) {
            return false;
        } else {
            availableBalance = availableBalance.subtract(debit);
            return true;
        }
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe matched while another order (of a different
    // symbol) may be matched simultaneously. In this case, this method may be called
    // simultaneously leading to race conditions. Therefore, it is better to have
    // this synchronized.
    // Return true for success, false for negative balance
    public synchronized boolean tryDeductFromTotalBalance(BigDecimal debit) {
        // Negative balance; Therefore, do not update balance
        if (totalBalance.compareTo(debit) < 0) {
            return false;
        } else {
            totalBalance = totalBalance.subtract(debit);
            return true;
        }
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe matched while another order (of a different
    // symbol) may be matched simultaneously. In this case, this method may be called
    // simultaneously leading to race conditions. Therefore, it is better to have
    // this synchronized.
    public synchronized void addToAvailableShares(int shares) {
        availableShares += shares;
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe matched while another order (of a different
    // symbol) may be matched simultaneously. In this case, this method may be called
    // simultaneously leading to race conditions. Therefore, it is better to have
    // this synchronized.
    public synchronized void addToTotalShares(int shares) {
        totalShares += shares;
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe matched while another order (of a different
    // symbol) may be matched simultaneously. In this case, this method may be called
    // simultaneously leading to race conditions. Therefore, it is better to have
    // this synchronized.
    // Return true for success, false for negative number of shares
    public synchronized boolean tryDeductFromAvailableShares(int shares) {
        // Negative number of shares; Do not update
        if (availableShares < shares) {
            return false;
        } else {
            availableShares -= shares;
            return true;
        }
    }

    // When we have multiple symbol pairs being traded by a single user, one
    // order in the order book maybe matched while another order (of a different
    // symbol) may be matched simultaneously. In this case, this method may be called
    // simultaneously leading to race conditions. Therefore, it is better to have
    // this synchronized.
    // Return true for success, false for negative number of shares
    public synchronized boolean tryDeductFromTotalShares(int shares) {
        // Negative number of shares; Do not update
        if (totalShares < shares) {
            return false;
        } else {
            totalShares -= shares;
            return true;
        }
    }

    @Override
    public String toString() {
        return String.format("""
                        Total balance: %s
                        Available balance: %s
                        
                        Total shares: %d
                        Available shares: %d""",
                totalBalance, availableBalance,
                totalShares, availableShares);
    }
}
