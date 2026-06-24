package me.codecraft07.rbd.mixin;

import me.codecraft07.rbd.ReturnByDeath;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementMixin{


    @Shadow
    public abstract AdvancementProgress getOrStartProgress(AdvancementHolder advancement);

    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void onAdvancementAwarded(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {

        // 1. Check if the method actually awarded something new (returns true)
        if (cir.getReturnValue()) {

            // 2. Check if the ENTIRE advancement is done.
            // (Advancements like "Monster Hunter" have multiple criteria.
            // We only want to trigger when the whole thing is finished.)
            AdvancementProgress progress = this.getOrStartProgress(holder);

            if (progress.isDone()) {

                // Get the string path of the advancement (e.g., "story/mine_stone")
                String path = holder.id().getPath();

                // 3. Filter out the recipe book spam
                if (!path.startsWith("recipes/")) {
                    ReturnByDeath.save(this.player.level().getServer());
                }
                }
            }
        }
}
