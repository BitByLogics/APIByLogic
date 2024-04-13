package net.justugh.japi.util;

import java.io.File;

public class FileUtil {

    public static void deleteDirectory(File directory) {
        if (directory == null) {
            return;
        }

        File[] contents = directory.listFiles();

        if (contents != null) {
            for (File file : contents) {
                deleteDirectory(file);
            }
        }

        directory.delete();
    }

}
