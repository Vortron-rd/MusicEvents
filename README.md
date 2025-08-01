#  *Musify!*

### *Musify!* is a versatile client-side mod designed to improve the immersion and auditory experience of Minecraft for both modpack developers and regular players. This mod allows for dynamic music customization based on in-game events like PvE Combat, Biome Exploration, Caving, and more, giving you complete control over Minecraft's soundtrack. Whether you’re crafting an immersive modpack or just want to personalize your solo adventures, *Musify!* provides a way to bring your world to life with the perfect soundtrack.

## Features
**Musify supports both .ogg and .mp3 files now!**
<br>This mod has a bunch of different features revolving around Minecraft's soundtrack. The following features are available and are polled in the specified order*:
- **JukeBox Music Mute** The mod will mute custom music when a JukeBox is playing a record, and will resume the music once the record stops playing.
- **Boss Music!** Customize music for boss mobs for a truely unique experience!
- **Doomlike Dungeons Support!** The mod will automatically save the locations of Doomlike Dungeons and can play custom music when the player is in range of a Doomlike Dungeon based on the dungeon's theme.
- **Recurrent Complex Structure Support!** The mod will automatically save the locations of Recurrent Complex structures and can play custom music when the player is in range of a Structure. What structures to save and which to not save can be configured in the config menu.
- **Dungeon Definitions!** The mod supports custom dungeon definitions, allowing you to set up custom music for specific dungeons that do not come from Recurrent or Doomlike's. This is useful for modpacks that add custom dungeons or for players who want to create their own unique dungeons.
- **Combat Music!** Enhance your battle experience by setting up custom combat music!
- **Cavern Music!** Want to feel a true Dwarf when mining underground? Customize your worlds underground music for a new experience when mining!
- **Customizable Biome Music!** This Mod allows you to customize and set up music for specific biomes, or for certain biome types. Great for that immersive feel when exploring!

<sup>* Polling order here means that the mod will check for conditions in the order specified above. it will first check for jukeboxes, then bosses, ect.

- **Main Menu Music!** Set up some custom music for Minecraft's main menu to personalize your modpack or client!
- **Ambient Mode!** Want to use sound effects or ambient tracks instead of music? Enable ambient mode to add to vanilla music instead of replacing it!

## How to use:
### 1. Set up the music folder.
The mod will take .ogg and .mp3 sound files from the folder called "music". This folder is located in the minecraft instance path (the same folder where your clients mods, resource packs and config folders are located.) which is usually your .minecraft directory, but can be in a different directory depending on if you are using another launcher like CurseForge. If this folder does not exist it will be created by the mod upon first startup. If you are a modpack developer that wants to use this mod you will have to mark this folder for export. Once you have created the folder or once the mod has created the folder for you, you can put any .ogg or .mp3 sound files you need in the folder.

<img width="151" height="201" alt="folder2" src="https://github.com/user-attachments/assets/b99b517a-b76d-4d1e-8049-7e6b8e155d36" />
<br><sup>Put the .ogg or .mp3 files in the music folder. Make sure they are .ogg or .mp3 files!

