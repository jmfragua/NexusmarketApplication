package application.adapters.persistence.jpa.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence shape of every user of the platform.
 *
 * <p>Buyers and sellers share a single table told apart by {@code userType}: the domain models them
 * as specializations of the same {@code User}, and a single table keeps that relationship obvious in
 * storage instead of duplicating identity columns across two tables.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "identity_document", nullable = false, unique = true, length = 60)
    private String identityDocument;

    @Column(name = "role", nullable = false, length = 40)
    private String role;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Buyer only; null for every other kind of user. */
    @Column(name = "primary_address", length = 400)
    private String primaryAddress;

    /** Buyer only; null for every other kind of user. */
    @Column(name = "commercial_status", length = 20)
    private String commercialStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_additional_addresses", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "address", length = 400)
    private List<String> additionalAddresses = new ArrayList<>();
}
