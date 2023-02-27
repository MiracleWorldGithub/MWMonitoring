package com.miracleworld.valete;

import com.miracleworld.valete.commands.Usage;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Monitoring extends JavaPlugin {

  public static Spark spark;
  public static String servername = null;
  public FileConfiguration config = this.getConfig();
  // threads
  ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Override
  public void onEnable() {
    // generate default config
    config.addDefault("host", "127.0.0.1");
    config.addDefault("port", "3695");
    config.addDefault("username", "username");
    config.addDefault("password", "password");
    config.options().copyDefaults(true);
    saveConfig();

    // dependencies
    spark = SparkProvider.get();

    // getting the server name from server.properties give ip:port if not found
    try (InputStream input = new FileInputStream("./server.properties")) {
      Properties prop = new Properties();
      prop.load(input);
      servername = prop.getProperty("server-name");
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    // Commands
    getCommand("usage").setExecutor(new Usage());

    // scheduling thread to run every minute (sending info every minute)
    executor.scheduleAtFixedRate(periodicTask, 0, 1, TimeUnit.MINUTES);
  }

  @Override
  public void onDisable() {
    executor.shutdown();
  }

  Runnable periodicTask = new Runnable() {
    public void run() {
      new Usagefeeding().sendFeed(getConfig());
    }
  };

}
