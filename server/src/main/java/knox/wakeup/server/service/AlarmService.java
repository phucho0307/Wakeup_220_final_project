package knox.wakeup.server.service;

import knox.wakeup.server.model.Alarm;
import knox.wakeup.server.model.MathProblem;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AlarmService {

    private final ConcurrentHashMap<String, Alarm> registry = new ConcurrentHashMap<>();
    private final ConfigService config;
    private final SoundService sound;

    public AlarmService(ConfigService config, SoundService sound) {
        this.config = config;
        this.sound = sound;
    }

    public synchronized Alarm setAlarm(String user, int seconds) {
        Instant deadline = Instant.now().plusSeconds(seconds);
        Alarm alarm = new Alarm(Alarm.State.armed, deadline, null, null);
        registry.put(user, alarm);
        refreshSound();
        return alarm;
    }

    public synchronized Alarm status(String user) {
        Alarm alarm = registry.get(user);
        if (alarm == null) {
            return Alarm.idle();
        }
        if (alarm.getState() == Alarm.State.armed && !Instant.now().isBefore(alarm.getDeadline())) {
            MathProblem problem = generateProblem();
            alarm.setState(Alarm.State.ringing);
            alarm.setProblem(problem.getText());
            alarm.setExpectedAnswer(problem.getAnswer());
            refreshSound();
        } else if (alarm.getState() == Alarm.State.dismissed) {
            Alarm fresh = Alarm.idle();
            registry.put(user, fresh);
            return fresh;
        }
        return alarm;
    }

    public synchronized SolveResult solve(String user, int answer) {
        Alarm alarm = registry.get(user);
        if (alarm == null || alarm.getState() != Alarm.State.ringing) {
            return SolveResult.notRinging();
        }
        if (answer == alarm.getExpectedAnswer()) {
            alarm.setState(Alarm.State.dismissed);
            alarm.setProblem(null);
            alarm.setExpectedAnswer(null);
            alarm.setDeadline(null);
            refreshSound();
            return SolveResult.correct(alarm);
        }
        return SolveResult.wrong(alarm);
    }

    public synchronized Alarm cancel(String user) {
        Alarm alarm = registry.get(user);
        if (alarm == null || alarm.getState() != Alarm.State.armed) {
            return null;
        }
        Alarm idle = Alarm.idle();
        registry.put(user, idle);
        refreshSound();
        return idle;
    }

    public long secondsRemaining(Alarm alarm) {
        if (alarm.getState() != Alarm.State.armed || alarm.getDeadline() == null) {
            return 0L;
        }
        long secs = Duration.between(Instant.now(), alarm.getDeadline()).getSeconds();
        return Math.max(0L, secs);
    }

    private void refreshSound() {
        boolean anyRinging = registry.values().stream()
                .anyMatch(a -> a.getState() == Alarm.State.ringing);
        if (anyRinging) {
            sound.start();
        } else {
            sound.stop();
        }
    }

    MathProblem generateProblem() {
        int min = config.getMinOperand();
        int max = config.getMaxOperand();
        int ops = Math.max(1, config.getOperationCount());

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int first = rnd.nextInt(min, max + 1);
        StringBuilder text = new StringBuilder(Integer.toString(first));
        int result = first;
        for (int i = 0; i < ops; i++) {
            boolean plus = rnd.nextBoolean();
            int operand = rnd.nextInt(min, max + 1);
            text.append(' ').append(plus ? '+' : '-').append(' ').append(operand);
            result = plus ? result + operand : result - operand;
        }
        return new MathProblem(text.toString(), result);
    }

    public static final class SolveResult {
        public enum Kind { NOT_RINGING, CORRECT, WRONG }
        private final Kind kind;
        private final Alarm alarm;

        private SolveResult(Kind kind, Alarm alarm) {
            this.kind = kind;
            this.alarm = alarm;
        }

        public static SolveResult notRinging() { return new SolveResult(Kind.NOT_RINGING, null); }
        public static SolveResult correct(Alarm a) { return new SolveResult(Kind.CORRECT, a); }
        public static SolveResult wrong(Alarm a) { return new SolveResult(Kind.WRONG, a); }

        public Kind getKind() { return kind; }
        public Alarm getAlarm() { return alarm; }
    }
}
