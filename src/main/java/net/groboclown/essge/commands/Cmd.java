package net.groboclown.essge.commands;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Cmd {
    /** Command help text.  Will line wrap automatically. */
    String help();

    /**
     * Argument prefix (like "-d") that take a File object.
     * @return array of file argument prefix.
     */
    String[] getFileArgs();

    /**
     * Argument prefix (like "-x") that take a String argument.
     * @return array of string argument prefix.
     */
    String[] getStrArgs();

    /**
     * Allowed stand-alone arguments (like "-v") that do not take arguments.
     * @return array of string arguments.
     */
    String[] getBoolArgs();

    int run(Out o, Map<String, File> files, Map<String, String> strs, Set<String> options, List<String> args) throws Exception;
}
