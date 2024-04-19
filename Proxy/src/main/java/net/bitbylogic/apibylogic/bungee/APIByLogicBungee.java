package net.bitbylogic.apibylogic.bungee;

import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.bitbylogic.apibylogic.APIByLogicProxy;
import net.bitbylogic.apibylogic.database.redis.RedisManager;
import net.bitbylogic.apibylogic.type.ProxyType;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

@Getter
public class APIByLogicBungee extends Plugin {

    @Getter
    private static APIByLogicBungee instance;

    private File configFile;
    private Configuration config;

    private RedisManager redisManager;

    @Override
    public void onEnable() {
        loadConfiguration();
        instance = this;

        redisManager = new RedisManager(
                getConfig().getString("Redis-Credentials.Host"),
                getConfig().getInt("Redis-Credentials.Port"),
                getConfig().getString("Redis-Credentials.Password"), getConfig().getString("Server-ID"));

        APIByLogicProxy.initialize(redisManager, ProxyType.BUNGEECORD);
    }

    @Override
    public void onDisable() {

    }

    private void loadConfiguration() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        configFile = new File(getDataFolder(), "config.yml");

        if (configFile.exists()) {
            try {
                config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        try {
            configFile.createNewFile();

            try (InputStream is = getResourceAsStream("config.yml"); OutputStream os = Files.newOutputStream(configFile.toPath())) {
                ByteStreams.copy(is, os);
            }

            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
