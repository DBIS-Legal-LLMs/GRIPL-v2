# Evaluation Report

## Evaluation Metadata
- **Models:** GPT-OSS-20B
- **Temperatures:** 0.1
- **Top Ps:** 1.0
- **Datasets:** DatasetInfo(id=4, name=Hospital Processes), DatasetInfo(id=5, name=Human Resources Processes), DatasetInfo(id=6, name=Commercial Operations), DatasetInfo(id=7, name=Logistics & Production), DatasetInfo(id=8, name=Other), DatasetInfo(id=9, name=University - Academic Operations)
- **Total Test Cases:** 5
- **Repetitions:** 3
- **Seed:** 24523833
- **Timestamp:** 2026-06-06 05:08:29.243
- **Default Evaluation Endpoint:** /gdpr/analysis/prompt-engineering

# Aggregate Statistics Across All Runs

## Model: GPT-OSS-20B

- Precision: 0.751 ± 0.031

- Recall: 0.958 ± 0.000

- F1-Score: 0.842 ± 0.019

- Accuracy: 0.827 ± 0.025

- True Positives: 23.000 ± 0.000

- False Positives: 7.667 ± 1.247

- False Negatives: 1.000 ± 0.000

- True Negatives: 18.333 ± 1.247

- Passed: 3.000 ± 0.816 / 5

- Failed: 2.000 ± 0.816 / 5

- Errors: 0.000 ± 0.000 / 5

- Amount of Retries: 0.000 ± 0.000


### RAG Metrics (averaged across 3 run(s))

- Context Utilization: 0.204 ± 0.023

- Faithfulness: 0.092 ± 0.029

# Run 1

## Summary (GPT-OSS-20B)

## Summary
Total: 5
Passed: 2
Failed: 3

Total Retries: 0

### Metrics
Accuracy: 0.820
Precision: 0.742
Recall: 0.958
F1-Score: 0.836

### Confusion Matrix
True Positives: 23
False Positives: 8
False Negatives: 1
True Negatives: 18

### RAG Metrics (Ragas) — averaged across 4 test case(s)
Faithfulness: 0.078
Context Utilization: 0.174
Total Samples Scored: 31 (failed: 0)

## Model: GPT-OSS-20B

## Test Case 43 - Technische Wartung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/43/preview?correctIds=&falsePositiveIds=&falseNegativeIds=&salt=65399.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 0
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** 
- **Actual:** 
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0


<details>
<summary><h3>Reasoning of the LLM</h3></summary>



</details>

## Test Case 96 - Durchführung Vergabe
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/96/preview?correctIds=sid-769A281E-121A-4CE5-BCA5-133855F4EBC5,sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725,sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F,sid-513CC109-2A72-4E35-A35F-48407221396C&falsePositiveIds=&falseNegativeIds=&salt=21795.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 4
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5), Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725), Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F), Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C)
- **Actual:** Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5), Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725), Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C), Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.146
- **Context Utilization:** 0.304
- **Samples:** 4 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5)**: The activity "Zulassungs-kriterien automatisiert prüfen" automatically evaluates admission criteria for applicants, which requires accessing and processing personal data such as student status, gender, semester, and other identifying attributes. This operation constitutes processing of personal data.
- **Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725)**: The activity "Ranking automatisiert durchführen" performs an automated ranking of applicants based on criteria such as roles, study programs, semesters, and other personal attributes. This requires processing personal data of each applicant.
- **Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C)**: The activity "Wartelistenplatz(SPZ)/Absage zuweisen/erteilen" assigns a waitlist position or declines an applicant, which involves handling personal data such as the applicant's name, contact details, and admission status.
- **Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F)**: The activity "Zulassung (SPZ)/Zulassung zuweisen/erteilen" grants admission to an applicant, which requires processing personal data such as the applicant's identity, contact information, and academic records.

</details>

