package edu.automarket.common;

/**
 * Response model following RFC 9457 "Problem Details for HTTP APIs".
 * Serialized with Content-Type: application/problem+json.
 */
public record ProblemDTO(
        String type,
        String title,
        int status
) {
}
