package edu.automarket.sms.dto;

public record PreludeTargetDTO(
        String type,
        String value
) {
    public PreludeTargetDTO(String phoneNumber) {
        this("phone_number", phoneNumber);
    }
}