## Test Case 63 - Warehouse Optimization
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/63/preview?correctIds=sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4,sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E,sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB,sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6,sid-EB25994B-55A9-40B3-958B-DB47E77C8728&falsePositiveIds=sid-AD6B4C0B-F71E-4A64-B751-485EFB5BDB74&falseNegativeIds=&salt=69823.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 5
- Amount of False Positives: 1
- Amount of False Negatives: 0

- **Expected:** send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4), capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E), 
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB), forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6), make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728)
- **Actual:** capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E), forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6), make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728), send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4), 
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB), determine sales and inventory  (sid-AD6B4C0B-F71E-4A64-B751-485EFB5BDB74)
- **Result:** ❌ Failed

- **False Positives:** determine sales and inventory  (sid-AD6B4C0B-F71E-4A64-B751-485EFB5BDB74)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.139
- **Context Utilization:** 0.115
- **Samples:** 6 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E)**: This activity captures order details, which include customer contact information and other personal data, thus processing personal data.
- **forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6)**: This activity forwards the order to the next step, carrying the order details that contain personal data of the customer.
- **make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728)**: This activity creates an offer and completes a sale, involving customer data such as name, address, and payment details.
- **send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4)**: This activity sends information to the customer, using the customer's contact details, thereby processing personal data.
- **
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB)**: This activity analyzes customer data to derive insights, directly processing personal data of customers.
- **determine sales and inventory  (sid-AD6B4C0B-F71E-4A64-B751-485EFB5BDB74)**: This activity determines sales and inventory levels using sales data that includes customer information, thus processing personal data.

</details>

## Test Case 94 - Lehrveranstaltung koordinieren
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/94/preview?correctIds=sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651,sid-24B998D4-5424-41DD-A564-6CEACF2F115D,sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9,sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745,sid-B1E78F49-99A0-4AFA-8019-A4042F018C63,sid-EC199047-4E2E-45A5-9475-EEF8357F1B49&falsePositiveIds=sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89,sid-C4B1896E-4290-4049-9753-3C607BFAAA46,sid-3CD2926F-2770-49BB-AB78-E78E12F25367,sid-34FCE951-492D-4098-8450-5EBEF19E80D0,sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4,sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35&falseNegativeIds=sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC&salt=16648.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 6
- Amount of False Positives: 6
- Amount of False Negatives: 1

- **Expected:** ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651), 7.1 Anlegen der Lehrendendaten (sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC), Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D), welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9), eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745), ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63), Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49)
- **Actual:** ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89),  ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46), Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367), ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651), Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D), aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0), welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9), Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49), ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4), ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63), eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745), Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)
- **Result:** ❌ Failed

- **False Positives:** ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89),  ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46), Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367), aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0), ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4), Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)
- **False Negatives:** 7.1 Anlegen der Lehrendendaten (sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.000
- **Context Utilization:** 0.173
- **Samples:** 12 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89)**: The task "ob neue Veranstaltungen angelegt werden müssen prüfen" checks whether new events need to be created, which requires access to teacher and event data, thus processing personal data.
- ** ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46)**: The task "ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen" evaluates if a teacher change is needed, involving teacher personal data.
- **Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367)**: The task "Lehrveranstaltung erstellen" creates a course, which includes teacher information and event details, processing personal data.
- **ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651)**: The task "ob Lehrender in CMS verfügbar ist prüfen" verifies teacher availability in the CMS, accessing teacher personal data.
- **Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D)**: The task "Lehrenden der Veranstaltung zuteilen" assigns teachers to a course, directly handling teacher personal data.
- **aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0)**: The task "aktuelle Veranstaltungs-Liste generieren" generates a list of events, which typically includes teacher names and contact details, thus processing personal data.
- **welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9)**: The task "welche Änderungen an der Veranstaltung notwendig sind prüfen" checks required changes to a course, involving teacher data.
- **Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49)**: The task "Änderungen vornehmen" makes changes to a course, which includes updating teacher information, processing personal data.
- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4)**: The task "ob neue Veranstaltungen angelegt werden müssen prüfen" (Lehrender lane) checks for new events, accessing teacher data, processing personal data.
- **ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63)**: The task "ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen" (Lehrender lane) evaluates teacher changes, processing personal data.
- **eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745)**: The task "eingeschränkt Änderungen vornehmen" performs restricted changes to a course, involving teacher data, thus processing personal data.
- **Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)**: The task "Änderungen, die er nicht selbst vornehmen darf melden" reports changes that cannot be made, which requires accessing teacher data, processing personal data.

