package net.kyrptonaught.serverutils.customMapLoader;

import net.kyrptonaught.serverutils.switchableresourcepacks.MusicPack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class PlayerInstanceData {
    private MusicPack masterPack;
    private MusicPack playingPack;

    private long endTime = 0;
    private Identifier currentSong;

    public PlayerInstanceData setMusic(MusicPack pack) {
        this.masterPack = pack;
        this.playingPack = new MusicPack();
        this.playingPack.songs = new LinkedHashMap<>();
        this.playingPack.play_order = pack.play_order;
        return this;
    }

    public boolean isSongFinished(long currentTime) {
        if (masterPack == null)
            return false;

        return currentTime > endTime;
    }

    public void skipSong(ServerPlayerEntity player) {
        if (currentSong != null) stopSong(player);
        playNextSong(player, System.currentTimeMillis());
    }

    public void playNextSong(ServerPlayerEntity player, long currentTime) {
        if (playingPack.songs.isEmpty()) {
            playingPack.songs.putAll(masterPack.songs);
        }

        Identifier songID;
        long songLength;

        if (playingPack.play_order == MusicPack.PLAY_ORDER.CHRONOLOGICAL) {
            songID = playingPack.songs.sequencedKeySet().getFirst();

        } else {
            int index = player.getRandom().nextInt(playingPack.songs.size());
            Iterator<Identifier> iter = playingPack.songs.keySet().iterator();
            for (int i = 0; i < index; i++) {
                iter.next();
            }
            songID = iter.next();
        }

        songLength = playingPack.songs.remove(songID) * 1000;
        endTime = currentTime + songLength;

        currentSong = songID;
        playSong(player, songID);
    }

    private void playSong(ServerPlayerEntity player, Identifier songID) {
        RegistryEntry<SoundEvent> registryEntry = RegistryEntry.of(SoundEvent.of(songID));
        Vec3d vec3d = player.getPos();

        player.networkHandler.sendPacket(new PlaySoundS2CPacket(registryEntry, SoundCategory.MUSIC, vec3d.getX(), vec3d.getY(), vec3d.getZ(), 1, 1, player.getRandom().nextLong()));
        System.out.println("Playing " + songID + " for " + player.getNameForScoreboard());
    }

    private void stopSong(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new StopSoundS2CPacket(currentSong, SoundCategory.MUSIC));
    }
}
