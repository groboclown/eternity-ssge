package net.groboclown.essge.sharp.errors;

public class SerializedTypeException extends SharpException {
    private final String key;

    public SerializedTypeException(final String key) {
        super("unknown serialized type '" + key + "'");
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
