# Operating Systems Synchronization Assignment

## Student Details

- **Name:** Diya P Shetty
- **USN:** NNM24IS080
- **Subject:** Operating Systems
- **Language:** Java
- **Repository:** `os_task2`

## 1. Project Overview

This project demonstrates two synchronization problems in Operating Systems: Dining Philosophers and Readers-Writers. Both problems are implemented using semaphores and monitors in Java.

The project explains how threads access shared resources and how synchronization helps prevent race conditions and maintain mutual exclusion. Each implementation also records execution events that are used to generate an HTML simulation.

## 2. Implementations

### A. Dining Philosophers Using Semaphores

This implementation uses Java threads and semaphores to control access to chopsticks. It demonstrates how synchronization can coordinate philosophers and help prevent conflicts when accessing shared resources.

### B. Dining Philosophers Using a Monitor

This implementation uses Java monitor synchronization to coordinate philosophers. The shared state and synchronization methods control when philosophers can start eating.

### C. Readers-Writers Using Semaphores

This implementation uses semaphores to coordinate readers and writers. Multiple readers may access shared data simultaneously, while writing requires exclusive access.

### D. Readers-Writers Using a Monitor

This implementation uses Java monitor synchronization to manage access to shared data. The program checks the reader and writer states before allowing access.

## 3. Technologies Used

- Java
- Java concurrency utilities, including semaphores
- Java threads and monitor synchronization
- HTML, CSS and JavaScript for execution visualizations
- A web browser to view the generated simulations
- Visual Studio Code or another Java-compatible editor

## 4. Repository Structure

```text
os_task2/
├── README.md
├── dining_philosophers/
│   ├── semaphore/
│   └── monitor/
├── readers_writers/
│   ├── semaphore/
│   └── monitor/
├── generated_html/
├── screenshots/
├── ai_prompts/
│   └── prompts.md
├── references/
│   └── references.md
└── report/
    └── Assignment_Report.pdf
```

## 5. Requirements

- Java Development Kit (JDK)
- A terminal or command prompt
- A web browser to view the HTML simulations

Check whether Java is installed by running:

```bash
java -version
javac -version
```

## 6. Compilation and Execution

Open a terminal in the folder containing the required Java source file.

Compile the program using:

```bash
javac FileName.java
```

Run the compiled program using:

```bash
java ClassName
```

Replace `FileName.java` and `ClassName` with the actual source filename and Java class name.

Repeat these steps for each of the four implementations. If a program uses multiple Java files, compile all required files together.

## 7. Generating and Viewing HTML Simulations

1. Compile the required Java program.
2. Run the program and allow its execution to finish.
3. Check the output for the generated HTML filename.
4. Confirm that the HTML file has been created.
5. Open the HTML file in a web browser to view the recorded execution.
6. Repeat the process for all four implementations.

The simulations help visualize thread activity, synchronization events and changes in shared resources.

## 8. Screenshots and Execution Evidence

The `screenshots/` folder contains execution evidence, including terminal output and browser views of the HTML simulations.

The `generated_html/` folder contains the four HTML simulation files.

## 9. Known Limitations

- The execution order of concurrent threads may vary between runs.
- The behaviour depends on the synchronization rules implemented in each program.
- Some synchronization policies may allow starvation under certain conditions.
- HTML playback displays recorded events; it does not control the Java threads after execution.

## 10. AI Assistance

ChatGPT (OpenAI) was used to assist with understanding synchronization concepts, developing and debugging programs, and improving the HTML visualizations. Claude (Anthropic) was used to review the report's grammar, readability and structure.

The AI prompts and relevant follow-up prompts are documented in `ai_prompts/prompts.md`. The implementations and execution results should be reviewed and understood by the student.

## 11. References and Acknowledgements

The project uses operating systems textbooks, Java documentation and concurrency references listed in `references/references.md`. AI assistance is disclosed above and in the assignment report.

## 12. Assignment Report

The final assignment report is available in `report/Assignment_Report.pdf`. It contains the problem descriptions, implementation explanations, observations, comparison of synchronization approaches, screenshots, references and academic integrity declaration.
