package net.groboclown.essge.sharp.errors;

public class SubPropertyNotFoundException extends SharpException {
    private final String name;

    public SubPropertyNotFoundException(final String name) {
        super("sub-property not found: '" + name + "'");
        this.name = name;
    }

    public String getSubProperty() {
        return name;
    }
}
