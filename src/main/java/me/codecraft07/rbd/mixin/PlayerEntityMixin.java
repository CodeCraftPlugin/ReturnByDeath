package me.codecraft07.rbd.mixin;

import com.mojang.authlib.GameProfile;
import me.codecraft07.rbd.ReturnByDeath;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.stats.Stats.CUSTOM;

@Mixin(ServerPlayer.class)
public abstract class PlayerEntityMixin extends Player implements PacketContextProvider {
	//time for one loop in seconds
	private int timer = 600;

	public PlayerEntityMixin(Level level, GameProfile gameProfile) {
		super(level, gameProfile);
	}


	@Shadow
	public abstract ServerStatsCounter getStats();

	@Shadow
	public abstract void resetStat(Stat<?> stat);


	@Shadow
	@Final
	private MinecraftServer server;


	@Inject(method = "tick",at  = @At("HEAD"))
	private void print(CallbackInfo ci){

		this.awardStat(ReturnByDeath.playTimeStat);
		int playtime = this.getStats().getValue(ReturnByDeath.playTimeStat);

		if ((playtime/20)>=timer){
			ReturnByDeath.save(this.server);
			this.resetStat(ReturnByDeath.playTimeStat);
			ReturnByDeath.LOGGER.info("made a save");
		}
	}



}