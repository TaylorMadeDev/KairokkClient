package com.kairokk.client;

import com.kairokk.KairokkClient;
import icyllis.modernui.core.Core;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.AdvancedPlayer;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;
import icyllis.modernui.widget.TextView;

/** Plays the bundled title-screen MP3 rotation without affecting Minecraft's sound options. */
public final class KairokkMusicPlayer {
	private static final float DEFAULT_VOLUME = 0.35F;
	private static final String[] TRACKS = {
		"/assets/kairokk/music/advait_subwoofer_lullaby.mp3",
		"/assets/kairokk/music/atlas_eyes_wethands.mp3",
		"/assets/kairokk/music/pauerful_excuse.mp3"
	};
	private static final String[] TRACK_NAMES = {
		"Advait — Subwoofer Lullaby",
		"Atlas Eyes — Wethands",
		"Pauerful — Excuse"
	};
	private static final KairokkMusicPlayer INSTANCE = new KairokkMusicPlayer();

	private final Object lock = new Object();
	private volatile boolean running;
	private volatile boolean muted;
	private volatile float volume = DEFAULT_VOLUME;
	private volatile float rememberedVolume = DEFAULT_VOLUME;
	private volatile AdvancedPlayer currentPlayer;
	private volatile Thread worker;
	private volatile String nowPlaying = TRACK_NAMES[0];
	private final CopyOnWriteArrayList<WeakReference<TextView>> nowPlayingLabels = new CopyOnWriteArrayList<>();

	private KairokkMusicPlayer() {
	}

	public static KairokkMusicPlayer getInstance() {
		return INSTANCE;
	}

	public void start() {
		synchronized (lock) {
			if (running) return;
			running = true;
			// A vanilla track may already have begun before the custom title UI appears.
			Core.executeOnMainThread(() -> Minecraft.getInstance().getMusicManager().stopPlaying());
			worker = new Thread(this::runLoop, "Kairokk title music");
			worker.setDaemon(true);
			worker.start();
		}
	}

	public void stop() {
		synchronized (lock) {
			running = false;
			AdvancedPlayer player = currentPlayer;
			if (player != null) player.close();
			Thread thread = worker;
			if (thread != null) thread.interrupt();
		}
	}

	public void toggleMute() {
		setMuted(!muted);
	}

	public void setMuted(boolean value) {
		if (value) {
			if (volume > 0F) rememberedVolume = volume;
			muted = true;
			volume = 0F;
		} else {
			volume = rememberedVolume > 0F ? rememberedVolume : DEFAULT_VOLUME;
			muted = false;
		}
	}

	public void setVolume(float value) {
		float next = Math.max(0F, Math.min(1F, value));
		if (next == 0F) {
			if (volume > 0F) rememberedVolume = volume;
			volume = 0F;
			muted = true;
		} else {
			volume = next;
			rememberedVolume = next;
			muted = false;
		}
	}

	public float getVolume() {
		return volume;
	}

	public boolean isMuted() {
		return muted;
	}

	public boolean isRunning() {
		return running;
	}

	public String getNowPlaying() {
		return nowPlaying;
	}

	/** Keeps a footer label in sync as the title music rotation advances. */
	public void bindNowPlayingLabel(TextView label) {
		nowPlayingLabels.add(new WeakReference<>(label));
		Core.executeOnUiThread(() -> label.setText("Now playing  •  " + nowPlaying));
	}

	private void runLoop() {
		int trackIndex = 0;
		while (running) {
			int nextTrack = trackIndex++ % TRACKS.length;
			String resource = TRACKS[nextTrack];
			nowPlaying = TRACK_NAMES[nextTrack];
			refreshNowPlayingLabels();
			try (InputStream stream = KairokkMusicPlayer.class.getResourceAsStream(resource)) {
				if (stream == null) {
					KairokkClient.LOGGER.error("Missing Kairokk title music resource: {}", resource);
					continue;
				}
				AdvancedPlayer player = new AdvancedPlayer(stream, new GainAudioDevice());
				currentPlayer = player;
				player.play();
			} catch (IOException | JavaLayerException exception) {
				if (running) KairokkClient.LOGGER.error("Could not play Kairokk title music: {}", resource, exception);
			} finally {
				currentPlayer = null;
			}
		}
		synchronized (lock) {
			if (Thread.currentThread() == worker) {
				worker = null;
				running = false;
			}
		}
	}

	private void refreshNowPlayingLabels() {
		for (WeakReference<TextView> reference : nowPlayingLabels) {
			TextView label = reference.get();
			if (label == null) {
				nowPlayingLabels.remove(reference);
			} else {
				Core.executeOnUiThread(() -> label.setText("Now playing  •  " + nowPlaying));
			}
		}
	}

	private final class GainAudioDevice extends JavaSoundAudioDevice {
		@Override
		public void write(short[] samples, int offset, int length) throws JavaLayerException {
			float gain = volume;
			if (gain >= 0.999F) {
				super.write(samples, offset, length);
				return;
			}
			short[] scaled = new short[length];
			for (int index = 0; index < length; index++) {
				scaled[index] = (short) Math.max(Short.MIN_VALUE,
						Math.min(Short.MAX_VALUE, Math.round(samples[offset + index] * gain)));
			}
			super.write(scaled, 0, length);
		}
	}
}