</details>

## Test Case 135 - Immatrikulation
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/135/preview?correctIds=sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277,sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB,sid-0728004E-21CC-4D5E-9D26-6FB3182102F5,sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D,sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A,sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF,sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A,sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A&falsePositiveIds=sid-BF2BB73A-1EC1-4D86-BA99-29D0BD43C5B6&falseNegativeIds=&salt=17736.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 8
- Amount of False Positives: 1
- Amount of False Negatives: 0

- **Expected:** Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277), Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB), Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5), traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D), Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A), Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF), Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A), Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A)
- **Actual:** Vorbereitungs-phase
 (sid-BF2BB73A-1EC1-4D86-BA99-29D0BD43C5B6), Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB), Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5), Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A), traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D), Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277), Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A), Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A), Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF)
- **Result:** ❌ Failed

- **False Positives:** Vorbereitungs-phase
 (sid-BF2BB73A-1EC1-4D86-BA99-29D0BD43C5B6)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.028
- **Context Utilization:** 0.107
- **Samples:** 9 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Vorbereitungs-phase
 (sid-BF2BB73A-1EC1-4D86-BA99-29D0BD43C5B6)**: The activity "Vorbereitungs-phase" is part of the admission process and involves reviewing application documents that contain personal data such as names, addresses, and academic records, thereby processing personal data.
- **Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB)**: The activity "Bewerbung bearbeiten" processes applicants’ personal data by reviewing, evaluating, and recording information contained in their applications.
- **Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5)**: The activity "Studienplatz vergeben" involves deciding on admission and recording the decision, which directly processes personal data of the applicant.
- **Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A)**: The activity "Bewerber immatrikulieren" creates and stores enrolment records that contain personal data of the student.
- **traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D)**: The activity "traditionelles Nachrück-verfahren" handles replacement procedures for students, which requires processing personal data such as student identifiers and contact information.
- **Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277)**: The activity "Bewerbung durchführen" collects and records applicant information, thereby processing personal data.
- **Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A)**: The activity "Koordinier-ungsphase 1 mit Entscheidungsphase" coordinates admission decisions and handles personal data of applicants during the decision-making process.
- **Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A)**: The activity "Koordinierungsphase 2" continues coordination of admission processes, involving further handling of applicants’ personal data.
- **Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF)**: The activity "Clearingphase" finalises and clears admission records, which requires processing personal data of the students.

</details>

# Run 2

## Summary (GPT-OSS-20B)

## Summary
Total: 5
Passed: 4
Failed: 1

Total Retries: 0

### Metrics
Accuracy: 0.860
Precision: 0.793
Recall: 0.958
F1-Score: 0.868

### Confusion Matrix
True Positives: 23
False Positives: 6
False Negatives: 1
True Negatives: 20

### RAG Metrics (Ragas) — averaged across 4 test case(s)
Faithfulness: 0.066
Context Utilization: 0.208
Total Samples Scored: 29 (failed: 0)

## Model: GPT-OSS-20B

## Test Case 43 - Technische Wartung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/43/preview?correctIds=&falsePositiveIds=&falseNegativeIds=&salt=73276.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 0
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** 
- **Actual:** 
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0


<details>
<summary><h3>Reasoning of the LLM</h3></summary>



</details>

