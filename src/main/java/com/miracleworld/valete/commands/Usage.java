package com.miracleworld.valete.commands;

import com.miracleworld.valete.Monitoring;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Arrays;

import static com.miracleworld.valete.Monitoring.spark;

public class Usage implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Player player = (Player) sender;

    if (command.getName().equalsIgnoreCase("usage") && player.hasPermission("valete.usage.debug")) {
      final DecimalFormat df = new DecimalFormat("#.##");

      Runtime r = Runtime.getRuntime();
      long memUsed = (r.totalMemory() - r.freeMemory());

      DoubleStatistic<StatisticWindow.TicksPerSecond> tps = spark.tps();
      double tpsLast10secs = tps.poll(StatisticWindow.TicksPerSecond.SECONDS_10);
      double tpsLast1min = tps.poll(StatisticWindow.TicksPerSecond.MINUTES_1);
      double tpsLast15mins = tps.poll(StatisticWindow.TicksPerSecond.MINUTES_15);

      GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();

      DoubleAverageInfo msptLast10secs = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
      DoubleAverageInfo msptLast1min = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);

      DoubleStatistic<StatisticWindow.CpuUsage> cpuUsage = spark.cpuSystem();

      double usagelast10secs = cpuUsage.poll(StatisticWindow.CpuUsage.SECONDS_10);
      double usagelast1min = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_1);
      double usagelast15min = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_15);

      int[] playersPing = {};
      for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
        playersPing  = Arrays.copyOf(playersPing, playersPing.length + 1);
        playersPing[playersPing.length - 1] = onlinePlayer.getPing();
      }
      double pingaverage = Arrays.stream(playersPing).average().orElse(0);
      int pingmax = Arrays.stream(playersPing).max().orElse(0);
      int pingmin = Arrays.stream(playersPing).min().orElse(0);
      Arrays.sort(playersPing);
      double pingmedian;
      if (playersPing.length % 2 == 0) {
        pingmedian = ((double)playersPing[playersPing.length/2] + (double)playersPing[playersPing.length/2 - 1])/2;
      } else {
        pingmedian = (double) playersPing[playersPing.length/2];
      }

      player.sendMessage("Current usage of " + Monitoring.servername);
      player.sendMessage("Current TPS (10s | 1m | 15m) : " + Math.floor(tpsLast10secs) + " | " + Math.floor(tpsLast1min) + " | " + Math.floor(tpsLast15mins));
      player.sendMessage("Current MSPT (§110s §r| §31m§r) (Mean | 95th | max) : \n§1" + df.format(msptLast10secs.mean()) + " | " + df.format(msptLast10secs.percentile95th()) + " | " + df.format(msptLast10secs.max()) + "\n§3" + df.format(msptLast1min.mean()) + " | " + df.format(msptLast1min.percentile95th()) + " | " + df.format(msptLast1min.max()));
      player.sendMessage("Current CPU usage (10s | 1m | 15m) : " + df.format(usagelast10secs) + " | " + df.format(usagelast1min) + " | " + df.format(usagelast15min));
      player.sendMessage("Current memory used : " + Math.floor(memUsed / 1048576) + "MB/" + Math.floor(r.maxMemory() / 1048576) + "MB (" + Math.floor((r.maxMemory()-memUsed) / 1048576) + "MB free)");
      player.sendMessage("Users ping (average | median | min | max ) : " + Math.floor(pingaverage) + " | " + Math.floor(pingmedian) + " | " + Math.floor(pingmin) + " | " + Math.floor(pingmax));

    }

    return false;
  }
}
