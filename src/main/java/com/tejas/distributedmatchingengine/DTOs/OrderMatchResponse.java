package com.tejas.distributedmatchingengine.DTOs;

import com.tejas.distributedmatchingengine.models.Status;
import com.tejas.distributedmatchingengine.models.Transaction;

import java.util.List;

public record OrderMatchResponse(
        Status status,
        List<Transaction> matches
) {
    public int filledQuantity() {
        int qtyFilled = 0;
        for (Transaction match : matches) {
            qtyFilled += match.quantityFilled();
        }
        return qtyFilled;
    }
}