## Test Case 96 - Durchführung Vergabe
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/96/preview?correctIds=sid-769A281E-121A-4CE5-BCA5-133855F4EBC5,sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725,sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F,sid-513CC109-2A72-4E35-A35F-48407221396C&falsePositiveIds=&falseNegativeIds=&salt=99920.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 4
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5), Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725), Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F), Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C)
- **Actual:** Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5), Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725), Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C), Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.000
- **Context Utilization:** 0.388
- **Samples:** 4 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5)**: This activity automates the checking of admission criteria, which involves evaluating personal attributes such as student status, gender, semester, and study program. These attributes are personal data, so the activity processes personal data.
- **Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725)**: This activity performs an automated ranking of applicants based on criteria like roles, study programs, semester, and prior achievements. These criteria are personal data, so the activity processes personal data.
- **Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C)**: This activity assigns a waiting list position or rejection to an applicant, which requires handling the applicant’s personal information. Thus it processes personal data.
- **Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F)**: This activity assigns admission to an applicant, which involves processing the applicant’s personal data. Therefore it processes personal data.

</details>

## Test Case 63 - Warehouse Optimization
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/63/preview?correctIds=sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4,sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E,sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB,sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6,sid-EB25994B-55A9-40B3-958B-DB47E77C8728&falsePositiveIds=&falseNegativeIds=&salt=25026.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 5
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4), capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E), 
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB), forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6), make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728)
- **Actual:** capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E), forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6), make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728), send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4), 
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.000
- **Context Utilization:** 0.088
- **Samples:** 5 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E)**: The activity 'capture order' involves collecting and recording order details that include customer personal data such as name, address, and payment information.
- **forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6)**: The activity 'forward order' forwards order information to downstream processes, which contains personal data of the customer.
- **make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728)**: The activity 'make offer and sell' creates a sales offer and completes a sale, processing personal data of the customer.
- **send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4)**: The activity 'send to customer' sends information to the customer, using the customer's contact details, thus processing personal data.
- **
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB)**: The activity 'analyze the customers' processes customer data to derive insights, involving personal data of customers.

</details>

## Test Case 135 - Immatrikulation
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/135/preview?correctIds=sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277,sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB,sid-0728004E-21CC-4D5E-9D26-6FB3182102F5,sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D,sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A,sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF,sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A,sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A&falsePositiveIds=&falseNegativeIds=&salt=2366.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 8
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277), Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB), Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5), traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D), Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A), Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF), Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A), Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A)
- **Actual:** Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB), Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5), Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A), traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D), Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277), Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A), Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A), Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.200
- **Context Utilization:** 0.123
- **Samples:** 8 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB)**: The activity 'Bewerbung bearbeiten' processes applicants' personal data such as contact details, academic records, and other identifying information as part of the application review.
- **Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5)**: The activity 'Studienplatz vergeben' involves deciding which applicant receives a study place, requiring the use of personal data such as names, academic history, and contact information.
- **Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A)**: The activity 'Bewerber immatrikulieren' enrolls an applicant, creating and storing personal data such as identity, contact details, and academic records.
- **traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D)**: The activity 'traditionelles Nachrück-verfahren' handles a replacement procedure that requires reviewing candidates' personal data to identify suitable replacements.
- **Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277)**: The activity 'Bewerbung durchführen' is the applicant's submission of an application, which inherently involves providing personal data such as name, address, and academic background.
- **Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A)**: The activity 'Koordinier-ungsphase 1 mit Entscheidungsphase' coordinates decisions about admissions, requiring access to applicants' personal data for evaluation.
- **Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A)**: The activity 'Koordinierungsphase 2' continues coordination of admission decisions, again involving the use of personal data.
- **Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF)**: The activity 'Clearingphase' involves clearing or finalizing records, which requires handling personal data such as student or applicant information.

</details>

## Test Case 94 - Lehrveranstaltung koordinieren
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/94/preview?correctIds=sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651,sid-24B998D4-5424-41DD-A564-6CEACF2F115D,sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9,sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745,sid-B1E78F49-99A0-4AFA-8019-A4042F018C63,sid-EC199047-4E2E-45A5-9475-EEF8357F1B49&falsePositiveIds=sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89,sid-C4B1896E-4290-4049-9753-3C607BFAAA46,sid-3CD2926F-2770-49BB-AB78-E78E12F25367,sid-34FCE951-492D-4098-8450-5EBEF19E80D0,sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4,sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35&falseNegativeIds=sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC&salt=55946.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 6
- Amount of False Positives: 6
- Amount of False Negatives: 1

