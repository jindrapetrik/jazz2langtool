package com.jpexs.jazz2langtool;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author JPEXS
 */
public class Jazz2Language {

    private static String characterEncoding
            = "                                 !\"#$% '()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]∞_`abcdefghijklmnopqrstuvwxyz"
            + "   ~   ‚ „…    Š Œ             š œ  Ÿ ¡ęóąśłżźćńĘÓĄŚŁŻŹĆŃ           "
            + "¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";
    //Note: Character Ó/ó is there twice.

    List<String> text = new ArrayList<>();
    List<Jazz2LevelHelpStrings> levelHelpStrings = new ArrayList<>();

    public Jazz2Language() {

    }

    private byte[] stringToBytes(String str) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == ' ') {
                baos.write(32);
            } else {
                int enc = characterEncoding.indexOf(c);
                if (enc == -1) {
                    System.err.println("WARNING: missing character: \"" + c + "\" , replaced with space - in string " + str);
                }
                baos.write(enc);
            }
        }
        return baos.toByteArray();
    }

    private String bytesToString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int c = data[i] & 0xff;
            sb.append(characterEncoding.charAt(c));
        }

        return sb.toString();
    }

    private void addLevel(String levelName, Map<Integer, String> texts) {
        int minIndex = Integer.MAX_VALUE;
        int maxIndex = Integer.MIN_VALUE;
        for (int i : texts.keySet()) {
            if (i < minIndex) {
                minIndex = i;
            }
            if (i > maxIndex) {
                maxIndex = i;
            }
        }
        if (levelName == null) {
            for (int i = 0; i <= maxIndex; i++) {
                if (texts.containsKey(i)) {
                    this.text.add(texts.get(i));
                } else {
                    this.text.add(""); //MISSING?
                }
            }
        } else {
            Jazz2LevelHelpStrings level = new Jazz2LevelHelpStrings();
            level.startIndex = minIndex; //not sure, probably may be zero
            level.endIndex = maxIndex;
            level.levelName = levelName;

            for (int i : texts.keySet()) {
                level.helpStrings.add(new Jazz2HelpString(i, texts.get(i)));
            }
            levelHelpStrings.add(level);
        }
    }

    public void fromString(String val) {
        BufferedReader br = new BufferedReader(new StringReader(val));
        try {
            String levelName = null;
            String line;
            Pattern levelPattern = Pattern.compile("^Level ([A-Z1-9]+):$");
            Pattern textPattern = Pattern.compile("^([0-9]+):(.*)$");
            Map<Integer, String> currentTexts = new TreeMap<>();
            while ((line = br.readLine()) != null) {
                Matcher levelMatcher = levelPattern.matcher(line);
                if (levelMatcher.matches()) {
                    addLevel(levelName, currentTexts);
                    currentTexts.clear();
                    levelName = levelMatcher.group(1);
                }
                Matcher textMatcher = textPattern.matcher(line);
                if (textMatcher.matches()) {
                    currentTexts.put(Integer.parseInt(textMatcher.group(1)), textMatcher.group(2));
                }
            }
            addLevel(levelName, currentTexts);
            currentTexts.clear();
        } catch (IOException ex) {
            //ignore
        }
    }

    private String getManual() {
        StringBuilder sb = new StringBuilder();
        sb.append("; Jazz 2 language file source").append("\r\n");
        sb.append("; Available characters: ").append(characterEncoding.replace(" ", "").replace("@", "").replace("#", "")).append("\r\n");
        sb.append("; Special characters:").append("\r\n");
        sb.append(";   @ - newline in level help strings").append("\r\n");
        sb.append(";   # - makes following text colored").append("\r\n");
        sb.append(";   ż1 to ż9 - makes following text more shrinked. ż without number can be used as standalone character. In polish and italian lang files, these characters are corrupted, having nonworking ' code instead.").append("\r\n");

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getManual());
        sb.append("\r\n");
        sb.append("Main:\r\n");
        for (int i = 0; i < text.size(); i++) {
            sb.append(i).append(":").append(text.get(i)).append("\r\n");
        }
        for (int i = 0; i < levelHelpStrings.size(); i++) {
            sb.append("Level ").append(levelHelpStrings.get(i).levelName).append(":").append("\r\n");
            for (int j = 0; j < levelHelpStrings.get(i).helpStrings.size(); j++) {
                sb.append(levelHelpStrings.get(i).helpStrings.get(j).index).append(":").append(levelHelpStrings.get(i).helpStrings.get(j).text).append("\r\n");
            }
        }
        return sb.toString();
    }

    public Jazz2Language(InputStream is) throws IOException {
        DataInputStream dais = new DataInputStream(is);

        int textCount = readUnsignedInt(is);
        int textByteCount = readUnsignedInt(is);
        byte textBytes[] = new byte[textByteCount];
        dais.readFully(textBytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(textBytes);
        bais.mark(textByteCount);
        for (int i = 0; i < textCount; i++) {
            bais.mark(textByteCount);
            int offset = readUnsignedInt(is);
            if (offset > 0) {
                bais.skip(offset);
            }
            text.add(readNullTerminatedString(bais));
            bais.reset();
        }
        int levelEntryCount = readUnsignedInt(is);
        for (int i = 0; i < levelEntryCount; i++) {
            byte[] name = new byte[8];
            is.read(name);
            int len = 0;
            for (int j = 0; j < name.length; j++) {
                if (name[j] == 0) {
                    break;
                }
                len++;
            }
            Jazz2LevelHelpStrings helpStrings = new Jazz2LevelHelpStrings();
            helpStrings.levelName = new String(name, 0, len);
            helpStrings.startIndex = is.read();
            helpStrings.endIndex = is.read();
            levelHelpStrings.add(helpStrings);
            is.skip(2); //offset                   
        }

        is.skip(4);

        for (int i = 0; i < levelHelpStrings.size(); i++) {
            int startIndex = levelHelpStrings.get(i).startIndex;
            int endIndex = levelHelpStrings.get(i).endIndex;
            int index;
            do {
                index = is.read();
                int len = is.read();
                byte data[] = new byte[len];
                dais.readFully(data);
                String txt = bytesToString(data);
                levelHelpStrings.get(i).helpStrings.add(new Jazz2HelpString(index, txt));
            } while (index < endIndex);
        }
    }

    private int readUnsignedInt(InputStream is) throws IOException {
        return is.read() + (is.read() << 8) + (is.read() << 16) + (is.read() << 24);
    }

    private int readUnsignedShort(InputStream is) throws IOException {
        return is.read() + (is.read() << 8);
    }

    private String readNullTerminatedString(InputStream is) throws IOException {
        int c;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((c = is.read()) > 0) {
            baos.write(c);
        }

        return bytesToString(baos.toByteArray());
    }

    private void writeUnsignedInt(OutputStream os, int val) throws IOException {
        os.write(val & 0xff);
        os.write((val >> 8) & 0xff);
        os.write((val >> 16) & 0xff);
        os.write((val >> 24) & 0xff);
    }

    private void writeUnsignedShort(OutputStream os, int val) throws IOException {
        os.write(val & 0xff);
        os.write((val >> 8) & 0xff);
    }

    public void saveToStream(OutputStream os) throws IOException {
        writeUnsignedInt(os, text.size());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<Integer> textOffsets = new ArrayList<>();
        for (String s : text) {
            textOffsets.add(baos.size());
            baos.write(stringToBytes(s));
            baos.write(0);
        }
        writeUnsignedInt(os, baos.toByteArray().length);
        os.write(baos.toByteArray());
        for (int offset : textOffsets) {
            writeUnsignedInt(os, offset);
        }

        writeUnsignedInt(os, levelHelpStrings.size());
        ByteArrayOutputStream helpStrBaos = new ByteArrayOutputStream();
        for (Jazz2LevelHelpStrings level : levelHelpStrings) {
            os.write(level.levelName.getBytes());
            for (int i = level.levelName.length(); i < 8; i++) {
                os.write(0);
            }
            os.write(level.startIndex);
            os.write(level.endIndex);
            writeUnsignedShort(os, helpStrBaos.size());
            for (Jazz2HelpString hs : level.helpStrings) {
                helpStrBaos.write(hs.index);
                byte[] textBytes = stringToBytes(hs.text);

                helpStrBaos.write(textBytes.length);

                helpStrBaos.write(textBytes);
            }
        }
        writeUnsignedInt(os, helpStrBaos.size());
        os.write(helpStrBaos.toByteArray());
    }
}
