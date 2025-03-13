package com.unleqitq.sqy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CommandSpyHandler implements Listener {
	
	private final Map<UUID, Set<UUID>> spyingPlayers = new HashMap<>();
	private final Map<UUID, Set<UUID>> spiedPlayers = new HashMap<>();
	
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, Sqy.getInstance());
	}
	
	public void unload() {
		spyingPlayers.clear();
		spiedPlayers.clear();
		HandlerList.unregisterAll(this);
	}
	
	public Set<UUID> getSpyingPlayers(UUID uuid) {
		return spiedPlayers.getOrDefault(uuid, Set.of());
	}
	
	public Set<UUID> getSpiedPlayers(UUID uuid) {
		return spyingPlayers.getOrDefault(uuid, Set.of());
	}
	
	public void addSpy(UUID spy, UUID spied) {
		spyingPlayers.computeIfAbsent(spy, k -> new HashSet<>()).add(spied);
		spiedPlayers.computeIfAbsent(spied, k -> new HashSet<>()).add(spy);
	}
	
	public void removeSpy(UUID spy, UUID spied) {
		spyingPlayers.computeIfAbsent(spy, k -> new HashSet<>()).remove(spied);
		spiedPlayers.computeIfAbsent(spied, k -> new HashSet<>()).remove(spy);
	}
	
	public boolean isSpying(UUID spy, UUID spied) {
		return spyingPlayers.getOrDefault(spy, Set.of()).contains(spied);
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCommand(
		PlayerCommandPreprocessEvent event
	) {
		Player player = event.getPlayer();
		if (player.hasPermission("sqy.command.bypass")) return;
		String command = event.getMessage();
		UUID uuid = player.getUniqueId();
		Set<UUID> spies = getSpyingPlayers(uuid);
		if (spies == null || spies.isEmpty()) return;
		ZoneId zoneId;
		try {
			zoneId = ZoneId.of(Sqy.getInstance().timeZone, ZoneId.SHORT_IDS);
		}
		catch (Exception e) {
			zoneId = ZoneId.systemDefault();
		}
		String formattedTime =
			DateTimeFormatter.ofPattern("HH:mm:ss").format(Clock.system(zoneId).instant());
		Component message = Sqy.getPrefix("CmdSqy")
			.append(player.name().color(NamedTextColor.GRAY))
			.append(Component.text(" -> ").color(NamedTextColor.DARK_GRAY))
			.append(Component.text(command)
				.color(NamedTextColor.WHITE)
				.decorate(TextDecoration.ITALIC)
				.decorate(TextDecoration.BOLD)
				.clickEvent(ClickEvent.suggestCommand(event.getMessage())))
			.appendSpace()
			.append(Component.text("[")
				.color(NamedTextColor.DARK_GRAY)
				.append(Component.text(formattedTime).color(NamedTextColor.GRAY))
				.append(Component.text("]").color(NamedTextColor.DARK_GRAY)));
		for (UUID spy : spies) {
			Player spyPlayer = Bukkit.getPlayer(spy);
			if (spyPlayer != null && spyPlayer.canSee(player) &&
				spyPlayer.hasPermission("sqy.command.use")) {
				spyPlayer.sendMessage(message);
			}
		}
	}
	
}
