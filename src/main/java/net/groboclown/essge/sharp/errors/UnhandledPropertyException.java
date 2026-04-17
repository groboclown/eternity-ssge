package net.groboclown.essge.sharp.errors;

public class UnhandledPropertyException extends SharpException {
    public UnhandledPropertyException(final String name) {
        super("unhandled property type '" + name + "'");
    }
}
