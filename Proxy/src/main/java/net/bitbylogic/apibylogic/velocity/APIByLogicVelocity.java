package net.bitbylogic.apibylogic.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.bitbylogic.apibylogic.APIByLogicProxy;
import net.bitbylogic.apibylogic.database.redis.RedisManager;
import net.bitbylogic.apibylogic.type.ProxyType;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Plugin(
        id = "apibylogic",
        name = "APIByLogic",
        version = "2024.4",
        authors = {"BitByLogic"}
)
@Getter
public class APIByLogicVelocity {

    private final ProxyServer server;
    private final Logger logger;

    private YAMLConfiguration config = new YAMLConfiguration();

    private RedisManager redisManager;

    @Inject
    public APIByLogicVelocity(ProxyServer proxyServer, Logger logger) {
        this.server = proxyServer;
        this.logger = logger;

        loadConfiguration();

        redisManager = new RedisManager(
                config.getString("Redis-Credentials.Host"),
                config.getInt("Redis-Credentials.Port"),
                config.getString("Redis-Credentials.Password"), config.getString("Server-ID"));

        APIByLogicProxy.initialize(redisManager, ProxyType.VELOCITY);
    }

    private void loadConfiguration() {
        try {
            Path configFilePath = Path.of("plugins", "APIByLogic", "config.yml");
            File configFile = new File("plugins/APIByLogic", "config.yml");

            if (configFile.exists()) {
                try {
                    config.read(new FileReader(configFile));
                } catch (ConfigurationException e) {
                    e.printStackTrace();
                }
                return;
            }

            configFile.mkdirs();
            configFile.createNewFile();

            Files.copy(getClass().getResourceAsStream("/config.yml"), configFilePath, StandardCopyOption.REPLACE_EXISTING);

            try {
                config.read(new FileReader(configFile));
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
