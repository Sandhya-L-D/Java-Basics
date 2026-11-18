# Explain ApplyTrack

## First run it yourself

Extract the project, open PowerShell in the extracted folder and run `java -jar applytrack.jar`. Use fictional data. Add a company, close the window, reopen it, select the row and change its status to INTERVIEW. Then set the follow-up to today and select **Due by today**. Close one copy before starting another. Read `data/applications.csv` in a text editor to see what was saved.

If Java is not found, the terminal needs the installed JDK's bin directory on PATH. You previously used Java 8 and Maven for your Appium work; this project needs Java only. Build from source with `.\build.bat`. A prebuilt JAR is included for the first run.

## Problem and approach

People applying to several companies can lose track of the stage and next follow-up. ApplyTrack keeps those records in one local file. It separates the interface, application rules and file storage so each part is easier to understand and test.

## A short explanation to practise

"ApplyTrack is a Java application for tracking internship and job applications. It stores the company, role, status and follow-up date. I can add an application, update its stage and list follow-ups that are due. The interface calls a service layer, which validates data and saves it to CSV. I chose a local file to keep the first version simple. It is designed for one user and one running instance."

Use this only after you can demonstrate those operations. Do not describe AI-written code as something you independently wrote without assistance. It is fine to explain which parts you reviewed, changed and tested.

## Read the code in this order

1. `Application.java`: understand one record, its fields and the constructor checks.
2. `Status.java`: explain why a fixed enum is clearer than arbitrary status text.
3. `TrackerService.add`: load records, find the highest ID, create a validated record, save.
4. `ApplicationStore.load` and `save`: reconstruct objects and replace the CSV file safely.
5. `Application.isDue` and `TrackerService.due`: select open records due on or before a date, then sort.
6. `TrackerFrame`: inspect how button clicks call the same service methods used by the console.
7. `AllTests.java`: trace the restart-persistence and corrupt-file protection tests.

## Questions and answers

**What is a class and an object here?** `Application` is the class, or definition. Each saved company application is an object with its own field values.

**Where is encapsulation?** The service owns an `ApplicationStore` through a private field. The store owns its path. Callers use methods rather than managing file writes themselves. `Application` is an immutable record with public final fields; its validated state cannot be reassigned after construction.

**Why is Application immutable?** A status change creates a validated replacement. Code cannot accidentally modify one field and leave the others inconsistent.

**Why an enum?** It limits statuses to known values and keeps the closed-status rule in one place.

**Why use LocalDate?** Follow-ups are calendar dates. LocalDate supports comparison without time-of-day or timezone conversions.

**How does persistence work?** The application reads UTF-8 CSV rows and creates objects. On a change it writes a temporary file, then replaces the main file. The replacement is atomic when supported by the filesystem; there is a non-atomic replacement fallback.

**Why not a database?** A local file makes the learning version easy to run. SQLite would be a sensible next step for transactions, indexing and concurrent access.

**What happens to a bad row?** Loading fails with its row number. The requested operation stops, leaving the original file unchanged. This prevents a partial load from silently overwriting valid data.

**How fast is it?** Add, update and summary operations scan or rewrite records, so they take O(n). Sorting the due list is O(n log n) in the worst case. This project does not claim a performance benchmark.

**What are the limitations?** No authentication, encryption, cloud sync, scheduled reminders or concurrent-writer locking. CSV fields cannot span lines. Closed offers are excluded from the due list by the current business rule.

## Make one change yourself

Add a `shortlisted` console command that lists only INTERVIEW and OFFER records. First write the condition in plain English, then implement it in `Main.java`, build and test it with the sample CSV. This is a practice task, not an implemented feature.

## Demo checklist

- Explain the problem in one sentence.
- Add and update a fictional record.
- Close and reopen to prove persistence.
- Demonstrate a validation error, such as a follow-up before the application date.
- Point to the service method and the corresponding test.
- Explain one limitation and one improvement.
