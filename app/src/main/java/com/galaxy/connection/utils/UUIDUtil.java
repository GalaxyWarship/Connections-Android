package com.galaxy.connection.utils;

import android.content.Context;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class UUIDUtil {
    private static String uuid;

    public static synchronized String resetUUID(Context context) {
        File uuidFile = new File(context.getFilesDir(), "uuid");
        if (uuidFile.exists()) {
            uuidFile.delete();
        }

        return getUUID(context);
    }

    public static synchronized String getUUID(Context context) {
        if (uuid != null)
            return uuid;

        File uuidFile = new File(context.getFilesDir(), "uuid");
        FileReader reader = null;
        try {

            reader = new FileReader(uuidFile);
            BufferedReader bufferedReader = new BufferedReader(reader);
            uuid = bufferedReader.readLine();
        }catch (Exception e) {
            uuid = createUUID();
            persist(uuidFile, uuid);
        }finally {
            try {
                if (reader != null)
                    reader.close();
            }catch (Exception e) {

            }
        }

        return uuid;
    }

    static String createUUID() {
        return String.valueOf(java.util.UUID.randomUUID())
                + String.valueOf(System.currentTimeMillis() % 1000)
                + String.valueOf(SystemClock.currentThreadTimeMillis() % 1000)
                + String.valueOf(SystemClock.elapsedRealtime() % 1000);
    }

    private static void persist(File uuidFile, String uuid) {
        FileWriter writer = null;
        try {
            uuidFile.getParentFile().mkdirs();
            writer = new FileWriter(uuidFile);
            writer.write(uuid);
            writer.close();
        }catch (Exception e) {

        }finally {
            try {

                if (writer != null) {
                    writer.close();
                }
            }catch (Exception e) {

            }
        }
    }

    private UUIDUtil() {
    }
}
