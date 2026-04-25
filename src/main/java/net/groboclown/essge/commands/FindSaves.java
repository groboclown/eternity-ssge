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

public class FindSaves implements Cmd {
    public static final FindSaves INSTANCE = new FindSaves();

    private final static String[] FILE_ARGS = {"-d"};
    private final static String[] STR_ARGS = {"-n", "-t"};
    private final static String[] BOOL_ARGS = {"-a"};

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
Find the save file matching the description.
Arguments:
  -d PILLARS_DIR
    (optional) the 'Pillars of Eternity' user configuration directory
    which contains the SavedGames directory, or the SavedGames
    directory itself.  If not given, then the tool guesses; if the guess
    does not return exactly 1 item, then this fails.
  -n CHARACTER_NAME
    (optional) the primary character's name.
  -t SAVE_TITLE
    (optional) the assigned custom name.  Autosaves don't set this.
  -a
    (optional) only show autosaves.
""";
    }

    @Override
    public int run(Out o, Map<String, File> files, Map<String, String> strs, Set<String> options, List<String> args) throws Exception {
        if (Helpers.isBadExtraArgs(o, args)) {
            return 1;
        }
        String nameMatch = strs.get("-n");
        String titleMatch = strs.get("-t");
        boolean requireAutosaves = options.contains("-a");
        Optional<File> saveDir = Helpers.getOptionSaveGameLocation(o, files);
        if (saveDir.isEmpty()) {
            // Already reported the errors.
            return 1;
        }

        for (SaveFile file: SaveFile.readFromSaveDir(saveDir.get())) {
            final SaveGameInfo info;
            try {
                info = file.readSaveGameInfo();
            } catch (IOException | SAXException | ParserConfigurationException e) {
                // No error reporting?
                continue;
            }

            // Filter operation needs improvement
            // AND the matches together.
            boolean match = false;
            if (nameMatch != null) {
                match = nameMatch.equals(info.playerName());
                if (!match) {
                    continue;
                }
            }
            if (requireAutosaves) {
                match = info.userSaveName() == null || info.userSaveName().isEmpty();
                if (!match) {
                    continue;
                }
            } else if (titleMatch != null) {
                match = titleMatch.equals(info.userSaveName());
                if (!match) {
                    continue;
                }
            }

            if (match) {
                o.pLn(file.getFile().getCanonicalPath());
            }
        }

        return 0;
    }
}
