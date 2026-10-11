package readers_writers.monitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class qstn4 {
    static final int READERS = 4;
    static final int WRITERS = 2;
    static final int ROUNDS = 3;

    static int readerCount = 0;
    static int sharedData = 0;
    static boolean writing = false;
    static int waitingWriters = 0;
    static final String[] readerStates = new String[READERS];
    static final String[] writerStates = new String[WRITERS];
    static final List<String> events = new ArrayList<>();

    static String json(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 32) out.append(String.format("\\u%04x", (int)c));
                    else out.append(c);
            }
        }
        return out.append('"').toString();
    }

    // Call only while holding the class monitor lock.
    static synchronized void record(String message) {
        StringBuilder e = new StringBuilder("{\"message\":").append(json(message));
        e.append(",\"data\":").append(sharedData);
        e.append(",\"readersInside\":").append(readerCount);
        e.append(",\"writing\":").append(writing);
        e.append(",\"readers\":[");
        for (int i = 0; i < READERS; i++) {
            if (i > 0) e.append(',');
            e.append(json(readerStates[i] == null ? "Waiting" : readerStates[i]));
        }
        e.append("],\"writers\":[");
        for (int i = 0; i < WRITERS; i++) {
            if (i > 0) e.append(',');
            e.append(json(writerStates[i] == null ? "Waiting" : writerStates[i]));
        }
        e.append("]}");
        events.add(e.toString());
        System.out.println(message);
    }

    static synchronized void startReading(int id, int round) throws InterruptedException {
        readerStates[id] = "Waiting";
        record("Reader " + (id + 1) + " is waiting for the shared resource");
        while (writing || waitingWriters > 0) qstn4.class.wait();
        readerCount++;
        readerStates[id] = "Reading";
        record("Reader " + (id + 1) + " started reading - Round " + round);
    }

    static synchronized void finishReading(int id) {
        readerCount--;
        readerStates[id] = "Finished";
        record("Reader " + (id + 1) + " finished reading");
        if (readerCount == 0) qstn4.class.notifyAll();
    }

    static synchronized void startWriting(int id, int round) throws InterruptedException {
        writerStates[id] = "Waiting";
        waitingWriters++;
        record("Writer " + (id + 1) + " is waiting for the shared resource");
        try {
            while (writing || readerCount > 0) qstn4.class.wait();
            writing = true;
            writerStates[id] = "Writing";
            record("Writer " + (id + 1) + " started writing - Round " + round);
        } finally {
            waitingWriters--;
            // Wake other waiters if this writer was interrupted before acquiring access.
            qstn4.class.notifyAll();
        }
    }

    static synchronized void finishWriting(int id) {
        writing = false;
        writerStates[id] = "Finished";
        record("Writer " + (id + 1) + " finished writing");
        qstn4.class.notifyAll();
    }

    static void markDone(boolean reader, int id) {
        synchronized (qstn4.class) {
            if (reader) readerStates[id] = "Done";
            else writerStates[id] = "Done";
            record((reader ? "Reader " : "Writer ") + (id + 1) + " completed all rounds");
        }
    }

    static void runReader(int id) {
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                Thread.sleep(250);
                startReading(id, round);
                int observed;
                synchronized (qstn4.class) { observed = sharedData; }
                System.out.println("Reader " + (id + 1) + " is reading Data = " + observed);
                Thread.sleep(350);
                finishReading(id);
                Thread.sleep(150);
            }
            markDone(true, id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            synchronized (qstn4.class) {
                readerStates[id] = "Interrupted";
                record("Reader " + (id + 1) + " was interrupted");
            }
        }
    }

    static void runWriter(int id) {
        boolean acquired = false;
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                Thread.sleep(400);
                startWriting(id, round);
                acquired = true;
                synchronized (qstn4.class) {
                    sharedData++;
                    record("Writer " + (id + 1) + " updated shared data to " + sharedData);
                }
                Thread.sleep(450);
                finishWriting(id);
                acquired = false;
                Thread.sleep(200);
            }
            markDone(false, id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            synchronized (qstn4.class) {
                if (acquired) {
                    writing = false;
                    qstn4.class.notifyAll();
                }
                writerStates[id] = "Interrupted";
                record("Writer " + (id + 1) + " was interrupted");
            }
        }
    }

    static void createHTML() throws Exception {
        String eventData = "[" + String.join(",", events) + "]";
        String html = """
<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Readers-Writers Monitor Simulation</title>
<style>
body{font-family:Arial,sans-serif;background:#f3f6fa;color:#202938;margin:0;padding:20px;text-align:center}
.wrap{max-width:1100px;margin:auto}.controls{margin:18px}button{padding:10px 16px;margin:4px;cursor:pointer;border:0;border-radius:7px;background:#315b91;color:white}
.cards{display:grid;grid-template-columns:repeat(4,minmax(130px,1fr));gap:12px}.panel,.person{background:white;border-radius:10px;padding:15px;box-shadow:0 2px 7px #0001}
.people{display:grid;grid-template-columns:repeat(4,minmax(120px,1fr));gap:10px}.person{margin:5px 0}.Reading{border-top:5px solid #2e8b57}.Writing{border-top:5px solid #e07a24}.Waiting{border-top:5px solid #d6a21c}.Finished,.Done{border-top:5px solid #718096}.Interrupted{border-top:5px solid #c62828}
#resource{font-size:20px;font-weight:bold;padding:20px;margin:18px 0}.timeline{text-align:left;max-height:280px;overflow:auto}.event{padding:8px;border-bottom:1px solid #eee}.active{background:#e5f1ff}
@media(max-width:700px){.cards,.people{grid-template-columns:repeat(2,minmax(120px,1fr))}}
</style></head><body><div class="wrap"><h1>Readers-Writers Problem</h1><p>Monitor-based synchronization · 4 readers · 2 writers</p>
<div class="controls"><button onclick="play()">Play</button><button onclick="pause()">Pause</button><button onclick="next()">Next Event</button><button onclick="reset()">Reset</button><label> Speed <input id="speed" type="range" min="200" max="1800" step="100" value="800"></label></div>
<div class="cards"><div class="panel"><h3>Shared data</h3><div id="data">0</div></div><div class="panel"><h3>Readers inside</h3><div id="count">0</div></div><div class="panel"><h3>Writer active</h3><div id="writing">No</div></div><div class="panel"><h3>Event</h3><div id="number">0</div></div></div>
<div class="panel" id="resource">Resource available</div><h2>Readers</h2><div id="readers" class="people"></div><h2>Writers</h2><div id="writers" class="people"></div><div class="panel timeline"><h2>Execution timeline</h2><div id="timeline"></div></div></div>
<script>
const events = EVENT_DATA; let index=0,timer=null;
function render(){if(!events.length)return;const e=events[index];document.getElementById('data').textContent=e.data;document.getElementById('count').textContent=e.readersInside;document.getElementById('writing').textContent=e.writing?'Yes':'No';document.getElementById('number').textContent=(index+1)+' / '+events.length;document.getElementById('resource').textContent=e.writing?'Writer is using the shared resource':e.readersInside?'Reader(s) are reading':'Resource available';
for(const [id,states] of [['readers',e.readers],['writers',e.writers]]){const box=document.getElementById(id);box.innerHTML='';states.forEach((s,i)=>{const d=document.createElement('div');d.className='person '+s.replaceAll(' ','');d.textContent=(id==='readers'?'Reader ':'Writer ')+(i+1)+': '+s;box.appendChild(d);});}
const tl=document.getElementById('timeline');tl.innerHTML='';events.slice(0,index+1).forEach((ev,i)=>{const d=document.createElement('div');d.className='event'+(i===index?' active':'');d.textContent=(i+1)+'. '+ev.message;tl.appendChild(d);});tl.scrollTop=tl.scrollHeight;}
function next(){if(index<events.length-1){index++;render()}else pause()}
function play(){if(timer!==null||!events.length)return;if(index>=events.length-1){index=0;render()}timer=setInterval(()=>{if(index>=events.length-1){pause();return}next()},Number(document.getElementById('speed').value));}
function pause(){if(timer!==null){clearInterval(timer);timer=null}}
function reset(){pause();index=0;render()}
render();
</script></body></html>
""".replace("EVENT_DATA", eventData);
        Files.writeString(Path.of("readers_writers_monitor.html"), html, StandardCharsets.UTF_8);
        System.out.println("HTML animation created: " + Path.of("readers_writers_monitor.html").toAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        Arrays.fill(readerStates, "Waiting");
        Arrays.fill(writerStates, "Waiting");
        record("Simulation started");
        Thread[] threads = new Thread[READERS + WRITERS];
        for (int i = 0; i < READERS; i++) {
            final int id = i;
            threads[i] = new Thread(() -> runReader(id), "Reader-" + (id + 1));
        }
        for (int i = 0; i < WRITERS; i++) {
            final int id = i;
            threads[READERS + i] = new Thread(() -> runWriter(id), "Writer-" + (id + 1));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        createHTML();
    }
}
