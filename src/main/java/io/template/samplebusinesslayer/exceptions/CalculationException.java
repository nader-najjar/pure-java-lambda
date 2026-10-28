package io.template.samplebusinesslayer.exceptions;

/**
 * Exception thrown when calculation fails.
 */
public class CalculationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CalculationException(String message) {
        super(message);
    }
}
