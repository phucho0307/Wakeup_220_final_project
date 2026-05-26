package knox.wakeup.server.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SoundService {

    private static final Logger log = LoggerFactory.getLogger(SoundService.class);

    private static final float SAMPLE_RATE = 44100f;
    private static final double TONE_HZ = 880.0;
    private static final int TONE_MS = 350;
    private static final int GAP_MS = 200;

    private final AtomicBoolean playing = new AtomicBoolean(false);
    private Thread worker;

    public synchronized void start() {
        if (playing.get()) return;
        playing.set(true);
        worker = new Thread(this::loop, "wakeup-sound");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        playing.set(false);
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    public boolean isPlaying() { return playing.get(); }

    @PreDestroy
    public void shutdown() { stop(); }

    private void loop() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        SourceDataLine line = null;
        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            byte[] tone = buildTone(TONE_HZ, TONE_MS);
            byte[] silence = new byte[(int) (SAMPLE_RATE * 2 * (GAP_MS / 1000.0))];
            while (playing.get() && !Thread.currentThread().isInterrupted()) {
                line.write(tone, 0, tone.length);
                if (!playing.get()) break;
                line.write(silence, 0, silence.length);
            }
        } catch (Exception e) {
            log.warn("Server sound unavailable: {}", e.getMessage());
        } finally {
            if (line != null) {
                try { line.drain(); } catch (Exception ignored) {}
                line.stop();
                line.close();
            }
        }
    }

    private byte[] buildTone(double hz, int durationMs) {
        int samples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
        byte[] data = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i * hz / SAMPLE_RATE;
            short value = (short) (Math.sin(angle) * 0.5 * Short.MAX_VALUE);
            data[i * 2] = (byte) (value & 0xff);
            data[i * 2 + 1] = (byte) ((value >> 8) & 0xff);
        }
        return data;
    }
}