- **Expected:** ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651), 7.1 Anlegen der Lehrendendaten (sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC), Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D), welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9), eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745), ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63), Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49)
- **Actual:** ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89),  ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46), Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367), ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651), Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D), aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0), welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9), Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49), ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4), ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63), eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745), Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)
- **Result:** ❌ Failed

- **False Positives:** ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89),  ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46), Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367), aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0), ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4), Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)
- **False Negatives:** 7.1 Anlegen der Lehrendendaten (sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.063
- **Context Utilization:** 0.232
- **Samples:** 12 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89)**: The activity checks whether new events need to be created, which requires reviewing lecturer and event information that includes personal data such as lecturer names, contact details, and possibly student enrolment data. Therefore it processes personal data.
- ** ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46)**: The activity checks whether a lecturer change is needed for an existing event, which involves accessing lecturer personal data such as names and contact details to determine if a change is required. Thus it processes personal data.
- **Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367)**: The activity creates a new event, which requires storing lecturer and possibly student personal data such as names and contact details in the system. Therefore it processes personal data.
- **ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651)**: The activity checks whether the lecturer is available in the CMS, which requires accessing the lecturer's personal data in the CMS, such as login credentials and profile information. Thus it processes personal data.
- **Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D)**: The activity assigns a lecturer to an event, which requires handling the lecturer's personal data such as name and contact details and updating the event record. Therefore it processes personal data.
- **aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0)**: The activity generates the current event list, which involves retrieving lecturer and event data that includes personal data such as lecturer names and contact details. Thus it processes personal data.
- **welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9)**: The activity checks what changes are needed for an event, which requires reviewing event details and lecturer information that contains personal data. Therefore it processes personal data.
- **Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49)**: The activity makes changes to an event, which involves updating lecturer and event records that contain personal data. Thus it processes personal data.
- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4)**: The activity checks whether new events need to be created from the lecturer's perspective, which requires accessing lecturer personal data such as name and contact details to determine if new events are needed. Therefore it processes personal data.
- **ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63)**: The activity checks whether a change is needed for an existing event from the lecturer's perspective, which involves accessing lecturer personal data to determine if a change is required. Thus it processes personal data.
- **eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745)**: The activity makes restricted changes to an event, which involves updating event records that contain lecturer personal data. Therefore it processes personal data.
- **Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)**: The activity reports changes that the lecturer cannot make themselves, which involves handling lecturer personal data and event details. Thus it processes personal data.

</details>

# Run 3

## Summary (GPT-OSS-20B)

## Summary
Total: 5
Passed: 3
Failed: 2

Total Retries: 0

### Metrics
Accuracy: 0.800
Precision: 0.719
Recall: 0.958
F1-Score: 0.821

### Confusion Matrix
True Positives: 23
False Positives: 9
False Negatives: 1
True Negatives: 17

### RAG Metrics (Ragas) — averaged across 4 test case(s)
Faithfulness: 0.133
Context Utilization: 0.230
Total Samples Scored: 32 (failed: 0)

## Model: GPT-OSS-20B

## Test Case 43 - Technische Wartung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/43/preview?correctIds=&falsePositiveIds=&falseNegativeIds=&salt=87460.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 0
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** 
- **Actual:** 
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0


<details>
<summary><h3>Reasoning of the LLM</h3></summary>



</details>

