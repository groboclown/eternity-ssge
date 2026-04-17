package net.groboclown.essge.sharp.errors;

import java.io.File;

public class EmptyFileException extends SharpException {
    private final File f;
    public EmptyFileException(File f) {
        super("empty file " + f);
        this.f = f;
    }

    public File getFile() {
        return this.f;
    }
}
