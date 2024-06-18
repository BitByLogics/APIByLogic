package net.bitbylogic.apibylogic.util.uuid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Setter;
import net.bitbylogic.apibylogic.APIByLogic;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UUIDUtil {

    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Setter
    private static File cacheFile;

    @Setter
    private static UUIDCache cache;

    public static void initialize(File dataFolder) {
        cacheFile = new File(APIByLogic.getInstance().getDataFolder(), "uuid_cache.json");

        if (!cacheFile.exists()) {
            try {
                cacheFile.createNewFile();
                cache = new UUIDCache();

                FileWriter writer = new FileWriter(cacheFile);
                writer.write(gson.toJson(cache, UUIDCache.class));
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        try {
            cache = gson.fromJson(new FileReader(cacheFile), UUIDCache.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read UUID data.
     *
     * @param data   The data to read.
     * @param result The StringBuilder to append the data onto.
     */
    private static void readData(String data, StringBuilder result) {
        int character = 7;

        if (data.length() < 7) {
            result.append("null");
            return;
        }

        while (character < 200) {
            if (!String.valueOf(data.charAt(character)).equalsIgnoreCase("\"")) {
                result.append(data.charAt(character));
            } else {
                break;
            }

            character++;
        }
    }

    /**
     * Call a URL to retrieve JSON data.
     *
     * @param URL The URL to be scanned.
     * @return The data retrieved.
     */
    private static String callURL(String URL) {
        StringBuilder jsonData = new StringBuilder();

        try {
            URL url = new URL(URL);
            URLConnection urlConnection = url.openConnection();

            if (urlConnection != null) {
                urlConnection.setReadTimeout(60 * 1000);
            }

            if (urlConnection != null && urlConnection.getInputStream() != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(urlConnection.getInputStream(), Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                int nextCharacter;

                while ((nextCharacter = bufferedReader.read()) != -1) {
                    jsonData.append((char) nextCharacter);
                }

                bufferedReader.close();
                inputStreamReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jsonData.toString();
    }

    /**
     * Get a player's UUID from Mojang, so we know it's up-to-date.
     *
     * @param playerName The player whose UUID is being retrieved.
     * @return The UUID retrieved.
     */
    public static UUID getUUID(String playerName) {
        if (cache == null) {
            return null;
        }

        Optional<UUID> uuidOptional = cache.getUUIDByName(playerName);

        if (uuidOptional.isPresent()) {
            return uuidOptional.get();
        }

        String output = callURL("https://api.mojang.com/users/profiles/minecraft/" + playerName);

        StringBuilder result = new StringBuilder();

        readData(output, result);

        String newResult = result.toString();

        if (newResult.equals("null")) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        StringBuilder uuidString = new StringBuilder();

        for (int i = 0; i <= 31; i++) {
            uuidString.append(newResult.charAt(i));
            if (i == 7 || i == 11 || i == 15 || i == 19) {
                uuidString.append("-");
            }
        }

        UUID uuid = UUID.fromString(uuidString.toString());

        List<String> playerNames = cache.getCachedUUIDs().getOrDefault(uuid, new ArrayList<>());
        playerNames.add(playerName);
        cache.getCachedUUIDs().put(uuid, playerNames);

        return uuid;
    }

}