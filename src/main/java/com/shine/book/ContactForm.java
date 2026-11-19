package com.shine.book;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

record ContactForm(@NotBlank @Size(max=80) String firstName, @NotBlank @Size(max=80) String lastName,
                   @NotNull @Size(max=100) String title,
                   @NotNull @Size(max=20) List<@NotNull @Valid EmailEntry> emails,
                   @NotNull @Size(max=20) List<@NotNull @Valid PhoneEntry> phones) {
    record EmailEntry(@NotBlank @Size(max=30) String label, @Email @NotBlank @Size(max=254) String value) {}
    record PhoneEntry(@NotBlank @Size(max=30) String label,
                      @NotBlank @Pattern(regexp="(?=(?:[^0-9]*[0-9]){7,15}[^0-9]*$)\\+?[0-9 ()-]{7,32}", message="must contain 7 to 15 digits") String value) {}

    void applyTo(Contact c) {
        c.firstName = firstName.strip();
        c.lastName = lastName.strip();
        c.title = title.strip();
        c.emails.clear();
        emails.forEach(e -> c.emails.add(new Contact.Entry(e.label(), e.value())));
        c.phones.clear();
        phones.forEach(p -> c.phones.add(new Contact.Entry(p.label(), p.value())));
    }
}
