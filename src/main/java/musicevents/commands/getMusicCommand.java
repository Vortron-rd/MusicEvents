package musicevents.commands;

import musicevents.MusicEvents;
import musicevents.handlers.EventHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import static musicevents.handlers.EventHandler.activeMusic;


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
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (activeMusic != null && activeMusic.isPlaying() && !activeMusic.isPaused()) {
            sender.sendMessage(new TextComponentString("Current Music Selector : " + EventHandler.MusicType + "\n Current Song : " + activeMusic.getFilename()));
            if(activeMusic.playlist != null) sender.sendMessage(new TextComponentString("Current Playlist: " + activeMusic.playlist));
        }
        else {
            sender.sendMessage(new TextComponentString("No " + MusicEvents.NAME + " songs are playing."));
        }
    }
}
