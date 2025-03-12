package com.unleqitq.sqy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.function.Consumer;

public class UpdateChecker {
	
	private static final String URL_PATH =
		"https://api.github.com/repos/UnleqitQ/Sqy/releases/latest";
	
	public record VersionInfo(
		int major, int minor, int patch
	) {
		
		@NotNull
		public static VersionInfo fromString(@NotNull String version) {
			String[] parts = version.split("\\.");
			if (parts.length != 3) throw new IllegalArgumentException("Invalid version format");
			return new VersionInfo(
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
		}
		
		public boolean isNewerThan(@NotNull VersionInfo other) {
			if (this.major > other.major) return true;
			if (this.major < other.major) return false;
			if (this.minor > other.minor) return true;
			if (this.minor < other.minor) return false;
			return this.patch > other.patch;
		}
		
		@Override
		@NotNull
		public String toString() {
			return major + "." + minor + "." + patch;
		}
		
	}
	
	public record ReleaseData(
		String tag_name, String download_url, VersionInfo version
	) {
	
	}
	
	@Contract (" -> new")
	public static @NotNull ReleaseData loadLatestRelease() {
		try (InputStream is = new URI(URL_PATH).toURL().openStream()) {
			JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
			String tagName = json.get("tag_name").getAsString();
			String downloadUrl = json.get("assets")
				.getAsJsonArray()
				.get(0)
				.getAsJsonObject()
				.get("browser_download_url")
				.getAsString();
			String version = tagName;
			if (version.startsWith("v")) version = version.substring(1);
			return new ReleaseData(tagName, downloadUrl, VersionInfo.fromString(version));
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load latest release data", e);
		}
		catch (URISyntaxException e) {
			throw new RuntimeException("Invalid URL", e);
		}
	}
	
	public static void scheduleUpdateCheck(@NotNull Consumer<BukkitTask> taskConsumer) {
		BukkitTask[] wrappedTask = new BukkitTask[1];
		BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(
			Sqy.getInstance(), () -> {
				VersionInfo currentVersion =
					VersionInfo.fromString(Sqy.getInstance().getPluginMeta().getVersion());
				ReleaseData latestRelease = loadLatestRelease();
				if (latestRelease.version().isNewerThan(currentVersion)) {
					Sqy.getInstance().getLogger().info("New version available: " + latestRelease.tag_name());
					Thread updateThread = downloadUpdate(latestRelease);
					wrappedTask[0].cancel();
					updateThread.setUncaughtExceptionHandler((t, e) -> {
						Sqy.getInstance().getLogger().warning("Failed to download update");
						scheduleUpdateCheck(taskConsumer);
					});
				}
				else {
					Sqy.getInstance().getLogger().info("Plugin is up to date");
				}
			}, 0, 20 * 60 * 60
		);
		wrappedTask[0] = task;
		taskConsumer.accept(task);
	}
	
	public static @NotNull Thread downloadUpdate(@NotNull ReleaseData release) {
		Thread thread = new Thread(() -> {
			try (InputStream is = new URI(release.download_url()).toURL().openStream()) {
				File updateFolder = Sqy.getInstance().getServer().getUpdateFolderFile();
				Sqy.getInstance().getLogger().info("Update folder: " + updateFolder);
				if (!updateFolder.exists() && !updateFolder.mkdirs()) {
					throw new IOException("Failed to create update folder");
				}
				File updateFile = new File(updateFolder, "Sqy-%s.jar".formatted(release.version));
				if (updateFile.exists() && !updateFile.delete()) {
					throw new IOException("Failed to delete existing update file");
				}
				Sqy.getInstance().getLogger().info("Downloading update to: " + updateFile);
				Files.copy(is, updateFile.toPath());
				Sqy.getInstance().getLogger().info("Update downloaded successfully");
			}
			catch (IOException e) {
				throw new RuntimeException("Failed to download update", e);
			}
			catch (URISyntaxException e) {
				throw new RuntimeException("Invalid URL", e);
			}
		});
		thread.start();
		return thread;
	}
	
}
