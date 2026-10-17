package io.github.traelmyx.dupepatches.mixin;

import io.github.traelmyx.dupepatches.DupePatches;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26 {
/*import net.minecraft.world.inventory.ContainerInput;
*///?} else
import net.minecraft.world.inventory.ClickType;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
	/**
	 * Trident dupe: while a trident is being charged, a swap click can move that same item into a container. On
	 * release the game throws a copy and then fails to find the original in the player's inventory, so it stays
	 * in the container. Clicks are ignored while charging a trident; the client gets the real slots back.
	 */
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	//? if >=26 {
	/*private void dupepatches$noClicksWhileChargingTrident(int slot, int button, ContainerInput input, Player player, CallbackInfo ci) {
	*///?} else
	private void dupepatches$noClicksWhileChargingTrident(int slot, int button, ClickType input, Player player, CallbackInfo ci) {
		if (player.isUsingItem() && player.getUseItem().is(Items.TRIDENT) && DupePatches.TRIDENT.on(player)) ci.cancel();
	}
}
