package edu.automarket.sms.dto;

public record PreludeVerifyRequestDTO(PreludeTargetDTO target, String code) {
    public PreludeVerifyRequestDTO(String phoneNumber, String code) {
        this(new PreludeTargetDTO(phoneNumber), code);
    }
}
