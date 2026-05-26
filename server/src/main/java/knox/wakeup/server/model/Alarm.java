package knox.wakeup.server.model;

import java.time.Instant;

public class Alarm {

    public enum State { idle, armed, ringing, dismissed }

    private State state;
    private Instant deadline;
    private String problem;
    private Integer expectedAnswer;

    public Alarm(State state, Instant deadline, String problem, Integer expectedAnswer) {
        this.state = state;
        this.deadline = deadline;
        this.problem = problem;
        this.expectedAnswer = expectedAnswer;
    }

    public static Alarm idle() {
        return new Alarm(State.idle, null, null, null);
    }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }

    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }

    public Integer getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(Integer expectedAnswer) { this.expectedAnswer = expectedAnswer; }
}
