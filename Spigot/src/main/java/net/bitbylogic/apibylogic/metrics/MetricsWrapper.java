package net.bitbylogic.apibylogic.metrics;

import net.bitbylogic.apibylogic.APIByLogic;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public class MetricsWrapper {

    private final Metrics metrics;

    public MetricsWrapper(APIByLogic plugin) {
        this.metrics = new Metrics(plugin, 21738);

        addDatabaseCharts(plugin);
    }

    private void addDatabaseCharts(APIByLogic plugin) {
        metrics.addCustomChart(new SimplePie("using_redis", () -> plugin.getRedisManager() == null ? "false" : "true"));
        metrics.addCustomChart(new SimplePie("using_hikari", () -> plugin.getHikariAPI() == null ? "false" : "true"));
    }

}
