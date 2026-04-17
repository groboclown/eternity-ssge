package net.groboclown.essge.sharp.errors;

public class ReferenceMismatchException extends SharpException {
    private final Class<?> from;
    private final String into;

    public ReferenceMismatchException(Object from, String into) {
        super("Tried to make " + into + " a flat copy of " + from.getClass().getSimpleName());
        this.from = from.getClass();
        this.into = into;
    }

    public Class<?> getFrom() {
        return from;
    }

    public String getInto() {
        return into;
    }
}
