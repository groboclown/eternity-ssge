package net.groboclown.essge.filetypes;

import java.io.File;
import java.util.*;

public class InstallLocation {
    public static final String SAVED_GAMES_DIR = "SavedGames";

    private static final String[] INSTALLATION_LOCATIONS = {
        "Program Files\\GOG Games\\Pillars of Eternity",
        "Program Files (x86)\\GOG Games\\Pillars of Eternity",
        "Program Files\\Steam\\SteamApps\\common\\Pillars of Eternity",
        "Program Files (x86)\\Steam\\SteamApps\\common\\Pillars of Eternity",

        // Microsoft Store/GamePass installs mount a hidden xbox volume for each game
        // A custom kernel driver (gameflt) prevents most access attempts
        // This is a sample mount point which may differ across PCs or game versions
        // "Program Files\WindowsApps\ParadoxInteractive.PillarsofEternity-MicrosoftStor_1.2.6.0_x64__zfnrdv2de78ny\",
    };

    private static final String[] ENV_PROGRAM_FILES = {
        "ProgramFiles", "ProgramFiles(x86)", "ProgramW6432"
    };

    private static final String[] POE_SUB_PATHS = {
        "PillarsOfEternity",
        "Pillars of Eternity",
        "GOG Games/Pillars of Eternity",
        "SteamApps/common/Pillars of Eternity",
    };

    public static Optional<File> getSavedGamesDir(final File userConfig) {
        if (userConfig == null || !userConfig.isDirectory()) {
            return Optional.empty();
        }
        if (userConfig.getName().equalsIgnoreCase(SAVED_GAMES_DIR)) {
            return Optional.of(userConfig);
        }
        Optional<File> opt = asSubDir(userConfig, SAVED_GAMES_DIR);
        if (opt.isPresent()) {
            return opt;
        }
        // Check if there's any savegame file in this directory.
        File[] items = userConfig.listFiles();
        if (items != null) {
            for (File f: items) {
                if (f.getName().endsWith(".savegame")) {
                    return Optional.of(userConfig);
                }
            }
        }
        return Optional.empty();
    }

    public static List<File> guessUserConfigLocation() {
        final List<File> guesses = new ArrayList<>();
        for (File loc: getGeneralDirs()) {
            for (String sub: POE_SUB_PATHS) {
                File f = new File(loc, sub);
                if (!f.exists() || !f.isDirectory()) {
                    continue;
                }
                File d = new File(f, SAVED_GAMES_DIR);
                if (d.exists() && d.isDirectory()) {
                    guesses.add(f);
                }
            }
        }
        return guesses;
    }

    static Set<File> getGeneralDirs() {
        final Set<File> ret = new HashSet<>();
        ret.addAll(getLocalDataDirs());
        ret.addAll(getProgramFilesDirs());
        return ret;
    }

    static Set<File> getLocalDataDirs() {
        final Set<File> ret = new HashSet<>();
        if (isWindows()) {
            String appData = System.getenv("LOCALAPPDATA");
            if (appData == null || appData.isEmpty()) {
                // Use the HOMEDRIVE + HOMEPATH to derive it.
                String homeDrive = System.getenv("HOMEDRIVE");
                String homePath = System.getenv("HOMEPATH");
                if (homeDrive == null || homePath == null || homeDrive.isEmpty() || homePath.isEmpty()) {
                    return Collections.emptySet();
                }
                appData = homeDrive + homePath + "\\AppData\\Local";
            }
            asDir(appData).ifPresent(ret::add);
            File local = new File(appData);
            File appFile = local.getParentFile();
            asSubDir(appFile, "Roaming").ifPresent(ret::add);
            asSubDir(appFile, "LocalLow").ifPresent(ret::add);
        } else {
            // Some Free Desktop stuff.
            // See https://specifications.freedesktop.org/basedir/latest/
            asDir(System.getenv("XDG_DATA_HOME")).ifPresent(ret::add);
            getHomeDir().flatMap(f -> asSubDir(f, "share")).ifPresent(ret::add);
            getHomeDir().flatMap(f -> asSubDir(f, ".local")).ifPresent(ret::add);
            getHomeDir().flatMap(f -> asSubDir(f, ".local", "share")).ifPresent(ret::add);
        }
        return ret;
    }

    static Set<File> getProgramFilesDirs() {
        if (!isWindows()) {
            return Collections.emptySet();
        }
        final Set<File> ret = new HashSet<>();
        for (String name: ENV_PROGRAM_FILES) {
            asDir(System.getenv(name)).ifPresent(ret::add);
        }
        return ret;
    }

    static boolean isWindows () {
        return getOperatingSystem().toLowerCase().contains("wind");
    }

    static String getOperatingSystem() {
        String ret = System.getProperty("os.name");
        if (ret == null || ret.isEmpty()) {
            return "unknown";
        }
        return ret;
    }

    static Optional<File> getHomeDir() {
        return asDir(System.getProperty("user.home"));
    }

    private static Optional<File> asDir(String path) {
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        File f = new File(path);
        if (f.exists() && f.isDirectory()) {
            return Optional.of(f);
        }
        return Optional.empty();
    }

    static Optional<File> asSubDir(final File parent, final String... paths) {
        File ret = parent;
        for (String path: paths) {
            if (path == null || path.isEmpty()) {
                return Optional.empty();
            }
            ret = new File(ret, path);
            if (!ret.exists() || !ret.isDirectory()) {
                return Optional.empty();
            }
        }
        return Optional.of(ret);
    }

    static String detectPlatform () {
        if (isWindows()) {
            if (System.getenv("ProgramFiles(x86)") == null) {
                return "win32";
            } else {
                return "win64";
            }
        } else {
            return "linux64";
        }
    }

}
