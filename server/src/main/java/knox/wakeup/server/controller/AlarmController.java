package knox.wakeup.server.controller;

import knox.wakeup.server.model.Alarm;
import knox.wakeup.server.service.AlarmService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/alarm")
@CrossOrigin(origins = "*")
public class AlarmController {

    private final AlarmService service;

    public AlarmController(AlarmService service) {
        this.service = service;
    }

    @PostMapping("/set")
    public ResponseEntity<?> set(@RequestParam String user,
                                 @RequestParam(required = false) Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return error("Seconds must be a positive integer.");
        }
        Alarm alarm = service.setAlarm(user, seconds);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", user);
        body.put("state", alarm.getState().name());
        body.put("secondsRemaining", service.secondsRemaining(alarm));
        body.put("message", "Alarm armed for " + seconds + " seconds.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestParam String user) {
        Alarm alarm = service.status(user);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", user);
        body.put("state", alarm.getState().name());
        body.put("secondsRemaining", service.secondsRemaining(alarm));
        body.put("problem", alarm.getProblem());
        body.put("message", messageFor(alarm));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/solve")
    public ResponseEntity<?> solve(@RequestParam String user,
                                   @RequestBody(required = false) Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("answer")) {
            return error("Answer must be an integer.");
        }
        Object raw = payload.get("answer");
        Integer answer = parseInt(raw);
        if (answer == null) {
            return error("Answer must be an integer.");
        }
        AlarmService.SolveResult result = service.solve(user, answer);
        switch (result.getKind()) {
            case NOT_RINGING:
                return error("There is no ringing alarm to solve.");
            case CORRECT: {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("user", user);
                body.put("state", "dismissed");
                body.put("correct", true);
                body.put("message", "Correct. Alarm dismissed. Good morning.");
                return ResponseEntity.ok(body);
            }
            case WRONG:
            default: {
                Alarm alarm = result.getAlarm();
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("user", user);
                body.put("state", "ringing");
                body.put("correct", false);
                body.put("problem", alarm.getProblem());
                body.put("message", "Wrong answer. The alarm is still ringing.");
                return ResponseEntity.ok(body);
            }
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(@RequestParam String user) {
        Alarm idle = service.cancel(user);
        if (idle == null) {
            return error("No armed alarm to cancel.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", user);
        body.put("state", "idle");
        body.put("message", "Alarm cancelled.");
        return ResponseEntity.ok(body);
    }

    private static ResponseEntity<?> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }

    private static Integer parseInt(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Integer i) return i;
        if (raw instanceof Long l) return Math.toIntExact(l);
        if (raw instanceof Number n) {
            double d = n.doubleValue();
            if (d != Math.floor(d) || Double.isInfinite(d)) return null;
            return (int) d;
        }
        if (raw instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static String messageFor(Alarm alarm) {
        return switch (alarm.getState()) {
            case idle -> "No alarm is set.";
            case armed -> "Alarm is counting down.";
            case ringing -> "Wake up! Solve the problem to stop the alarm.";
            case dismissed -> "Alarm dismissed.";
        };
    }
}
