package com.tt.tripbook.exception;

// Wrong email or password. The message is the same in both cases on purpose,
// so the API does not reveal which emails have an account.
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
