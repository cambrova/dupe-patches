package io.github.traelmyx.dupepatches.neoforge;

//? if neoforge {
import io.github.traelmyx.dupepatches.DupePatches;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(DupePatches.MOD_ID)
public class DupePatchesNeoForge {
	public DupePatchesNeoForge() {
		DupePatches.loadConfig(FMLPaths.CONFIGDIR.get());
	}
}
//?}
