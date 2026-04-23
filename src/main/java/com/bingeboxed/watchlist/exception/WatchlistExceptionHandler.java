package com.bingeboxed.watchlist.exception;

import com.bingeboxed.shared.exception.GlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.bingeboxed.watchlist.controller")
public class WatchlistExceptionHandler extends GlobalExceptionHandler {
    // Reuses parent exception handling logic
}