## Test Case 135 - Immatrikulation
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/135/preview?correctIds=sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277,sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB,sid-0728004E-21CC-4D5E-9D26-6FB3182102F5,sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D,sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A,sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF,sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A,sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A&falsePositiveIds=&falseNegativeIds=&salt=22825.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 8
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277), Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB), Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5), traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D), Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A), Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF), Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A), Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A)
- **Actual:** Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB), Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5), Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A), traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D), Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277), Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A), Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A), Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.296
- **Context Utilization:** 0.159
- **Samples:** 8 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB)**: The task "Bewerbung bearbeiten" (Process application) involves handling applicants’ personal details such as names, addresses, contact information, and academic records. This activity collects, records, and stores personal data, thereby constituting processing of personal data.
- **Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5)**: The task "Studienplatz vergeben" (Assign study place) uses personal data of applicants to decide admission and allocate a study place. It processes personal data by evaluating and recording the applicant’s eligibility.
- **Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A)**: The task "Bewerber immatrikulieren" (Enroll applicant) creates a student record that contains personal data such as name, address, and academic history. This activity stores and updates personal data, which is processing under the GDPR.
- **traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D)**: The task "traditionelles Nachrück-verfahren" (traditional replacement procedure) handles personal data of applicants who are considered for replacement positions. It processes personal data by reviewing and evaluating applicant information.
- **Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277)**: The task "Bewerbung durchführen" (Conduct application) collects and processes applicants’ personal data such as contact details and academic information. This activity is a direct processing of personal data.
- **Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A)**: The task "Koordinier-ungsphase 1 mit Entscheidungsphase" (Coordination phase 1 with decision phase) coordinates decisions regarding applicants, requiring access to and processing of their personal data for admission decisions.
- **Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A)**: The task "Koordinierungsphase 2" (Coordination phase 2) continues the coordination of applicant data, involving further processing of personal data for final admission decisions.
- **Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF)**: The task "Clearingphase" (Clearing phase) involves clearing or finalising applicant records, which requires handling and processing personal data such as names, addresses, and admission status.

</details>

## Test Case 63 - Warehouse Optimization
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/63/preview?correctIds=sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4,sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E,sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB,sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6,sid-EB25994B-55A9-40B3-958B-DB47E77C8728&falsePositiveIds=sid-AD6B4C0B-F71E-4A64-B751-485EFB5BDB74,sid-7FE2BAAA-9BB8-4791-8BA5-08ED0864351B,sid-B9F0696C-3718-40CE-BDEE-006D25D213CC&falseNegativeIds=&salt=1399.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 5
- Amount of False Positives: 3
- Amount of False Negatives: 0

- **Expected:** send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4), capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E), 
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB), forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6), make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728)
- **Actual:** capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E), forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6), make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728), send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4), 
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB), determine sales and inventory  (sid-AD6B4C0B-F71E-4A64-B751-485EFB5BDB74), determine result of market research (sid-7FE2BAAA-9BB8-4791-8BA5-08ED0864351B), prepare optimization
report (sid-B9F0696C-3718-40CE-BDEE-006D25D213CC)
- **Result:** ❌ Failed

- **False Positives:** determine sales and inventory  (sid-AD6B4C0B-F71E-4A64-B751-485EFB5BDB74), determine result of market research (sid-7FE2BAAA-9BB8-4791-8BA5-08ED0864351B), prepare optimization
report (sid-B9F0696C-3718-40CE-BDEE-006D25D213CC)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.042
- **Context Utilization:** 0.189
- **Samples:** 8 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E)**: The capture order task collects customer order details, which include personal identifiers such as name, address, and contact information. This is a direct collection of personal data.
- **forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6)**: The forward order task forwards the order information to the next department. The order contains personal data of the customer, so the task processes personal data.
- **make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728)**: The make offer and sell task creates an offer and a sales record that includes the customer’s personal data, such as contact details and payment information.
- **send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4)**: The send to customer task transmits order confirmation or other communications to the customer, using the customer’s personal data (e.g., email address). This is a transmission of personal data.
- **
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB)**: The analyze the customers task processes customer data to derive insights. The data used contains personal identifiers, so the task processes personal data.
- **determine sales and inventory  (sid-AD6B4C0B-F71E-4A64-B751-485EFB5BDB74)**: The determine sales and inventory task reads sales records from the database, which include customer personal data. Therefore it processes personal data.
- **determine result of market research (sid-7FE2BAAA-9BB8-4791-8BA5-08ED0864351B)**: The determine result of market research task uses market research data that may contain personal data of respondents. Thus it processes personal data.
- **prepare optimization
report (sid-B9F0696C-3718-40CE-BDEE-006D25D213CC)**: The prepare optimization report task reads from the database, which likely contains sales and customer data. Consequently, it processes personal data.

