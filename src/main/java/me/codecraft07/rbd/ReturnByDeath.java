package me.codecraft07.rbd;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import java.util.Random;
import java.util.Set;

import static net.minecraft.stats.Stats.CUSTOM;

public class  ReturnByDeath implements ModInitializer {
	public static final String MOD_ID = "rbd";
	public static Stat<?> playTimeStat;
	public static final Identifier DEATH_SOUND_ID_1 = Identifier.fromNamespaceAndPath(MOD_ID,"return_death_death_1");
	public static final Identifier DEATH_SOUND_ID_2 = Identifier.fromNamespaceAndPath(MOD_ID,"return_death_death_2");
	public static final SoundEvent DEATH_SOUND_1 = register(DEATH_SOUND_ID_1,DEATH_SOUND_ID_1);
	public static final SoundEvent DEATH_SOUND_2 = register(DEATH_SOUND_ID_2,DEATH_SOUND_ID_2);

	public static boolean load_save = false;

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
	public static boolean pendingSound = false;
	public static boolean pendingRewind = false;
	public static String targetWorldName = "";
	public static void loadSave_server(MinecraftServer server) {
		// 1. Flag that a rewind needs to happen once the server dies
		pendingRewind = true;
		pendingSound =  true;
		targetWorldName = server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();

		var client = Minecraft.getInstance();

		// 2. Schedule the disconnect on the Client/Render thread safely
		client.execute(() -> {
			// This will safely kick the player and initiate the Integrated Server shutdown
			client.disconnectFromWorld(Component.literal("Loading save..."));
		});
	}

	@Override
	public void onInitialize() {

		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		returnTime = makeCustomStat("return_time", StatFormatter.TIME);
		playTimeStat= CUSTOM.get(returnTime);
		LOGGER.info("Hello Fabric world!");

//      Server Start Event:
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var client = Minecraft.getInstance();
			if (!client.hasSingleplayerServer()) return;
			if (Files.exists(server.getWorldPath(LevelResource.ROOT).resolve(SAVE_DIR).resolve("level.dat"))) return;

			save(server);
		});

		//Using event rather than the method itself to load the server, it's better since one It can be used on both server and client threads
		// second, it makes sures that the server is actually stopped before attempting to save game
		//Server Stopped Event
		ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
			// this is done to prevent save and quite to cause return by death.
			if (ReturnByDeath.pendingRewind) {
				ReturnByDeath.pendingRewind = false;

				var levelPath = server.getWorldPath(LevelResource.ROOT);
				var savePath = levelPath.resolve(SAVE_DIR);

				// this is the real logics , created by ThePotatoArchivist
				tryLoading(server,levelPath, savePath);


				var client = Minecraft.getInstance();
				client.execute(() -> {
					client.createWorldOpenFlows().openWorld(
							ReturnByDeath.targetWorldName,
							() -> client.gui.setScreen(new TitleScreen())
					);
				});
			}
		});

		//plays the fucking sound , why does it have to be so obfuscated
		//Player Join Event
		ServerPlayerEvents.JOIN.register((player) -> {
			boolean whichSound = new Random().nextBoolean();
			if (pendingSound){
				if (whichSound){
					player.level().playSound(null,player,ReturnByDeath.DEATH_SOUND_2,SoundSource.NEUTRAL,1.0F,1.0F);
				}
				else {
					player.level().playSound(null,player.getOnPos(),DEATH_SOUND_1,SoundSource.NEUTRAL,1.0F,1.0F);
				}
			}
		});
	}

	private static void tryLoading(MinecraftServer server,Path levelPath, Path savePath) {

		// pretty self-explanatory
		if (!Files.exists(server.getWorldPath(LevelResource.ROOT).resolve(savePath))) { new RuntimeException("The save path does not exits, will not attempt to load save");}

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

	private static SoundEvent register(final Identifier id, final Identifier soundId) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(soundId));
	}
}
