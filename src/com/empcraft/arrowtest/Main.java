package com.empcraft.arrowtest;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.empcraft.arrowtest.util.ProjectileUtil;

public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new ProjectileUtil(this), this);
    }
}
