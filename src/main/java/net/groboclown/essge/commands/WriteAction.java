package net.groboclown.essge.commands;

import net.groboclown.essge.Main;
import net.groboclown.essge.UnpackedSaveGame;
import net.groboclown.essge.commands.util.Helpers;
import net.groboclown.essge.filetypes.SaveFile;
import net.groboclown.essge.rw.GameCharacter;
import net.groboclown.essge.filetypes.MobileObjects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class WriteAction implements Cmd {
    public static final WriteAction INSTANCE = new WriteAction();

    private final static String[] FILE_ARGS = {"-f", "-@"};
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
Perform a series of write actions on a save game.  This will overwrite the save game, so back it up (copy it to another location or name) to ensure you don't lose your data!

Format:
  write -f SAVE_GAME_FILE ACTION ... [/ ACTION ...]
Arguments:
  -f SAVE_GAME_FILE
    (required) the save game file to inspect.
  -@ ACTION_LIST_FILE
    (optional) a file to replace the ACTION arguments.  Newline
    characters are treated as spaces.
  ACTION ...
    An action name followed by its positional parameters.  See the
    list of actions below.
  /
    Separates actions, so that you can chain multiple actions within
    the same execution.
Actions:
  player-stat STAT VALUE
    Set the main player character's stat STAT to the given VALUE.
    For a complete list of stats you can set, see the 'show-stats'
    command.
  all-companions-stat STAT VALUE
    Set every companion's stat STAT to the given VALUE.
  character-stat NAME STAT VALUE
    Set the character named NAME stat STAT to the VALUE.
  money VALUE
    Set the party's money.
  # any value
    Comment.
""";
    }

    @Override
    public int run(Out o, Map<String, File> files, Map<String, String> strs, Set<String> options, List<String> args) throws Exception {
        File file = Helpers.getFileArg(o, files);
        if (file == null) {
            return 1;
        }

        List<String> allArgs = new ArrayList<>(args);
        allArgs.addAll(Helpers.readArgFile(files.get("-@")));

        final SaveFile save = new SaveFile(file);
        int failures = 0;
        try (SaveFile.Opened opened = save.open(true)) {
            UnpackedSaveGame contents = opened.getSave();
            MobileObjects mobile = contents.readMobileObjects();

            for (List<String> group : Helpers.splitArgGroups(allArgs)) {
                o.pLn("Handling action: " + String.join(" ", group));

                // group is required to have at least 1 element.
                String cmd = group.getFirst();
                if ("player-stat".equals(cmd)) {
                    if (Helpers.isNotSized(o, group, 3, 3)) {
                        opened.setPersistent(false);
                        failures++;
                        continue;
                    }
                    setPlayerStat(o, mobile, group.get(1), group.get(2));
                    continue;
                }
                if ("all-companions-stat".equals(cmd)) {
                    if (Helpers.isNotSized(o, group, 3, 3)) {
                        opened.setPersistent(false);
                        failures++;
                        continue;
                    }
                    setAllCompanionsStat(o, mobile, group.get(1), group.get(2));
                    continue;
                }
                if ("character-stat".equals(cmd)) {
                    if (Helpers.isNotSized(o, group, 4, 4)) {
                        opened.setPersistent(false);
                        failures++;
                        continue;
                    }
                    if (!setNamedCharacterStat(o, mobile, group.get(1), group.get(2), group.get(3))) {
                        opened.setPersistent(false);
                        failures++;
                    }
                    continue;
                }
                if ("money".equals(cmd) || "gold".equals(cmd)) {
                    if (Helpers.isNotSized(o, group, 2, 2)) {
                        opened.setPersistent(false);
                        failures++;
                        continue;
                    }
                    if (!setMoney(o, mobile, group.get(1))) {
                        opened.setPersistent(false);
                        failures++;
                        continue;
                    }
                    continue;
                }
                if ("#".equals(cmd)) {
                    continue;
                }

                o.pLn("ERROR: unknown write action " + cmd);
                opened.setPersistent(false);
                failures++;
            }

            // This will save to the temporary location, but, on failure,
            // persistent is false, so the final .savegame zip isn't overwritten
            // from the temporary location.
            contents.writeMobileObject(mobile);
        }

        if (failures > 0) {
            o.pLn("ERROR: encountered " + failures + " errors");
        } else {
            o.pLn("SUCCESS");
        }

        return failures;
    }

    public static void setPlayerStat(Out o, MobileObjects mobile, String name, String value) {
        for (GameCharacter gc: mobile.findPlayerObjects()) {
            setCharacterStat(o, gc, name, value);
        }
    }

    public static void setAllCompanionsStat(Out o, MobileObjects mobile, String name, String value) {
        for (GameCharacter gc: mobile.findCompanionObjects()) {
            setCharacterStat(o, gc, name, value);
        }
    }

    public static boolean setNamedCharacterStat(Out o, MobileObjects mobile, String charName, String statName, String value) {
        List<GameCharacter> characters = mobile.findCharactersNamed(charName);
        if (characters.isEmpty()) {
            o.pLn("ERROR: could not find character named '" + charName + "'");
            return false;
        }
        for (GameCharacter gc: characters) {
            setCharacterStat(o, gc, statName, value);
        }
        return true;
    }

    public static boolean setMoney(Out o, MobileObjects mobile, String value) {
        float money;
        try {
            money = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            o.pLn("ERROR: invalid number for setting the party money: '" + value + "'");
            return false;
        }
        boolean found = false;
        for (GameCharacter gc: mobile.findPlayerObjects()) {
            found = found || gc.setCurrencyTotalValue(money);
        }
        return found;
    }

    static void setCharacterStat(Out o, GameCharacter gc, String name, String value) {
        Optional<GameCharacter.Stats> stats = gc.getStats();
        if (stats.isEmpty()) {
            return;
        }
        Object orig = stats.get().getValue(name);
        if (orig == null) {
            o.pLn("- stat '" + name + "' not known");
            return;
        }
        Object newVal;
        if (orig instanceof Integer) {
            newVal = Integer.parseInt(value);
        } else if (orig instanceof Float) {
            newVal = Float.parseFloat(value);
        } else if (orig instanceof Boolean) {
            newVal = value.equalsIgnoreCase("true");
        } else if (orig instanceof String) {
            newVal = value;
        } else {
            o.pLn("- stat '" + name + "' cannot be set; type is " + orig.getClass().getSimpleName());
            return;
        }
        if (stats.get().setValue(name, newVal)) {
            o.pLn("- stat '" + name + "': " + orig + " -> " + newVal);
        } else {
            o.pLn("- stat '" + name + "': could not change from " + orig + " to " + newVal);
        }
    }

}
