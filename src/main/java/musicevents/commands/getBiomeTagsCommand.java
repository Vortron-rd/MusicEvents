package musicevents.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

public class getBiomeTagsCommand extends CommandBase {
    @Override
    public String getName() {
        return "biometags";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/biometags";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if(sender instanceof EntityPlayer) {
            Biome biome = sender.getEntityWorld().getBiome(sender.getPosition());
            sender.sendMessage(new TextComponentString("Biome : " + biome + "\n Tags : " + BiomeDictionary.getTypes(biome)));
        }
        else {
            sender.sendMessage(new TextComponentString("Only Players may send this command."));
        }
    }
}
