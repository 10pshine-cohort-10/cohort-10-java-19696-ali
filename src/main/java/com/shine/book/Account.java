package com.shine.book;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "accounts")
class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Nationalized @Column(nullable = false, length = 100)
    String displayName;
    @Column(nullable = false, unique = true, length = 254)
    String loginId;
    @Column(nullable = false, length = 200)
    String passwordHash;
    @Column(nullable = false)
    long credentialVersion;

    protected Account() {}
    Account(String name, String loginId, String hash) {
        this.displayName = name;
        this.loginId = loginId;
        this.passwordHash = hash;
    }
}
