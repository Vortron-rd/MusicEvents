package musify.handlers;

import ivorius.reccomplex.commands.CommandVanilla;
import ivorius.reccomplex.commands.RCTextStyle;
import ivorius.reccomplex.shadow.mcopts.commands.parameters.MCP;
import ivorius.reccomplex.shadow.mcopts.commands.parameters.Parameters;
import ivorius.reccomplex.world.gen.feature.WorldStructureGenerationData;
import musify.Musify;
import musify.config.BiomeMusicConfig;
import musify.musicplayer.MusicPlayer;
import net.minecraft.command.CommandException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ivorius.reccomplex.commands.structure.sight.CommandSightCheck.list;
import static ivorius.reccomplex.shadow.mcopts.commands.parameters.Parameters.expect;
import static musify.handlers.BiomeMusicEventHandler.*;
import static musify.handlers.HandleCombatMusic.combatMusicPlayer;

@Mod.EventBusSubscriber
@SideOnly(Side.CLIENT)
public class RecurrentHandler {

    private static String currentDungeon = null;

    public static ITextComponent getStructuresAt(EntityPlayer player) throws CommandException {
        Parameters parameters = Parameters.of(new String[0], expect()::declare);
        World world = player.getEntityWorld();

        BlockPos pos = parameters.get(0).to(MCP.pos(player.getPosition(), false)).require();

        List<ITextComponent> names = WorldStructureGenerationData.get(world).entriesAt(pos)
                .map(RCTextStyle::sight)
                .collect(Collectors.toCollection(ArrayList::new));

        return list(names);
    }

    public static void handleRecurrentComplexMusic(EntityPlayer player) throws CommandException {
        if (player == null || player.world == null) return;
        ITextComponent structures = RecurrentHandler.getStructuresAt(player);
        if (structures.toString().isEmpty()) return;
        Musify.LOGGER.debug(structures.getUnformattedText());
        for (String entry : BiomeMusicConfig.recurrentComplexOptions.recurrentComplexMusicList) {
            if (!entry.contains(":")) continue;

            String[] parts = entry.split(":");
            if (parts.length != 2) continue;

            String structureName = parts[0].trim().toLowerCase();
            String musicFile = parts[1].trim();

            Musify.LOGGER.debug("-----------------------------------");
            Musify.LOGGER.debug(structureName);
            Musify.LOGGER.debug(musicFile);
            Musify.LOGGER.debug(structures.getUnformattedText());
            Musify.LOGGER.debug(structures.toString().toLowerCase().contains(structureName));
            Musify.LOGGER.debug("-----------------------------------");

            if (structures.toString().toLowerCase().contains(structureName)) {
                recurrentCount = 200;
                isDungeonMusicPlaying = true;
                currentDungeon = structureName;
                if (currentMusicFile != null && currentMusicFile.equals(musicFile)) {
                    return; // Already playing this music
                }
                if (activeTagMusic != null) {
                    activeTagMusic.stopWithFadeOut(BiomeMusicConfig.fadeOptions.customMusicFadeOutTime);
                    activeTagMusic = null;
                }
                if (combatMusicPlayer != null) {
                    combatMusicPlayer.stopWithFadeOut(BiomeMusicConfig.fadeOptions.customMusicFadeOutTime);
                    combatMusicPlayer = null;
                }
                if (activeMusic != null) {
                    activeMusic.stopWithFadeOut(BiomeMusicConfig.fadeOptions.customMusicFadeOutTime);
                    activeMusic = null;
                    activeMusic = new MusicPlayer(musicFile, true);
                    activeMusic.playWithFadeIn(BiomeMusicConfig.fadeOptions.customMusicFadeInTime);
                    currentMusicFile = musicFile;
                } else if (activeTagMusic == null) {
                    activeTagMusic = new MusicPlayer(musicFile, true);
                    activeTagMusic.playWithFadeIn(BiomeMusicConfig.fadeOptions.customMusicFadeInTime);
                    currentMusicFile = musicFile;
                }
                return;
            }
        }
    }

    public static void checkCorrectDungeon(EntityPlayer player) throws CommandException {
        if (player == null || player.world == null) return;
        ITextComponent structures = RecurrentHandler.getStructuresAt(player);
        Musify.LOGGER.debug(structures.getUnformattedText());

        if (structures.toString().isEmpty()) return;

        for (String entry : BiomeMusicConfig.recurrentComplexOptions.recurrentComplexMusicList) {
            if (!entry.contains(":")) continue;

            String[] parts = entry.split(":");
            if (parts.length != 2) continue;

            String structureName = parts[0].trim().toLowerCase();

            Musify.LOGGER.debug("2 -----------------------------------");
            Musify.LOGGER.debug(structureName);
            Musify.LOGGER.debug(structures.getUnformattedText());
            Musify.LOGGER.debug(structures.toString().toLowerCase().contains(structureName));
            Musify.LOGGER.debug("2 -----------------------------------");

            if (structures.toString().toLowerCase().contains(structureName)) {
                recurrentCount = 200;
                return;
            }
        }
    }
}
