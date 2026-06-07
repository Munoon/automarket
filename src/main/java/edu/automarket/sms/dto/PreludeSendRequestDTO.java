package edu.automarket.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PreludeSendRequestDTO(Options options, PreludeTargetDTO target) {
    public record Options(
            String method,
            String locale,
            @JsonProperty("code_size") int codeSize
    ) {}
}
