package io.github.traelmyx.dupepatches.mixin;

import io.github.traelmyx.dupepatches.DupePatches;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
	/**
	 * /give dupe: after giving an item, /give drops a copy that can never be picked up, only to show the pickup
	 * animation (MC-120507). Hoppers and hopper minecarts ignored that and collected it. They now leave it alone.
	 */
	@Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at = @At("HEAD"), cancellable = true)
	private static void dupepatches$skipFakeItems(Container container, ItemEntity item, CallbackInfoReturnable<Boolean> cir) {
		if (((ItemEntityAccessor) item).dupepatches$pickupDelay() != Short.MAX_VALUE) return;
		boolean on = container instanceof BlockEntity be ? DupePatches.GIVE_HOPPER.on(be.getBlockPos())
			: container instanceof Entity entity ? DupePatches.GIVE_HOPPER.on(entity) : DupePatches.GIVE_HOPPER.enabled;
		if (on) cir.setReturnValue(false);
	}
}
