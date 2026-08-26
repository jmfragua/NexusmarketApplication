package application.domain.valuesObjects;

/**
 * Operational condition of a user.
 *
 * <p>Lifecycle: {@code ACTIVE <-> BLOCKED}. Only an active user can execute operations
 * (RG-01).</p>
 */
public enum UserStatus implements Enumeration {

    ACTIVE("Active. The user can operate on the platform."),
    BLOCKED("Blocked. The user cannot execute operations.");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String code() {
        return name();
    }

    /**
     * Declares the allowed transitions; every transition not declared here is forbidden
     * (RD-VO-12).
     */
    public boolean canTransitionTo(UserStatus target) {
        return target != null && target != this;
    }

    public boolean canOperate() {
        return this == ACTIVE;
    }
}
