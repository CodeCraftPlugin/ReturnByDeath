package me.codecraft07.rbd.mixin;

import com.mojang.authlib.GameProfile;
import me.codecraft07.rbd.ReturnByDeath;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ServerPlayer.RespawnConfig;

@Mixin(ServerPlayer.class)
public abstract class PlayerEntityMixin extends Player implements PacketContextProvider {
	//time for one loop in seconds
	private int timer = 1200;

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


	@Shadow
	public abstract ServerLevel level();

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

	@Inject(method = "die", at = @At("HEAD"), cancellable = true)
	public void onFatalDamage(DamageSource source, CallbackInfo ci){

		if(!server.getGameRules().get(GameRules.IMMEDIATE_RESPAWN).booleanValue()) return;
		if (!Minecraft.getInstance().hasSingleplayerServer()) return;
		var server = Minecraft.getInstance().getSingleplayerServer();
		if (server == null) return;
		ReturnByDeath.loadSave_server(server);
		ci.cancel();
	}

	@Inject(
			method = "setRespawnPosition",
			at = @At("HEAD")
	)
	private void save(RespawnConfig respawn, boolean sendMessage, CallbackInfo ci) {
		if (Minecraft.getInstance().hasSingleplayerServer())
			ReturnByDeath.save(server);
	}


}
