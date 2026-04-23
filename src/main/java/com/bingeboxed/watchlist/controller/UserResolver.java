// src/main/java/com/bingeboxed/watchlist/controller/UserResolver.java
package com.bingeboxed.watchlist.controller;

/**
 * Resolves the internal user ID from an authenticated email.
 * Implemented as a separate bean to keep the controller thin and
 * avoid importing the profiles package directly.
 */
public interface UserResolver {
    Long resolveUserIdByEmail(String email);
}