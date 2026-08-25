package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.HTSLReborn;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundSilencerMixin {

    @Inject(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (!HTSLReborn.INSTANCE.getCONFIG().getSilenceImportSounds()) return;
        if (!HTSLReborn.INSTANCE.getImporting() && !HTSLReborn.INSTANCE.getExporting()) return;

        Identifier soundId = sound.getIdentifier();
        if (SoundEvents.NOTE_BLOCK_PLING.is(soundId) || SoundEvents.UI_BUTTON_CLICK.is(soundId)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

}
