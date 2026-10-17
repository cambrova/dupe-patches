package io.github.traelmyx.dupepatches.test;

import io.github.traelmyx.dupepatches.DupePatches;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
*///?} else
import net.minecraft.world.entity.projectile.ThrownTrident;
//? if >=26 {
/*import net.minecraft.world.inventory.ContainerInput;
*///?} else
import net.minecraft.world.inventory.ClickType;

/**
 * Every patch is tested twice: with the patch on, and as a control with the patch off for that one player or block,
 * where the dupe has to happen. If the control can't reproduce the dupe on a Minecraft version, Mojang fixed it
 * there, and the control instead checks the vanilla protection.
 */
public final class DupeTests {
	/** name -> test, registered by the loader-specific class. Every entry runs once with the patch and once without. */
	public static final java.util.Map<String, Consumer<GameTestHelper>> TESTS = new java.util.LinkedHashMap<>();

	static {
		pair("trident", DupeTests::trident);
		pair("book", DupeTests::book);
		pair("gravity_block", DupeTests::gravityBlock);
	}

	private DupeTests() {
	}

	private interface Test {
		void run(GameTestHelper helper, boolean on);
	}

	private static void pair(String name, Test test) {
		TESTS.put(name, helper -> test.run(helper, true));
		TESTS.put(name + "_vanilla", helper -> test.run(helper, false));
	}

