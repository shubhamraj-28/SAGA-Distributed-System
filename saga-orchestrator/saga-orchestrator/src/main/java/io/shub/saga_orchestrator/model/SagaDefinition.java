package io.shub.saga_orchestrator.model;

import java.util.List;

/**
 * Defines a complete saga as an ordered sequence of steps.
 *
 * @param sagaType unique name for this saga type (e.g., "ORDER_SAGA")
 * @param steps    ordered list of steps to execute
 */
public record SagaDefinition(
        String sagaType,
        List<SagaStep> steps
) {
    /**
     * Get the total number of steps in this saga.
     */
    public int totalSteps() {
        return steps.size();
    }

    /**
     * Get the step at the given index.
     */
    public SagaStep getStep(int index) {
        return steps.get(index);
    }
}
