package net.groboclown.essge.filetypes;

import net.groboclown.essge.UnpackedSaveGame;
import net.groboclown.essge.rw.GameCharacter;
import net.groboclown.essge.sharp.errors.SharpException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class SaveFileTest {
    @Test
    void readFile() throws IOException, SharpException {
        File tmp = toTmpFile("example1.savegame");
        try {
            SaveFile saveFile = new SaveFile(tmp);
            try (SaveFile.Opened opened = saveFile.open(false)) {
                UnpackedSaveGame save = opened.getSave();
                MobileObjects mobile = save.readMobileObjects();
                int players = 0;
                for (GameCharacter gc: mobile.findPlayerObjects()) {
                    players++;
                    assertEquals(100.0f, gc.getCurrencyTotalValue());
                }
                assertEquals(1, players);
            }
        } finally {
            tmp.delete();
        }
    }

    @Test
    void saveChanges() throws IOException, SharpException {
        File tmp = toTmpFile("example1.savegame");
        try {
            SaveFile saveFile = new SaveFile(tmp);
            try (SaveFile.Opened opened = saveFile.open(true)) {
                UnpackedSaveGame save = opened.getSave();
                MobileObjects mobile = save.readMobileObjects();
                int players = 0;
                for (GameCharacter gc: mobile.findPlayerObjects()) {
                    players++;
                    gc.setCurrencyTotalValue(9.0f);
                    GameCharacter.Stats stats = gc.getStats().orElseThrow();
                    assertTrue(stats.setBaseStat("Might", 11));
                    assertEquals(11, stats.getBaseStat("Might"));
                    assertTrue(stats.setBaseStat("Resolve", 18));
                    assertEquals(18, stats.getBaseStat("Resolve"));
                }
                assertEquals(1, players);
                save.writeMobileObject(mobile);
            }

            // Ensure it saved with changes.
            try (SaveFile.Opened opened = saveFile.open(false)) {
                UnpackedSaveGame save = opened.getSave();
                MobileObjects mobile = save.readMobileObjects();
                int players = 0;
                for (GameCharacter gc: mobile.findPlayerObjects()) {
                    players++;
                    assertEquals(9.0f, gc.getCurrencyTotalValue());
                    GameCharacter.Stats stats = gc.getStats().orElseThrow();
                    assertEquals(11, stats.getBaseStat("Might"));
                    assertEquals(18, stats.getBaseStat("Resolve"));
                }
                assertEquals(1, players);
            }

            // Ensure it matches the other saved game.
        } finally {
            tmp.delete();
        }
    }

    private static File toTmpFile(String resource) throws IOException {
        File ret = File.createTempFile("savegame", ".savegame");
        try (FileOutputStream out = new FileOutputStream(ret)) {
            try (InputStream inp = SaveFileTest.class.getResourceAsStream(resource)) {
                inp.transferTo(out);
            }
        } catch (Exception e) {
            ret.delete();
            throw new RuntimeException(e);
        }
        return ret;
    }
}
