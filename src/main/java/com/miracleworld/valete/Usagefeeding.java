package com.miracleworld.valete;

import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Base64;

import static com.miracleworld.valete.Monitoring.spark;

public class Usagefeeding {
  public void sendFeed(FileConfiguration config) {
    Bukkit.getLogger().info("sending feed");

    final DecimalFormat df = new DecimalFormat("#.##");

    // get current memory used from runtime
    Runtime r = Runtime.getRuntime();
    long memUsed = (r.totalMemory() - r.freeMemory());

    // Using spark's api to get tps and cpu usage
    DoubleStatistic<StatisticWindow.TicksPerSecond> tps = spark.tps();
    double tpsLast1min = tps.poll(StatisticWindow.TicksPerSecond.MINUTES_1);

    DoubleStatistic<StatisticWindow.CpuUsage> cpuUsage = spark.cpuSystem();
    double usagelast1min = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_1);

    // Getting average|median|min|max ping of online players
    // creating and appending ping from all players to array
    int[] playersPing = {};
    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      playersPing  = Arrays.copyOf(playersPing, playersPing.length + 1);
      playersPing[playersPing.length - 1] = onlinePlayer.getPing();
    }
    // deducting simple average, min and max
    double pingaverage = Arrays.stream(playersPing).average().orElse(0);
    int pingmax = Arrays.stream(playersPing).max().orElse(0);
    int pingmin = Arrays.stream(playersPing).min().orElse(0);
    // sorting array and getting the median
    Arrays.sort(playersPing);
    double pingmedian;
    if (playersPing.length == 0) {
      pingmedian = 0;
    } else if (playersPing.length % 2 == 0) {
      pingmedian = ((double)playersPing[playersPing.length/2] + (double)playersPing[playersPing.length/2 - 1])/2;
    } else {
      pingmedian = playersPing[playersPing.length/2];
    };

    // Sending all the collected information to the Merger server
    try {
      URL url = new URL("http://"+config.getString("host")+":"+config.getInt("port")+"/usagefeed");
      HttpURLConnection http = (HttpURLConnection)url.openConnection();
      http.setReadTimeout(4000);
      http.setConnectTimeout(4000);
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      http.setRequestProperty("Accept", "application/json");
      String auth = config.getString("username")+":"+config.getString("password");
      String token64 = Base64.getEncoder().withoutPadding().encodeToString(auth.getBytes(StandardCharsets.US_ASCII));
      http.setRequestProperty("Authorization", "Basic " + token64);
      http.setRequestProperty("Content-Type", "application/json");

      // creating body with all the informations
      String data = "{\n" +
          "\"servername\":\""+Monitoring.servername+"\",\n" +
          "\"tps\":"+df.format(tpsLast1min)+",\n" +
          "\"cpu\":"+df.format(usagelast1min)+",\n" +
          "\"ramMB\": {\n" +
          "\"used\":"+Math.floor(memUsed / 1048576)+",\n" +
          "\"total\":"+Math.floor(r.maxMemory() / 1048576)+",\n" +
          "\"free\":"+Math.floor((r.maxMemory()-memUsed) / 1048576)+"},\n" +
          "\"ping\": {\n" +
          "\"average\":"+Math.floor(pingaverage)+",\n" +
          "\"median\":"+Math.floor(pingmedian)+",\n" +
          "\"min\":"+Math.floor(pingmin)+",\n" +
          "\"max\":"+Math.floor(pingmax)+"},\n" +
          "\"players\": {\n" +
          "\"current\":"+Bukkit.getOnlinePlayers().size()+",\n" +
          "\"max\":"+Bukkit.getMaxPlayers()+"}\n" +
          "}";

      byte[] out = data.getBytes(StandardCharsets.UTF_8);

      OutputStream stream = http.getOutputStream();
      stream.write(out);

      if (http.getResponseCode() != 200) {
        Bukkit.getLogger().info(http.getResponseCode() + " " + http.getResponseMessage());
      }
      http.disconnect();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}
