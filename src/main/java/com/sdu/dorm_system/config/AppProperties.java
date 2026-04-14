package com.sdu.dorm_system.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    MailProperties mail,
    BootstrapProperties bootstrap
) {

    public record MailProperties(
        boolean enabled,
        @NotBlank String from
    ) {
    }

    public record BootstrapProperties(
        AdminSeed leadAdmin,
        AdminSeed boysAdmin,
        AdminSeed girlsAdmin
    ) {
    }

    public record AdminSeed(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String surname
    ) {
    }
}
