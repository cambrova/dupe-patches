package io.github.traelmyx.dupepatches.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
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

	/** TNT pushed by a piston while powered without having been updated; the dust in front pops and updates it. */
	public static void tnt(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(new BlockPos(1, 1, 3), piston(Blocks.PISTON, Direction.EAST));
		helper.setBlock(new BlockPos(2, 1, 3), Blocks.TNT);
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.REDSTONE_WIRE);
		helper.getLevel().setBlock(helper.absolutePos(new BlockPos(2, 2, 3)), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
		helper.runAfterDelay(2, () -> helper.setBlock(new BlockPos(0, 1, 3), Blocks.REDSTONE_BLOCK));
		helper.runAfterDelay(10, () -> {
			var primed = helper.getLevel().getEntitiesOfClass(PrimedTnt.class, DupeTests.area(helper));
			log(helper, "tnt: primed=" + primed.size() + " blocks=" + blocks(helper, Blocks.TNT));
			primed.forEach(e -> e.discard());
			helper.succeed();
		});
	}

	/** Wiki carpet duper: sticky piston, two slime blocks, carpet. Variant a: carpet on the piston. */
	public static void carpetA(GameTestHelper helper) {
		carpet(helper, true, Blocks.RED_CARPET, Items.RED_CARPET, "carpetA");
	}

	public static void carpetB(GameTestHelper helper) {
		carpet(helper, false, Blocks.RED_CARPET, Items.RED_CARPET, "carpetB");
	}

	public static void railA(GameTestHelper helper) {
		carpet(helper, true, Blocks.RAIL, Items.RAIL, "railA");
	}

	public static void railB(GameTestHelper helper) {
		carpet(helper, false, Blocks.RAIL, Items.RAIL, "railB");
	}

	static void carpet(GameTestHelper helper, boolean onPiston, Block block, Item item, String name) {
		floor(helper);
		// bottom row at z=3: x=1 observer-ish filler (stone), x=2 smooth stone, x=3 slime, x=4 sticky piston facing west
		helper.setBlock(new BlockPos(2, 1, 3), Blocks.SMOOTH_STONE);
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.SLIME_BLOCK);
		helper.setBlock(new BlockPos(4, 1, 3), piston(Blocks.STICKY_PISTON, Direction.WEST));
		helper.setBlock(new BlockPos(3, 2, 3), Blocks.SLIME_BLOCK);
		helper.setBlock(onPiston ? new BlockPos(4, 2, 3) : new BlockPos(3, 3, 3), block);
		int before = blocks(helper, block);
		StringBuilder trace = new StringBuilder();
		for (int t = 0; t < 40; t++) {
			int tick = t;
			helper.runAfterDelay(t, () -> trace.append(blocks(helper, block)).append(','));
		}
		pulses(helper, new BlockPos(5, 1, 3), 4, 8, 1, () -> {
			int items = 0;
			for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, DupeTests.area(helper).inflate(4)))
				if (e.getItem().is(item)) items += e.getItem().getCount();
			log(helper, name + ": before=" + before + " blocks=" + blocks(helper, block) + " items=" + items + " trace=" + trace);
			helper.succeed();
		});
	}

	/** Sand landing inside an end portal in the same tick it enters it. */
	public static void gravity(GameTestHelper helper) {
		floor(helper);
		StringBuilder out = new StringBuilder();
		double[] dys = {-0.3, -0.6, -1.0, -1.5};
		for (int i = 0; i < dys.length; i++) {
			BlockPos portal = new BlockPos(1 + i * 2, 1, 3);
			helper.setBlock(portal, Blocks.END_PORTAL);
		}
		helper.runAfterDelay(2, () -> {
			for (int i = 0; i < dys.length; i++) {
				Vec3 at = helper.absoluteVec(new Vec3(1.5 + i * 2, 2.1, 3.5));
				FallingBlockEntity sand = FallingBlockEntity.fall(helper.getLevel(), helper.absolutePos(new BlockPos(1 + i * 2, 5, 3)), Blocks.SAND.defaultBlockState());
				sand.setPos(at.x, at.y, at.z);
				sand.setDeltaMovement(0, dys[i], 0);
			}
		});
		helper.runAfterDelay(30, () -> {
			int items = 0;
			for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, DupeTests.area(helper)))
				if (e.getItem().is(Items.SAND)) items += e.getItem().getCount();
			int falling = helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class, DupeTests.area(helper)).size();
			log(helper, "gravity: overworld sand items=" + items + " blocks=" + blocks(helper, Blocks.SAND) + " falling=" + falling);
			helper.succeed();
		});
	}
}
