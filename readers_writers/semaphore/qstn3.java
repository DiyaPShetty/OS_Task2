package readers_writers.semaphore;

import java.util.*;
import java.nio.file.*;
import java.util.concurrent.Semaphore;

public class qstn3 {

    static final int READERS = 4;
    static final int WRITERS = 2;

    static Semaphore mutex = new Semaphore(1);
    static Semaphore writeLock = new Semaphore(1);

    static int readCount = 0;
    static int sharedData = 0;

    static List<String> events = new ArrayList<>();
    static String[] readerStates = new String[READERS];
    static String[] writerStates = new String[WRITERS];
    static boolean writerActive = false;

    static synchronized void record(String message) {
        StringBuilder json = new StringBuilder();

        json.append("{\"message\":\"")
            .append(message)
            .append("\",\"readers\":[");

        for (int i = 0; i < READERS; i++) {
            json.append("\"").append(readerStates[i]).append("\"");
            if (i < READERS - 1) json.append(",");
        }

        json.append("],\"writers\":[");

        for (int i = 0; i < WRITERS; i++) {
            json.append("\"").append(writerStates[i]).append("\"");
            if (i < WRITERS - 1) json.append(",");
        }

        json.append("],\"readCount\":").append(readCount)
            .append(",\"writerActive\":").append(writerActive)
            .append(",\"data\":").append(sharedData)
            .append("}");

        events.add(json.toString());
        System.out.println(message);
    }

    static void reader(int id) {
        try {
            readerStates[id] = "Waiting";
            record("Reader " + id + " is waiting");

            mutex.acquire();
            readCount++;

            if (readCount == 1) {
                writeLock.acquire();
            }

            readerStates[id] = "Reading";
            record("Reader " + id + " starts reading");
            mutex.release();

            Thread.sleep(700);

            mutex.acquire();
            readCount--;
            readerStates[id] = "Finished";
            record("Reader " + id + " finishes reading");

            if (readCount == 0) {
                writeLock.release();
            }
            mutex.release();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void writer(int id) {
        try {
            writerStates[id] = "Waiting";
            record("Writer " + id + " is waiting");

            writeLock.acquire();

            writerActive = true;
            writerStates[id] = "Writing";
            record("Writer " + id + " starts writing");

            sharedData++;
            Thread.sleep(900);

            writerActive = false;
            writerStates[id] = "Finished";
            record("Writer " + id + " finishes writing");

            writeLock.release();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void generateHTML(String data) throws Exception {
        String html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="UTF-8">
        <title>Readers-Writers Semaphore</title>
        <style>
        body {
            font-family: Arial;
            text-align: center;
            background: #f2f5f9;
        }
        .container {
            display: flex;
            justify-content: center;
            gap: 40px;
            flex-wrap: wrap;
            margin: 25px;
        }
        .group {
            background: white;
            padding: 20px;
            border-radius: 12px;
            min-width: 250px;
        }
        .person {
            margin: 10px;
            padding: 12px;
            border-radius: 8px;
            background: #ddd;
        }
        .Waiting { background: #ffe08a; }
        .Reading { background: #a8d5f5; }
        .Writing { background: #91dfa7; }
        .Finished { background: #ddd; }
        #resource {
            border: 3px solid #555;
            padding: 20px;
            margin: 20px auto;
            width: 250px;
            border-radius: 12px;
            background: white;
        }
        #timeline {
            max-width: 750px;
            max-height: 250px;
            overflow-y: auto;
            text-align: left;
            background: white;
            padding: 15px;
            margin: 20px auto;
        }
        button { padding: 9px 18px; margin: 5px; }
        </style>
        </head>
        <body>
        <h2>Readers-Writers - Semaphore Simulation</h2>

        <div class="container">
            <div class="group">
                <h3>Readers</h3>
                <div id="readers"></div>
            </div>
            <div class="group">
                <h3>Writers</h3>
                <div id="writers"></div>
            </div>
        </div>

        <div id="resource">
            <h3>Shared Resource</h3>
            <p id="resourceStatus">Available</p>
            <p id="dataValue">Data: 0</p>
            <p id="readerCount">Active readers: 0</p>
        </div>

        <h3 id="message">Ready</h3>
        <button onclick="play()">Play</button>
        <button onclick="pause()">Pause</button>
        <button onclick="reset()">Reset</button>
        <br><br>
        <label>Speed</label>
        <input type="range" id="speed"
               min="200" max="2000" value="900" step="100">

        <h3>Execution Timeline</h3>
        <div id="timeline"></div>

        <script>
        const events = [EXECUTION_DATA];
        let index = 0;
        let timer = null;

        function renderPeople(containerId, states, prefix) {
            const container = document.getElementById(containerId);
            container.innerHTML = "";

            states.forEach((state, i) => {
                const div = document.createElement("div");
                div.className = "person " + state;
                div.textContent = prefix + i + ": " + state;
                container.appendChild(div);
            });
        }

        function showEvent(i) {
            const e = events[i];

            document.getElementById("message").textContent =
                e.message;

            renderPeople("readers", e.readers, "Reader ");
            renderPeople("writers", e.writers, "Writer ");

            document.getElementById("resourceStatus").textContent =
                e.writerActive ? "Writer is writing" :
                e.readCount > 0 ? "Readers are reading" : "Available";

            document.getElementById("dataValue").textContent =
                "Data: " + e.data;

            document.getElementById("readerCount").textContent =
                "Active readers: " + e.readCount;

            document.getElementById("timeline").innerHTML =
                events.slice(0, i + 1)
                .map((event, n) =>
                    (n + 1) + ". " + event.message
                ).join("<br>");
        }

        function play() {
            if (timer !== null) return;

            timer = setInterval(() => {
                if (index >= events.length) {
                    pause();
                    return;
                }
                showEvent(index++);
            }, Number(document.getElementById("speed").value));
        }

        function pause() {
            clearInterval(timer);
            timer = null;
        }

        function reset() {
            pause();
            index = 0;
            showEvent(0);
        }

        showEvent(0);
        </script>
        </body>
        </html>
        """;

        html = html.replace("EXECUTION_DATA", data);
        Files.writeString(
            Path.of("readers_writers_semaphore.html"), html
        );
    }

    public static void main(String[] args) throws Exception {
        Arrays.fill(readerStates, "Thinking");
        Arrays.fill(writerStates, "Thinking");

        Thread[] threads = new Thread[READERS + WRITERS];

        for (int i = 0; i < READERS; i++) {
            final int id = i;
            threads[i] = new Thread(() -> reader(id));
        }

        for (int i = 0; i < WRITERS; i++) {
            final int id = i;
            threads[READERS + i] =
                new Thread(() -> writer(id));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        generateHTML(String.join(",", events));

        System.out.println(
            "HTML simulation generated: readers_writers_semaphore.html"
        );
    }
}
