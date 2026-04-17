package net.groboclown.essge;

import net.groboclown.essge.filetypes.MobileObjects;
import net.groboclown.essge.sharp.errors.EmptyFileException;
import net.groboclown.essge.sharp.errors.SharpException;
import net.groboclown.essge.sharp.serializer.DeserializedPackets;
import net.groboclown.essge.sharp.serializer.PacketDeserializer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

/**
 * Manages the files in an unpacked save game directory.
 * <p>
 * The save game files are Zip files.  This requires unpacking the files into a separate directory before
 * allowing use.
 */
public class UnpackedSaveGame {
    private static final String SAVE_GAME_INFO_NAME = "saveinfo.xml";
    private static final String MOBILE_OBJECTS_NAME = "MobileObjects.save";


    private final File baseDir;

    public UnpackedSaveGame(final File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Read the save info file, without first parsing it.
     * @return the raw String contents of the file
     * @throws IOException any read error
     */
    public String readRawSaveInfo() throws IOException {
        return FileUtils.readFileToString(new File(this.baseDir, SAVE_GAME_INFO_NAME), StandardCharsets.UTF_8);
    }

    public MobileObjects readMobileObjects() throws IOException, SharpException {
        final File mobileObjectsFile = getMobileObjectsFile();
        final PacketDeserializer deserializer = new PacketDeserializer(mobileObjectsFile);
        final Optional<DeserializedPackets> deserialized = deserializer.deserialize();
        if (deserialized.isEmpty()) {
            throw new EmptyFileException(mobileObjectsFile);
        }
        DeserializedPackets des = deserialized.get();
        return new MobileObjects(des.getPackets(), des.getCount());
    }

    public void writeMobileObject(final MobileObjects objects) throws IOException, SharpException {
        final File mobileObjectsFile = getMobileObjectsFile();
        writeMobileObjectAs(objects, mobileObjectsFile);
    }

    public void writeMobileObjectAs(final MobileObjects objects, File newName) throws IOException, SharpException {
        if (newName.exists() && !newName.delete()) {
            throw new IOException("Unable to remove " + newName.getAbsolutePath());
        }
        Files.createFile(newName.toPath());
        objects.asDeserialized().reserialize(newName);
    }


    private File getMobileObjectsFile() {
        return new File(this.baseDir, MOBILE_OBJECTS_NAME);
    }
}
