package net.justugh.japi.database;

import org.bukkit.configuration.ConfigurationSection;

public class HikariAPI extends net.justugh.japi.database.hikari.HikariAPI {

    public HikariAPI(ConfigurationSection section) {
        super(section.getString("Address"), section.getString("Database"),
                section.getString("Port"), section.getString("Username"),
                section.getString("Password"));
    }

}
