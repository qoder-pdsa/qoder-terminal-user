package com.qoder.terminal.user.activity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Activity history of the current user. Users can only read and write their own records (user_id comes from the token, not the request body). */
@RestController
@Validated
public class ActivityController {

    public record ActivityRequest(@NotNull ActivityType type, @NotBlank @Size(max = 2000) String content) {}

    public record ActivityView(long id, ActivityType type, String content, Instant createdAt) {
        static ActivityView of(Activity a) {
            return new ActivityView(a.getId(), a.getType(), a.getContent(), a.getCreatedAt());
        }
    }

    private final ActivityRepository activities;
    private final Clock clock;

    public ActivityController(ActivityRepository activities, Clock clock) {
        this.activities = activities;
        this.clock = clock;
    }

    @PostMapping("/v1/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityView record(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ActivityRequest req) {
        Activity saved = activities.save(new Activity(userId(jwt), req.type(), req.content(), clock.instant()));
        return ActivityView.of(saved);
    }

    @GetMapping("/v1/activities")
    public List<ActivityView> list(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return activities.findByUserIdOrderByCreatedAtDescIdDesc(userId(jwt), PageRequest.of(0, limit)).stream()
                .map(ActivityView::of)
                .toList();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
