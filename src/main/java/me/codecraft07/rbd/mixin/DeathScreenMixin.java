package me.codecraft07.rbd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.codecraft07.rbd.ReturnByDeath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Inject(
            method = {
                    "lambda$init$0",
                    "lambda$handleExitToTitleScreen$0"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;respawn()V"),
            cancellable = true
    )
    private void loadSave(CallbackInfo ci) {
        if (!Minecraft.getInstance().hasSingleplayerServer()) return;
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return;
        ReturnByDeath.loadSave(server);
        ci.cancel();
    }

    @ModifyExpressionValue(
            method = "init",
            at = @At(value = "CONSTANT", args = "stringValue=deathScreen.respawn")
    )
    private String changeRespawnText(String original) {
        return "deathScreen.rbd.return_by_death";
    }
}
