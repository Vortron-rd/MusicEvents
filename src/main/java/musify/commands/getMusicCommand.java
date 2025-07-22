package musify.commands;

import musify.musicplayer.MusicPlayer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import static musify.handlers.BiomeMusicEventHandler.activeMusic;
import static musify.handlers.BiomeMusicEventHandler.activeTagMusic;
import static musify.handlers.HandleCombatMusic.getCombatMusicPlayer;

public class getMusicCommand extends CommandBase {
    @Override
    public String getName() {
        return "music";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/music";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int ig = 0;
        if (activeMusic != null && activeMusic.isPlaying()) {
            String result = activeMusic.getFilename();
            sender.sendMessage(new TextComponentString("Current Biome Music: " + result));
            ig++;
        }
        if (activeTagMusic != null && activeTagMusic.isPlaying()) {
            String result = activeTagMusic.getFilename();
            sender.sendMessage(new TextComponentString("Current Biome Tag Music: " + result));
            ig++;
        }
        if (getCombatMusicPlayer() != null && getCombatMusicPlayer().isPlaying()) {
            String result = getCombatMusicPlayer().getFilename();
            sender.sendMessage(new TextComponentString("Current Combat Music (playing or in background): " + result));
            ig++;
        }
        if (ig == 0) {
            sender.sendMessage(new TextComponentString("No Music Playing"));
        }
    }
}
