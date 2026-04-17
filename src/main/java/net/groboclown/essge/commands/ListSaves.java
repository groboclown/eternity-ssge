package net.groboclown.essge.commands;

import net.groboclown.essge.commands.util.Helpers;
import net.groboclown.essge.filetypes.InstallLocation;
import net.groboclown.essge.filetypes.SaveFile;
import net.groboclown.essge.filetypes.SaveGameInfo;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ListSaves implements Cmd {
    public static final ListSaves INSTANCE = new ListSaves();

    private final static String[] FILE_ARGS = {"-d"};
    private final static String[] STR_ARGS = {};
    private final static String[] BOOL_ARGS = {"-v"};

    @Override
    public String[] getFileArgs() {
        return FILE_ARGS;
    }

    @Override
    public String[] getStrArgs() {
        return STR_ARGS;
    }

    @Override
    public String[] getBoolArgs() {
        return BOOL_ARGS;
    }

    @Override
    public String help() {
        return """
List the descriptions of the save files.
Arguments:
  -d PILLARS_DIR
    (optional) the 'Pillars of Eternity' user configuration directory
    which contains the SavedGames directory, or the SavedGames
    directory itself.  If not given, then the tool guesses; if the guess
    does not return exactly 1 item, then this fails.
""";
    }

    @Override
    public int run(Out o, Map<String, File> files, Map<String, String> strs, Set<String> options, List<String> args) throws Exception {
        if (Helpers.isBadExtraArgs(o, args)) {
            return 1;
        }
        File dir = files.get("-d");
        if (dir == null) {
            List<File> guesses = InstallLocation.guessUserConfigLocation();
            if (guesses.isEmpty()) {
                o.pLn("ERROR: could not find any user configuration location.  Pass '-d' with the directory.");
                return 1;
            }
            if (guesses.size() != 1) {
                o.pLn("ERROR: could not determine the user configuration location.  Discovered:");
                for (File g: guesses) {
                    o.psLn(g.getAbsolutePath());
                }
                return 1;
            }
        }
        Optional<File> saveDir = InstallLocation.getSavedGamesDir(dir);
        if (saveDir.isEmpty()) {
            o.pLn("ERROR: '" + dir + "' is not a save game directory, and does not contain a directory named " +
                    InstallLocation.SAVED_GAMES_DIR);
            return 1;
        }

        final boolean verbose = options.contains("-v");

        for (SaveFile file: SaveFile.readFromSaveDir(saveDir.get())) {
            final SaveGameInfo info;
            try {
                info = file.readSaveGameInfo();
            } catch (IOException | SAXException | ParserConfigurationException e) {
                // No error reporting?
                continue;
            }
            if (info.userSaveName() == null || info.userSaveName().isEmpty()) {
                o.pLn(info.playerName() + ": " + file.getFile().getName());
            } else {
                o.pLn(info.playerName() + ": " + info.userSaveName());
            }
            o.pLn("    Chapter " + info.chapter());
            o.pLn("    - Save file: " + file.getFile().getCanonicalFile());
            o.pLn("    - Scene title: " + info.sceneTitle());
            o.pLn("    - Saved at: " + info.saveTime());
            if (verbose) {
                o.pLn("    - Seconds Unpaused: " + info.playTimeSeconds());
                o.pLn("    - Seconds Running: " + info.realTimeSeconds());
                o.pLn("    - Trial of iron? " + info.trialOfIron());
                o.pLn("    - Difficulty: " + info.difficulty());
                o.pLn("    - Complete? " + info.gameComplete());
                o.pLn("    - Active packages: " + info.activePackages());
                o.pLn("    - Tactical mode: " + info.tacticalMode());
            }
        }

        return 0;
    }
}
