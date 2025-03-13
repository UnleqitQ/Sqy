package com.unleqitq.sqy;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

public final class Sqy extends JavaPlugin {
	
	@Nullable
	private static Sqy instance;
	
	private CommandSpyHandler commandSpyHandler;
	
	public String timeZone;
	
	private BukkitTask updateCheckTask;
	
	@Override
	public void onEnable() {
		instance = this;
		cleanup();
		UpdateChecker.scheduleUpdateCheck(task -> updateCheckTask = task);
		commandSpyHandler = new CommandSpyHandler();
		commandSpyHandler.load();
		getLifecycleManager().registerEventHandler(
			LifecycleEvents.COMMANDS, event -> registerCommands(event.registrar()));
		onReload();
	}
	
	public void cleanup() {
		UpdateChecker.VersionInfo currentVersion =
			UpdateChecker.VersionInfo.fromString(getPluginMeta().getVersion());
		for (File file : Objects.requireNonNull(getServer().getPluginsFolder()
			.listFiles((dir, name) -> name.startsWith("Sqy-") && name.endsWith(".jar")))) {
			String fileName = file.getName();
			String versionString = fileName.substring(4, fileName.length() - 4);
			try {
				UpdateChecker.VersionInfo version = UpdateChecker.VersionInfo.fromString(versionString);
				if (currentVersion.isNewerThan(version)) {
					if (!file.delete()) {
						getLogger().warning("Failed to delete outdated plugin file: " + file.getName());
					}
				}
			}
			catch (IllegalArgumentException e) {
				getLogger().warning("Failed to parse version from file name: " + file.getName());
			}
		}
	}
	
	private void onReload() {
		saveDefaultConfig();
		reloadConfig();
		timeZone = getConfig().getString("time-zone", "UTC");
	}
	
	public static @NotNull Component getPrefix() {
		return getPrefix("Sqy");
	}
	
	public static @NotNull Component getPrefix(@NotNull String label) {
		return Component.text("")
			.append(Component.text("[%s]".formatted(label))
				.color(NamedTextColor.GOLD)
				.decorate(TextDecoration.BOLD)
				.hoverEvent(HoverEvent.showText(
					Component.text("Version: " + getInstance().getPluginMeta().getVersion())
						.color(NamedTextColor.GRAY)))
				.append(Component.text(" ")));
	}
	
