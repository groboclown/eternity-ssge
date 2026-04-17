package net.groboclown.essge.commands;

import net.groboclown.essge.commands.util.Helpers;
import net.groboclown.essge.filetypes.InstallLocation;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuessConfigDir implements Cmd {
    public static final GuessConfigDir INSTANCE = new GuessConfigDir();

    private final static String[] FILE_ARGS = {};
    private final static String[] STR_ARGS = {};
    private final static String[] BOOL_ARGS = {};

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
        return "Use some heuristics to try to find the Pillars of Eternity user configuration directory which contains the saved games.";
    }

    @Override
    public int run(Out o, Map<String, File> files, Map<String, String> strs, Set<String> options, List<String> args) throws Exception {
        if (Helpers.isBadExtraArgs(o, args)) {
            return 1;
        }
        int notFound = 1;
        for (File d: InstallLocation.guessUserConfigLocation()) {
            o.pLn(d.getAbsolutePath());
            notFound = 0;
        }
        return notFound;
    }
}
