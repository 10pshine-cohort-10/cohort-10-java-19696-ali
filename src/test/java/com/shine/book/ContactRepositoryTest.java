package com.shine.book;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
class ContactRepositoryTest {
    @Autowired AccountRepository accounts;
    @Autowired ContactRepository contacts;

    @Test void bothNameFiltersRespectOwnerAndPagination() {
        Account a = accounts.save(new Account("First", "first@test.example", "test"));
        Account b = accounts.save(new Account("Second", "second@test.example", "test"));
        add(a, "Nadia", "Iqbal"); add(a, "Sam", "Nadia"); add(b, "Nadia", "Other");
        var page = contacts.findByOwnerIdAndFirstNameContainingIgnoreCaseOrOwnerIdAndLastNameContainingIgnoreCase(
                a.id, "nAD", a.id, "nAD", PageRequest.of(0,1));
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(1, page.getNumberOfElements());
        assertEquals(a.id, page.getContent().get(0).owner.id);
    }
    private void add(Account owner, String first, String last) {
        Contact c = new Contact(owner);
        c.firstName=first; c.lastName=last; c.title="";
        contacts.save(c);
    }
}
