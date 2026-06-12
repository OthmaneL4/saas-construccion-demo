package com.lsototalbouw.common.web;

/**
 * Utility class providing safe redirect mechanisms.
 *
 * <p>Prevents Open Redirect and HTTP Response Splitting vulnerabilities by validating
 * that redirection URLs are relative local paths and free of carriage returns or line feeds.
 */
public final class SafeRedirects {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SafeRedirects() {
    }

    /**
     * Validates whether a given URL is a safe local relative path.
     *
     * <p>A URL is considered a safe local path if:
     * <ul>
     *   <li>It is not {@code null}.</li>
     *   <li>It starts with a single forward slash {@code "/"} (ensuring it is a relative path).</li>
     *   <li>It does not start with double slashes {@code "//"} (which could represent a protocol-relative absolute URL).</li>
     *   <li>It contains no carriage return {@code "\r"} or line feed {@code "\n"} characters (preventing HTTP response splitting).</li>
     * </ul>
     *
     * @param returnUrl the URL path to validate
     * @return {@code true} if the URL meets the local path criteria, otherwise {@code false}
     */
    public static boolean isLocalPath(String returnUrl) {
        return returnUrl != null
                && returnUrl.startsWith("/")
                && !returnUrl.startsWith("//")
                && !returnUrl.contains("\r")
                && !returnUrl.contains("\n");
    }

    /**
     * Formats a Spring MVC redirect string safely.
     *
     * <p>Inspects the target {@code returnUrl} using {@link #isLocalPath(String)}. If safe, returns a redirect
     * mapping to it. Otherwise, defaults to the designated {@code fallbackPath}.
     *
     * @param returnUrl    the requested URL path to redirect to
     * @param fallbackPath the fallback URL path if the requested redirect URL is unsafe or invalid
     * @return a redirect instruction string formatted for Spring MVC (e.g. {@code "redirect:/dashboard"})
     */
    public static String redirectTo(String returnUrl, String fallbackPath) {
        return "redirect:" + (isLocalPath(returnUrl) ? returnUrl : fallbackPath);
    }
}
