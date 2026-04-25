package edu.automarket.upload.dto;

import java.util.List;
import java.util.Map;

public record SignUrlResponseDTO(
        String uploadUrl,
        Map<String, List<String>> uploadHeaders,
        String fileKey,
        String fileUrl
) {
}
