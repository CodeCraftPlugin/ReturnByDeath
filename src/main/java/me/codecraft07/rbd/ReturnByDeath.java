package me.codecraft07.rbd;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

import static net.minecraft.stats.Stats.CUSTOM;

public class  ReturnByDeath implements ModInitializer {
	public static final String MOD_ID = "rbd";
	public static Stat<?> playTimeStat;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

//	MIT License
//
//	Copyright (c) 2025 ThePotatoArchivist
//
//	Permission is hereby granted, free of charge, to any person obtaining a copy
//	of this software and associated documentation files (the "Software"), to deal
//	in the Software without restriction, including without limitation the rights
//	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//	copies of the Software, and to permit persons to whom the Software is
//	furnished to do so, subject to the following conditions:
//
//	The above copyright notice and this permission notice shall be included in all
//	copies or substantial portions of the Software.
//
//	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//	SOFTWARE.

	public static final String SAVE_DIR = MOD_ID + "_save";

	public static final Set<String> IGNORED_PATHS = Set.of(
			SAVE_DIR,
			"session.lock"
	);
	public static Identifier returnTime;

	private static void deleteRecursive(Path path) throws IOException {
		try (var stream = Files.walk(path)) {
			for (var path2 : (Iterable<Path>) stream.sorted(Comparator.reverseOrder())::iterator)
				Files.delete(path2);
		}
	}

	private static void copyRecursive(Path src, Path dest) throws IOException {
		if (Files.isDirectory(src))
			FileUtils.copyDirectoryToDirectory(src.toFile(), dest.toFile());
		else
			Files.copy(src, dest.resolve(src.getFileName()));
	}

	public static void save(MinecraftServer server) {
		server.saveEverything(false, true, true);
		var levelPath = server.getWorldPath(LevelResource.ROOT);
		var savePath = levelPath.resolve(SAVE_DIR);
		try {
			if (Files.isDirectory(savePath))
				try (var stream = Files.newDirectoryStream(savePath)) {
					for (var path : stream)
						deleteRecursive(path);
				}
			else
				Files.createDirectory(savePath);

			try (var stream = Files.newDirectoryStream(levelPath)) {
				for (var path : stream) {
					if (IGNORED_PATHS.contains(path.getFileName().toString())) continue;
					copyRecursive(path, savePath);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error creating save: ", e);
		}
	}

	public static void loadSave(MinecraftServer server) {
		var levelPath = server.getWorldPath(LevelResource.ROOT);
		var client = Minecraft.getInstance();
		client.disconnectFromWorld(Component.literal("Loading save..."));

		var savePath = levelPath.resolve(SAVE_DIR);
		try {
			try (var stream = Files.newDirectoryStream(levelPath)) {
				for (var path : stream) {
					if (IGNORED_PATHS.contains(path.getFileName().toString())) continue;
					deleteRecursive(path);
				}
			}
			try (var stream = Files.newDirectoryStream(savePath)) {
				for (var path : stream) {
					copyRecursive(path, levelPath);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error loading save: ", e);
		}

		client.createWorldOpenFlows().openWorld(levelPath.normalize().getFileName().toString(), () -> client.gui.setScreen(new TitleScreen()));
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		returnTime = makeCustomStat("return_time", StatFormatter.TIME);
		playTimeStat= CUSTOM.get(returnTime);
		LOGGER.info("Hello Fabric world!");
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var client = Minecraft.getInstance();
			if (!client.hasSingleplayerServer()) return;
			if (Files.exists(server.getWorldPath(LevelResource.ROOT).resolve(SAVE_DIR).resolve("level.dat"))) return;

			save(server);
		});
	}

	public static Identifier makeCustomStat(final String id, final StatFormatter formatter) {
		Identifier location = ReturnByDeath.id(id);
		Registry.register(BuiltInRegistries.CUSTOM_STAT, id, location);
		CUSTOM.get(location, formatter);
		return location;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
