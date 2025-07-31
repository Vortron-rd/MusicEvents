package musify.config;

import musify.Musify;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Config(modid = Musify.MODID)
public class BiomeMusicConfig {

	@Config.Comment("What music to play on the main menu. DEFAULT: [default_music]")
	@Config.Name("Main Menu Music")
	public static String acmainMenuMusic = "default_music";

	@Config.Name("Available Music Files")
	@Config.Comment("List of recognized .ogg music files. \nWhen setting up custom music, include .ogg extension and make sure to spell the name of the file correctly. \nThe mod will work even if the music you specify is not in this list if the music is in the correct folder, \nas this is just a second verification step to make sure the music file is correctly placed.")
	public static String[] abavailableMusicFiles = new String[0]; // Start with an empty array

	@Config.Name("Biome Music Map")
	@Config.Comment("Biome Music Mapping. List of all recognised biomes and a corresponding input field for the music. Music specified in here will overwrite the music from biome tags.")
	public static Map<String, String> biomeMusicMap = new HashMap<>();

	@Config.Name("Biome Tag Music Map")
	@Config.Comment("Biome Tag Music Mapping. List of all Biome Tags. \nExample: If you want a certain music to play in all biomes with snow, use the snowy tag.")
	public static Map<String, String> biomeTagMusicMap = new HashMap<>();

	@Config.Name("Fade Options")
	@Config.Comment("Fade Options. These control the music fading. \nDO NOT TOUCH UNLESS YOU KNOW EXACTLY WHAT YOU ARE DOING, THIS CAN AND WILL BREAK THE MOD IF GIVEN INCORRECT VALES. DEFAULTS: [20000,20000,140,10000]")
	public static final FadeOptions fadeOptions = new FadeOptions();

	@Config.Name("Ambient Mode")
	@Config.Comment("Enable or Disable Ambient mode. In this mode vanilla music will not be turned off when a custom music is set for a biome." +
					"\nEnable if you want to use this mod for ambience sound tracks instead of music.")
	public static boolean adambientMode = false;

	@Config.Name("Combat Music Options")
	@Config.Comment("Combat options to toggle or set.")
	public static final CombatOptions combatOptions = new CombatOptions();

	@Config.Comment("Link normal music to battle music here!")
	public static Map<String, String> musicLink = new HashMap<>();

	@Config.Name("Cavern Music Options")
	@Config.Comment("Cavern Music Options.")
	public static final UndergroundOptions cpundergroundOptions = new UndergroundOptions();

	@Config.Name("Boss Music Options")
	@Config.Comment("Boss Music Options.")
	public static final BossMusicOptions bossMusicOptions = new BossMusicOptions();

	@Config.Name("Dungeon Definition Options")
	@Config.Comment("Dungeon Definition Options. This is used to define what a dungeon is and what music to play in it." +
					"\nA dungeon consists of a certain block and a certain amount of spawners within a radius." +
					"\nThis option is moreso for custom and modded dungeons than vanilla ones.")
	public static final DungeonDefinitionOptions dungeonDefinitionOptions = new DungeonDefinitionOptions();

	@Config.Name("Recurrent Complex Structure Options")
	@Config.Comment("Recurrent Complex Structure Options. This is used to define what music to play in Recurrent Complex structures.")
	public static final RecurrentComplexOptions recurrentComplexOptions = new RecurrentComplexOptions();

	@Config.Name("Doomlike Dungeons Options")
	@Config.Comment("Doomlike Dungeons Options. This is used to define what music to play in Doomlike Dungeons structures. It works based on the Doomlike Dungeon Themes.")
	public static final DoomlikeDungeonsOptions doomlikeDungeonsOptions = new DoomlikeDungeonsOptions();


	@Config.Name("Misc Options.")
	@Config.Comment("Miscelanious Options.")
	public static final MiscOptions miscOptions = new MiscOptions();

