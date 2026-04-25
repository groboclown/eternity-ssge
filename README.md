# Eternity Simple Save Game Editor

A simple command line tool for editing your characters in Obsidian's Pillars of Eternity game.  It foregoes a fancy user interface, and instead provides some simple ways to inspect and change the save game files.

Tested against Pillars of Eternity version 3.9.3.88783.

This was based on the groundwork by the people who made the phenomenal [Eternity Keeper](https://github.com/ktully/eternity-keeper) project.  Unfortunately, Eternity Keeper has grown a bit stale.  It has many native binary dependencies that make it hard to use.  Additionally, it hasn't kept up with later changes to the game which altered the save game format slightly.

Supported actions:

* [Find your save games](#list-and-describe-the-save-files)
* [Inspect your save games](#show-character-stats)
* [Change character stats and party money](#write-to-save)

Currently not supported actions:

* Modify individual pieces of equipment.
* Add or remove items from the character inventories.
* Any fancy interface.  It's just a text-based command line tool right now.

Because this was based on Eternity Keeper, this project is under the [GPL-3.0-or-later](LICENSE) license.

## Running

You'll need at least Java 21 installed.

You'll need to download the 'all' jar file, or [build it yourself](#building-from-source).  The following instructions assume you downloaded `eternity-ssge-1.1-all.jar` to your home directory, under the name `eternity-ssge.jar`.  If not, you can replace the references to `eternity-ssge.jar` with the actual location.

To run it, you need to open a shell (cmd, PowerShell, xterm, etc), and from there, run:

```shell
java -jar eternity-ssge.jar
```

If that doesn't run, then you may need to find where you installed Java and directly reference that location.  Same for the `eternity-ssge.jar`.

Running the program like that brings up a long help menu.  The below sections show the different commands you can perform with the tool.

If you're really good, you can shorthand that execution with a shell script.

```bash
$JAVA_HOME/bin/java -jar $HOME/Downloads/eternity-ssge-1.1-all.jar "$@"
```

### Guess the user configuration directory

```shell
java -jar eternity-ssge.jar guess-dir
```

This uses some heuristics and standard installation directory names to try to find where the Pillars of Eternity placed its user configuration directory.  This can differ from the install directory, as the configuration directory contains your saved games.

The command may not find the right location, in which case you need to go hunting yourself.  Also, feel free to open a bug on this, so that the project can find ways to improve its guessing attempts.

### List and describe the save files

```shell
java -jar eternity-ssge.jar list-saves
```

Looks in the [best guess](#guess-the-user-configuration-directory) user configuration directory, and gives a short description about each discovered save file.

If the best guess isn't good enough, you may need to provide the user directory explicitly:

```shell
java -jar eternity-ssge.jar list-saves -d PILLARS_USER_DIR
```

The listed save game files includes the full path to the file, which you can use when running other commands that work on a specific save file.

**Arguments**:

* `-d PILLARS_USER_DIR`

  (optional) The 'Pillars of Eternity' user configuration directory which contains the SavedGames directory, or the SavedGames directory itself.  If not given, then the tool guesses; if the guess does not return exactly 1 item, then this fails.

### Find specific save files

```shell
java -jar eternity-ssge.jar \
  find-saves \
  [-d PILLARS_USER_DIR] [-n CHARACTER_NAME] [-t SAVE_TITLE] [-a]
```

Finds the save files that match all the given descriptions.  This prints just the full path to the save game.

This allows for some automation through scripts to discover a saved game and perform changes or inspections it.  When you save a game, the file name can change.  This allows you to create a method to easily use the same save game slot even when the file name changes.

**Arguments:**

* `-d PILLARS_USER_DIR`

  (optional) The 'Pillars of Eternity' user configuration directory which contains the SavedGames directory, or the SavedGames directory itself.  If not given, then the tool guesses; if the guess does not return exactly 1 item, then this fails.
* `-n CHARACTER_NAME`

  (optional) the primary character's name.
* `-t SAVE_TITLE`

  (optional) the assigned custom name.  Autosaves don't set this.
* `-a`

  (optional) only show autosaves.

### Show Character Stats

```shell
java -jar eternity-ssge.jar show-stats -f SAVE_GAME_FILE
```

Show all the party members' stat names and values.

You can use this as reference in the [write actions](#write-to-save).

**Arguments:**

* `-f SAVE_GAME_FILE`

  (required) the save game file location to inspect.  You can find the save game file locations by running the [list-saves](#list-and-describe-the-save-files) command.

### Show Save Game Contents

```shell
java -jar eternity-ssge.jar show-save -f SAVE_GAME_FILE
```

Show the full, exhaustive details of a save game file.

This outputs an enormous amount of data, especially if the game contains a lot of progress.  To deal with this, you'll probably want to send the output to a file, so you can read and search through the file:

```shell
java -jar eternity-ssge.jar show-save -f SAVE_GAME_FILE > contents.txt
```

**Arguments:**

* `-f SAVE_GAME_FILE`

  (required) the save game file location to inspect.  You can find the save game file locations by running the [list-saves](#list-and-describe-the-save-files) command.

### Write to Save

```shell
java -jar eternity-ssge.jar write -f SAVE_GAME_FILE ACTION ... [/ ACTION ...]
```

Perform a series of write operations on the save game, and write those values back to the save game.

If the write actions cause any error, the command will **not** save the changes.  The command will keep processing the others so you can see if there's other issues with your list of commands. 

Note that it's possible to break your game in weird ways with this command, so take care.  This will overwrite your save game, so you may want to create a backup of it, so you don't lose your progress!

An example of backing up your save game for Windows:

```shell
copy SAVE_GAME_FILE backup.savegame
java -jar eternity-ssge.jar write -f SAVE_GAME_FILE money 1000
```

... and for POSIX:

```shell
cp SAVE_GAME_FILE backup.savegame
java -jar eternity-ssge.jar write -f SAVE_GAME_FILE -@ set-money.txt
```

**Arguments:**

* `-f SAVE_GAME_FILE`

  (required) the save game file location you wish to alter.  You can find the save game file locations by running the [list-saves](#list-and-describe-the-save-files) command.

* `-@ ACTION_FILE`

  (optional) Allows you to specify the actions in a plain (UTF-8 encoded) text file, rather than on the command line.  The program will count line breaks as spaces, to make it easy to read.  You can find examples of this in the [`examples`](examples) directory.

* `ACTION ...`

  A list of actions to perform, separated by a slash (`/`).  This allows you to perform multiple changes to the contents in a single write operation.

  For example: `player-stat BaseMight 14 / player-stat LoreSkill 4` will set the main character's might to 14 (before bonuses) and lore skill to 4.

**Actions:**

* `# any text`

  Ignored.  Acts as a comment.  Note that this still needs proper `/` separation, so to insert a comment, it would look like `/ # Make me poor / money 0`

* `money VALUE`

  Set the party's available currency to the given VALUE.  VALUE must be a simple number (0, 100, 2553) (a "non-negative integer").

* `player-stat STAT VALUE`

  Set the primary character's stat to the given value.  The STAT name is case sensitive, meaning that `level` will not work, but `Level` will. 
  
  Use the [show-stats](#show-character-stats) command 
Any name with a `(String)`, `(Float)`, or `(Integer)` after it means you can change that value.  "String" means free-form text; it has a maximum length, but you'll have to figure that out yourself if you want to be adventurous.  "Float" means a number with a possible decimal point (4, 5.321, -10.5).  "Integer" means a number without a decimal point.  Each stat may have its own value requirements (minimum, maximum), but this action does not perform those checks.

* `all-companions-stat STAT VALUE`

  Similar to `player-stat`, but updates every companion character in the save game.



## Building from Source

The program uses Gradle to build.  The source comes with the `gradlew` wrapper, so you don't need to download the right version of Gradle yourself.

* On POSIX systems:
  ```shell
  cd ROOT_DIRECTORY
  ./gradlew 
  ```
* On Windows:
  ```shell
  cd ROOT_DIRECTORY
  gradlew
  ```

To build the stand-alone "fat" jar (one jar containing the compiled files + dependencies all in one):

```shell
gradlew build
```

This will create the distribution jar file under `build/libs/eternity-ssge-${version}.jar`.


## Acknowledgements

This work could not have been made without the amazing effort of the [Eternity Keeper](https://github.com/ktully/eternity-keeper) project.  See the [CONTRIBUTORS](CONTRIBUTORS) file for the list of people who worked to bring this project home.

Pillars of Eternity was developed by Obsidian Entertainment, and published by Paradox Interactive.  This project is not endorsed by them, and claims no ownership over anything they built.

## License

Because the original Eternity Trapper project is under the [GPL-3.0-or-later license](LICENSE), so is this one.
