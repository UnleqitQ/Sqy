package com.unleqitq.sqy;

import com.google.common.collect.Streams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.*;
import java.util.stream.Collectors;

public class CommandSpyHandler implements Listener {
	
	private final Map<UUID, Set<UUID>> spyingPlayers = new HashMap<>();
	private final Map<UUID, Set<UUID>> spiedPlayers = new HashMap<>();
	private final Set<UUID> globalSpies = new HashSet<>();
	
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, Sqy.getInstance());
	}
	
	public void unload() {
		spyingPlayers.clear();
		spiedPlayers.clear();
		HandlerList.unregisterAll(this);
	}
	
	public Set<UUID> getSpyingPlayers(UUID uuid) {
		return Streams.concat(spiedPlayers.getOrDefault(uuid, Set.of()).stream(), globalSpies.stream())
			.collect(Collectors.toSet());
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
	
	public void addGlobalSpy(UUID spy) {
		globalSpies.add(spy);
	}
	
	public void removeGlobalSpy(UUID spy) {
		globalSpies.remove(spy);
	}
	
	public boolean isGlobalSpy(UUID spy) {
		return globalSpies.contains(spy);
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
		// TODO: Add back time and date
		// I fucking hate how time and date is handled in every language (well c++ is kinda chill)
		// This is so bs!!!
		// I just want an object,
		// that contains the current time and date not an object
		// that seems like it should contain both
		// but only contains the time and another that only contains the date
		// like wtf, why is this so hard?????
		Component message = Sqy.getPrefix("CmdSqy")
			.append(player.name().color(NamedTextColor.GRAY))
			.append(Component.text(" -> ").color(NamedTextColor.DARK_GRAY))
			.append(Component.text(command)
				.color(NamedTextColor.WHITE)
				.decorate(TextDecoration.ITALIC)
				.decorate(TextDecoration.BOLD)
				.clickEvent(ClickEvent.suggestCommand(event.getMessage()))
				.hoverEvent(HoverEvent.showText(Component.text("Click to paste this command in chat"))));
		for (UUID spy : spies) {
			if (spy.equals(uuid)) continue;
			Player spyPlayer = Bukkit.getPlayer(spy);
			if (spyPlayer != null && spyPlayer.canSee(player) &&
				spyPlayer.hasPermission("sqy.command.use")) {
				spyPlayer.sendMessage(message);
			}
		}
	}
	
}
