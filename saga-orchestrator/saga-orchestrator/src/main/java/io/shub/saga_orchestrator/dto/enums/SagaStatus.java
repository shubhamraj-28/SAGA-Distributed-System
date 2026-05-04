package io.shub.saga_orchestrator.dto.enums;

/**
 * Lifecycle states of a saga instance.
 */
public enum SagaStatus {
    /** Saga created, first step not yet dispatched */
    STARTED,

    /** At least one step is in progress */
    RUNNING,

    /** All steps completed successfully */
    COMPLETED,

    /** A step failed; compensation is in progress */
    COMPENSATING,

    /** Compensation finished; saga is rolled back */
    FAILED
}
