package com.shine.book;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContactRepository extends JpaRepository<Contact, Long> {
    Optional<Contact> findByIdAndOwnerId(long id, long ownerId);
    Page<Contact> findByOwnerIdAndFirstNameContainingIgnoreCaseOrOwnerIdAndLastNameContainingIgnoreCase(
            long firstOwner, String first, long lastOwner, String last, Pageable pageable);
}