	private void registerCommands(@NotNull Commands registrar) {
		LiteralArgumentBuilder<CommandSourceStack> root =
			Commands.literal("spy").requires(ctx -> ctx.getSender().hasPermission("sqy.use"));
		root.executes(ctx -> {
			// Missing subcommand
			CommandSender sender = ctx.getSource().getSender();
			sender.sendMessage(
				getPrefix().append(Component.text("Missing subcommand").color(NamedTextColor.DARK_RED)));
			Runnable sendUsage = () -> sender.sendMessage(
				getPrefix().append(Component.text("Usages:").color(NamedTextColor.DARK_RED)));
			boolean sentUsages = false;
			if (sender.hasPermission("sqy.command.use")) {
				//noinspection ConstantValue
				if (!sentUsages) {
					sendUsage.run();
					sentUsages = true;
				}
				sender.sendMessage(
					getPrefix().append(Component.text("    /spy ").color(NamedTextColor.DARK_RED))
						.append(Component.text("command")
							.color(NamedTextColor.DARK_PURPLE)
							.hoverEvent(
								HoverEvent.showText(Component.text("Command spying").color(NamedTextColor.GRAY)))));
			}
			if (sender.hasPermission("sqy.reload")) {
				if (!sentUsages) {
					sendUsage.run();
					sentUsages = true;
				}
				sender.sendMessage(
					getPrefix().append(Component.text("    /spy ").color(NamedTextColor.DARK_RED))
						.append(Component.text("reload")
							.color(NamedTextColor.DARK_PURPLE)
							.hoverEvent(HoverEvent.showText(
								Component.text("Reload the plugin").color(NamedTextColor.GRAY)))));
			}
			
			return 0;
		});
		root.then(Commands.literal("reload")
			.requires(ctx -> ctx.getSender().hasPermission("sqy.reload"))
			.executes(ctx -> {
				onReload();
				ctx.getSource()
					.getSender()
					.sendMessage(
						getPrefix().append(Component.text("Plugin reloaded").color(NamedTextColor.GREEN)));
				return 1;
			}));
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("command")
			.requires(ctx -> ctx.getSender().hasPermission("sqy.command.use") &&
				ctx.getSender() instanceof Player);
		command.executes(ctx -> {
			// Missing subcommand
			CommandSender sender = ctx.getSource().getSender();
			sender.sendMessage(
				getPrefix().append(Component.text("Missing subcommand").color(NamedTextColor.DARK_RED)));
			sender.sendMessage(
				getPrefix().append(Component.text("Usages:").color(NamedTextColor.DARK_RED)));
			sender.sendMessage(
				getPrefix().append(Component.text("    /spy command ").color(NamedTextColor.DARK_RED))
					.append(Component.text("on")
						.color(NamedTextColor.DARK_PURPLE)
						.hoverEvent(HoverEvent.showText(
							Component.text("Start spying on a player").color(NamedTextColor.GRAY))))
					.append(Component.text("|").color(NamedTextColor.DARK_RED))
					.append(Component.text("off")
						.color(NamedTextColor.DARK_PURPLE)
						.hoverEvent(HoverEvent.showText(Component.text("Stop spying on a player")
							.color(NamedTextColor.GRAY)
							.appendNewline()
							.append(Component.text("The player does not have to be online")
								.color(NamedTextColor.GRAY)))))
					.append(Component.text(" "))
					.append(Component.text("<player>")
						.color(NamedTextColor.DARK_PURPLE)
						.hoverEvent(
							HoverEvent.showText(Component.text("Player to spy on").color(NamedTextColor.GRAY)))));
			sender.sendMessage(
				getPrefix().append(Component.text("    /spy command ").color(NamedTextColor.DARK_RED))
					.append(Component.text("list")
						.color(NamedTextColor.DARK_PURPLE)
						.hoverEvent(HoverEvent.showText(
							Component.text("List players you are spying on").color(NamedTextColor.GRAY)))));
			if (sender.hasPermission("sqy.command.list.others")) {
				sender.sendMessage(
					getPrefix().append(Component.text("    /spy command ").color(NamedTextColor.DARK_RED))
						.append(Component.text("list")
							.color(NamedTextColor.DARK_PURPLE)
							.hoverEvent(HoverEvent.showText(
								Component.text("List players the player is spying on").color(NamedTextColor.GRAY)))
							.append(Component.text(" "))
							.append(Component.text("<player>")
								.color(NamedTextColor.DARK_PURPLE)
								.hoverEvent(HoverEvent.showText(
									Component.text("Player to check").color(NamedTextColor.GRAY))))));
			}
			return 0;
		});
		command.then(Commands.literal("on").executes(ctx -> {
			// Missing player argument
			ctx.getSource()
				.getSender()
				.sendMessage(getPrefix().append(
					Component.text("Missing player argument").color(NamedTextColor.DARK_RED)));
			ctx.getSource()
				.getSender()
				.sendMessage(getPrefix().append(
					Component.text("Usage: /spy command on <player>").color(NamedTextColor.DARK_RED)));
			return 0;
		}).then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> {
			if (!(ctx.getSource().getSender() instanceof Player sender)) {
				return builder.buildFuture();
			}
			getServer().getOnlinePlayers()
				.stream()
				.filter(p -> !p.equals(sender))
				.filter(sender::canSee)
				.filter(p -> !p.hasPermission("sqy.command.bypass"))
				.filter(p -> !commandSpyHandler.isSpying(sender.getUniqueId(), p.getUniqueId()))
				.map(Player::getName)
				.filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
				.forEach(builder::suggest);
			return builder.buildFuture();
		}).executes(ctx -> {
			Player sender = (Player) ctx.getSource().getSender();
			String playerName = StringArgumentType.getString(ctx, "player");
			OfflinePlayer offlinePlayer = getServer().getOfflinePlayerIfCached(playerName);
			if (offlinePlayer == null || !offlinePlayer.isOnline() ||
				!(offlinePlayer instanceof Player player) || !sender.canSee(player)) {
				sender.sendMessage(
					getPrefix().append(Component.text("Player not found").color(NamedTextColor.DARK_RED)));
				return 0;
			}
			if (sender.equals(player)) {
				sender.sendMessage(getPrefix().append(
					Component.text("You can't spy on yourself").color(NamedTextColor.DARK_RED)));
				return 0;
			}
			if (player.hasPermission("sqy.command.bypass")) {
				sender.sendMessage(getPrefix().append(
					Component.text("You can't spy on this player").color(NamedTextColor.DARK_RED)));
				return 0;
			}
			
			if (commandSpyHandler.isSpying(sender.getUniqueId(), player.getUniqueId())) {
				sender.sendMessage(getPrefix().append(
					Component.text("You are already spying on this player").color(NamedTextColor.DARK_RED)));
				return 0;
			}
			commandSpyHandler.addSpy(sender.getUniqueId(), player.getUniqueId());
			sender.sendMessage(getPrefix().append(
				Component.text("You are now spying on " + player.getName()).color(NamedTextColor.GREEN)));
			return 1;
		})));
		command.then(Commands.literal("off").executes(ctx -> {
			// Missing player argument
			ctx.getSource()
				.getSender()
				.sendMessage(getPrefix().append(
					Component.text("Missing player argument").color(NamedTextColor.DARK_RED)));
			ctx.getSource()
				.getSender()
				.sendMessage(getPrefix().append(
					Component.text("Usage: /spy command off <player>").color(NamedTextColor.DARK_RED)));
			return 0;
		}).then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> {
			if (!(ctx.getSource().getSender() instanceof Player sender)) {
				return builder.buildFuture();
			}
			commandSpyHandler.getSpiedPlayers(sender.getUniqueId())
				.stream()
				.map(getServer()::getOfflinePlayer)
				.map(OfflinePlayer::getName)
				.filter(Objects::nonNull)
				.filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
				.forEach(builder::suggest);
			return builder.buildFuture();
		}).executes(ctx -> {
			Player sender = (Player) ctx.getSource().getSender();
			String playerName = StringArgumentType.getString(ctx, "player");
			OfflinePlayer player = getServer().getOfflinePlayerIfCached(playerName);
			if (player == null) {
				sender.sendMessage(getPrefix().append(
					Component.text("Player does not exist").color(NamedTextColor.DARK_RED)));
				return 0;
			}
			if (sender.equals(player)) {
				sender.sendMessage(getPrefix().append(
					Component.text("You can't spy on yourself").color(NamedTextColor.DARK_RED)));
				return 0;
			}
			
			if (!commandSpyHandler.isSpying(sender.getUniqueId(), player.getUniqueId())) {
				sender.sendMessage(getPrefix().append(
					Component.text("You are not spying on this player").color(NamedTextColor.DARK_RED)));
				return 0;
			}
			commandSpyHandler.removeSpy(sender.getUniqueId(), player.getUniqueId());
			sender.sendMessage(getPrefix().append(
				Component.text("You are no longer spying on " + player.getName())
					.color(NamedTextColor.GREEN)));
			return 1;
		})));
		command.then(Commands.literal("list")
			.executes(ctx -> {
				Player sender = (Player) ctx.getSource().getSender();
				if (commandSpyHandler.getSpiedPlayers(sender.getUniqueId()).isEmpty()) {
					sender.sendMessage(getPrefix().append(
						Component.text("You are not spying on anyone").color(NamedTextColor.GREEN)));
					return 1;
				}
				sender.sendMessage(getPrefix().append(
					Component.text("Players you are spying on:").color(NamedTextColor.GREEN)));
				for (UUID puuid : commandSpyHandler.getSpiedPlayers(sender.getUniqueId())) {
					OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(puuid);
					Component playerComponent;
					if (!offlinePlayer.hasPlayedBefore()) {
						playerComponent = Component.text(offlinePlayer.getUniqueId().toString())
							.color(NamedTextColor.RED)
							.decorate(TextDecoration.ITALIC)
							.hoverEvent(HoverEvent.showText(Component.text("Never played before")
								.color(NamedTextColor.GRAY)
								.appendNewline()
								.append(Component.text(
										"We don't know, how you were able to spy on them in the first place")
									.color(NamedTextColor.GRAY))));
					}
					if (offlinePlayer.isOnline() && offlinePlayer instanceof Player onlinePlayer &&
						sender.canSee(onlinePlayer) && !onlinePlayer.hasPermission("sqy.command.bypass")) {
						playerComponent = onlinePlayer.name().color(NamedTextColor.GREEN);
					}
					else {
						String playerName = offlinePlayer.getName();
						assert playerName != null;
						playerComponent = Component.text(playerName)
							.color(NamedTextColor.GRAY)
							.decorate(TextDecoration.ITALIC)
							.hoverEvent(
								HoverEvent.showText(Component.text("[Offline]").color(NamedTextColor.GRAY)));
					}
					Component messageComponent =
						getPrefix().append(Component.text("- ").color(NamedTextColor.DARK_GRAY))
							.append(playerComponent);
					if (offlinePlayer.hasPlayedBefore()) {
						String playerName = offlinePlayer.getName();
						assert playerName != null;
						messageComponent = messageComponent.appendSpace()
							.append(Component.text("[X]")
								.color(NamedTextColor.DARK_RED)
								.decorate(TextDecoration.BOLD)
								.hoverEvent(HoverEvent.showText(
									Component.text("Stop spying on " + playerName).color(NamedTextColor.GRAY)))
								.clickEvent(ClickEvent.runCommand("/spy command off " + playerName)));
					}
					sender.sendMessage(messageComponent);
				}
				return 1;
			})
			.then(Commands.argument("player", StringArgumentType.word())
				.requires(ctx -> ctx.getSender().hasPermission("sqy.command.list.others"))
				.suggests((ctx, builder) -> {
					if (!(ctx.getSource().getSender() instanceof Player sender)) {
						return builder.buildFuture();
					}
					getServer().getOnlinePlayers()
						.stream()
						.filter(sender::canSee)
						//.filter(p -> !commandSpyHandler.getSpiedPlayers(p.getUniqueId()).isEmpty())
						.map(Player::getName)
						.filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
						.forEach(builder::suggest);
					return builder.buildFuture();
				})
				.executes(ctx -> {
					Player sender = (Player) ctx.getSource().getSender();
					String playerName = StringArgumentType.getString(ctx, "player");
					OfflinePlayer offlinePlayer = getServer().getOfflinePlayerIfCached(playerName);
					if (offlinePlayer == null || !offlinePlayer.isOnline() ||
						!(offlinePlayer instanceof Player player) || !sender.canSee(player)) {
						sender.sendMessage(getPrefix().append(
							Component.text("Player not found").color(NamedTextColor.DARK_RED)));
						return 0;
					}
					
					if (commandSpyHandler.getSpiedPlayers(player.getUniqueId()).isEmpty()) {
						sender.sendMessage(getPrefix().append(
							Component.text(playerName + " is not spying on anyone").color(NamedTextColor.GREEN)));
						return 1;
					}
					
					sender.sendMessage(getPrefix().append(
						Component.text("Players " + playerName + " is spying on:")
							.color(NamedTextColor.GREEN)));
					for (UUID puuid : commandSpyHandler.getSpiedPlayers(player.getUniqueId())) {
						OfflinePlayer spiedPlayer = getServer().getOfflinePlayer(puuid);
						Component playerComponent;
						if (!spiedPlayer.hasPlayedBefore()) {
							playerComponent = Component.text(spiedPlayer.getUniqueId().toString())
								.color(NamedTextColor.RED)
								.decorate(TextDecoration.ITALIC)
								.hoverEvent(HoverEvent.showText(Component.text("Never played before")
									.color(NamedTextColor.GRAY)
									.appendNewline()
									.append(Component.text(
											"We don't know, how they were able to spy on them in the first place")
										.color(NamedTextColor.GRAY))));
						}
						if (spiedPlayer.isOnline() && spiedPlayer instanceof Player onlinePlayer &&
							sender.canSee(onlinePlayer) && !onlinePlayer.hasPermission("sqy.command.bypass")) {
							playerComponent = onlinePlayer.name().color(NamedTextColor.GREEN);
						}
						else {
							String spiedPlayerName = spiedPlayer.getName();
							assert spiedPlayerName != null;
							playerComponent = Component.text(spiedPlayerName)
								.color(NamedTextColor.GRAY)
								.decorate(TextDecoration.ITALIC)
								.hoverEvent(
									HoverEvent.showText(Component.text("[Offline]").color(NamedTextColor.GRAY)));
						}
						Component messageComponent =
							getPrefix().append(Component.text("- ").color(NamedTextColor.DARK_GRAY))
								.append(playerComponent);
						sender.sendMessage(messageComponent);
					}
					return 1;
				})));
		root.then(command);
		
		registrar.register(root.build());
	}
	
	@Override
	public void onDisable() {
		commandSpyHandler.unload();
		if (updateCheckTask != null) {
			try {
				updateCheckTask.cancel();
			}
			catch (IllegalStateException e) {
				// Ignore
			}
		}
		instance = null;
	}
	
	@NotNull
	public static Sqy getInstance() {
		if (instance == null) {
			throw new IllegalStateException("Plugin is not enabled");
		}
		return instance;
	}
	
}
