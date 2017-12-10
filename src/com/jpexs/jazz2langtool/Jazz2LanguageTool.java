package com.jpexs.jazz2langtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 *
 * @author JPEXS
 */
public class Jazz2LanguageTool {

    private static final String TXT_CHARSET = "UTF-8";

    public static void badArgs() {
        System.out.println("Invalid arguments");
        printUsage();
        System.exit(1);
    }

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("java -jar jazz2langtool.jar export english.j2s english.txt");
        System.out.println("java -jar jazz2langtool.jar import mylang.txt english.j2s");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            badArgs();
        }

        switch (args[0]) {
            case "export":
                System.out.println("Exporting " + args[1] + " to " + args[2] + " ...");
                try (FileInputStream fis = new FileInputStream(new File(args[1]))) {
                    Jazz2Language langFile = new Jazz2Language(fis);
                    try (FileOutputStream fos = new FileOutputStream(new File(args[2]))) {
                        fos.write(langFile.toString().getBytes(TXT_CHARSET));
                    } catch (IOException iex) {
                        System.err.println("Error writing file - " + iex.getMessage());
                        System.exit(1);
                    }

                } catch (IOException iex) {
                    System.err.println("Error reading file - " + iex.getMessage());
                    System.exit(1);
                }
                System.out.println("OK");
                break;
            case "import":
                System.out.println("Importing " + args[1] + " to " + args[2] + " ...");
                try (FileInputStream fis = new FileInputStream(new File(args[1]))) {
                    String txt = new String(Files.readAllBytes(new File(args[1]).toPath()), TXT_CHARSET);
                    Jazz2Language langFile = new Jazz2Language();
                    langFile.fromString(txt);
                    try (FileOutputStream fos = new FileOutputStream(new File(args[2]))) {
                        langFile.saveToStream(fos);
                    } catch (IOException iex) {
                        System.err.println("Error writing file - " + iex.getMessage());
                        System.exit(1);
                    }

                } catch (IOException iex) {
                    System.err.println("Error reading file - " + iex.getMessage());
                    System.exit(1);
                }
                System.out.println("OK");
                break;
            default:
                badArgs();
        }
    }

}
