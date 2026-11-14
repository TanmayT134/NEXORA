package com.nexora.nexora_backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter; import lombok.Setter;
@Getter @Setter
public class IntegrityEventRequest {
    @NotBlank @Size(max=40) private String eventType;
    @NotBlank @Size(max=20) private String severity;
    @Size(max=2000) private String details;
}
