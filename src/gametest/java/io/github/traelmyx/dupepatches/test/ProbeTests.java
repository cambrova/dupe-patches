package io.github.traelmyx.dupepatches.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Temporary: logs what vanilla does in candidate dupe setups. */
public final class ProbeTests {
	private ProbeTests() {
	}

	static void log(GameTestHelper helper, String what) {
		System.out.println("[probe] " + what);
	}

	static int blocks(GameTestHelper helper, Block block) {
		int n = 0;
		for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) for (int z = 0; z < 8; z++)
			if (helper.getBlockState(new BlockPos(x, y, z)).is(block)) n++;
		return n;
	}

	static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
	}

	static BlockState piston(Block block, Direction facing) {
		return block.defaultBlockState().setValue(PistonBaseBlock.FACING, facing);
	}

	static void pulses(GameTestHelper helper, BlockPos power, int count, int gap, int width, Runnable done) {
		for (int i = 0; i < count; i++) {
			int at = 2 + i * gap;
			helper.runAfterDelay(at, () -> helper.setBlock(power, Blocks.REDSTONE_BLOCK));
			helper.runAfterDelay(at + width, () -> helper.setBlock(power, Blocks.AIR));
		}
		helper.runAfterDelay(2 + count * gap + 20, done);
	}

	/** Sand that lands half over an end portal: its box touches the portal while its block position is beside it. */
	public static void gravity(GameTestHelper helper) {
		floor(helper);
		double[] xs = {2.05, 2.2, 2.4, 2.45};
		for (int i = 0; i < xs.length; i++) helper.setBlock(new BlockPos(1, 1, 1 + i * 2), Blocks.END_PORTAL);
		StringBuilder trace = new StringBuilder();
		helper.runAfterDelay(2, () -> {
			for (int i = 0; i < xs.length; i++) {
				Vec3 at = helper.absoluteVec(new Vec3(xs[i], 2.1, 1.5 + i * 2));
				FallingBlockEntity sand = FallingBlockEntity.fall(helper.getLevel(), helper.absolutePos(new BlockPos(5, 5, 1 + i * 2)), Blocks.SAND.defaultBlockState());
				sand.setPos(at.x, at.y, at.z);
				sand.setDeltaMovement(0, -1.5, 0);
			}
		});
		for (int t = 3; t < 12; t++) helper.runAfterDelay(t, () -> {
			int items = 0;
			for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, DupeTests.area(helper)))
				if (e.getItem().is(Items.SAND)) items += e.getItem().getCount();
			trace.append(blocks(helper, Blocks.SAND)).append('/').append(items).append('/')
				.append(helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class, DupeTests.area(helper)).size()).append(' ');
		});
		helper.runAfterDelay(14, () -> {
			var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
			int inEnd = end == null ? -1 : end.getEntitiesOfClass(FallingBlockEntity.class, new net.minecraft.world.phys.AABB(-300, -64, -300, 300, 320, 300)).size();
			log(helper, "gravity: blocks/items/falling per tick " + trace + " falling in end=" + inEnd);
			helper.succeed();
		});
	}

	/** /give next to a hopper minecart, which ticks before the display item expires. */
	public static void giveMinecart(GameTestHelper helper) {
		floor(helper);
		var cart = helper.spawn(net.minecraft.world.entity.EntityType.HOPPER_MINECART, new BlockPos(1, 1, 1));
		var player = DupeTests.player(helper, "test-cart", true);
		var server = helper.getLevel().getServer();
		helper.runAfterDelay(2, () -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(), "give @s minecraft:stick 1"));
		helper.runAfterDelay(8, () -> {
			log(helper, "give_minecart: player=" + DupeTests.count(player.getInventory(), Items.STICK) + " cart=" + DupeTests.count((net.minecraft.world.Container) cart, Items.STICK));
			DupeTests.leave(helper, player);
			cart.discard();
			helper.succeed();
		});
	}
}
