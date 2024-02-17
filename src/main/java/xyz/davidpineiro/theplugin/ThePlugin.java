package xyz.davidpineiro.theplugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ThePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage("hello world!");
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage("goodbye!");
    }
}
