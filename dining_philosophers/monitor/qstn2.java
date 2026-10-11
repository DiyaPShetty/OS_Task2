package dining_philosophers.monitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class qstn2 {

    static final int N = 4;
    static final int MEALS = 3;

    static final String THINKING = "Thinking";
    static final String HUNGRY = "Hungry";
    static final String WAITING = "Waiting";
    static final String ACQUIRING = "Acquiring Forks";
    static final String EATING = "Eating";
    static final String RELEASING = "Releasing Forks";

    static class DiningMonitor {
        private final String[] state = new String[N];
        private final int[] forkOwner = new int[N];
        private final List<String> events = new ArrayList<>();

        DiningMonitor() {
            Arrays.fill(state, THINKING);
            Arrays.fill(forkOwner, -1);
            record("Simulation started");
        }

        // Escape strings so they can safely be stored in JSON.
        private String quote(String s) {
            StringBuilder result = new StringBuilder("\"");

            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"':
                        result.append("\\\"");
                        break;
                    case '\\':
                        result.append("\\\\");
                        break;
                    case '\n':
                        result.append("\\n");
                        break;
                    case '\r':
                        result.append("\\r");
                        break;
                    case '\t':
                        result.append("\\t");
                        break;
                    default:
                        if (c < 32) {
                            result.append(String.format("\\u%04x", (int) c));
                        } else {
                            result.append(c);
                        }
                }
            }

            return result.append("\"").toString();
        }

        // Called only while holding the monitor lock.
        private void record(String message) {
            StringBuilder json = new StringBuilder("{");

            json.append("\"message\":").append(quote(message));
            json.append(",\"states\":[");

            for (int i = 0; i < N; i++) {
                if (i > 0) json.append(",");
                json.append(quote(state[i]));
            }

            json.append("],\"forks\":[");

            for (int i = 0; i < N; i++) {
                if (i > 0) json.append(",");
                json.append(forkOwner[i]);
            }

            json.append("]}");

            events.add(json.toString());
            System.out.println(message);
        }

        private int left(int i) {
            return (i + N - 1) % N;
        }

        private int right(int i) {
            return (i + 1) % N;
        }

        // Check whether philosopher i can eat.
        private void test(int i) {
            if ((state[i].equals(HUNGRY) ||
                 state[i].equals(WAITING)) &&
                !state[left(i)].equals(EATING) &&
                !state[right(i)].equals(EATING) &&
                forkOwner[i] == -1 &&
                forkOwner[right(i)] == -1) {

                state[i] = ACQUIRING;
                record("Philosopher " + i + " is acquiring forks");

                // Allocate both forks to philosopher i.
                forkOwner[i] = i;
                forkOwner[right(i)] = i;

                state[i] = EATING;
                record("Philosopher " + i + " starts eating");

                notifyAll();
            }
        }

        synchronized void think(int i) {
            state[i] = THINKING;
            record("Philosopher " + i + " is thinking");
        }

        synchronized void pickup(int i) throws InterruptedException {
            state[i] = HUNGRY;
            record("Philosopher " + i + " is hungry");

            test(i);

            while (!state[i].equals(EATING)) {
                state[i] = WAITING;
                record("Philosopher " + i + " is waiting");
                wait();
            }
        }

        synchronized void putdown(int i) {
            state[i] = RELEASING;
            record("Philosopher " + i + " is releasing forks");

            // Release the two forks used by philosopher i.
            forkOwner[i] = -1;
            forkOwner[right(i)] = -1;

            state[i] = THINKING;
            record("Philosopher " + i + " finished eating");

            // Check the neighboring philosophers.
            test(left(i));
            test(right(i));
        }

        synchronized String getEvents() {
            return String.join(",", events);
        }
    }

    static void generateHTML(String eventData) throws IOException {

        String html = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Dining Philosophers Monitor</title>

<style>
body {
    font-family: Arial, sans-serif;
    text-align: center;
    background: #f3f6fa;
    color: #202938;
    padding: 15px;
}
.table {
    width: 430px;
    height: 430px;
    border: 10px solid #987044;
    border-radius: 50%;
    margin: 40px auto;
    position: relative;
    background: #e8d5b5;
}
.philosopher {
    position: absolute;
    width: 125px;
    padding: 12px 4px;
    border-radius: 12px;
    font-weight: bold;
    font-size: 13px;
    border: 2px solid #555;
    background: #ddd;
    z-index: 2;
}
#p0 { top: -28px; left: 145px; }
#p1 { right: -60px; top: 155px; }
#p2 { bottom: -28px; left: 145px; }
#p3 { left: -60px; top: 155px; }

.Thinking { background: #b7d7f0; }
.Hungry, .Waiting { background: #ffe08a; }
.AcquiringForks { background: #ffc078; }
.Eating { background: #91dfa7; }
.ReleasingForks { background: #e5b4ed; }

.fork {
    position: absolute;
    width: 82px;
    padding: 9px 2px;
    border-radius: 10px;
    border: 2px solid #444;
    background: #b7e3bc;
    font-weight: bold;
    font-size: 12px;
    z-index: 1;
}
.used {
    background: #f19a85;
}
#f0 { top: 55px; right: 75px; }
#f1 { bottom: 55px; right: 75px; }
#f2 { bottom: 55px; left: 75px; }
#f3 { top: 55px; left: 75px; }

button {
    padding: 9px 18px;
    margin: 5px;
    cursor: pointer;
}
#timeline {
    max-width: 750px;
    margin: 20px auto;
    text-align: left;
    background: white;
    padding: 15px;
    border-radius: 10px;
    max-height: 250px;
    overflow-y: auto;
}
</style>
</head>

<body>
<h2>Dining Philosophers - Monitor Simulation</h2>
<p>Four philosophers sharing four chopsticks</p>

<p>🟢 Available chopstick &nbsp;&nbsp; 🔴 In use</p>

<div class="table">
    <div class="philosopher" id="p0">P0</div>
    <div class="philosopher" id="p1">P1</div>
    <div class="philosopher" id="p2">P2</div>
    <div class="philosopher" id="p3">P3</div>

    <div class="fork" id="f0">Fork 0</div>
    <div class="fork" id="f1">Fork 1</div>
    <div class="fork" id="f2">Fork 2</div>
    <div class="fork" id="f3">Fork 3</div>
</div>

<h3 id="message">Ready</h3>
<p id="counter"></p>

<button onclick="play()">Play</button>
<button onclick="pause()">Pause</button>
<button onclick="reset()">Reset</button>

<br><br>
<label>Animation speed:</label>
<input type="range" id="speed"
       min="200" max="2000" value="900" step="100">

<h3>Execution Timeline</h3>
<div id="timeline"></div>

<script>
const events = [EXECUTION_DATA];

let index = 0;
let timer = null;

function showEvent(i) {
    if (i < 0 || i >= events.length) return;

    const e = events[i];

    document.getElementById("message").textContent = e.message;
    document.getElementById("counter").textContent =
        "Event " + (i + 1) + " of " + events.length;

    for (let p = 0; p < 4; p++) {
        const el = document.getElementById("p" + p);
        const className = e.states[p].replaceAll(" ", "");

        el.className = "philosopher " + className;
        el.textContent = "P" + p + ": " + e.states[p];
    }

    for (let f = 0; f < 4; f++) {
        const el = document.getElementById("f" + f);
        const owner = e.forks[f];

        if (owner === -1) {
            el.className = "fork";
            el.textContent = "Fork " + f + ": Free";
        } else {
            el.className = "fork used";
            el.textContent = "Fork " + f + ": P" + owner;
        }
    }

    const timeline = document.getElementById("timeline");
    timeline.textContent = "";

    for (let n = 0; n <= i; n++) {
        const line = document.createElement("div");
        line.textContent = (n + 1) + ". " + events[n].message;
        timeline.appendChild(line);
    }

    timeline.scrollTop = timeline.scrollHeight;
}

function play() {
    if (timer !== null || index >= events.length) return;

    showEvent(index);
    index++;

    timer = setInterval(() => {
        if (index >= events.length) {
            pause();
            return;
        }

        showEvent(index);
        index++;
    }, Number(document.getElementById("speed").value));
}

function pause() {
    if (timer !== null) {
        clearInterval(timer);
        timer = null;
    }
}

function reset() {
    pause();
    index = 0;

    if (events.length > 0) {
        showEvent(0);
        index = 1;
    } else {
        document.getElementById("message").textContent =
            "No events available";
        document.getElementById("counter").textContent = "";
        document.getElementById("timeline").textContent = "";
    }
}

reset();
</script>
</body>
</html>
""";

        html = html.replace("EXECUTION_DATA", eventData);

        Path file = Path.of("dining_philosophers_monitor.html");
        Files.writeString(file, html, StandardCharsets.UTF_8);

        System.out.println("HTML simulation generated: "
                + file.toAbsolutePath());
    }

    public static void main(String[] args) throws Exception {

        DiningMonitor monitor = new DiningMonitor();
        Thread[] philosophers = new Thread[N];

        for (int i = 0; i < N; i++) {
            final int id = i;

            philosophers[i] = new Thread(() -> {
                try {
                    for (int meal = 0; meal < MEALS; meal++) {
                        monitor.think(id);
                        Thread.sleep(500);

                        monitor.pickup(id);
                        Thread.sleep(700);

                        monitor.putdown(id);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println(
                        "Philosopher " + id + " was interrupted"
                    );
                }
            });

            philosophers[i].start();
        }

        for (Thread t : philosophers) {
            t.join();
        }

        generateHTML(monitor.getEvents());

        System.out.println(
            "HTML simulation generated: "
            + "dining_philosophers_monitor.html"
        );
    }
}