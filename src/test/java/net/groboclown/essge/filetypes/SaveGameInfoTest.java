package net.groboclown.essge.filetypes;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;


class SaveGameInfoTest {
    @Test
    void readFile() throws ParserConfigurationException, IOException, SAXException {
        final InputStream resource = getClass().getResourceAsStream("saveinfo.xml");
        final SaveGameInfo data = SaveGameInfo.parseXml(resource);
        assertEquals("04/10/2026 13:45:14", data.saveTime());
    }
}
