package knox.wakeup.server.model;

public class MathProblem {
    private final String text;
    private final int answer;

    public MathProblem(String text, int answer) {
        this.text = text;
        this.answer = answer;
    }

    public String getText() { return text; }
    public int getAnswer() { return answer; }
}
