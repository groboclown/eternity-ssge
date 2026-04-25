package net.groboclown.essge.commands;

import net.groboclown.essge.UnpackedSaveGame;
import net.groboclown.essge.commands.util.Helpers;
import net.groboclown.essge.filetypes.MobileObjects;
import net.groboclown.essge.filetypes.SaveFile;
import net.groboclown.essge.rw.GameCharacter;

import java.io.File;
import java.util.*;

public class ShowStats implements Cmd {
    public static final ShowStats INSTANCE = new ShowStats();

    private final static String[] FILE_ARGS = {"-f"};
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
Show the player's character (PC) and companions' stats.  Any stat shown as a 'String' or 'Integer' or 'Float' can be changed with the 'write player-stat STAT VALUE' command.
Arguments:
  -f SAVE_GAME_FILE
    (required) the save game file to inspect.
  NAME ...
    One or more character names to show stats for.  If no name given, then this shows all characters.
""";
    }

    @Override
    public int run(Out o, Map<String, File> files, Map<String, String> strs, Set<String> options, List<String> args) throws Exception {
        File file = Helpers.getFileArg(o, files);
        if (file == null) {
            return 1;
        }
        final SaveFile save = new SaveFile(file);
        try (SaveFile.Opened opened = save.open(false)) {
            UnpackedSaveGame contents = opened.getSave();
            MobileObjects objects = contents.readMobileObjects();
            final List<GameCharacter> allChars = new ArrayList<>();
            if (args.isEmpty()) {
                allChars.addAll(objects.findAllCharacterObjects());
            } else {
                for (String name: args) {
                    List<GameCharacter> characters = objects.findCharactersNamed(name);
                    if (characters.isEmpty()) {
                        o.pLn("WARNING: no character found named '" + name + "'");
                    }
                    allChars.addAll(characters);
                }
            }
            for (GameCharacter gc: allChars) {
                Optional<GameCharacter.Stats> stats = gc.getStats();
                if (stats.isEmpty()) {
                    continue;
                }
                o.pLn(gc.getObjectName() + ":");
                for (Map.Entry<String, Object> stat: stats.get().getComponent().listObjs().entrySet()) {
                    o.pp("  " + stat.getKey() + ": ");
                    Object val = stat.getValue();
                    if (val == null) {
                        o.pLn("null");
                        continue;
                    }
                    if (val.getClass().isArray()) {
                        o.pLn("(" + val.getClass().getComponentType().getSimpleName() + ")");
                        for (Object item: (Object[]) val) {
                            o.pLn("    - " + item);
                        }
                        continue;
                    }
                    if (val instanceof Integer || val instanceof Float || val instanceof String || val.getClass().isEnum()) {
                        o.pLn(val + " (" + val.getClass().getSimpleName() + ")");
                        continue;
                    }
                    o.pLn("(internal type)");
                }
            }
        }

        return 0;
    }

}
