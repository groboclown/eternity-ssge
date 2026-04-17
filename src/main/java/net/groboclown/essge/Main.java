package net.groboclown.essge;

import net.groboclown.essge.commands.*;
import net.groboclown.essge.sharp.errors.SharpException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    static final NC[] CMDS = {
            NC.as(GuessConfigDir.INSTANCE, "guess-dir", "guess-config-dir"),
            NC.as(ListSaves.INSTANCE, "list-saves"),
            NC.as(FindSaves.INSTANCE, "find-saves"),
            NC.as(ShowStats.INSTANCE, "show-stats", "show-save-stats"),
            NC.as(ShowSave.INSTANCE, "show-save"),
            NC.as(WriteAction.INSTANCE, "write"),
    };


    public static void main(final String[] args) throws IOException, SharpException {
        List<String> argList = new ArrayList<>(List.of(args));
        if (argList.isEmpty() || argList.contains("-h") || argList.contains("--help")) {
            Out.n(
                    """
                            Pillars of Eternity Simple Save Game Editor
                            Usage: eternity-ssge COMMAND [ARGS ...]
                            
                            Commands:
                            """);
            for (NC cmd : CMDS) {
                Out.n("  " + cmd.names[0]);
                for (String line : Out.wrapText(cmd.cmd.help(), 70)) {
                    Out.n("    " + line);
                }
            }
            return;
        }

        String cmdName = argList.removeFirst();
        for (NC cmd : CMDS) {
            boolean matches = false;
            for (String name : cmd.names) {
                if (name.equals(cmdName)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                continue;
            }
            List<String> knownFiles = List.of(cmd.cmd.getFileArgs());
            Map<String, File> files = new HashMap<>();

            List<String> knownStrs = List.of(cmd.cmd.getStrArgs());
            Map<String, String> strs = new HashMap<>();

            List<String> knownOptions = List.of(cmd.cmd.getBoolArgs());
            Set<String> options = new HashSet<>();

            List<String> positional = new ArrayList<>();
            for (int i = 0; i < argList.size(); i++) {
                String arg = argList.get(i);
                if (knownFiles.contains(arg)) {
                    i++;
                    if (i >= argList.size()) {
                        Out.n("ERROR: " + arg + " requires a value");
                        System.exit(1);
                    }
                    files.put(arg, new File(argList.get(i)));
                    continue;
                }
                if (knownStrs.contains(arg)) {
                    i++;
                    if (i >= argList.size()) {
                        Out.n("ERROR: " + arg + " requires a value");
                        System.exit(1);
                    }
                    strs.put(arg, argList.get(i));
                    continue;
                }
                if (knownOptions.contains(arg)) {
                    options.add(arg);
                    continue;
                }
                positional.add(arg);
            }
            int ret;
            try {
                // Out.n("Running command " + cmd.names[0]);
                ret = cmd.cmd.run(new Out(), files, strs, options, positional);
            } catch (Exception e) {
                e.printStackTrace();
                ret = 1;
            }
            System.exit(ret);
        }

        System.err.println("Unknown command: " + cmdName);
        System.exit(2);
    }

    static class NC {
        final String[] names;
        final Cmd cmd;


        NC(String[] names, Cmd cmd) {
            this.names = names;
            this.cmd = cmd;
        }

        static NC as(Cmd cmd, String... names) {
            return new NC(names, cmd);
        }
    }
}