![image](https://github.com/user-attachments/assets/e1c3f13d-3f9c-44c0-88c5-eff1e8559d73)
### 2. Set up the config.
While it is possible to use the config file to set up the mod, it is recommended to use the Forge in-game config menu in the Forge mod list.

<img width="251" height="306" alt="image" src="https://github.com/user-attachments/assets/12dd244d-e4ae-4fb7-a30d-6fa89e61742a" />
<img width="1189" height="635" alt="image" src="https://github.com/user-attachments/assets/657b6387-2ace-4682-b323-c13ec16e0df2" />

### 3. Set up the music.
By default most features are disabled. 
This is because while leaving enabled features empty does not impact performance too much, they do still use some resources in the background. 
At the top of the config menu there will be a list called "Available Music Files", this can be used to see what sound files in the music folder are recognized and can be used. 
For all music configurations except boss music; recurrent complex music and doomlike dungeons music, you can specify more then one music track by seperating them with a comma e.g. [song1.ogg,song2.ogg]. 
If ambient Mode is enabled custom music will not stop vanilla music.

### Here is an explanation of how this mods features work in order:
For **Boss Music**, There is an option to set the detection range for bosses. If a boss is detected within that radius the music will change if the mob is specified as a boss mob. To set up the music for a boss mob there is the [Boss Music List]. For each boss mob you want to set up music for, create a new line and specify the mob and the music seperated by a comma: [modid:mobid,bossmusic1.ogg]. Keep in mind to not forget the Mod Id from the mob! (e.g. [lycanitesmobs:rahovart,bossmusic1.ogg]). The Boss Music has priority based on the index of the list. It will always try to play the music for the boss mob it can find first in the list. So if you have both the wither and the Ender Dragon in the list and you spawn in both of them, it will play the music from whichever mob is first in the list.

For **Doomlike Dungeons** music, the mod will automatically save key position points from doomlike dungeons to a dungeon_rooms.csv file in the instances saves folder when the dungeon is generated. The important part here is that this only happens once a dungeon generates. This means that if you add Musify to your modpack after having an established world, existing Doomlike Dungeons will _NOT_ support music playback (unless you add the position points manually). The saving of doomlike dungeons happens on the server side, so for servers, the dungeon_rooms.csv file needs only to exist on the server side.
For music playback in these dungeons, you can specify the dungeons theme, and the music to play there. Example: [vulcanic:dungeonmusic1.ogg]. You can find a list of existing themes in the Doomlike Dungeons config folder.
The [Max Distance] option for Doomlike Dungeons is the distance from the saved dungeon position (in x and z axis) a player can be for the music playback to activate. The [Max Y Level Difference] option is to specify how many blocks above or below a dungeon a player can be for music player to happen.

For **Recurrent Complex Structures**, the mod will save all Recurrent structures to a recurent_structures.csv file upon generation if that structure passes the specified conditions. This happens in the same way as for Doomlike Dungeons. To prevent the recurrent csv file from becoming too large and filled with miscellaneous structures like trees and boulders, a series of editable check have been implemented. The first check is [ignoring a structure if the name contains] a certain string. By default this is set to one entry "Tree", to prevent most of the default recurrent trees from being saved, but you can add or remove entries as you wish. The second check is a specific [ignore structure list]. All structures whos name maches an entry in this list will be ignores as well. The last check is a [volume] check, here you can set how large a structure needs to be for its data to be saved. This is to ignore structures that are too small for proper music playback.
For music playback, you can specify a structure name and its music in the [Recurrent Complex Music List]. Example: [structure_name:musicfile.ogg].
The [Extra Distance option] is to have some leeway for the structures bounding box. If set to 0, music will play only once the player is actually inside the bounding box of the strucutre. For some structures however, this might not be ideal. This option exists to create a bit of leeway for that, and allows the player to be a bit outside the bounding box of the structure while preserving music playback.

The **Dungeon Definition** option is an option which i personally do not recommend using, but left in anyways because it might be nice to have sometimes. This feature exists for custom dungeons that are not part of Recurrent or Doomlike's.
It works by specifying a block and music for a dungeon. When the option is enabled, it scans the players surroundings for blocks in the list, and if enough of the specified block and spawners are found in the radius, it will assume the player is in the specified dungeon and play the music. Example: [minecraft:stone_bricks:stonedungeonmusic1.mp3]. The dungeon check radius can be a bit performance heavy, so if you have to use this option, i recommend to not put it above 40 blocks, preferably 30 or under.

For **Combat Music**, you can set the range in which it tries to look for aggro'd mobs, the amount of aggro'd mobs you need to start the combat music, and the amount of mobs that need to be left to stop the combat music. In The [Combat Music List] you can set general combat soundtracks, If there is no [Linked SoundTrack] specified in the [Combat Music Track Link] option a random song from here will play when combat music engages. Example of Combat music list entry: [combat_music1.mp3,combatmusic2.ogg,combatmusic3.ogg]

For **Cavern Music** you are able to set the [Starting Y Level] at which cavern music starts, and the [Stop Y Level] at which it stops. Please keep in mind to place the stop Y level sufficiently above the Start Y level as to not fade-in and fade-out the cavern music too much, which can be annoying. You can set what cavern music you want in the [Cavern Music List]. Example: [undergroundmusic1.ogg,cavernmusic.mp3]

For **Biome Music**, there are two options to consider:
The [Biome Music Mapping] is used to specify custom music for individual biomes, this will overwrite the music set for tags of the biome. As with most music, multiple music options can be specified by seperating them with a comma.
Setting music for a biome tag in the [Biome Tag Music Mapping] will play this music in all biomes with that tag. If a biome has more than one tag, a random song from any tag will play with slight priority set in the biomes tag order. 
For example, if you set up two music tracks for the dry and the savanna tags, it will play one of them randomly when you enter a savanna, but will more often choose a song from the savanna tag list then from the dry tag list, because savanna is the first tag for the savanna biome. If a custom song is already playing and you enter a new biome the song will not change if the song is specified in one of the tags for that biome, unless you specify a different song in the [Biome Music Mapping].

**Fade Options** is an important but dangerous config to change on a whim. I cannot stress this enough, **If you do not know what you are doing, do not change these options.** Changing these option to bad values can and will break the mod. 
The [Fade-in] and [Fade-out] options control how fast or slow music fades in or out when changing, while you can set these to different values to customize the mod to your liking, please do not set them to excessively low or high values.
The [Polling Rate] option is the heart of the mod. Every action the mod takes is controlled by the polling rate, which acts as a sort of timer. Increasing the polling rate value slow down how fast the mod reacts to changes and the other way around for decreasing the polling rate. In general it is recommended to only try higher values for a slower and less intrusive experience if you find that the music still changes too fast even after first setting up the rest of the mod completely.

**MusicLink**. The [Combat Music Track Link] option is used if you have two of the same soundstracks of which one is a combat track. To use this, look for the normal track in the list, and in the input field give the combat variant of that track. This will cause the combat variant of the track to start silently in the background if the normal track starts playing. If the music switches to combat music, it will fade to the combat track synchronized with the normal track. If there is no musiclink specified, it will play a random song from the [Combat Music List] in the combat options.

## Disclaimer
- While the mod does work with *most* Modded entities and biomes, some might not work. The mod was tested with mods like DefiledLands and Lycanites, But some mobs from other mods with which the mod was not tested might not be compatible. Similarly for some custom biomes from other mods, they might not be recognized. However, Even if the mod does not recognize a biome, as long as they either have a parent biome or biome tags, the music set using biome tags will still work.

- Please do not forget to add the .ogg/.mp3 to the sound files when setting up music. Without .ogg/.mp3 behind the file name it will not work!
- Please do not forget to set values back to "default_music" when you want to disable something. It might break certain functions if left empty.
- For Doomlike Dungeons compatibility this mod uses a mixin. To make this work, please also install FermiumBooter. The mod will load fine without FermiumBooter, but Doomlike Dungeons support will NOT work (dungeon positions will not be saved).
