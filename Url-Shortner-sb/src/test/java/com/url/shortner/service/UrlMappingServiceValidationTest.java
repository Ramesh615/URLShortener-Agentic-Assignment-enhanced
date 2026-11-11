package com.url.shortner.service;

import com.url.shortner.models.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlMappingServiceValidationTest {
    private final UrlMappingService service = new UrlMappingService();

    @Test
    void rejectsJavascriptDestination() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createShortUrl("javascript:alert(1)", new User()));
    }

    @Test
    void rejectsRelativeDestination() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createShortUrl("/account/profile", new User()));
    }

    @Test
    void rejectsEmbeddedCredentials() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createShortUrl("https://user:password@example.com/path", new User()));
    }

    @Test
    void rejectsOversizedDestination() {
        String url = "https://example.com/" + "a".repeat(2048);
        assertThrows(IllegalArgumentException.class,
                () -> service.createShortUrl(url, new User()));
    }
}
