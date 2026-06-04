package com.vcmc.sleep;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class VcmcSleep extends JavaPlugin {

    private SleepListener sleepListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        sleepListener = new SleepListener(this);
        getServer().getPluginManager().registerEvents(sleepListener, this);

        // バニラの就寝スキップを無効化（Global Region スレッドで実行）
        if (getConfig().getBoolean("disable-vanilla-sleep", true)) {
            getServer().getGlobalRegionScheduler().run(this, t ->
                Bukkit.getWorlds().stream()
                        .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                        .forEach(sleepListener::disableVanillaSleep)
            );
        }

        double percentage = getConfig().getDouble("sleep-percentage", 50.0);
        getLogger().info("vcmc-sleep が有効化されました。");
        getLogger().info("就寝割合: " + percentage + "%");
    }

    @Override
    public void onDisable() {
        getLogger().info("vcmc-sleep が無効化されました。");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("vcmcsleep")) return false;

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vcmcsleep.reload")) {
                sender.sendMessage(colorize(getConfig().getString(
                        "messages.no-permission", "&cこのコマンドを実行する権限がありません。")));
                return true;
            }
            reloadConfig();
            sender.sendMessage(colorize(getConfig().getString(
                    "messages.reload", "&avcmc-sleep の設定をリロードしました。")));
            getLogger().info("設定をリロードしました。就寝割合: "
                    + getConfig().getDouble("sleep-percentage", 50.0) + "%");
            return true;
        }

        sender.sendMessage(colorize("&e使用方法: /" + label + " reload"));
        return true;
    }

    /**
     * カラーコード（&）を Minecraft カラーコード（§）に変換する
     */
    String colorize(String message) {
        if (message == null) return "";
        return message.replace("&", "\u00A7");
    }
}
