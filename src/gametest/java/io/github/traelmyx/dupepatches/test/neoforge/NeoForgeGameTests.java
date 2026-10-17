package io.github.traelmyx.dupepatches.test.neoforge;

//? if neoforge {
import io.github.traelmyx.dupepatches.test.ClientSmoke;
import io.github.traelmyx.dupepatches.test.DupeTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;
//? if >=1.21.5 {
/*import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.List;
//?}

//? if <1.21.5 {
@GameTestHolder(NeoForgeGameTests.MOD_ID)
//?}
@Mod(NeoForgeGameTests.MOD_ID)
public class NeoForgeGameTests {
	public static final String MOD_ID = "dupepatches_gametest";
	private static final int MAX_TICKS = 100;

	public NeoForgeGameTests(IEventBus modBus, Dist dist) {
		//? if >=1.21.5 {
		/*DupeTests.TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test));
		FUNCTIONS.register(modBus);
		modBus.addListener(NeoForgeGameTests::registerTests);
		*///?}
		if (dist.isClient() && ClientSmoke.start()) {
			NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientSmoke.tick(net.minecraft.client.Minecraft.getInstance()));
		}
	}

	//? if >=1.21.5 {
	/*private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MOD_ID);
	private static final Identifier EMPTY = Identifier.fromNamespaceAndPath(MOD_ID, "empty");

	private static void registerTests(RegisterGameTestsEvent event) {
		//? if >=26 {
		Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MOD_ID, "default"));
		//?} else
		/^Holder<TestEnvironmentDefinition> env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MOD_ID, "default"));^/
		for (String name : DupeTests.TESTS.keySet()) {
			Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
			event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
				//? if >=26.3 {
				new TestData<>(env, net.minecraft.world.level.Level.OVERWORLD, EMPTY, MAX_TICKS, 0, true,
					net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 0)));
				//?} else if >=26 {
				/^new TestData<>(env, EMPTY, MAX_TICKS, 0, true,
					net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 0)));
				^///?} else {
				/^new TestData<>(env, EMPTY, MAX_TICKS, 0, true,
					net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false)));
				^///?}
		}
	}
	*///?} else {
	@GameTestGenerator
	public static List<TestFunction> tests() {
		List<TestFunction> tests = new ArrayList<>();
		for (var entry : DupeTests.TESTS.entrySet()) {
			Consumer<GameTestHelper> test = entry.getValue();
			// Own batch per test, so a stray entity or block from one test can't reach another
			tests.add(new TestFunction(entry.getKey(), MOD_ID + ":" + entry.getKey(), MOD_ID + ":empty", Rotation.NONE, MAX_TICKS, 0, true, false, 1, 1, false, test));
		}
		return tests;
	}
	//?}
}
//?}
