package com.shine.book;

import jakarta.persistence.*;
import java.util.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name="contacts")
class Contact {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="owner_id", nullable=false)
    Account owner;
    @Nationalized @Column(nullable=false, length=80) String firstName;
    @Nationalized @Column(nullable=false, length=80) String lastName;
    @Nationalized @Column(nullable=false, length=100) String title;
    @ElementCollection @CollectionTable(name="contact_emails", joinColumns=@JoinColumn(name="contact_id"))
    @OrderColumn(name="position")
    List<Entry> emails = new ArrayList<>();
    @ElementCollection @CollectionTable(name="contact_phones", joinColumns=@JoinColumn(name="contact_id"))
    @OrderColumn(name="position")
    List<Entry> phones = new ArrayList<>();

    protected Contact() {}
    Contact(Account owner) { this.owner = owner; }

    @Embeddable
    static class Entry {
        @Nationalized @Column(nullable=false,length=30) String label;
        @Nationalized @Column(name="entry_value",nullable=false,length=254) String value;
        protected Entry() {}
        Entry(String label, String value) { this.label = label.strip(); this.value = value.strip(); }
    }
}