</details>

## Test Case 96 - Durchführung Vergabe
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/96/preview?correctIds=sid-769A281E-121A-4CE5-BCA5-133855F4EBC5,sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725,sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F,sid-513CC109-2A72-4E35-A35F-48407221396C&falsePositiveIds=&falseNegativeIds=&salt=2939.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 4
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5), Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725), Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F), Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C)
- **Actual:** Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5), Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725), Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C), Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.063
- **Context Utilization:** 0.296
- **Samples:** 4 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5)**: The task "Zulassungs-kriterien automatisiert prüfen" collects and evaluates application data such as student identifiers, contact details, and eligibility criteria, which are personal data. It processes this information to determine admission eligibility.
- **Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725)**: The task "Ranking automatisiert durchführen" performs automated ranking of applicants based on personal data such as academic records, application details, and eligibility criteria. This involves processing personal data to produce a ranking outcome.
- **Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C)**: The task "Wartelistenplatz(SPZ)/Absage zuweisen/erteilen" assigns waiting list positions or rejects applicants, which requires processing personal data such as applicant identifiers, contact information, and application status.
- **Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F)**: The task "Zulassung (SPZ)/Zulassung zuweisen/erteilen" grants admission to applicants, which involves processing personal data such as applicant identifiers, contact details, and eligibility information.

</details>

## Test Case 94 - Lehrveranstaltung koordinieren
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/94/preview?correctIds=sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651,sid-24B998D4-5424-41DD-A564-6CEACF2F115D,sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9,sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745,sid-B1E78F49-99A0-4AFA-8019-A4042F018C63,sid-EC199047-4E2E-45A5-9475-EEF8357F1B49&falsePositiveIds=sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89,sid-C4B1896E-4290-4049-9753-3C607BFAAA46,sid-3CD2926F-2770-49BB-AB78-E78E12F25367,sid-34FCE951-492D-4098-8450-5EBEF19E80D0,sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4,sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35&falseNegativeIds=sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC&salt=17289.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 6
- Amount of False Positives: 6
- Amount of False Negatives: 1

- **Expected:** ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651), 7.1 Anlegen der Lehrendendaten (sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC), Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D), welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9), eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745), ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63), Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49)
- **Actual:** ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89),  ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46), Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367), ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651), Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D), aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0), welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9), Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49), ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4), ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63), eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745), Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)
- **Result:** ❌ Failed

- **False Positives:** ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89),  ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46), Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367), aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0), ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4), Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)
- **False Negatives:** 7.1 Anlegen der Lehrendendaten (sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.132
- **Context Utilization:** 0.277
- **Samples:** 12 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89)**: The task checks whether new courses need to be created, which requires reading information about existing courses and lecturers, including names and contact details, thereby processing personal data.
- ** ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46)**: The task verifies whether a lecturer change is needed for an existing course, involving reading lecturer identities and contact information, thus processing personal data.
- **Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367)**: The task creates a new course, which involves entering lecturer and student details such as names, addresses, and email addresses, thereby processing personal data.
- **ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651)**: The task checks if a lecturer is available in the CMS, requiring access to the lecturer’s personal data, thus processing personal data.
- **Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D)**: The task assigns lecturers to a course, which involves handling lecturer identities and contact details, thereby processing personal data.
- **aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0)**: The task generates the current list of courses, which includes lecturer names and possibly student enrolments, thus processing personal data.
- **welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9)**: The task checks which changes are necessary for a course, requiring review of course and lecturer data, thereby processing personal data.
- **Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49)**: The task makes changes to a course, which involves updating lecturer and student information, thus processing personal data.
- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4)**: The task checks whether new courses need to be created, requiring access to lecturer and course data, thereby processing personal data.
- **ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63)**: The task checks if a change is needed for an existing course, involving lecturer and course data, thus processing personal data.
- **eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745)**: The task makes restricted changes to a course, which involves updating personal data of lecturers or students, thereby processing personal data.
- **Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)**: The task reports changes that cannot be made, requiring knowledge of the course and lecturer data, thus processing personal data.

</details>