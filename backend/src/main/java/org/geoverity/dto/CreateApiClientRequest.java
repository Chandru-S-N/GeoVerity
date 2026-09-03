package org.geoverity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiClientRequest {

    @NotBlank(message = "Client name is required")
    private String clientName;

    private String permissions; // e.g. CAPTURE,VERIFY,TIME_TOKEN
}
