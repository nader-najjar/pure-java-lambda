package io.template.samplebusinesslayer.models;

/**
 * Result of a calculation operation.
 *
 * @param result The calculated result
 * @param operation The operation performed
 */
public record CalculationResult(
        double result,
        String operation
) { }
