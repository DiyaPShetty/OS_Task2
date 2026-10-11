package dining_philosophers.semaphore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class qstn1 {

    static final int N = 4;
    static final int MEALS = 3;

    static Semaphore[] chopsticks = new Semaphore[N];
    static Semaphore mutex = new Semaphore(1, true);
    static Semaphore[] permission = new Semaphore[N];

    static Queue<Integer> waitingQueue = new ArrayDeque<>();
    static String[] state = new String[N];
    static int[] owner = {-1, -1, -1, -1};
    static List<String> events = new ArrayList<>();

    // This method must be called while holding mutex.
    static void recordEvent(String message) {
        StringBuilder json = new StringBuilder("{");

        json.append("\"message\":").append(quote(message)).append(",");
        json.append("\"states\":[");

        for (int i = 0; i < N; i++) {
            if (i > 0) json.append(",");
            json.append(quote(state[i]));
        }

        json.append("],\"owners\":[");

        for (int i = 0; i < N; i++) {
            if (i > 0) json.append(",");
            json.append(owner[i]);
        }

        json.append("],\"queue\":[");

        int count = 0;
        for (int id : waitingQueue) {
            if (count++ > 0) json.append(",");
            json.append(id);
        }

        json.append("]}");

        events.add(json.toString());
        System.out.println(message);
    }

    // Convert a Java string into a safe JSON string.
    static String quote(String s) {
        StringBuilder result = new StringBuilder("\"");

        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': result.append("\\\"");
                    break;
                case '\\': result.append("\\\\");
                    break;
                case '\n': result.append("\\n");
                    break;
                case '\r': result.append("\\r");
                    break;
                case '\t': result.append("\\t");
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

    // Called only while holding mutex.
    static void schedule() {
        if (waitingQueue.isEmpty()) return;

        // Only the first hungry philosopher is considered.
        int id = waitingQueue.peek();

        int left = id;
        int right = (id + 1) % N;

        // Try to acquire both chopsticks without blocking.
        if (chopsticks[left].tryAcquire()) {
            if (chopsticks[right].tryAcquire()) {

                waitingQueue.remove();

                owner[left] = id;
                owner[right] = id;
                state[id] = "Eating";

                recordEvent("Philosopher " + id
                        + " acquired both chopsticks and is eating");

                permission[id].release();

            } else {
                // Return the first chopstick if the second is unavailable.
                chopsticks[left].release();
            }
        }
    }

    static class Philosopher extends Thread {
        int id;

        Philosopher(int id) {
            this.id = id;
        }

        public void run() {
            try {
                for (int meal = 1; meal <= MEALS; meal++) {

                    // Thinking
                    mutex.acquire();
                    try {
                        state[id] = "Thinking";
                        recordEvent("Philosopher " + id + " is thinking");
                    } finally {
                        mutex.release();
                    }

                    Thread.sleep(500);

                    // Become hungry and enter the FIFO queue.
                    mutex.acquire();
                    try {
                        state[id] = "Hungry";
                        waitingQueue.add(id);

                        recordEvent("Philosopher " + id
                                + " is hungry and joins the queue");

                        schedule();
                    } finally {
                        mutex.release();
                    }

                    // Wait until the scheduler grants both chopsticks.
                    permission[id].acquire();

                    // Eating
                    Thread.sleep(700);

                    // Release chopsticks and schedule the next philosopher.
                    mutex.acquire();
                    try {
                        state[id] = "Releasing";
                        recordEvent("Philosopher " + id
                                + " is releasing chopsticks");

                        int left = id;
                        int right = (id + 1) % N;

                        owner[left] = -1;
                        owner[right] = -1;

                        chopsticks[left].release();
                        chopsticks[right].release();

                        state[id] = "Thinking";
                        recordEvent("Philosopher " + id
                                + " finished eating");

                        schedule();
                    } finally {
                        mutex.release();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Philosopher " + id + " was interrupted");
            }
        }
    }

    // Generate the HTML simulation from the actual recorded execution.
    static void generateHTML() throws IOException {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < events.size(); i++) {
            if (i > 0) json.append(",");
            json.append(events.get(i));
        }

        json.append("]");

        String html = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Dining Philosophers Simulation</title>

<style>
body {
    font-family: Arial, sans-serif;
    background: #f4f6f9;
    color: #202938;
    text-align: center;
    padding: 20px;
}
.panel {
    background: white;
    max-width: 850px;
    margin: 15px auto;
    padding: 20px;
    border-radius: 14px;
    box-shadow: 0 3px 12px #0001;
}
#table {
    width: 340px;
    height: 340px;
    position: relative;
    margin: 15px auto;
    border-radius: 50%;
    background: #e8d9bf;
    border: 12px solid #b99b72;
}
.philosopher, .fork {
    position: absolute;
    transform: translate(-50%, -50%);
    display: flex;
    justify-content: center;
    align-items: center;
    font-weight: bold;
    text-align: center;
}
.philosopher {
    width: 85px;
    height: 58px;
    border-radius: 14px;
    color: white;
    font-size: 13px;
    flex-direction: column;
}
.fork {
    width: 54px;
    height: 32px;
    border-radius: 10px;
    font-size: 12px;
    background: #e1e5e9;
    border: 2px solid #9aa3ae;
}
.Thinking { background: #64748b; }
.Hungry, .Waiting { background: #d99a19; }
.Eating { background: #26834d; }
.Releasing { background: #3578bc; }

button {
    padding: 10px 18px;
    margin: 5px;
    border: 0;
    border-radius: 8px;
    background: #315b91;
    color: white;
    cursor: pointer;
}
#timeline {
    text-align: left;
    max-height: 220px;
    overflow-y: auto;
}
li { margin: 7px 0; }
</style>
</head>

<body>
<h1>Dining Philosophers Problem</h1>
<p>Semaphore + FIFO Queue | 4 Philosophers</p>

<div class="panel">
    <div id="table">
        <div class="philosopher" id="p0"
             style="left:50%;top:8%">P0</div>
        <div class="philosopher" id="p1"
             style="left:92%;top:50%">P1</div>
        <div class="philosopher" id="p2"
             style="left:50%;top:92%">P2</div>
        <div class="philosopher" id="p3"
             style="left:8%;top:50%">P3</div>

        <div class="fork" id="f0"
             style="left:75%;top:23%">F0</div>
        <div class="fork" id="f1"
             style="left:75%;top:77%">F1</div>
        <div class="fork" id="f2"
             style="left:25%;top:77%">F2</div>
        <div class="fork" id="f3"
             style="left:25%;top:23%">F3</div>
    </div>

    <div id="event">Ready to start</div>

    <button onclick="play()">▶ Play</button>
    <button onclick="pause()">⏸ Pause</button>
    <button onclick="reset()">↺ Reset</button>

    <p>
        Speed:
        <input id="speed" type="range"
               min="200" max="2000" value="900" step="100">
    </p>

    <p id="counter"></p>
</div>

<div class="panel">
    <h2>Current States</h2>
    <div id="states"></div>
    <h2>Waiting Queue</h2>
    <div id="queue"></div>
</div>

<div class="panel">
    <h2>Execution Timeline</h2>
    <ol id="timeline"></ol>
</div>

<script>
const events = EXECUTION_DATA;
let index = 0;
let timer = null;

function render() {
    if (index >= events.length) {
        pause();
        return;
    }

    const e = events[index];

    for (let i = 0; i < 4; i++) {
        const p = document.getElementById("p" + i);
        p.className = "philosopher " + e.states[i];
        p.innerHTML = "P" + i + "<br>" + e.states[i];

        const f = document.getElementById("f" + i);

        if (e.owners[i] === -1) {
            f.style.background = "#e1e5e9";
            f.style.color = "#202938";
            f.innerHTML = "F" + i + "<br>Free";
        } else {
            f.style.background = "#26834d";
            f.style.color = "white";
            f.innerHTML = "F" + i + "<br>P" + e.owners[i];
        }
    }

    document.getElementById("event").textContent = e.message;
    document.getElementById("counter").textContent =
        "Event " + (index + 1) + " of " + events.length;

    document.getElementById("states").textContent =
        e.states.map((s, i) => "P" + i + ": " + s).join(" | ");

    document.getElementById("queue").textContent =
        e.queue.length
            ? e.queue.map(x => "P" + x).join(" → ")
            : "Empty";

    const li = document.createElement("li");
    li.textContent = e.message;
    document.getElementById("timeline").appendChild(li);
    li.scrollIntoView({block: "nearest"});

    index++;
}

function play() {
    if (timer !== null || index >= events.length) return;

    render();

    timer = setInterval(() => {
        if (index >= events.length) {
            pause();
        } else {
            render();
        }
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

    document.getElementById("timeline").innerHTML = "";
    document.getElementById("event").textContent = "Ready to start";
    document.getElementById("counter").textContent = "";
    document.getElementById("states").textContent = "";
    document.getElementById("queue").textContent = "";

    for (let i = 0; i < 4; i++) {
        const p = document.getElementById("p" + i);
        p.className = "philosopher Thinking";
        p.innerHTML = "P" + i + "<br>Thinking";

        const f = document.getElementById("f" + i);
        f.style.background = "#e1e5e9";
        f.style.color = "#202938";
        f.innerHTML = "F" + i + "<br>Free";
    }
}

reset();
</script>
</body>
</html>
""";

        html = html.replace("EXECUTION_DATA", json.toString());

        Path file = Path.of("dining_philosophers_semaphore.html");
        Files.writeString(file, html, StandardCharsets.UTF_8);

        System.out.println("\nHTML simulation generated: "
                + file.toAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        Arrays.fill(state, "Thinking");

        for (int i = 0; i < N; i++) {
            chopsticks[i] = new Semaphore(1);
            permission[i] = new Semaphore(0);
        }

        Philosopher[] philosophers = new Philosopher[N];

        for (int i = 0; i < N; i++) {
            philosophers[i] = new Philosopher(i);
            philosophers[i].start();
        }

        for (Philosopher p : philosophers) {
            p.join();
        }

        generateHTML();
    }
}