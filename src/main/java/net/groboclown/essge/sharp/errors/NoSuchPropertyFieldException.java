package net.groboclown.essge.sharp.errors;

public class NoSuchPropertyFieldException extends SharpException {
    private final Class<?> srcClass;
    private final String fieldName;

    public NoSuchPropertyFieldException(Class<?> src, String name) {
        super("class '" + src.getSimpleName() + "' has no such field '" + name + "'");
        srcClass = src;
        fieldName = name;
    }

    public Class<?> getSrcClass() {
        return srcClass;
    }

    public String getFieldName() {
        return fieldName;
    }
}
