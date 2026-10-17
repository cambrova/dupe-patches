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
		pair("give_hopper", DupeTests::giveHopper);
		TESTS.put("probe_gravity", ProbeTests::gravity);
		TESTS.put("probe_give_minecart", ProbeTests::giveMinecart);
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
			if (on) {
				helper.assertTrue(!moved, "the charged trident was moved");
				helper.assertTrue(total == 1, "with the patch there are " + total + " tridents (" + held + " held, " + thrown + " thrown)");
			}
			//? if >=1.21.11 {
			/*else helper.assertTrue(total == 1, "vanilla now duplicates the trident again: " + total);
			*///?} else
			else helper.assertTrue(moved && total == 2, "vanilla did not duplicate the trident: moved=" + moved + ", " + held + " held, " + thrown + " thrown");
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

	// /give into a hopper: the display-only copy that /give drops ends up in the hopper

	private static void giveHopper(GameTestHelper helper, boolean on) {
		floor(helper);
		BlockPos hopperAt = new BlockPos(1, 1, 1);
		helper.setBlock(hopperAt, Blocks.HOPPER);
		vanillaAt(helper, hopperAt, on);
		ServerPlayer player = player(helper, on ? "test-give" : "test-give-v", on);
		MinecraftServer server = helper.getLevel().getServer();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(),
			"give @s minecraft:stick 1");
		helper.runAfterDelay(5, () -> {
			int inPlayer = count(player.getInventory(), Items.STICK);
			int inHopper = count((Container) helper.getLevel().getBlockEntity(helper.absolutePos(hopperAt)), Items.STICK);
			leave(helper, player);
			DupePatches.VANILLA.remove(helper.absolutePos(hopperAt));
			helper.assertTrue(inPlayer == 1, "the player got " + inPlayer + " sticks");
			if (on) helper.assertTrue(inHopper == 0, "the hopper caught the /give display item");
			else helper.assertTrue(inHopper == 1, "the vanilla hopper did not catch the /give display item");
			helper.succeed();
		});
	}
}
