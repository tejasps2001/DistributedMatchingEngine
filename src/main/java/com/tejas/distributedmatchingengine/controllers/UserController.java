package com.tejas.distributedmatchingengine.controllers;

import com.tejas.distributedmatchingengine.exceptions.UserNotFoundException;
import com.tejas.distributedmatchingengine.models.UserWallet;
import com.tejas.distributedmatchingengine.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/portfolio")
    // Get the total and the available balance of the user
    public ResponseEntity<UserWallet> getPortfolio(@RequestParam String userID) {
        UserWallet wallet = userRepository.findUserByID(userID)
                .orElseThrow(() -> new UserNotFoundException("No user with" +
                        " userID:" + userID));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(wallet);
    }

    @PostMapping("/createUser")
    // Create a new user and return the userID of the created account
    public ResponseEntity<String> createNewUser(@RequestParam double balance) {
        if (balance < 0) return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body("Negative balance not allowed!");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userRepository.addUser(BigDecimal.valueOf(balance)));
    }
}