	@Mod.EventBusSubscriber(modid = Musify.MODID)
	public static class EventHandler {

		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if (event.getModID().equals(Musify.MODID)) {
				ConfigManager.sync(Musify.MODID, Config.Type.INSTANCE);
				updateBiomeList();
				updateBiomeTagList();
				updateMusicList();
			}
		}
	}

	public static void updateBiomeList() {
		IForgeRegistry<Biome> biomeRegistry = RegistryManager.ACTIVE.getRegistry(Biome.class);
		for (Biome biome : biomeRegistry) {
			String biomeName = biome.getRegistryName().toString();
			biomeMusicMap.putIfAbsent(biomeName, "default_music");
		}
		ConfigManager.sync(Musify.MODID, Config.Type.INSTANCE);
	}

	public static void updateBiomeTagList() {
		IForgeRegistry<Biome> biomeRegistry = RegistryManager.ACTIVE.getRegistry(Biome.class);

		Set<String> allTags = biomeRegistry.getValues().stream()
				.flatMap(biome -> BiomeDictionary.getTypes(biome).stream())
				.map(type -> type.getName().toLowerCase())
				.collect(Collectors.toSet());

		for (String tag : allTags) {
			biomeTagMusicMap.putIfAbsent(tag, "default_music");
		}

		ConfigManager.sync(Musify.MODID, Config.Type.INSTANCE);
	}


	public static void updateMusicList() {

		String[] musicFiles = MusicFileHandler.getAvailableMusicFiles().toArray(new String[0]);
		abavailableMusicFiles = musicFiles;

		for (String music : musicFiles) {
			musicLink.putIfAbsent(music, "");
		}
		ConfigManager.sync(Musify.MODID, Config.Type.INSTANCE);
	}

	public static class FadeOptions {

		@Config.Name("DANGEROUS OPTION! | Polling Rate")
		@Config.Comment("Polling rate for the music change. This is a dangerous option to change if you don't know what you are doing! Default: 140 | [INT / TICKS]")
		public int pollingRate = 140;

//		@Config.Name("DEPRECATED UNUSED | Vanilla Fade-out")
//		@Config.Comment("Vanilla Music Fade-out Time. Default: 10000 | [INT / MS]")
//		public int vanillaMusicFadeOutTime = 7000;

		@Config.Name("Biome Music Fade-in")
		@Config.Comment("Custom Music Fade-in Time. Default: 20000 | [INT / MS]")
		public int customMusicFadeInTime = 12500;

		@Config.Name("Biome Music Fade-out")
		@Config.Comment("Custom Music Fade-out Time. Default: 20000 | [INT / MS]")
		public int customMusicFadeOutTime = 10000;

		@Config.Name("Combat Music Fade-in Time")
		@Config.Comment("Custom Combat Music Fade-in Time. Default: 10000 | [INT / MS]")
		public int combatMusicFadeInTime = 8500;

		@Config.Name("Music Volume Multiplier")
		@Config.Comment("The Volume Multiplier for custom music.")
		public double musicVolumeMultiplier = 0.8;
	}

	public static class CombatOptions {

		@Config.Name("Combat Music")
		@Config.Comment("Enable or Disable Combat Music.")
		public boolean enableCombatMusic = false;

		@Config.Name("Combat Music List")
		@Config.Comment("Put any music you want to be played during combat encounters in here.")
		public String combatMusicList = "default_music";

		@Config.Name("Combat Music Range")
		@Config.Comment("Range radius in which aggro'd mobs are counted for combat music trigger")
		public int combatRadius = 15;

		@Config.Name("Combat Music Start Number")
		@Config.Comment("The amount of mobs needed to start the combat music. always put higher than stop amount.")
		public int combatStartNumber = 5;

		@Config.Name("Combat Music Stop Number")
		@Config.Comment("The amount of mobs that should be left before the combat music stops. always put lower than start number.")
		public int combatStopNumber = 2;

	}

	public static class UndergroundOptions {
		@Config.Name("Cavern Music")
		@Config.Comment("Enable for music under a certain Y level. Good for cavern music.")
		public boolean enableUndergroundMusic = false;

		@Config.Name("Cavern Music Start Level")
		@Config.Comment("The Y level at which cavern music starts.")
		public int undergroundMusicYLevelStart = 40;

		@Config.Name("Cavern Music Stop Level")
		@Config.Comment("The Y Level at which Cavern Music Stops. To prevent frequent switching when at the start level, please set this value higher then the start Y level.")
		public int undergroundMusicYLevelStop = 50;

		@Config.Name("Cavern Music list:")
		@Config.Comment("The Cavern Music that will play underground.")
		public String CavernMusic = "default_music";
	}

	public static class BossMusicOptions {

		@Config.Name("Boss Music")
		@Config.Comment("Enable or Disable Boss Music!")
		public boolean enableBossMusic = false;

		@Config.Name("Boss Music List")
		@Config.Comment("Set A boss mob and its music here! boss mob followed by a comma for its music e.g. [lycanitesmobs:rahovart,doomfear.ogg]")
		public String[] bossMusicList = new String[0];

		@Config.Name("Boss mob detection range.")
		@Config.Comment("The range in which the mod will check for a boss mob.")
		public int bossMusicRange = 50;
	}

	public static class MiscOptions {

		@Config.Name("Jukebox Detection range")
		@Config.Comment("Range in which the mod will detect Jukeboxes and silence the music when in range of Jukebox that is playing.")
		public int jukeboxRange = 25;
	}

	public static class DungeonDefinitionOptions {

		@Config.Name("Enable Dungeon Music")
		@Config.Comment("Enable or Disable Dungeon Music.")
		public boolean enableDungeonMusic = false;

		@Config.Name("Dungeon Music List")
		@Config.Comment("Put any music you want to be played in dungeons in here.")
		public String[] dungeonDefinitionList = new String[0];

		@Config.Name("Dungeon check Radius")
		@Config.Comment("The radius in which the mod will detect a dungeon. Default: 40.")
		public int dungeonRadius = 40;

		@Config.Name("Min Spawners to consider a dungeon")
		@Config.Comment("The minimum amount of spawners that need to be present in a dungeon for the music to play. Default: 3.")
		public int minSpawners = 3;

		@Config.Name("Min blocks")
		@Config.Comment("The minimum amount of blocks defined in the dungeon list that need to be present to consider a dungeon. Default: 10.")
		public int minBlocks = 20;
	}

	public static class RecurrentComplexOptions {
		@Config.Name("Recurrent Complex Dungeon Music")
		@Config.Comment("Enable or Disable Recurrent Complex Dungeon Music. This will play music when a Recurrent Complex structure is detected.")
		public boolean enableRecurrentComplexMusic = false;

		@Config.Name("Recurrent Complex Music List")
		@Config.Comment("Put any music you want to be played in Recurrent Complex structures in here. \nThis will play when a Recurrent Complex structure is detected." + "\nExample: [structure_name:music_file.ogg]\n")
		public String[] recurrentComplexMusicList = new String[0];

		@Config.Name("Recurrent Complex minimal structure volume")
		@Config.Comment("The minimum volume of a Recurrent Complex structure to be considered for music playback. \nThis is calculated as width * height * depth of the structure. Default: 200.")
		public int recurrentMinimalStructureVolume = 200;

		@Config.Name("Recurrent Complex ignored structures list")
		@Config.Comment("The Recurrent Complex structures that the mod should ignore. \nThis is useful for structures that are too small or not suitable for music playback.")
		public String[] recurrentComplexIgnoreStructure = new String[]{"LodgepolePine", "AlascaCedar", "WhiteWillowLarge", "QuakingAspen", "Basswood", "Fiberpalm"};

		@Config.Name("Recurrent Complex ignore structure if name contains")
		@Config.Comment("The mod will not save data on Recurrent Complex structure if their name contains any of the specified strings. \nThis is useful to ignore trees for example.")
		public String[] recurrentComplexIgnoreNameContains = new String[]{"Tree"};
	}

	public static class DoomlikeDungeonsOptions {

		@Config.Name("Doomlike Dungeons Music")
		@Config.Comment("Enable or Disable Doomlike Dungeons Music. This will play music when a Doomlike Dungeon is detected.")
		public boolean enableDoomlikeDungeonsMusic = false;

		@Config.Name("Doomlike Dungeons Music List")
		@Config.Comment("Put any music you want to be played for any Doomlike Dungeon theme in here. \nThis will play when a Doomlike Dungeon is detected." + "\nExample: [structure_name:music_file.ogg]\n")
		public String[] doomlikeDungeonsMusicList = new String[0];

		@Config.Name("Doomlike Dungeons max distance")
		@Config.Comment("The maximum distance from the player in which the mod will detect a Doomlike Dungeon. Default: 40.")
		public int DoomlikeDistance = 40;

		@Config.Name("Doomlike Dungeons max Y level")
		@Config.Comment("The maximum Y level difference in which the mod will detect a Doomlike Dungeon. Default: 7.")
		public int DoomlikeMaxYLevel = 7;
	}
}
