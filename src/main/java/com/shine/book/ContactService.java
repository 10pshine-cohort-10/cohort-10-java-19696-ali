package com.shine.book;

import java.util.List;
import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional(readOnly=true)
class ContactService {
    record Labeled(String label, String value) {
        static Labeled of(Contact.Entry e) { return new Labeled(e.label, e.value); }
    }
    record View(long id, String firstName, String lastName, String title, List<Labeled> emails, List<Labeled> phones) {
        static View of(Contact c) {
            return new View(c.id, c.firstName, c.lastName, c.title,
                    c.emails.stream().map(Labeled::of).toList(), c.phones.stream().map(Labeled::of).toList());
        }
    }
    record Listing(List<View> items, long total, int pages) {}
    private static final Logger log = LoggerFactory.getLogger(ContactService.class);
    private final ContactRepository contacts;
    ContactService(ContactRepository contacts) { this.contacts = contacts; }

    Listing search(long owner, String query, int page) {
        if (page < 0 || page > 1_000_000 || query.length() > 160) throw new ApiError(BAD_REQUEST, "Invalid search or page number.");
        String term = query.strip();
        Pageable request = PageRequest.of(page, 10, Sort.by("lastName", "firstName", "id"));
        Page<Contact> result = contacts.findByOwnerIdAndFirstNameContainingIgnoreCaseOrOwnerIdAndLastNameContainingIgnoreCase(
                owner, term, owner, term, request);
        return new Listing(result.map(View::of).getContent(), result.getTotalElements(), result.getTotalPages());
    }
    private Contact owned(long owner, long id) {
        return contacts.findByIdAndOwnerId(id, owner).orElseThrow(() -> new ApiError(NOT_FOUND, "Contact not found."));
    }
    View get(long owner, long id) { return View.of(owned(owner, id)); }

    @Transactional
    View create(Account owner, ContactForm form) {
        Contact c = new Contact(owner);
        form.applyTo(c);
        contacts.saveAndFlush(c);
        log.info("Contact created, accountId={} contactId={}", owner.id, c.id);
        return View.of(c);
    }
    @Transactional
    View update(long owner, long id, ContactForm form) {
        Contact c = owned(owner, id);
        form.applyTo(c);
        log.info("Contact updated, accountId={} contactId={}", owner, id);
        return View.of(c);
    }
    @Transactional
    void delete(long owner, long id) {
        contacts.delete(owned(owner, id));
        log.info("Contact deleted, accountId={} contactId={}", owner, id);
    }
}
