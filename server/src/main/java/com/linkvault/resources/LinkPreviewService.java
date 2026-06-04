package com.linkvault.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LinkPreviewService {

    public static final String STATUS_OK = "OK";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_BLOCKED = "BLOCKED";

    private static final Logger log = LoggerFactory.getLogger(LinkPreviewService.class);
    private static final int MAX_HTML_BYTES = 1024 * 1024;
    private static final int MAX_REDIRECTS = 3;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Pattern META_TAG_PATTERN = Pattern.compile("<meta\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK_TAG_PATTERN = Pattern.compile("<link\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ATTRIBUTE_PATTERN_TEMPLATE = Pattern.compile("\\b%s\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC_ENTITY_PATTERN = Pattern.compile("&#(\\d+);");
    private static final Pattern HEX_ENTITY_PATTERN = Pattern.compile("&#x([0-9a-fA-F]+);");

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    private final Map<String, CachedPreview> cache = new ConcurrentHashMap<>();

    public LinkPreviewResponse fetch(String rawUrl) {
        return fetch(rawUrl, false);
    }

    public LinkPreviewResponse fetch(String rawUrl, boolean forceRefresh) {
        Instant now = Instant.now();
        String normalizedUrl = null;
        try {
            normalizedUrl = normalizeForFetch(rawUrl);
            CachedPreview cached = cache.get(normalizedUrl);
            if (!forceRefresh && cached != null && cached.cachedAt().plus(CACHE_TTL).isAfter(now)) {
                return cached.response();
            }

            URI target = URI.create(normalizedUrl);
            FetchResult fetchResult = fetchHtml(target);
            LinkPreviewResponse response = parse(fetchResult.html(), rawUrl, fetchResult.finalUri(), now);
            cache.put(normalizedUrl, new CachedPreview(response, now));
            return response;
        } catch (LinkPreviewException exception) {
            log.warn("Link preview fetch skipped for {}: {}", rawUrl, exception.getMessage());
            return failure(rawUrl, normalizedUrl, exception.status(), exception.getMessage(), now);
        } catch (Exception exception) {
            log.warn("Link preview fetch failed for {}: {}", rawUrl, exception.getMessage());
            log.debug("Link preview fetch stacktrace", exception);
            return failure(rawUrl, normalizedUrl, STATUS_FAILED, "Could not fetch preview metadata", now);
        }
    }

    public String displayDomain(String rawUrl) {
        try {
            URI uri = URI.create(normalizeForFetch(rawUrl));
            return cleanDomain(uri.getHost());
        } catch (RuntimeException exception) {
            return "";
        }
    }

    public String normalizeUserUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            if (lower.contains("://")) {
                return trimmed;
            }
            trimmed = "https://" + trimmed;
        }
        return trimmed;
    }

    private FetchResult fetchHtml(URI initialUri) throws IOException, InterruptedException {
        URI current = initialUri;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            validateFetchTarget(current);
            HttpRequest request = HttpRequest.newBuilder(current)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "LinkVaultBot/1.0")
                .header("Accept", "text/html,application/xhtml+xml,text/plain;q=0.8,*/*;q=0.1")
                .GET()
                .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                closeQuietly(response.body());
                Optional<String> location = response.headers().firstValue("location");
                if (location.isEmpty()) {
                    throw new LinkPreviewException(STATUS_FAILED, "Preview request was redirected without a location");
                }
                current = current.resolve(location.get());
                continue;
            }

            if (status < 200 || status >= 300) {
                closeQuietly(response.body());
                throw new LinkPreviewException(STATUS_FAILED, "Preview request returned HTTP " + status);
            }

            String contentType = response.headers()
                .firstValue("content-type")
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
            if (!contentType.isBlank() && !isHtmlLike(contentType)) {
                closeQuietly(response.body());
                throw new LinkPreviewException(STATUS_FAILED, "URL did not return an HTML page");
            }

            long contentLength = response.headers().firstValueAsLong("content-length").orElse(-1);
            if (contentLength > MAX_HTML_BYTES) {
                closeQuietly(response.body());
                throw new LinkPreviewException(STATUS_FAILED, "HTML page is too large to preview");
            }

            try (InputStream body = response.body()) {
                return new FetchResult(current, readLimited(body));
            }
        }

        throw new LinkPreviewException(STATUS_FAILED, "Preview request redirected too many times");
    }

    private LinkPreviewResponse parse(String html, String requestedUrl, URI finalUri, Instant fetchedAt) {
        String ogTitle = firstMeta(html, "property", "og:title");
        String twitterTitle = firstMeta(html, "name", "twitter:title");
        String title = firstNonBlank(ogTitle, twitterTitle, titleTag(html));

        String ogDescription = firstMeta(html, "property", "og:description");
        String twitterDescription = firstMeta(html, "name", "twitter:description");
        String description = firstNonBlank(ogDescription, twitterDescription, firstMeta(html, "name", "description"));

        String image = resolveWebUrl(finalUri, firstNonBlank(
            firstMeta(html, "property", "og:image"),
            firstMeta(html, "name", "twitter:image")
        ));
        String canonical = resolveWebUrl(finalUri, firstNonBlank(
            firstMeta(html, "property", "og:url"),
            linkHref(html, "canonical"),
            finalUri.toString()
        ));
        String siteName = firstMeta(html, "property", "og:site_name");
        String favicon = resolveWebUrl(finalUri, firstNonBlank(linkHref(html, "icon"), linkHref(html, "shortcut icon"), "/favicon.ico"));
        String domain = cleanDomain(finalUri.getHost());
        String sourceName = firstNonBlank(siteName, domain);

        return new LinkPreviewResponse(
            requestedUrl,
            finalUri.toString(),
            truncate(sourceName, 255),
            domain,
            truncate(image, 2000),
            truncate(firstNonBlank(title, domain), 500),
            truncate(description, 1000),
            truncate(favicon, 2000),
            truncate(siteName, 255),
            truncate(canonical, 2000),
            fetchedAt,
            STATUS_OK,
            null
        );
    }

    private String normalizeForFetch(String value) {
        String normalized = normalizeUserUrl(value);
        if (normalized == null) {
            throw new LinkPreviewException(STATUS_FAILED, "URL is required");
        }

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new LinkPreviewException(STATUS_BLOCKED, "Only http and https URLs can be previewed");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new LinkPreviewException(STATUS_FAILED, "URL host is missing");
            }
            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new LinkPreviewException(STATUS_FAILED, "URL is not valid");
        }
    }

    private void validateFetchTarget(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new LinkPreviewException(STATUS_BLOCKED, "Only http and https URLs can be previewed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new LinkPreviewException(STATUS_FAILED, "URL host is missing");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (
            lowerHost.equals("localhost") ||
            lowerHost.endsWith(".localhost") ||
            lowerHost.equals("0.0.0.0")
        ) {
            throw new LinkPreviewException(STATUS_BLOCKED, "Local addresses cannot be previewed");
        }

        try {
            String asciiHost = IDN.toASCII(host);
            for (InetAddress address : InetAddress.getAllByName(asciiHost)) {
                if (isBlockedAddress(address)) {
                    throw new LinkPreviewException(STATUS_BLOCKED, "Private network addresses cannot be previewed");
                }
            }
        } catch (LinkPreviewException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LinkPreviewException(STATUS_FAILED, "Could not resolve URL host");
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (
            address.isAnyLocalAddress() ||
            address.isLoopbackAddress() ||
            address.isLinkLocalAddress() ||
            address.isSiteLocalAddress() ||
            address.isMulticastAddress()
        ) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            return first == 10 ||
                (first == 172 && second >= 16 && second <= 31) ||
                (first == 192 && second == 168) ||
                (first == 127);
        }

        if (bytes.length == 16) {
            int first = bytes[0] & 0xff;
            return (first & 0xfe) == 0xfc;
        }

        return false;
    }

    private String readLimited(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_HTML_BYTES) {
                throw new LinkPreviewException(STATUS_FAILED, "HTML page is too large to preview");
            }
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private boolean isHtmlLike(String contentType) {
        return contentType.contains("text/html") ||
            contentType.contains("application/xhtml+xml") ||
            contentType.contains("text/plain");
    }

    private String firstMeta(String html, String attributeName, String attributeValue) {
        Matcher matcher = META_TAG_PATTERN.matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String value = attribute(tag, attributeName);
            if (attributeValue.equalsIgnoreCase(value)) {
                return cleanText(attribute(tag, "content"));
            }
        }
        return "";
    }

    private String linkHref(String html, String relValue) {
        Matcher matcher = LINK_TAG_PATTERN.matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String rel = attribute(tag, "rel").toLowerCase(Locale.ROOT);
            if (rel.equals(relValue) || rel.contains(relValue)) {
                return cleanText(attribute(tag, "href"));
            }
        }
        return "";
    }

    private String titleTag(String html) {
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        return cleanText(matcher.group(1));
    }

    private String attribute(String tag, String name) {
        Pattern pattern = Pattern.compile(String.format(ATTRIBUTE_PATTERN_TEMPLATE.pattern(), Pattern.quote(name)), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(tag);
        if (!matcher.find()) {
            return "";
        }
        for (int group = 2; group <= 4; group++) {
            String value = matcher.group(group);
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    private String resolveWebUrl(URI base, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        try {
            URI resolved = base.resolve(value.trim());
            String scheme = resolved.getScheme() == null ? "" : resolved.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return "";
            }
            return resolved.toString();
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private String cleanDomain(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        String value = host.toLowerCase(Locale.ROOT);
        return value.startsWith("www.") ? value.substring(4) : value;
    }

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return decodeHtml(value.replaceAll("\\s+", " ").trim());
    }

    private String decodeHtml(String value) {
        String decoded = value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ");

        decoded = replaceNumericEntities(decoded, NUMERIC_ENTITY_PATTERN, 10);
        decoded = replaceNumericEntities(decoded, HEX_ENTITY_PATTERN, 16);
        return decoded;
    }

    private String replaceNumericEntities(String value, Pattern pattern, int radix) {
        Matcher matcher = pattern.matcher(value);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            try {
                int codePoint = Integer.parseInt(matcher.group(1), radix);
                matcher.appendReplacement(builder, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
            } catch (RuntimeException exception) {
                matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private LinkPreviewResponse failure(String requestedUrl, String normalizedUrl, String status, String error, Instant fetchedAt) {
        String domain = displayDomain(normalizedUrl == null ? requestedUrl : normalizedUrl);
        return new LinkPreviewResponse(
            requestedUrl,
            normalizedUrl,
            domain,
            domain,
            null,
            null,
            null,
            null,
            null,
            normalizedUrl,
            fetchedAt,
            status,
            truncate(error, 1000)
        );
    }

    private void closeQuietly(InputStream inputStream) {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException ignored) {
            // Response body is being discarded after redirect/error.
        }
    }

    private record FetchResult(URI finalUri, String html) {
    }

    private record CachedPreview(LinkPreviewResponse response, Instant cachedAt) {
    }

    private static class LinkPreviewException extends RuntimeException {
        private final String status;

        LinkPreviewException(String status, String message) {
            super(message);
            this.status = status;
        }

        String status() {
            return status;
        }
    }
}
