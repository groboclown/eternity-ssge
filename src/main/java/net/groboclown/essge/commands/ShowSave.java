package net.groboclown.essge.commands;

import net.groboclown.essge.UnpackedSaveGame;
import net.groboclown.essge.commands.util.Helpers;
import net.groboclown.essge.filetypes.MobileObjects;
import net.groboclown.essge.filetypes.SaveFile;
import net.groboclown.essge.rw.Component;
import net.groboclown.essge.sharp.RootProperty;
import net.groboclown.essge.sharp.serializer.TypePair;
import net.groboclown.essge.sharp.serializer.properties.*;

import java.io.File;
import java.util.*;

public class ShowSave implements Cmd {
    public static final ShowSave INSTANCE = new ShowSave();

    private final static String[] FILE_ARGS = {"-f"};
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
        return "Show exhaustive details of a saved game file.  Saved game files " +
                "are in the user configuration directory, and end with '.savegame'.  " +
                "Use the 'list-saves' to find a save game to inspect.  This list outputs an " +
                "*excessive* amount of data, so you probably want to pipe this into a file:\n" +
                "  'pessge show-save -d SAVE_GAME_FILE > the-save.txt'\n" +
                "Arguments:\n" +
                "  -f SAVE_GAME_FILE\n" +
                "    (required) the save game file to inspect.";
    }

    @Override
    public int run(Out o, Map<String, File> files, Map<String, String> strs, Set<String> options, List<String> args) throws Exception {
        if (Helpers.isBadExtraArgs(o, args)) {
            return 1;
        }
        File file = Helpers.getFileArg(o, files);
        if (file == null) {
            return 1;
        }
        final SaveFile save = new SaveFile(file);
        try (SaveFile.Opened opened = save.open(false)) {
            UnpackedSaveGame contents = opened.getSave();
            MobileObjects objects = contents.readMobileObjects();
            o.pLn("Roots:");
            for (RootProperty root: objects.getRoots()) {
                o.pLn("- ComponentPackets:");
                for (Component comp: root.getComponents()) {
                    showProperty(o.sub("  - ", "    "), comp.getLowProperty());
                }
            }
        }
        return 0;
    }

    static void showProperty(Out o, Property property) {
        switch (property) {
            case null -> throw new IllegalArgumentException("invalid null property!");
            case NullProperty nullProperty -> {
                showNullProperty(o, nullProperty);
                return;
            }
            case SimpleProperty simpleProperty -> {
                showSimpleProperty(o, simpleProperty);
                return;
            }
            default -> {
            }
        }

        if (showReference(o, (ReferenceTargetProperty) property)) {
            return;
        }

        showReferenceTarget(o, (ReferenceTargetProperty) property);
    }

    private static void showReferenceTarget(Out o, ReferenceTargetProperty property) {
        o.pLn(property.name + ":");
        Out next = o.sub("  ");
        switch (property) {
            case MultiDimensionalArrayProperty multiDimensionalArrayProperty -> {
                showMultiDimensionalArrayProperty(next, multiDimensionalArrayProperty);
            }
            case SingleDimensionalArrayProperty singleDimensionalArrayProperty -> {
                showSingleDimensionalArrayProperty(next, singleDimensionalArrayProperty);
            }
            case DictionaryProperty dictionaryProperty -> {
                showDictionaryProperty(next, dictionaryProperty);
            }
            case CollectionProperty collectionProperty -> {
                showCollectionProperty(next, collectionProperty);
            }
            case ComplexProperty complexProperty -> {
                showComplexProperty(next, complexProperty);
            }
            default -> {
                next.pLn("  - (unknown)");
            }
        }

    }

    private static void showMultiDimensionalArrayProperty(Out o, MultiDimensionalArrayProperty property) {
        o.pLn("Dim:");
        StringBuilder dims = new StringBuilder();
        boolean first = true;
        for (MultiDimensionalArrayProperty.ArrayDimension dim : property.dimensions) {
            if (first) {
                first = false;
            } else {
                dims.append(" x ");
            }
            dims.append(dim.length);
        }
        o.pLn("  dimensions: " + dims);
        if (property.items.isEmpty()) {
            o.pLn("  items: []");
        } else {
            o.pLn("  items:");
            Out next = o.sub("  - ", "    ");
            for (MultiDimensionalArrayProperty.MultiDimensionalArrayItem item : property.items) {
                showProperty(next, item.value);
            }
        }
    }

    private static void showSingleDimensionalArrayProperty(Out o, SingleDimensionalArrayProperty property) {
        o.pLn("Array:");
        o.pLn("  itemType: " + typeName(property.elementType));
        if (property.items.isEmpty()) {
            o.pLn("  items: []");
        } else {
            o.pLn("  items:");
            Out next = o.sub("  - ", "    ");
            for (Property prop : property.items) {
                showProperty(next, prop);
            }
        }
    }

    private static void showDictionaryProperty(Out o, DictionaryProperty property) {
        o.pLn("Dictionary:");
        o.pLn("  keyType: " + typeName(property.keyType));
        o.pLn("  valueType: " + typeName(property.valueType));
        if (property.properties.isEmpty()) {
            o.pLn("  properties: []");
        } else {
            o.pLn("  properties:");
            Out next = o.sub("  - ", "    ");
            for (Property prop : property.properties) {
                showProperty(next, prop);
            }
        }
        if (property.items.isEmpty()) {
            o.pLn("  entries: []");
        } else {
            o.pLn("  entries:");
            Out next = o.sub("      ", "      ");
            for (Map.Entry<Property, Property> item : property.items) {
                o.pLn("  - key:");
                showProperty(next, item.getKey());
                o.pLn("    value:");
                showProperty(next, item.getValue());
            }
        }
    }

    private static void showCollectionProperty(Out o, CollectionProperty property) {
        o.pLn(typeName(property.elementType) + ":");
        if (property.properties.isEmpty()) {
            o.pLn("  properties: []");
        } else {
            o.pLn("  properties:");
            Out next = o.sub("  - ", "    ");
            for (Property prop : property.properties) {
                showProperty(next, prop);
            }
        }
        if (property.items.isEmpty()) {
            o.pLn("  items: []");
        } else {
            o.pLn("  items:");
            Out next = o.sub("  - ", "    ");
            for (Property prop : property.items) {
                showProperty(next, prop);
            }
        }
    }

    private static void showComplexProperty(Out o, ComplexProperty property) {
        // TODO add in reference target
        o.pLn("ComplexProperty:");
        Out next = o.sub("- ", "  ");
        for (Property prop: property.properties) {
            showProperty(next, prop);
        }
    }

    private static boolean showReference(Out o, ReferenceTargetProperty property) {
        if (property.reference == null) {
            return false;
        }

        if (property.reference.count < 2) {
            // Should also not do this if !property.reference.isProcessed, but that *will* mess up serialization.
            o.pLn(property.name + ": $ref(" + property.reference.id + ")");
            return true;
        }

        return false;
    }

    private static void showSimpleProperty(Out o, SimpleProperty simpleProperty) {
        o.pLn(simpleProperty.name + ": " + simpleProperty.value);
    }

    private static void showNullProperty(Out o, NullProperty nullProperty) {
        o.pLn(nullProperty.name + ": null");
    }

    private static String typeName(TypePair type) {
        if (type == null) {
            return "null";
        }
        if (type.cSharpType == null) {
            return "*null";
        }
        return type.cSharpType;
    }
}
