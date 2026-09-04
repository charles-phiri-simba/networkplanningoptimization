package com.simba.snip.npo.vendorcertification.api;

import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = {
        "com.simba.snip.npo.vendorcertification",
        "com.simba.snip.npo.targetonboarding"
})
public class Phase17ExceptionAdvice {

    @ExceptionHandler(Phase17Exception.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> deny(Phase17Exception ex) {
        return Map.of("code", ex.denialCode().name(), "message", ex.getMessage());
    }
}
