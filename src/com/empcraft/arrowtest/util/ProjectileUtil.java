package com.empcraft.arrowtest.util;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class ProjectileUtil implements Listener {
    
    public static Entity getBestTarget(Player player) {
        double optimal = 128;
        double min = 128;
        
        Entity closest = null;
        Location loc = player.getLocation();
        for (Entity ent : player.getWorld().getEntities()) {
            if (!(ent instanceof LivingEntity)) {
                continue;
            }
            if (!player.hasLineOfSight(ent)) {
                continue;
            }
            if (ent instanceof Player && ((Player) ent).getName().equals(player.getName())) {
                continue;
            }
            Location eLoc = ent.getLocation();
            Vector direction = loc.getDirection();
            Vector pointDir = getDirection(loc, eLoc);
            double diff = Math.abs(direction.subtract(pointDir).length()) * optimal + Math.sqrt(loc.distanceSquared(eLoc));
            if (diff < min) {
                min = diff;
                closest = ent;
            }  
        }
        return closest;
    }
    
    public static Vector getDirection(Location pos1, Location pos2) {
        Vector vec1 = pos1.toVector();
        Vector vec2 = pos2.toVector();
        return vec2.subtract(vec1).normalize();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("aimbot.instant")) {
            return;
        }
        ItemStack hand = player.getItemInHand();
        if (hand.getType() != Material.BOW) {
            return;
        }
        if (!player.getInventory().contains(Material.ARROW)) {
            player.sendMessage("Not enough arrows!");
            return;
        }
        Entity target = getBestTarget(player);
        if (target == null) {
            player.sendMessage("Cannot find target");
            return;
        }
        Vector velocity = null;
        if (target instanceof Player) {
            velocity = velocities.get(((Player) target).getName());
        }
        else {
            velocity = target.getVelocity();
        }
        if (velocity == null) {
            velocity = new Vector(0, 0, 0);
        }
        ItemStack[] inv = player.getInventory().getContents();
        for (int i = 0; i < inv.length; i++) {
            ItemStack item = inv[i];
            if (item != null && item.getType() == Material.ARROW) {
                int amount = item.getAmount();
                if (amount == 1) {
                    player.getInventory().remove(item);
                }
                else {
                    item.setAmount(amount - 1);
                }
                break;
            }
        }
        player.updateInventory();
        shoot(player, target, velocity, null);
        
    }
    
    public ProjectileUtil(Plugin plugin) {
    }
    
    private long timestamp = 0;
    
    private HashMap<String, Vector> velocities = new HashMap<>();
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        long time = world.getTime();
        if (timestamp != time) {
            timestamp = time;
            velocities = new HashMap<>();
        }
        velocities.put(player.getName(), event.getTo().subtract(event.getFrom()).toVector());
    }
    
    @EventHandler
    public void onPlayerShootArrow(EntityShootBowEvent  event) {
        if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
            return;
        }
        Player player = (Player) event.getEntity();
        Entity target = getBestTarget(player);
        if (target == null) {
            return;
        }
        if (player.hasPermission("aimbot.auto")) {
            Vector velocity;
            if (target instanceof Player) {
                velocity = velocities.get(((Player) target).getName());
            }
            else {
                velocity = target.getVelocity();
            }
            if (velocity == null) {
                velocity = new Vector(0, 0, 0);
            }
            shoot(player, target, velocity, (Arrow) event.getProjectile());
        }
    }
    
    public Arrow shoot(Player player, Entity entity, Vector velocity, Arrow arrow) {
        Location loc;
        if (arrow == null) {
            loc = player.getLocation().add(0, 1, 0);
        }
        else {
            loc = arrow.getLocation();
        }
        
        double speed;
        if (arrow == null) {
            speed = 2;
        }
        else {
            speed = arrow.getVelocity().length();
        }
        
        double g = 20;
        double v = speed * 20;
        
        Location eloc = entity.getLocation();
        double dx = Math.abs(loc.getX() - eloc.getX());
        double dz = Math.abs(loc.getZ() - eloc.getZ());
        
        double dy = (eloc.getY() - loc.getY()) / 2;
        
        if (entity instanceof Player || entity instanceof Monster) {
            dy++;
        }
        
        double dh = Math.sqrt(dx * dx + dz * dz);
        if (dh > 32) {
            dh += Math.sqrt(dh);
        }
        
        double pitch = Math.atan( (v * v - Math.sqrt(v * v * v * v - g * (g * dh * dh + 2 * 2 * dy * v * v))) / ( g * dh ) );// * (180/3.14159265359);
        
        if (Double.isNaN(pitch)) {
            return null;
        }
        double time = dh/v;
        Vector target = entity.getLocation().toVector().subtract(loc.toVector()).add(velocity.multiply(time * 20));
        target.setY(Math.tan(pitch) * Math.sqrt(dh * dh + dy * dy));
        if (arrow == null) {
            arrow = player.shootArrow();
            arrow.setCritical(true);
        }
        arrow.setVelocity(target.normalize().multiply(speed));
        return arrow;
//        
//        
//        return player.getWorld().spawnArrow(loc, target, (float) speed, 1);
    }
}
