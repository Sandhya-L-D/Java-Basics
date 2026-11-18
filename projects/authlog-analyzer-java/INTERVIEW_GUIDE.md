# Explain AuthLog Analyzer

## First run it yourself

In PowerShell inside the extracted project, run `java -jar authlog-analyzer.jar`. Then open `output\report.html`. Expect 9 valid events, 1 rejected row and 2 alerts. Open `samples\login-events.csv` beside the report and find the five failed logins followed by a successful login. All data is synthetic.

To build from source and run tests, use `.\build.bat`. The project uses Java 8-compatible code and no external Java dependencies.

## Problem and approach

A log file can contain too many records to inspect manually. This tool groups login attempts by source and user, keeps recent failures in a queue, and flags patterns that a person should review. It does not prove that an attack occurred.

## A short explanation to practise

"AuthLog Analyzer is an offline Java tool for normalized login logs. It sorts events by time and tracks recent failed attempts for each source and username. A configurable number of failures within a time window creates an alert. A success after enough recent failures creates a second review signal. The tool produces an HTML report and an alert CSV. I tested time boundaries, invalid input and separate users. The detection uses rules, not machine learning."

Use first-person claims only for work you have personally reviewed and can explain. If asked about AI help, describe it honestly and show the code you understand, your own changes and the tests you ran.

## Understand a sliding window

Suppose the rule is 5 failures in 300 seconds. At 09:05:00, keep failures from 09:00:00 through 09:05:00. Remove older failures from the front of the queue. Add a new failed attempt at the back. If the queue reaches 5, create an alert. A failure at exactly the boundary is included. One a fraction of a second outside it is removed.

The queue is separate for each `(source, user)` pair. Four failures by one user plus one failure by another user do not become five failures for either user.

## Read the code in this order

1. `LoginEvent.java`: fields for time, source, user and result.
2. `LogReader.java`: parse the CSV and count malformed rows.
3. `Analyzer.Key`: understand grouping by both source and user.
4. `Analyzer.Window`: a deque of recent failures and an alert-suppression flag.
5. `Analyzer.analyze`: sort, expire, append or clear, and create alerts.
6. `ReportWriter`: turn alert objects into safe HTML and CSV.
7. `AllTests.java`: trace the exact-boundary, fractional-boundary and success-reset tests.

## Questions and answers

**Why HashMap?** It associates each source/user pair with its own window. Lookup is expected O(1); this is not a worst-case constant-time guarantee.

**Why ArrayDeque?** Events arrive in chronological order after sorting. We append recent failures at the back and remove expired failures from the front efficiently.

**Why sort the logs first?** Files may contain out-of-order events. Window expiration depends on chronological processing. Stable sorting preserves input order for equal timestamps.

**What happens on a successful login?** If the same pair has enough failures still inside the window, the tool records SUCCESS_AFTER_FAILURES. It then clears that pair's queue.

**Why suppress repeat alerts?** A continuous burst should not create an identical alert for every further failure. A new alert becomes possible after success or when the count first falls below the threshold.

**What is a false positive?** A legitimate person repeatedly typing the wrong password can trigger the rule. A later success can also be legitimate. The report asks for human review.

**What can this miss?** Attempts spread across different sources, slow attempts below the threshold, or malicious logins with no failed attempts. Duplicate log rows are counted, so duplicates can inflate results.

**What is the complexity?** Sorting is O(n log n). After sorting, each failure enters and leaves a deque at most once; with expected constant-time map lookups, processing is O(n). The batch implementation uses O(n) memory.

**Why is it not machine learning?** There is no learned model. The rules and thresholds are explicit. No accuracy, precision or recall claim is made without a labelled evaluation dataset.

**How is output handled safely?** HTML special characters are escaped, so a username cannot become a script tag. Spreadsheet formula-like identifiers are prefixed in exported CSV cells. The input file is only read.

## Make one change yourself

Run the sample with threshold 6: `java -jar authlog-analyzer.jar samples/login-events.csv output-threshold6 6 300`. Predict the output before running. The sample should produce zero alerts because there are only five failures in that burst. Then change the window to 60 seconds and explain why the default threshold is no longer met.

## Demo checklist

- Run the default sample and explain both alerts.
- Locate the malformed line and the diagnostic in warnings.txt.
- Change the threshold and predict the changed output.
- Trace one queue through the five failures and success.
- Explain a false positive and a missed attack pattern.
