package net.groboclown.essge.sharp.errors;

public class UnknownReferenceException extends SharpException {
    private final int refId;

    public UnknownReferenceException(final int refId) {
        super("unknown reference ID " + refId);
        this.refId = refId;
    }

    public int getRefId() {
        return refId;
    }
}
