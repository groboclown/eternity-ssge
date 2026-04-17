package net.groboclown.essge.commands.util;

import net.groboclown.essge.commands.Out;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Helpers {
    public static File getFileArg(Out o, Map<String, File> files) {
        File file = files.get("-f");
        if (file == null) {
            o.pLn("ERROR: You must specify the '-f path/to/file.savegame' argument");
            return null;
        }
        if (!file.exists() || file.isDirectory()) {
            // Might be a link, which means file.isFile() fails.
            o.pLn("ERROR: savegame file '" + file + "' does not exist or is not a file.");
            return null;
        }
        return file;
    }

    public static boolean isBadExtraArgs(Out o, List<String> args) {
        if (!args.isEmpty()) {
            o.pLn("ERROR: command does not accept positional arguments.");
            return true;
        }
        return false;
    }

    public static List<List<String>> splitArgGroups(List<String> args) {
        List<List<String>> ret = new ArrayList<>();
        List<String> next = new ArrayList<>();
        for (String arg: args) {
            if ("/".equals(arg)) {
                if (!next.isEmpty()) {
                    ret.add(next);
                    next = new ArrayList<>();
                }
            } else {
                next.add(arg);
            }
        }
        if (!next.isEmpty()) {
            ret.add(next);
        }
        return ret;
    }

    public static boolean isNotSized(Out o, List<String> group, int min, int max) {
        if (group.size() < min || group.size() > max) {
            o.pLn("ERROR: group '" + String.join(" ", group) + "'");
            if (min == max) {
                o.pLn("Expected " + min + " arguments");
            } else if (max < 1000) {
                o.pLn("Expected at least " + min + " arguments");
            } else {
                o.pLn("Expected " + min + " to " + max + " arguments");
            }
            return true;
        }
        return false;
    }

    public static List<String> readArgFile(File file) throws IOException {
        if (file == null) {
            return List.of();
        }
        try (FileReader in = new FileReader(file)) {
            return readArgFromReader(in);
        }
    }

    public static List<String> readArgFromReader(Reader in) throws IOException {
        List<String> ret = new ArrayList<>();
        // Very simple shell-like argument splitter.
        char inQuote = 0;
        boolean onEscape = false;
        boolean onWhitespace = true;
        boolean lastIncludedQuote = false;
        StringBuilder remainder = new StringBuilder();
        char[] buff = new char[4096];
        int len;
        while ((len = in.read(buff)) > 0) {
            for (int p = 0; p < len; p++) {
                char c = buff[p];
                if (onEscape) {
                    // TODO add \n, \r, \t, etc.
                    remainder.append(c);
                    onEscape = false;
                    onWhitespace = false;
                    continue;
                }
                if (c == '\\') {
                    onEscape = true;
                    continue;
                }
                if (inQuote == 0) {
                    if (c == '"' || c == '\'') {
                        onWhitespace = false;
                        lastIncludedQuote = true;
                        inQuote = c;
                        continue;
                    }
                    if (Character.isWhitespace(c)) {
                        if (!onWhitespace && (!remainder.isEmpty() || lastIncludedQuote)) {
                            ret.add(remainder.toString());
                            remainder.setLength(0);
                        }
                        onWhitespace = true;
                        lastIncludedQuote = false;
                        continue;
                    }
                    onWhitespace = false;
                    remainder.append(c);
                    continue;
                }
                if (inQuote == c) {
                    inQuote = 0;
                } else {
                    // inside quote
                    remainder.append(c);
                }
            }
        }
        if (!remainder.isEmpty() || lastIncludedQuote) {
            ret.add(remainder.toString());
        }
        return ret;
    }
}
