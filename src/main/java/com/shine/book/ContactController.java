package com.shine.book;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
class ContactController {
    private final SessionAccess sessions;
    private final ContactService contacts;
    ContactController(SessionAccess sessions, ContactService contacts) {
        this.sessions = sessions;
        this.contacts = contacts;
    }
    @GetMapping
    ContactService.Listing list(@RequestParam(defaultValue="") String q, @RequestParam(defaultValue="0") int page, HttpServletRequest req) {
        return contacts.search(sessions.require(req).id, q, page);
    }
    @GetMapping("/{id}")
    ContactService.View get(@PathVariable long id, HttpServletRequest req) {
        return contacts.get(sessions.require(req).id, id);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ContactService.View create(@Valid @RequestBody ContactForm form, HttpServletRequest req) {
        return contacts.create(sessions.require(req), form);
    }
    @PutMapping("/{id}")
    ContactService.View update(@PathVariable long id, @Valid @RequestBody ContactForm form, HttpServletRequest req) {
        return contacts.update(sessions.require(req).id, id, form);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id, HttpServletRequest req) { contacts.delete(sessions.require(req).id, id); }
}
