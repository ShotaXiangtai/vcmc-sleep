package com.vcmc.sleep;

import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.List;
import java.util.stream.Collectors;

public class SleepListener implements Listener {

    private final VcmcSleep plugin;

    public SleepListener(VcmcSleep plugin) {
        this.plugin = plugin;
    }

    /**
     * 新しいワールドがロードされたときにバニラの就寝スキップを無効化する
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (plugin.getConfig().getBoolean("disable-vanilla-sleep", true)
                && event.getWorld().getEnvironment() == World.Environment.NORMAL) {
            World world = event.getWorld();
            plugin.getServer().getGlobalRegionScheduler().run(plugin, t -> disableVanillaSleep(world));
        }
    }

    /**
     * プレイヤーがベッドに入ったとき
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        // ベッドに入れなかった場合は無視
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();

        // プレイヤーが就寝したことをアナウンス
        String sleepMsg = plugin.getConfig().getString(
                "messages.player-sleep", "&7{player} がベッドに入りました。");
        String formatted = sleepMsg.replace("{player}", player.getName());
        world.getPlayers().forEach(p -> p.sendMessage(plugin.colorize(formatted)));

        // isSleeping() が true になるのを待って就寝チェック（2tick後）
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, t -> checkAndSkipNight(world), 2L);
    }

    /**
     * プレイヤーがサーバーを抜けたとき（就寝中なら再チェック）
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.isSleeping()) {
            World world = player.getWorld();
            // プレイヤーが除外された後に再チェック
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, t -> checkAndSkipNight(world), 2L);
        }
    }

    /**
     * 就寝割合をチェックし、条件を満たした場合は朝にスキップする
     */
    void checkAndSkipNight(World world) {
        // オーバーワールドのみ対象
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        // 夜間または雷雨でなければスキップしない
        if (!canSkipTime(world)) return;

        boolean excludeSpectators = plugin.getConfig().getBoolean("exclude-spectators", true);

        // 対象プレイヤーのリストを取得
        List<Player> eligible = world.getPlayers().stream()
                .filter(p -> !excludeSpectators || p.getGameMode() != GameMode.SPECTATOR)
                .collect(Collectors.toList());

        if (eligible.isEmpty()) return;

        long sleeping = eligible.stream().filter(Player::isSleeping).count();
        double percentage = (double) sleeping / eligible.size() * 100.0;
        double required = plugin.getConfig().getDouble("sleep-percentage", 50.0);

        // 誰かが就寝中なら状況を表示
        if (sleeping > 0) {
            String countMsg = plugin.getConfig().getString(
                    "messages.sleep-count",
                    "&e{sleeping}/{total} 人が就寝中 ({percentage}% / 必要: {required}%)");
            String msg = countMsg
                    .replace("{sleeping}", String.valueOf(sleeping))
                    .replace("{total}", String.valueOf(eligible.size()))
                    .replace("{percentage}", String.format("%.0f", percentage))
                    .replace("{required}", String.format("%.0f", required));
            world.getPlayers().forEach(p -> p.sendMessage(plugin.colorize(msg)));
        }

        // 就寝割合が必要割合以上なら朝にスキップ
        if (percentage >= required) {
            skipNight(world);
        }
    }

    /**
     * 夜をスキップして朝にする
     */
    private void skipNight(World world) {
        world.setTime(0);       // 夜明け（time=0）に設定
        world.setStorm(false);
        world.setThundering(false);

        String morningMsg = plugin.getConfig().getString(
                "messages.morning", "&a朝になりました！おはようございます！");

        world.getPlayers().forEach(p -> {
            p.sendMessage(plugin.colorize(morningMsg));
            // 就寝中のプレイヤーを起こす（Folia: EntityScheduler で entity のスレッドに移譲）
            p.getScheduler().run(plugin, t -> {
                if (p.isSleeping()) p.wakeup(false);
            }, null);
        });
    }

    /**
     * 時間をスキップできる状態かどうかを確認する
     * 夜間（time >= 12541 && time <= 23458）または雷雨中であればスキップ可能
     */
    private boolean canSkipTime(World world) {
        long time = world.getTime();
        boolean isNight = time >= 12541 && time <= 23458;
        boolean isThundering = world.isThundering();
        return isNight || isThundering;
    }

    /**
     * バニラの playersSleepingPercentage ゲームルールを 101 に設定して
     * バニラの就寝スキップを無効化する
     */
    void disableVanillaSleep(World world) {
        // 101% = 絶対に達成できないため、バニラの就寝スキップが発生しない
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 101);
        plugin.getLogger().info("[" + world.getName() + "] バニラ就寝スキップを無効化しました。");
    }
}
