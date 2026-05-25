package com.siide.linkup.feature.profile.domain.model;

import com.siide.linkup.core.audit.Auditable;
import com.siide.linkup.feature.profile.domain.exception.ProfileInvalidStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;

/**
 * Profile aggregate root. Holds user-facing personal data tied 1:1 to a
 * {@code feature.auth.User} via {@code userId}. The link is a bare UUID — no
 * cross-module FK — so the auth and profile schemas can be extracted into
 * separate services later (CLAUDE.md §9, §3.4).
 * <p>
 * Lifecycle: {@code ACTIVE} → {@code DELETION_PENDING} (one-way until purge).
 * Once {@code DELETION_PENDING}, all mutating operations are refused — the
 * row is going to be hard-deleted by the scheduler.
 */
@Entity
@Table(
        name = "profiles",
        indexes = {
                // Partial index helps the deletion scheduler scan only pending rows.
                @Index(name = "ix_profiles_status_scheduled",
                        columnList = "status, deletion_scheduled_at")
        }
)
public class Profile extends Auditable {

    /** Defensive cap matching DB CHECK; protects against pathological inputs. */
    public static final int BIO_MAX_LENGTH = 150;
    public static final int CITY_MAX_LENGTH = 100;
    /** Minimum age to use the platform — aligns with most West-African e-commerce KYC. */
    public static final int MIN_AGE_YEARS = 13;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Link to {@code feature.auth.User#id}. Unique — one profile per user. */
    @Column(name = "user_id", nullable = false, updatable = false, unique = true)
    private UUID userId;

    @Column(name = "bio", length = BIO_MAX_LENGTH)
    private String bio;

    @Column(name = "city", length = CITY_MAX_LENGTH)
    private String city;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    /** Object key in the photo bucket (MinIO / S3). Resolved to a URL by the controller. */
    @Column(name = "photo_key", length = 255)
    private String photoKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProfileStatus status;

    @Column(name = "deletion_scheduled_at")
    private Instant deletionScheduledAt;

    protected Profile() {
        // JPA
    }

    private Profile(UUID id, UUID userId) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.status = ProfileStatus.ACTIVE;
    }

    /** Build an empty profile for a freshly provisioned user. */
    public static Profile empty(UUID userId) {
        return new Profile(UUID.randomUUID(), userId);
    }

    /**
     * Update the user-editable fields. {@code null} means "leave unchanged" only
     * for {@code dateOfBirth} and {@code gender} (those are explicitly optional);
     * passing {@code null} for {@code bio} or {@code city} CLEARS them, mirroring
     * a PUT semantic.
     *
     * @param now caller-provided clock for deterministic age validation
     */
    public void update(String bio, String city, LocalDate dateOfBirth, Gender gender, Instant now) {
        Objects.requireNonNull(now, "now");
        requireMutable();
        setBio(bio);
        setCity(city);
        setDateOfBirth(dateOfBirth, now);
        this.gender = gender;
    }

    /** Whether all "required for completeness" fields are filled. Drives ProfileCompletedEvent. */
    public boolean isComplete() {
        return bio != null && !bio.isBlank()
                && city != null && !city.isBlank()
                && dateOfBirth != null;
    }

    /** Marker for the soft-delete flow (PR #8). Made package-private until then to avoid premature use. */
    void markForDeletion(Instant scheduledAt) {
        Objects.requireNonNull(scheduledAt, "scheduledAt");
        requireMutable();
        this.status = ProfileStatus.DELETION_PENDING;
        this.deletionScheduledAt = scheduledAt;
    }

    void cancelDeletion() {
        if (status != ProfileStatus.DELETION_PENDING) {
            throw new ProfileInvalidStateException("Profile is not pending deletion");
        }
        this.status = ProfileStatus.ACTIVE;
        this.deletionScheduledAt = null;
    }

    void attachPhoto(String photoKey) {
        requireMutable();
        this.photoKey = photoKey;
    }

    void clearPhoto() {
        requireMutable();
        this.photoKey = null;
    }

    private void setBio(String bio) {
        if (bio != null) {
            String trimmed = bio.trim();
            if (trimmed.length() > BIO_MAX_LENGTH) {
                throw new IllegalArgumentException("bio must be at most " + BIO_MAX_LENGTH + " characters");
            }
            this.bio = trimmed.isEmpty() ? null : trimmed;
        } else {
            this.bio = null;
        }
    }

    private void setCity(String city) {
        if (city != null) {
            String trimmed = city.trim();
            if (trimmed.length() > CITY_MAX_LENGTH) {
                throw new IllegalArgumentException("city must be at most " + CITY_MAX_LENGTH + " characters");
            }
            this.city = trimmed.isEmpty() ? null : trimmed;
        } else {
            this.city = null;
        }
    }

    private void setDateOfBirth(LocalDate dateOfBirth, Instant now) {
        if (dateOfBirth == null) {
            this.dateOfBirth = null;
            return;
        }
        LocalDate today = now.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        if (!dateOfBirth.isBefore(today)) {
            throw new IllegalArgumentException("dateOfBirth must be in the past");
        }
        if (Period.between(dateOfBirth, today).getYears() < MIN_AGE_YEARS) {
            throw new IllegalArgumentException("user must be at least " + MIN_AGE_YEARS + " years old");
        }
        this.dateOfBirth = dateOfBirth;
    }

    private void requireMutable() {
        if (status == ProfileStatus.DELETION_PENDING) {
            throw new ProfileInvalidStateException(
                    "Profile is pending deletion — cancel the deletion before updating");
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getBio() { return bio; }
    public String getCity() { return city; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Gender getGender() { return gender; }
    public String getPhotoKey() { return photoKey; }
    public ProfileStatus getStatus() { return status; }
    public Instant getDeletionScheduledAt() { return deletionScheduledAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Profile other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
