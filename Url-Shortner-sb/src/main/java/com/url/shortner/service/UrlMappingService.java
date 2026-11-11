package com.url.shortner.service;

import com.url.shortner.dtos.ClickEventDTO;
import com.url.shortner.dtos.UrlMappingDTO;
import com.url.shortner.models.ClickEvent;
import com.url.shortner.models.UrlMapping;
import com.url.shortner.models.User;
import com.url.shortner.repository.ClickEventRepository;
import com.url.shortner.repository.UrlMappingRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.security.SecureRandom;
import java.net.URI;
import java.util.stream.Collectors;

@Service
public class UrlMappingService {

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    // Create a short URL for the given original URL and user
    public UrlMappingDTO createShortUrl(String originalUrl, User user) {
        validateDestination(originalUrl);
        String shortUrl = generateShortUrl();
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl.trim());
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedDate(LocalDateTime.now());
        UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);
        return convertToDto(savedUrlMapping);

    }

    // Cryptographically strong random code; DB uniqueness remains the final concurrency guard.
    private String generateShortUrl() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(8);
            for (int i = 0; i < 8; i++) code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
            if (!urlMappingRepository.existsByShortUrl(code.toString())) return code.toString();
        }
        throw new IllegalStateException("Unable to allocate a unique short code; retry the request");
    }

    private void validateDestination(String value) {
        if (value == null || value.isBlank() || value.length() > 2048)
            throw new IllegalArgumentException("originalUrl is required and must be at most 2048 characters");
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || uri.getUserInfo() != null || scheme == null ||
                    !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
                throw new IllegalArgumentException("Only absolute HTTP or HTTPS URLs without embedded credentials are allowed");
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Only absolute")) throw ex;
            throw new IllegalArgumentException("originalUrl must be a valid absolute HTTP(S) URL");
        }
    }

    // Convert UrlMapping entity to UrlMappingDTO
    private UrlMappingDTO convertToDto(UrlMapping urlMapping){
        UrlMappingDTO urlMappingDTO = new UrlMappingDTO();
        urlMappingDTO.setId(urlMapping.getId());
        urlMappingDTO.setOriginalUrl(urlMapping.getOriginalUrl());
        urlMappingDTO.setShortUrl(urlMapping.getShortUrl());
        urlMappingDTO.setClickCount(urlMapping.getClickCount());
        urlMappingDTO.setCreatedDate(urlMapping.getCreatedDate());
        urlMappingDTO.setUsername(urlMapping.getUser().getUsername());
        return urlMappingDTO;
    }

    // Get all URLs for a specific user
    public List<UrlMappingDTO> getUrlsByUser(User user) {
        return urlMappingRepository.findByUser(user).stream().map(this::convertToDto).toList();
    }

    // Get click events for a specific short URL in a date range
    public List<ClickEventDTO> getClickEventsByDate(String shortUrl, LocalDateTime start, LocalDateTime end, User user) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
        if(urlMapping != null && java.util.Objects.equals(urlMapping.getUser().getId(), user.getId())){
            return clickEventRepository.findByUrlMappingAndClickDateBetween(urlMapping , start , end)
                    .stream().collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(),Collectors.counting()))
                    .entrySet().stream()
                    .map(entry ->
                    {
                        ClickEventDTO clickEventDTO = new ClickEventDTO();
                        clickEventDTO.setClickDate(entry.getKey());
                        clickEventDTO.setCount(entry.getValue());
                        return clickEventDTO;
                    })
                    .collect(Collectors.toList());
        }
        throw new NoSuchElementException("URL mapping not found or access denied");
    }

    // Get total clicks for a user in a date range
    public Map<LocalDate, Long> getTotalClicksByUserAndDate(User user, LocalDate start, LocalDate end) {
        List<UrlMapping> urlMappings = urlMappingRepository.findByUser(user);
        List<ClickEvent> clickEvents = clickEventRepository.findByUrlMappingInAndClickDateBetween(urlMappings , start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return clickEvents.stream().collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(), Collectors.counting()));
    }

    // Retrieve original URL and increment click count
    @Transactional
    public UrlMapping getOriginalUrl(String shortUrl) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
        if(urlMapping != null){
            // Atomic database increment prevents lost updates when redirects arrive concurrently.
            urlMappingRepository.incrementClickCount(shortUrl);
            urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

            // record Click event

            ClickEvent clickEvent =  new ClickEvent();
            clickEvent.setClickDate(LocalDateTime.now());
            clickEvent.setUrlMapping(urlMapping);
            clickEventRepository.save(clickEvent);
        }
        return urlMapping;
    }

    // Delete URL mapping along with its click events
    @Transactional  // Ensure all deletions happen in a single transaction
    public void deleteUrlMapping(String shortUrl, User user) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (urlMapping == null) {
            throw new NoSuchElementException("URL mapping not found");
        }

        if (!java.util.Objects.equals(urlMapping.getUser().getId(), user.getId())) {
            throw new SecurityException("You don't have permission to delete this URL");
        }

        // Delete all click events related to this URL
        clickEventRepository.deleteByUrlMapping(urlMapping);

        // Delete the URL mapping itself
        urlMappingRepository.delete(urlMapping);
    }

    // Update the original URL for a given short URL
    public UrlMappingDTO updateOriginalUrl(String shortUrl, String newOriginalUrl, User user) {
        validateDestination(newOriginalUrl);
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (urlMapping == null) {
            throw new NoSuchElementException("URL mapping not found");
        }

        if (!java.util.Objects.equals(urlMapping.getUser().getId(), user.getId())) {
            throw new SecurityException("You don't have permission to update this URL");
        }

        urlMapping.setOriginalUrl(newOriginalUrl.trim());
        urlMappingRepository.save(urlMapping);
        return convertToDto(urlMapping);
    }
}
