package edu.automarket.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramGatewayAPIRequestDTO(
        @JsonProperty("phone_number") String phoneNumber,
        String code,
        int ttl
) {
}
