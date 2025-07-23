package musify.config;

import musify.Musify;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class MusicFileHandler {

    public static List<String> getAvailableMusicFiles() {
        List<String> musicFiles = new ArrayList<>();

        File folder = Musify.musicFolder;

        if (folder != null && folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".ogg"));
            File[] files2 = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

            if (files != null) {
                for (File file : files) {
                    musicFiles.add(file.getName());
                }
            }
            if (files2 != null) {
                for (File file : files2) {
                    musicFiles.add(file.getName());
                }
            }
        }

        return musicFiles;
    }
}