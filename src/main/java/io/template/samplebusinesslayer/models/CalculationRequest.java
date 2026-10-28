package io.template.samplebusinesslayer.models;

/**
 * Request for a calculation operation.
 *
 * @param operandA First operand
 * @param operandB Second operand
 * @param operation Operation to perform (ADD, SUBTRACT, MULTIPLY, DIVIDE)
 */
public record CalculationRequest(
        double operandA,
        double operandB,
        String operation
) { }
