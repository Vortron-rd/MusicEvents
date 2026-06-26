# MusicEvents
## *Custom Music for Minecraft 1.12.2*

This mod is built upon [Musify](https://github.com/KwemberDev/BiomeMusic), and started out as a few pull requests but turned into a huge rewrite.

MusicEvents allows you to tweak Minecraft's base music system to allow for more dynamic changing of music.
It does so by switching playlists set by conditions.

The following rules are applied from bottom to top when selecting the music playlist: 
* Menu Music
  * If the player is in the Title Screen, music plays from this playlist.
* Cavern Music
  * This music plays when the player goes below a certain y-level, which is nice for setting the mood for dangerous caves. 
* Biome Music
  * Music can be set for specific biomes, including modded ones from eg. Biomes O' Plenty.
* Biome Tag Music
  * As a "Catch-all," you can set playlists for specific biome types (tags) in the Forge BiomeDictionary. (I.e "wet," "forest," "mountain," "spooky") Modded biome tags should also work.

## Music Files
All the music files referenced in the config should be placed in the /music folder in the Minecraft folder. Currently, only .mp3 and .ogg file types are supported.
After this, they may be referenced directly such as "music.mp3" or "music.ogg". 

## Creating a playlist
A playlist typically looks something like this: [music1.mp3,music2.ogg,music3.ogg]
Files are separated using a comma. You must include the file extension in the name. (.mp3, .ogg)
MusicEvents will select a random song from the list to play, and when that is done, it will randomly select another song. A song cannot be played twice in a row, unless it is the only file in the playlist.

## In-Game Config
Under Settings→Mod-Options→MusicEvents, you should be able to hot-swap values in the config file for easier testing. All the options there are fully described with tool-tips.

## Ambient Mode
In the Config file, "Ambient Mode" may be turned on to play files *alongside vanilla music.* Usually, this would be for implementing ambient noise.
