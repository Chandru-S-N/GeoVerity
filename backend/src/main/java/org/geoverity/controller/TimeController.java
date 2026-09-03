package org.geoverity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.geoverity.dto.TimeTokenRequest;
import org.geoverity.dto.TimeTokenResponse;
import org.geoverity.service.TimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/time")
@RequiredArgsConstructor
@Tag(name = "Time Authority", description = "Endpoints for obtaining trusted server time tokens")
public class TimeController {

    private final TimeService timeService;

    @PostMapping("/token")
    @Operation(summary = "Obtain trusted server time token", description = "Returns an authoritative, cryptographically signed time token with millisecond precision.")
    public ResponseEntity<TimeTokenResponse> getTrustedTimeToken(@RequestBody(required = false) TimeTokenRequest request) {
        TimeTokenResponse response = timeService.generateTimeToken(request);
        return ResponseEntity.ok(response);
    }
}
