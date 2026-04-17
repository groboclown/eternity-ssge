package net.groboclown.essge.filetypes;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public record SaveGameInfo(String playerName, String sceneTitle, int chapter, long realTimeSeconds,
                           long playTimeSeconds, boolean trialOfIron, String saveTime, String difficulty,
                           String activePackages, String tacticalMode, boolean gameComplete, String userSaveName) {

    public static SaveGameInfo parseXml(final InputStream input) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();

        String playerName = "";
        String sceneTitle = "";
        int chapter = 0;
        long realTimeSeconds = 0;
        long playTimeSeconds = 0;
        boolean trialOfIron = false;
        String saveTime = "";
        String difficulty = "";
        String activePackages = "";
        String tacticalMode = "";
        boolean gameComplete = false;
        String userSaveName = "";

        NodeList nodes = doc.getElementsByTagName("Simple");
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);
            final NamedNodeMap attribs = node.getAttributes();
            String name = "";
            String value = "";
            for (int j = 0; j < attribs.getLength(); j++) {
                final Node attr = attribs.item(j);
                if (attr.getNodeName().equals("name")) {
                    name = attr.getNodeValue();
                } else if (attr.getNodeName().equals("value")) {
                    value = attr.getNodeValue();
                }
            }
            if ("PlayerName".equals(name)) {
                playerName = value;
            } else if ("SceneTitle".equals(name)) {
                sceneTitle = value;
            } else if ("Chapter".equals(name)) {
                chapter = Integer.parseInt(value);
            } else if ("RealtimePlayDurationSeconds".equals(name)) {
                realTimeSeconds = Long.parseLong(value);
            } else if ("PlaytimeSeconds".equals(name)) {
                playTimeSeconds = Long.parseLong(value);
            } else if ("TrialOfIron".equals(name)) {
                trialOfIron = "True".equals(value);
            } else if ("RealTimestamp".equals(name)) {
                saveTime = value;
            } else if ("GameComplete".equals(name)) {
                gameComplete = "True".equals(value);
            } else if ("UserSaveName".equals(name)) {
                userSaveName = value;
            } else if ("Difficulty".equals(name)) {
                difficulty = value;
            } else if ("ActivePackages".equals(name)) {
                activePackages = value;
            } else if ("TacticalMode".equals(name)) {
                tacticalMode = value;
            }
        }

        return new SaveGameInfo(
                playerName,
                sceneTitle,
                chapter,
                realTimeSeconds,
                playTimeSeconds,
                trialOfIron,
                saveTime,
                difficulty,
                activePackages,
                tacticalMode,
                gameComplete,
                userSaveName);
    }
}
