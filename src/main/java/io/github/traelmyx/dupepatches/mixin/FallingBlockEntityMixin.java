package io.github.traelmyx.dupepatches.mixin;

import io.github.traelmyx.dupepatches.DupePatches;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
	/**
	 * Sand dupe: a falling block that goes through an end portal keeps ticking for the rest of that tick in the
	 * dimension it left (vanilla does this on purpose), so it can land there as well as arrive on the other side.
	 * With the patch on, a falling block that has just left stops there.
	 */
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;handlePortal()V", shift = At.Shift.AFTER), cancellable = true)
	private void dupepatches$stopAfterPortal(CallbackInfo ci) {
		FallingBlockEntity self = (FallingBlockEntity) (Object) this;
		if (self.isRemoved() && DupePatches.GRAVITY_BLOCK.on(self)) ci.cancel();
	}
}