	// Helpers

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
	}

	/** A real survival player with a connection, so packets and saving behave as on a server. */
	static ServerPlayer player(GameTestHelper helper, String name, boolean on) {
		MinecraftServer server = helper.getLevel().getServer();
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name), false);
		var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		if (!on) DupePatches.VANILLA.add(player.getUUID());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		//? if >=1.21.11 {
		/*player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		*///?}
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
		player.setPos(at.x, at.y, at.z);
		return player;
	}

	static void leave(GameTestHelper helper, ServerPlayer player) {
		player.getInventory().clearContent();
		player.inventoryMenu.getCraftSlots().clearContent();
		helper.getLevel().getServer().getPlayerList().remove(player);
		DupePatches.VANILLA.remove(player.getUUID());
	}

	static BlockPos vanillaAt(GameTestHelper helper, BlockPos relative, boolean on) {
		BlockPos pos = helper.absolutePos(relative);
		if (!on) DupePatches.VANILLA.add(pos);
		return pos;
	}

	static AABB area(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 8, origin.getY() + 8, origin.getZ() + 8);
	}

	static int count(Container container, Item item) {
		int n = 0;
		for (int i = 0; i < container.getContainerSize(); i++) if (container.getItem(i).is(item)) n += container.getItem(i).getCount();
		return n;
	}

	static int droppedCount(GameTestHelper helper, Item item) {
		int n = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area(helper))) if (entity.getItem().is(item)) n += entity.getItem().getCount();
		return n;
	}

	static void click(ServerPlayer player, int slot, int button, String type) {
		//? if >=26 {
		/*player.containerMenu.clicked(slot, button, ContainerInput.valueOf(type), player);
		*///?} else
		player.containerMenu.clicked(slot, button, ClickType.valueOf(type), player);
	}

	// Trident: swap the trident being charged into the crafting grid, then throw it in the same tick

	private static void trident(GameTestHelper helper, boolean on) {
		floor(helper);
		ServerPlayer player = player(helper, on ? "test-trident" : "test-trident-v", on);
		player.getInventory().setItem(0, new ItemStack(Items.TRIDENT));
		//? if >=1.21.11 {
		/*player.getInventory().setSelectedSlot(0);
		*///?} else
		player.getInventory().selected = 0;
		player.startUsingItem(InteractionHand.MAIN_HAND);
		ItemStack charging = player.getUseItem();
		helper.assertTrue(charging.is(Items.TRIDENT), "the player is not charging the trident");
		// SWAP with button 0 = hotbar slot 0; menu slot 1 is the first crafting grid slot of the player's inventory
		click(player, 1, 0, "SWAP");
		boolean moved = player.inventoryMenu.getCraftSlots().getItem(0) == charging;
		// What the server does when the release packet arrives: release with 20 ticks of charge
		charging.releaseUsing(helper.getLevel(), player, charging.getUseDuration(player) - 20);
		player.stopUsingItem();
		helper.runAfterDelay(2, () -> {
			int held = count(player.getInventory(), Items.TRIDENT) + count(player.inventoryMenu.getCraftSlots(), Items.TRIDENT);
			int thrown = helper.getLevel().getEntitiesOfClass(ThrownTrident.class, area(helper).inflate(64)).stream()
				.filter(t -> t.getOwner() == player).toList().size();
			leave(helper, player);
			int total = held + thrown;
			//? if >=1.21.11 {
			/*// Fixed by Mojang: releasing the trident uses up the charged stack wherever it is, so nothing to patch
			helper.assertTrue(total == 1, "the trident was duplicated: " + held + " held, " + thrown + " thrown");
			*///?} else {
			if (on) {
				helper.assertTrue(!moved, "the charged trident was moved");
				helper.assertTrue(total == 1, "with the patch there are " + total + " tridents (" + held + " held, " + thrown + " thrown)");
			} else helper.assertTrue(moved && total == 2, "vanilla did not duplicate the trident: moved=" + moved + ", " + held + " held, " + thrown + " thrown");
			//?}
			helper.succeed();
		});
	}

	// Book: sign a book with a 40-character title, which the 1.21.1 network code still accepts

	private static void book(GameTestHelper helper, boolean on) {
		floor(helper);
		ServerPlayer player = player(helper, on ? "test-book" : "test-book-v", on);
		player.getInventory().setItem(0, new ItemStack(Items.WRITABLE_BOOK));
		var sent = new ServerboundEditBookPacket(0, List.of("page one"), Optional.of("A".repeat(40)));
		ServerboundEditBookPacket received;
		try {
			FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
			ServerboundEditBookPacket.STREAM_CODEC.encode(buf, sent);
			received = ServerboundEditBookPacket.STREAM_CODEC.decode(buf);
		} catch (RuntimeException e) {
			received = null;
		}
		//? if >=1.21.11 {
		/*// Fixed by Mojang: the packet itself refuses titles over 32 characters
		leave(helper, player);
		helper.assertTrue(received == null, "the network code accepted a 40-character book title");
		helper.succeed();
		*///?} else {
		helper.assertTrue(received != null, "the network code refused a 40-character book title");
		player.connection.handleEditBook(received);
		helper.runAfterDelay(3, () -> {
			ItemStack book = player.getInventory().getItem(0);
			boolean saveable = ItemStack.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), book).isSuccess();
			leave(helper, player);
			if (on) helper.assertTrue(book.is(Items.WRITABLE_BOOK) && saveable, "the book was signed anyway: " + book + ", saveable=" + saveable);
			else helper.assertTrue(book.is(Items.WRITTEN_BOOK) && !saveable, "vanilla made a saveable book: " + book + ", saveable=" + saveable);
			helper.succeed();
		});
		//?}
	}

	// Sand: falling half over an end portal, so it touches the portal while its block position is beside it

	private static void gravityBlock(GameTestHelper helper, boolean on) {
		// Off by default; switched on here, and the control opts its own sand out
		DupePatches.GRAVITY_BLOCK.enabled = true;
		floor(helper);
		double[] xs = {2.05, 2.2, 2.4};
		java.util.List<FallingBlockEntity> sand = new java.util.ArrayList<>();
		for (int i = 0; i < xs.length; i++) helper.setBlock(new BlockPos(1, 1, 1 + i * 2), Blocks.END_PORTAL);
		helper.runAfterDelay(2, () -> {
			for (int i = 0; i < xs.length; i++) {
				Vec3 at = helper.absoluteVec(new Vec3(xs[i], 2.1, 1.5 + i * 2));
				FallingBlockEntity entity = FallingBlockEntity.fall(helper.getLevel(), helper.absolutePos(new BlockPos(5, 5, 1 + i * 2)), Blocks.SAND.defaultBlockState());
				if (!on) DupePatches.VANILLA.add(entity.getUUID());
				entity.setPos(at.x, at.y, at.z);
				entity.setDeltaMovement(0, -1.5, 0);
				sand.add(entity);
			}
		});
		helper.runAfterDelay(8, () -> {
			sand.forEach(e -> DupePatches.VANILLA.remove(e.getUUID()));
			long through = sand.stream().filter(e -> e.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION).count();
			int landed = 0;
			for (int x = 0; x < 8; x++) for (int y = 1; y < 8; y++) for (int z = 0; z < 8; z++)
				if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.SAND)) landed++;
			helper.assertTrue(through == xs.length, "only " + through + " of " + xs.length + " sand blocks went through the portal");
			if (on) helper.assertTrue(landed == 0, landed + " sand blocks also landed in the overworld");
			else helper.assertTrue(landed == xs.length, "vanilla landed " + landed + " of " + xs.length + " sand blocks in the overworld too");
			helper.succeed();
		});
	}
}
