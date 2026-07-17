# Evaluation Report

## Evaluation Metadata
- **Models:** GPT-OSS-20B
- **Temperatures:** 0.1
- **Top Ps:** 1.0
- **Datasets:** DatasetInfo(id=4, name=Hospital Processes), DatasetInfo(id=5, name=Human Resources Processes), DatasetInfo(id=6, name=Commercial Operations), DatasetInfo(id=7, name=Logistics & Production), DatasetInfo(id=8, name=Other), DatasetInfo(id=9, name=University - Academic Operations)
- **Total Test Cases:** 19
- **Repetitions:** 3
- **Seed:** 24523833
- **Timestamp:** 2026-06-06 03:14:59.364
- **Default Evaluation Endpoint:** /gdpr/analysis/prompt-engineering

# Aggregate Statistics Across All Runs

## Model: GPT-OSS-20B

- Precision: 0.842 ± 0.031

- Recall: 0.950 ± 0.004

- F1-Score: 0.892 ± 0.019

- Accuracy: 0.840 ± 0.025

- True Positives: 120.000 ± 4.899

- False Positives: 22.667 ± 5.249

- False Negatives: 6.333 ± 0.471

- True Negatives: 31.667 ± 8.957

- Passed: 8.000 ± 0.816 / 19

- Failed: 8.000 ± 0.000 / 19

- Errors: 3.000 ± 0.816 / 19

- Amount of Retries: 0.000 ± 0.000


### RAG Metrics (averaged across 3 run(s))

- Context Utilization: 0.313 ± 0.007

- Faithfulness: 0.190 ± 0.025

# Run 1

## Summary (GPT-OSS-20B)

## Summary
Total: 19
Passed: 8
Failed: 8
Error: 3
Total Retries: 0

### Metrics
Accuracy: 0.810
Precision: 0.800
Recall: 0.945
F1-Score: 0.866

### Confusion Matrix
True Positives: 120
False Positives: 30
False Negatives: 7
True Negatives: 38

### RAG Metrics (Ragas) — averaged across 14 test case(s)
Faithfulness: 0.193
Context Utilization: 0.314
Total Samples Scored: 150 (failed: 0)

## Model: GPT-OSS-20B

## Test Case 43 - Technische Wartung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/43/preview?correctIds=&falsePositiveIds=&falseNegativeIds=&salt=23862.0" alt="Test Case BPMN XML" />

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

## Test Case 70 - Wartungsplanung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/70/preview?correctIds=&falsePositiveIds=&falseNegativeIds=&salt=9855.0" alt="Test Case BPMN XML" />

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

## Test Case 29 - Arzttermin buchen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/29/preview?correctIds=Activity_0gi1msm,Activity_1agrvaf,Activity_1molpic&falsePositiveIds=Activity_0khomk4&falseNegativeIds=&salt=32675.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 3
- Amount of False Positives: 1
- Amount of False Negatives: 0

- **Expected:** Patientendaten aufnehmen (Activity_0gi1msm), Termin bestätigen (Activity_1agrvaf), Erinnerung senden (Activity_1molpic)
- **Actual:** Terminoptionen prüfen (Activity_0khomk4), Patientendaten aufnehmen (Activity_0gi1msm), Termin bestätigen (Activity_1agrvaf), Erinnerung senden (Activity_1molpic)
- **Result:** ❌ Failed

- **False Positives:** Terminoptionen prüfen (Activity_0khomk4)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.258
- **Context Utilization:** 0.290
- **Samples:** 4 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Terminoptionen prüfen (Activity_0khomk4)**: The activity "Terminoptionen prüfen" involves checking appointment options for a patient, which requires accessing the patient’s appointment details and contact information. This activity therefore processes personal data.
- **Patientendaten aufnehmen (Activity_0gi1msm)**: The activity "Patientendaten aufnehmen" explicitly collects patient data, which are personal data. This activity directly processes personal data.
- **Termin bestätigen (Activity_1agrvaf)**: The activity "Termin bestätigen" confirms an appointment and therefore uses the patient’s contact details and appointment information, which are personal data. This activity processes personal data.
- **Erinnerung senden (Activity_1molpic)**: The activity "Erinnerung senden" sends a reminder to the patient, which requires the patient’s contact information and possibly other personal details. This activity processes personal data.

</details>

## Test Case 63 - Warehouse Optimization
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/63/preview?correctIds=sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4,sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E,sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB,sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6,sid-EB25994B-55A9-40B3-958B-DB47E77C8728&falsePositiveIds=&falseNegativeIds=&salt=41605.0" alt="Test Case BPMN XML" />

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
- **Faithfulness:** 0.200
- **Context Utilization:** 0.174
- **Samples:** 5 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E)**: The task captures order details, which include customer contact information and other identifiers, thereby collecting personal data.
- **forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6)**: Forwarding an order forwards the order details, including personal data such as the customer’s name and address, to the next process step.
- **make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728)**: Creating an offer and selling a product involves processing the customer’s personal data for direct marketing and sales purposes.
- **send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4)**: Sending the order to the customer uses the customer’s personal data (contact details, address) to deliver the product, which is a processing activity.
- **
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB)**: Analyzing customers involves profiling and evaluating personal data of individual customers, which is a processing activity under GDPR.

</details>

## Test Case 59 - Neuen Mitarbeiter erfassen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/59/preview?correctIds=sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551,sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4,x7f-f3cda2a0d4d7330d9353884ec580e5e4,vv8-f3cda2a0d4d7330d9353884ec580e5e4,yys-f3cda2a0d4d7330d9353884ec580e5e4,sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4&falsePositiveIds=&falseNegativeIds=sid-7hno-95f654cf6849a76aaf94b5a98ebc5551&salt=56360.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 6
- Amount of False Positives: 0
- Amount of False Negatives: 1

- **Expected:** QIS-Nutzer anlegen (sid-7hno-95f654cf6849a76aaf94b5a98ebc5551), QIS-Rolle zuordnen (sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551), Stammsätze anlegen (sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4), Chipkarte zuordnen (x7f-f3cda2a0d4d7330d9353884ec580e5e4), Arbeitszeitmodell zuordnen (vv8-f3cda2a0d4d7330d9353884ec580e5e4), Arbeitszeit eintragen (yys-f3cda2a0d4d7330d9353884ec580e5e4), Abweichende Sollarbeitszeit eintragen (sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4)
- **Actual:** Arbeitszeitmodell zuordnen (vv8-f3cda2a0d4d7330d9353884ec580e5e4), Chipkarte zuordnen (x7f-f3cda2a0d4d7330d9353884ec580e5e4), Arbeitszeit eintragen (yys-f3cda2a0d4d7330d9353884ec580e5e4), Abweichende Sollarbeitszeit eintragen (sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4), Stammsätze anlegen (sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4), QIS-Rolle zuordnen (sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551)
- **Result:** ❌ Failed

- **False Positives:** None
- **False Negatives:** QIS-Nutzer anlegen (sid-7hno-95f654cf6849a76aaf94b5a98ebc5551)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.056
- **Context Utilization:** 0.407
- **Samples:** 6 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Arbeitszeitmodell zuordnen (vv8-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Arbeitszeitmodell zuordnen" assigns a working time model to an employee, which requires accessing the employee’s personal data such as employee ID and employment details.
- **Chipkarte zuordnen (x7f-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Chipkarte zuordnen" assigns a chip card to an employee, which involves handling the employee’s personal identifiers and contact information.
- **Arbeitszeit eintragen (yys-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Arbeitszeit eintragen" records an employee’s working hours, which is a direct manipulation of personal data related to that employee.
- **Abweichende Sollarbeitszeit eintragen (sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Abweichende Sollarbeitszeit eintragen" logs a deviation from the standard working time for an employee, which involves handling that employee’s personal data.
- **Stammsätze anlegen (sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Stammsätze anlegen" creates master records for employees, which necessarily involves storing personal identifiers and employment details.
- **QIS-Rolle zuordnen (sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551)**: The activity "QIS-Rolle zuordnen" assigns a role to a user in the QIS system, which requires accessing the user’s personal data such as identity and employment status.

</details>

## Test Case 57 - Dienstreiseabrechnung genehmigen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/57/preview?correctIds=xtre-ed2f4ad43858602ad6e00efd76b0f56e,xttr-ed2f4ad43858602ad6e00efd76b0f56e,xu5h-ed2f4ad43858602ad6e00efd76b0f56e,xu3y-ed2f4ad43858602ad6e00efd76b0f56e,xtts-ed2f4ad43858602ad6e00efd76b0f56e,xtrf-ed2f4ad43858602ad6e00efd76b0f56e,xu2f-ed2f4ad43858602ad6e00efd76b0f56e&falsePositiveIds=&falseNegativeIds=&salt=14420.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 7
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Dienstreiseabrechnung sichten (xtre-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung genehmigen (xttr-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung ablehnen (xu5h-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an andere Sachbearbeitung weiterleiten (xu3y-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnungsdaten anpassen (xtts-ed2f4ad43858602ad6e00efd76b0f56e), Kommentar eingeben (xtrf-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an Antragsteller zurücksenden (xu2f-ed2f4ad43858602ad6e00efd76b0f56e)
- **Actual:** Dienstreiseabrechnung sichten (xtre-ed2f4ad43858602ad6e00efd76b0f56e), Kommentar eingeben (xtrf-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung genehmigen (xttr-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnungsdaten anpassen (xtts-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an Antragsteller zurücksenden (xu2f-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an andere Sachbearbeitung weiterleiten (xu3y-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung ablehnen (xu5h-ed2f4ad43858602ad6e00efd76b0f56e)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.148
- **Context Utilization:** 0.354
- **Samples:** 7 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Dienstreiseabrechnung sichten (xtre-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Dienstreiseabrechnung sichten" involves reviewing a travel expense claim, which contains personal data such as the employee’s name, travel details, and expense amounts. This activity processes personal data.
- **Kommentar eingeben (xtrf-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Kommentar eingeben" requires entering a comment on the travel expense claim, which is linked to the employee’s personal data. This activity processes personal data.
- **Abrechnung genehmigen (xttr-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnung genehmigen" involves approving the travel expense claim, which requires access to personal data such as the employee’s identity and travel details. This activity processes personal data.
- **Abrechnungsdaten anpassen (xtts-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnungsdaten anpassen" requires adjusting the data of the travel expense claim, which includes personal data such as the employee’s name, travel dates, and expense amounts. This activity processes personal data.
- **Abrechnung an Antragsteller zurücksenden (xu2f-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnung an Antragsteller zurücksenden" involves sending the travel expense claim back to the applicant, which requires transmitting personal data such as the applicant’s contact details and claim information. This activity processes personal data.
- **Abrechnung an andere Sachbearbeitung weiterleiten (xu3y-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnung an andere Sachbearbeitung weiterleiten" forwards the travel expense claim to another department, which involves sharing personal data such as the employee’s identity and travel details. This activity processes personal data.
- **Abrechnung ablehnen (xu5h-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnung ablehnen" involves rejecting the travel expense claim, which requires access to personal data such as the employee’s identity and claim details. This activity processes personal data.

</details>

## Test Case 3 - Preparing surgery
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/3/preview?correctIds=sid-D0309BAA-2BC7-4866-BD2B-36789C42198D,sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745,sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B,sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA,sid-A76861E0-B426-4259-997B-A72EEB63E3A1,sid-87E2518C-79DE-403D-8A33-010389B828A6,sid-5DA96990-76B4-40D0-88B6-915685E7E279,sid-8018CC06-69E9-4E62-977B-E441C039AEF0,sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF&falsePositiveIds=sid-E05999ED-FBEC-4936-994E-EC0F5808EF69,sid-C17C448D-8303-4345-BD8A-C773D4A6A63B&falseNegativeIds=sid-6EFE25FB-0893-440F-A48A-876386148A36,sid-71AF5554-8150-4459-9577-961A5D078CB2,sid-8048EF97-74FF-4BD4-B97A-FB3D04E435E2&salt=25870.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 9
- Amount of False Positives: 2
- Amount of False Negatives: 3

- **Expected:** Collect and evaluate all results (sid-D0309BAA-2BC7-4866-BD2B-36789C42198D), Check results (sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745), Perform pre-operation discussion  (sid-6EFE25FB-0893-440F-A48A-876386148A36), Enter patient into surgery board (sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B), Enter surgery into Oplus for surgery plan (sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA), Ordain premedication (sid-A76861E0-B426-4259-997B-A72EEB63E3A1), Introduce patient to head physician  (sid-71AF5554-8150-4459-9577-961A5D078CB2), Cancel surgery (sid-8048EF97-74FF-4BD4-B97A-FB3D04E435E2), Give medicaments (sid-87E2518C-79DE-403D-8A33-010389B828A6), Check electrolytes (sid-5DA96990-76B4-40D0-88B6-915685E7E279), Perform colonoskopy (sid-8018CC06-69E9-4E62-977B-E441C039AEF0), Order preparations of surgery in kardex (sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF)
- **Actual:** Patient fasts (sid-E05999ED-FBEC-4936-994E-EC0F5808EF69), Give medicaments (sid-87E2518C-79DE-403D-8A33-010389B828A6), Perform colonoskopy (sid-8018CC06-69E9-4E62-977B-E441C039AEF0), Check electrolytes (sid-5DA96990-76B4-40D0-88B6-915685E7E279), Enter patient into surgery board (sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B), Collect and evaluate all results (sid-D0309BAA-2BC7-4866-BD2B-36789C42198D), Enter surgery into Oplus for surgery plan (sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA), Order preparations of surgery in kardex (sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF), Meet patient (sid-C17C448D-8303-4345-BD8A-C773D4A6A63B), Check results (sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745), Ordain premedication (sid-A76861E0-B426-4259-997B-A72EEB63E3A1)
- **Result:** ❌ Failed

- **False Positives:** Patient fasts (sid-E05999ED-FBEC-4936-994E-EC0F5808EF69), Meet patient (sid-C17C448D-8303-4345-BD8A-C773D4A6A63B)
- **False Negatives:** Perform pre-operation discussion  (sid-6EFE25FB-0893-440F-A48A-876386148A36), Introduce patient to head physician  (sid-71AF5554-8150-4459-9577-961A5D078CB2), Cancel surgery (sid-8048EF97-74FF-4BD4-B97A-FB3D04E435E2)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.273
- **Context Utilization:** 0.199
- **Samples:** 11 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Patient fasts (sid-E05999ED-FBEC-4936-994E-EC0F5808EF69)**: The task involves monitoring a patient's fasting status, which is health‑related personal data.
- **Give medicaments (sid-87E2518C-79DE-403D-8A33-010389B828A6)**: Administering medication to a patient involves handling the patient's health data.
- **Perform colonoskopy (sid-8018CC06-69E9-4E62-977B-E441C039AEF0)**: The colonoscopy procedure collects and processes the patient's medical information.
- **Check electrolytes (sid-5DA96990-76B4-40D0-88B6-915685E7E279)**: Checking electrolytes involves analyzing the patient's blood test results, which are personal health data.
- **Enter patient into surgery board (sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B)**: Adding a patient to the surgery board records the patient's identity and medical details.
- **Collect and evaluate all results (sid-D0309BAA-2BC7-4866-BD2B-36789C42198D)**: Collecting and evaluating results involves processing the patient's test results.
- **Enter surgery into Oplus for surgery plan (sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA)**: Entering surgery into Oplus records the patient's surgery plan, containing personal data.
- **Order preparations of surgery in kardex (sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF)**: Ordering preparations in kardex involves storing the patient's surgery details.
- **Meet patient (sid-C17C448D-8303-4345-BD8A-C773D4A6A63B)**: Meeting the patient involves accessing the patient's personal information.
- **Check results (sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745)**: Checking results involves reviewing the patient's test outcomes.
- **Ordain premedication (sid-A76861E0-B426-4259-997B-A72EEB63E3A1)**: Ordaining premedication involves administering medication based on the patient's health data.

</details>

## Test Case 94 - Lehrveranstaltung koordinieren
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/94/preview?correctIds=sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651,sid-24B998D4-5424-41DD-A564-6CEACF2F115D,sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9,sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745,sid-B1E78F49-99A0-4AFA-8019-A4042F018C63,sid-EC199047-4E2E-45A5-9475-EEF8357F1B49&falsePositiveIds=sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89,sid-C4B1896E-4290-4049-9753-3C607BFAAA46,sid-3CD2926F-2770-49BB-AB78-E78E12F25367,sid-34FCE951-492D-4098-8450-5EBEF19E80D0,sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4,sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35&falseNegativeIds=sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC&salt=69984.0" alt="Test Case BPMN XML" />

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
- **Faithfulness:** 0.104
- **Context Utilization:** 0.249
- **Samples:** 12 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89)**: The task "ob neue Veranstaltungen angelegt werden müssen prüfen" reads the current event list, which includes teacher names and contact details. Reading such data constitutes processing of personal data.
- ** ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46)**: The task "ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen" evaluates whether a teacher’s assignment needs to be changed, which requires accessing teacher personal data. This is processing personal data.
- **Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367)**: The task "Lehrveranstaltung erstellen" creates a new event record that includes teacher identifiers and contact information, thereby processing personal data.
- **ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651)**: The task "ob Lehrender in CMS verfügbar ist prüfen" checks whether a teacher’s profile exists in the CMS, which requires reading teacher personal data. This is processing personal data.
- **Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D)**: The task "Lehrenden der Veranstaltung zuteilen" assigns teachers to an event, which involves writing teacher identifiers and contact details into the event record. This is processing personal data.
- **aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0)**: The task "aktuelle Veranstaltungs-Liste generieren" generates a list of events that includes teacher names and contact information, thereby processing personal data.
- **welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9)**: The task "welche Änderungen an der Veranstaltung notwendig sind prüfen" reviews event details that include teacher identifiers, which is processing personal data.
- **Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49)**: The task "Änderungen vornehmen" applies changes to event records that contain teacher personal data, thus processing personal data.
- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4)**: The task "ob neue Veranstaltungen angelegt werden müssen prüfen" (in the Lehrender lane) checks whether new events need to be created, which requires reading teacher personal data. This is processing personal data.
- **ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63)**: The task "ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen" evaluates whether a change is needed for an existing event, which involves accessing teacher personal data. This is processing personal data.
- **eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745)**: The task "eingeschränkt Änderungen vornehmen" makes restricted changes to event records that contain teacher personal data, thereby processing personal data.
- **Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)**: The task "Änderungen, die er nicht selbst vornehmen darf melden" reports changes that cannot be made, which requires reading event records that include teacher personal data. This is processing personal data.

</details>

## Test Case 19 - Introduce patient to head physician
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/19/preview?correctIds=sid-1177E778-9B60-4A48-895E-82DAAC180C9E,sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B,sid-4FAB8B03-8311-42F4-91B2-BDC72772535E&falsePositiveIds=&falseNegativeIds=&salt=33497.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 3
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Introduce patient with all results to head physician (sid-1177E778-9B60-4A48-895E-82DAAC180C9E), Order surgery to be performed (sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B), Examine patient (sid-4FAB8B03-8311-42F4-91B2-BDC72772535E)
- **Actual:** Introduce patient with all results to head physician (sid-1177E778-9B60-4A48-895E-82DAAC180C9E), Examine patient (sid-4FAB8B03-8311-42F4-91B2-BDC72772535E), Order surgery to be performed (sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.600
- **Context Utilization:** 0.478
- **Samples:** 3 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Introduce patient with all results to head physician (sid-1177E778-9B60-4A48-895E-82DAAC180C9E)**: Introduce patient with all results to head physician processes personal data because it involves presenting the patient’s medical results, which include medical history, clinical treatment, and physiological or biomedical state.
- **Examine patient (sid-4FAB8B03-8311-42F4-91B2-BDC72772535E)**: Examine patient processes personal data because it involves a medical examination of a patient, collecting and using health information such as medical history and clinical findings.
- **Order surgery to be performed (sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B)**: Order surgery to be performed processes personal data because it requires the patient’s identity, medical condition, and treatment plan, all of which are personal health data.

</details>

## Test Case 98 - Pflege der Lehrendendaten
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/98/preview?correctIds=sid-6AF85299-5179-4780-B917-67C3678C0062,sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B,sid-CCFEBADD-0645-4F1F-936A-F2094AC68729,sid-9C71399D-C2E4-45E3-8256-FBD48257840A,sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A,sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110,sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF,sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F&falsePositiveIds=&falseNegativeIds=&salt=74713.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 8
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Lehrendendaten
überprüfen (sid-6AF85299-5179-4780-B917-67C3678C0062), Lehrenden deaktivieren (sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B), Lehrenden aktivieren (sid-CCFEBADD-0645-4F1F-936A-F2094AC68729), Lehrenden entfernen (sid-9C71399D-C2E4-45E3-8256-FBD48257840A), Lehrenden entfernen (sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A), Lehrenden-
daten aktualisieren (sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110), Lehrenden-
daten aktualisieren (sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF), Art der Pflege
überprüfen (sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F)
- **Actual:** Lehrenden-
daten aktualisieren (sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF), Lehrenden entfernen (sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A), Lehrendendaten
überprüfen (sid-6AF85299-5179-4780-B917-67C3678C0062), Art der Pflege
überprüfen (sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F), Lehrenden deaktivieren (sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B), Lehrenden entfernen (sid-9C71399D-C2E4-45E3-8256-FBD48257840A), Lehrenden-
daten aktualisieren (sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110), Lehrenden aktivieren (sid-CCFEBADD-0645-4F1F-936A-F2094AC68729)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.281
- **Context Utilization:** 0.515
- **Samples:** 8 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Lehrenden-
daten aktualisieren (sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF)**: The task "Lehrenden-daten aktualisieren" updates teacher records, which contain personal information such as names, contact details, and employment data. Updating these records constitutes processing of personal data.
- **Lehrenden entfernen (sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A)**: The task "Lehrenden entfernen" removes a teacher from the system, which requires handling personal data such as identity, contact information, and employment history. Deleting or erasing such data is a form of processing.
- **Lehrendendaten
überprüfen (sid-6AF85299-5179-4780-B917-67C3678C0062)**: The task "Lehrendendaten überprüfen" involves inspecting teacher data, which includes personal information. Reviewing such data is a processing activity.
- **Art der Pflege
überprüfen (sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F)**: The task "Art der Pflege überprüfen" checks the type of care required for a person, which involves personal data such as health or care needs. This constitutes processing of personal data.
- **Lehrenden deaktivieren (sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B)**: The task "Lehrenden deaktivieren" deactivates a teacher’s account, which requires handling personal data such as identity and contact details. Deactivation is a processing activity.
- **Lehrenden entfernen (sid-9C71399D-C2E4-45E3-8256-FBD48257840A)**: The task "Lehrenden entfernen" (removal) handles personal data of a teacher, such as identity and employment information. Removing such data is a processing operation.
- **Lehrenden-
daten aktualisieren (sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110)**: The task "Lehrenden-daten aktualisieren" updates teacher records, which contain personal data such as names, contact details, and employment information. Updating these records is a processing activity.
- **Lehrenden aktivieren (sid-CCFEBADD-0645-4F1F-936A-F2094AC68729)**: The task "Lehrenden aktivieren" activates a teacher’s account, which involves personal data such as identity and contact information. Activation is a processing activity.

</details>

## Test Case 142 - Datenübermittlung Bewerber
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/142/preview?correctIds=sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561,sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67,sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21,sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE,sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0,sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76,sid-3492B288-6611-4891-954A-44FF10D5A30E,sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411,sid-5D601043-5FEE-4681-B123-44CE520423FF&falsePositiveIds=sid-8188439C-7014-4A94-ADE3-80E22199EEBF,sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7&falseNegativeIds=sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156&salt=62393.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 9
- Amount of False Positives: 2
- Amount of False Negatives: 1

- **Expected:** Bewerber-account anlegen (sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561), vorhandene 
Qualifikationen eintragen (sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67), Qualifikationen (automatisch) prüfen (sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21), Angaben vervollständigen (sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE), Bewerbung auf Vollständigkeit prüfen (automatisch) (sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0), Antrag ausfüllen
und Unterlagen hinzufügen (sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76), nachzu-reichende Unterlagen im CMS
vermerken (sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156), Unterlagen auf Vollständigkeit prüfen und einscannen (sid-3492B288-6611-4891-954A-44FF10D5A30E), Unterlagen vervollständigen (sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411), Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen
 (sid-5D601043-5FEE-4681-B123-44CE520423FF)
- **Actual:** Bewerber-account anlegen (sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561), vorhandene 
Qualifikationen eintragen (sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67), Antrag ausfüllen
und Unterlagen hinzufügen (sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76), Angaben vervollständigen (sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE), Bewerbung abschließen (sid-8188439C-7014-4A94-ADE3-80E22199EEBF), Studiengang wählen (sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7), Unterlagen vervollständigen (sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411), Qualifikationen (automatisch) prüfen (sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21), Bewerbung auf Vollständigkeit prüfen (automatisch) (sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0), Unterlagen auf Vollständigkeit prüfen und einscannen (sid-3492B288-6611-4891-954A-44FF10D5A30E), Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen
 (sid-5D601043-5FEE-4681-B123-44CE520423FF)
- **Result:** ❌ Failed

- **False Positives:** Bewerbung abschließen (sid-8188439C-7014-4A94-ADE3-80E22199EEBF), Studiengang wählen (sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7)
- **False Negatives:** nachzu-reichende Unterlagen im CMS
vermerken (sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.076
- **Context Utilization:** 0.376
- **Samples:** 11 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Bewerber-account anlegen (sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561)**: The task "Bewerber-account anlegen" creates a new applicant account, which involves collecting and storing personal data such as name, contact details, and other identifying information.
- **vorhandene 
Qualifikationen eintragen (sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67)**: The task "vorhandene Qualifikationen eintragen" records existing qualifications of an applicant, which are personal data that must be processed.
- **Antrag ausfüllen
und Unterlagen hinzufügen (sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76)**: The task "Antrag ausfüllen und Unterlagen hinzufügen" involves the applicant filling out an application form and adding supporting documents, all of which contain personal data.
- **Angaben vervollständigen (sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE)**: The task "Angaben vervollständigen" completes missing applicant information, thereby processing personal data.
- **Bewerbung abschließen (sid-8188439C-7014-4A94-ADE3-80E22199EEBF)**: The task "Bewerbung abschließen" finalises the application, which includes transmitting the applicant’s personal data to the university’s admission system.
- **Studiengang wählen (sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7)**: The task "Studiengang wählen" records the applicant’s chosen study programme, which is personal data.
- **Unterlagen vervollständigen (sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411)**: The task "Unterlagen vervollständigen" involves adding missing documents that contain personal data.
- **Qualifikationen (automatisch) prüfen (sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21)**: The task "Qualifikationen (automatisch) prüfen" automatically checks qualifications, which requires processing personal data such as academic records.
- **Bewerbung auf Vollständigkeit prüfen (automatisch) (sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0)**: The task "Bewerbung auf Vollständigkeit prüfen (automatisch)" automatically verifies completeness of the application, processing personal data contained in the application.
- **Unterlagen auf Vollständigkeit prüfen und einscannen (sid-3492B288-6611-4891-954A-44FF10D5A30E)**: The task "Unterlagen auf Vollständigkeit prüfen und einscannen" scans documents that contain personal data, thereby processing that data.
- **Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen
 (sid-5D601043-5FEE-4681-B123-44CE520423FF)**: The task "Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen" creates a confirmation that references the applicant’s personal data and any missing documents.

</details>

## Test Case 132 - Rückmeldung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/132/preview?correctIds=sid-716BD875-CB9C-4173-91AE-20E2BEFB723E,sid-9E177368-C98D-401A-8179-38BEB5DF6390,sid-2A902299-023A-44C5-9A76-16F47701996C,sid-9A7FCC57-F21E-4924-9968-224952C660DD,sid-EF63A163-83B2-46A7-85E9-0089AC7D0786,sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66,sid-F8D21C49-24E6-479B-84DF-98B5D63593C8,sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F,sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB&falsePositiveIds=&falseNegativeIds=sid-82F7C1B0-B225-469A-8C21-15B72F0A53CF&salt=87466.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 9
- Amount of False Positives: 0
- Amount of False Negatives: 1

- **Expected:** Gebühren ermitteln (gebsql) (sid-716BD875-CB9C-4173-91AE-20E2BEFB723E), Informations-E-Mail erstellen und an Student senden
(Fristen und Zahlungs-
details) (sid-9E177368-C98D-401A-8179-38BEB5DF6390), Beiträge überweisen (sid-2A902299-023A-44C5-9A76-16F47701996C), 4.1 Gebührenmanagement (sid-82F7C1B0-B225-469A-8C21-15B72F0A53CF), Datensätze in das CMS einspielen (sid-9A7FCC57-F21E-4924-9968-224952C660DD), ob 
Ist = Soll
prüfen (sid-EF63A163-83B2-46A7-85E9-0089AC7D0786), Mahnschreiben erstellen und an Student senden (sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66), auf Rückmelde-sperre prüfen und Mängel beheben (sid-F8D21C49-24E6-479B-84DF-98B5D63593C8), Studierenden
informieren (sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F), Studenten exmatrikulieren (sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB)
- **Actual:** Gebühren ermitteln (gebsql) (sid-716BD875-CB9C-4173-91AE-20E2BEFB723E), Datensätze in das CMS einspielen (sid-9A7FCC57-F21E-4924-9968-224952C660DD), ob 
Ist = Soll
prüfen (sid-EF63A163-83B2-46A7-85E9-0089AC7D0786), auf Rückmelde-sperre prüfen und Mängel beheben (sid-F8D21C49-24E6-479B-84DF-98B5D63593C8), Studierenden
informieren (sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F), Mahnschreiben erstellen und an Student senden (sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66), Informations-E-Mail erstellen und an Student senden
(Fristen und Zahlungs-
details) (sid-9E177368-C98D-401A-8179-38BEB5DF6390), Studenten exmatrikulieren (sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB), Beiträge überweisen (sid-2A902299-023A-44C5-9A76-16F47701996C)
- **Result:** ❌ Failed

- **False Positives:** None
- **False Negatives:** 4.1 Gebührenmanagement (sid-82F7C1B0-B225-469A-8C21-15B72F0A53CF)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.000
- **Context Utilization:** 0.112
- **Samples:** 9 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Gebühren ermitteln (gebsql) (sid-716BD875-CB9C-4173-91AE-20E2BEFB723E)**: The task "Gebühren ermitteln (gebsql)" calculates fees for students, which requires accessing student personal data such as name, student ID, and financial details. Therefore it processes personal data.
- **Datensätze in das CMS einspielen (sid-9A7FCC57-F21E-4924-9968-224952C660DD)**: The task "Datensätze in das CMS einspielen" imports data records into the CMS, which likely include personal data of students. Thus it processes personal data.
- **ob 
Ist = Soll
prüfen (sid-EF63A163-83B2-46A7-85E9-0089AC7D0786)**: The task "ob Ist = Soll prüfen" checks whether the actual amount equals the expected amount, which involves student financial data. Therefore it processes personal data.
- **auf Rückmelde-sperre prüfen und Mängel beheben (sid-F8D21C49-24E6-479B-84DF-98B5D63593C8)**: The task "auf Rückmelde-sperre prüfen und Mängel beheben" checks if a student has a return block and fixes issues, requiring personal data about the student. Thus it processes personal data.
- **Studierenden
informieren (sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F)**: The task "Studierenden informieren" informs students, sending communications that contain personal data such as student names and contact details. Therefore it processes personal data.
- **Mahnschreiben erstellen und an Student senden (sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66)**: The task "Mahnschreiben erstellen und an Student senden" creates a reminder letter and sends it to a student, using personal data like name and address. Thus it processes personal data.
- **Informations-E-Mail erstellen und an Student senden
(Fristen und Zahlungs-
details) (sid-9E177368-C98D-401A-8179-38BEB5DF6390)**: The task "Informations-E-Mail erstellen und an Student senden (Fristen und Zahlungs-details)" creates an information email with deadlines and payment details and sends it to a student, using personal data. Therefore it processes personal data.
- **Studenten exmatrikulieren (sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB)**: The task "Studenten exmatrikulieren" deregisters a student, which involves handling personal data of the student. Thus it processes personal data.
- **Beiträge überweisen (sid-2A902299-023A-44C5-9A76-16F47701996C)**: The task "Beiträge überweisen" transfers contributions, which requires personal data such as bank account details of the student. Therefore it processes personal data.

</details>

## Test Case 55 - Dienstreiseantrag stellen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/55/preview?correctIds=abo9-b0399020c4b54c4fa3a4761cf43b01e1,abpu-b0399020c4b54c4fa3a4761cf43b01e1,sid-1v10-b0399020c4b54c4fa3a4761cf43b01e1,sid-1v3m-b0399020c4b54c4fa3a4761cf43b01e1&falsePositiveIds=sid-1tzo-b0399020c4b54c4fa3a4761cf43b01e1,sid-1tzp-b0399020c4b54c4fa3a4761cf43b01e1,sid-1u3t-b0399020c4b54c4fa3a4761cf43b01e1,sid-1u4n-b0399020c4b54c4fa3a4761cf43b01e1,sid-1u8s-b0399020c4b54c4fa3a4761cf43b01e1,sid-1um4-b0399020c4b54c4fa3a4761cf43b01e1,sid-1up1-b0399020c4b54c4fa3a4761cf43b01e1,sid-1uxn-b0399020c4b54c4fa3a4761cf43b01e1,i46u-b0399020c4b54c4fa3a4761cf43b01e1,sid-10lea-b0399020c4b54c4fa3a4761cf43b01e1,sid-12so0-b0399020c4b54c4fa3a4761cf43b01e1,sid-1dl33-b0399020c4b54c4fa3a4761cf43b01e1,sid-1egam-b0399020c4b54c4fa3a4761cf43b01e1,sid-208pr-b0399020c4b54c4fa3a4761cf43b01e1,sid-2ct8-ed2f4ad43858602ad6e00efd76b0f56e,lhyw-ed2f4ad43858602ad6e00efd76b0f56e,sm4r-ed2f4ad43858602ad6e00efd76b0f56e&falseNegativeIds=&salt=35433.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 4
- Amount of False Positives: 17
- Amount of False Negatives: 0

- **Expected:** Bankverbindung einsehen (abo9-b0399020c4b54c4fa3a4761cf43b01e1), Änderungen zur Bankverbindung eingeben (abpu-b0399020c4b54c4fa3a4761cf43b01e1), Dokumente hochladen (sid-1v10-b0399020c4b54c4fa3a4761cf43b01e1), Antrag beenden und weiterleiten (sid-1v3m-b0399020c4b54c4fa3a4761cf43b01e1)
- **Actual:** Genehmigten Dienstreiseantrag suchen (sid-1tzo-b0399020c4b54c4fa3a4761cf43b01e1), Reisedaten anpassen (sid-1tzp-b0399020c4b54c4fa3a4761cf43b01e1), Ausländischen Reiseverlauf anpassen (sid-1u3t-b0399020c4b54c4fa3a4761cf43b01e1), Kürzungen angeben (sid-1u4n-b0399020c4b54c4fa3a4761cf43b01e1), Fahrkosten anpassen (sid-1u8s-b0399020c4b54c4fa3a4761cf43b01e1), Finanzierungsdaten eingeben (sid-1um4-b0399020c4b54c4fa3a4761cf43b01e1), Daten zu Zahlungspartnern eingeben (sid-1up1-b0399020c4b54c4fa3a4761cf43b01e1), Notizen eingeben (sid-1uxn-b0399020c4b54c4fa3a4761cf43b01e1), Dokumente hochladen (sid-1v10-b0399020c4b54c4fa3a4761cf43b01e1), Antrag beenden und weiterleiten (sid-1v3m-b0399020c4b54c4fa3a4761cf43b01e1), Bankverbindung einsehen (abo9-b0399020c4b54c4fa3a4761cf43b01e1), Änderungen zur Bankverbindung eingeben (abpu-b0399020c4b54c4fa3a4761cf43b01e1), Reiseverlauf anpassen (i46u-b0399020c4b54c4fa3a4761cf43b01e1), Beförderungsmittel anpassen (sid-10lea-b0399020c4b54c4fa3a4761cf43b01e1), Hotelrechnung anpassen (sid-12so0-b0399020c4b54c4fa3a4761cf43b01e1), Übernachtung eingeben (sid-1dl33-b0399020c4b54c4fa3a4761cf43b01e1), Kürzungen angeben (sid-1egam-b0399020c4b54c4fa3a4761cf43b01e1), Nebenkosten eingeben (sid-208pr-b0399020c4b54c4fa3a4761cf43b01e1), Wegstrecke angeben (sid-2ct8-ed2f4ad43858602ad6e00efd76b0f56e), Detailsansicht anzeigen (lhyw-ed2f4ad43858602ad6e00efd76b0f56e), PDF ausgeben (sm4r-ed2f4ad43858602ad6e00efd76b0f56e)
- **Result:** ❌ Failed

- **False Positives:** Genehmigten Dienstreiseantrag suchen (sid-1tzo-b0399020c4b54c4fa3a4761cf43b01e1), Reisedaten anpassen (sid-1tzp-b0399020c4b54c4fa3a4761cf43b01e1), Ausländischen Reiseverlauf anpassen (sid-1u3t-b0399020c4b54c4fa3a4761cf43b01e1), Kürzungen angeben (sid-1u4n-b0399020c4b54c4fa3a4761cf43b01e1), Fahrkosten anpassen (sid-1u8s-b0399020c4b54c4fa3a4761cf43b01e1), Finanzierungsdaten eingeben (sid-1um4-b0399020c4b54c4fa3a4761cf43b01e1), Daten zu Zahlungspartnern eingeben (sid-1up1-b0399020c4b54c4fa3a4761cf43b01e1), Notizen eingeben (sid-1uxn-b0399020c4b54c4fa3a4761cf43b01e1), Reiseverlauf anpassen (i46u-b0399020c4b54c4fa3a4761cf43b01e1), Beförderungsmittel anpassen (sid-10lea-b0399020c4b54c4fa3a4761cf43b01e1), Hotelrechnung anpassen (sid-12so0-b0399020c4b54c4fa3a4761cf43b01e1), Übernachtung eingeben (sid-1dl33-b0399020c4b54c4fa3a4761cf43b01e1), Kürzungen angeben (sid-1egam-b0399020c4b54c4fa3a4761cf43b01e1), Nebenkosten eingeben (sid-208pr-b0399020c4b54c4fa3a4761cf43b01e1), Wegstrecke angeben (sid-2ct8-ed2f4ad43858602ad6e00efd76b0f56e), Detailsansicht anzeigen (lhyw-ed2f4ad43858602ad6e00efd76b0f56e), PDF ausgeben (sm4r-ed2f4ad43858602ad6e00efd76b0f56e)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.294
- **Context Utilization:** 0.159
- **Samples:** 21 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Genehmigten Dienstreiseantrag suchen (sid-1tzo-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity searches for an approved business travel request, which requires accessing personal data such as the employee’s name, travel dates, and destination.
- **Reisedaten anpassen (sid-1tzp-b0399020c4b54c4fa3a4761cf43b01e1)**: Adjusting travel data involves editing personal information such as dates, destinations, and contact details of the traveler.
- **Ausländischen Reiseverlauf anpassen (sid-1u3t-b0399020c4b54c4fa3a4761cf43b01e1)**: Modifying a foreign travel itinerary requires handling personal data such as the traveler’s name, passport details, and travel dates.
- **Kürzungen angeben (sid-1u4n-b0399020c4b54c4fa3a4761cf43b01e1)**: Specifying reductions (e.g., cost reductions) involves processing personal data such as the employee’s name and travel expenses.
- **Fahrkosten anpassen (sid-1u8s-b0399020c4b54c4fa3a4761cf43b01e1)**: Adjusting travel costs requires handling personal data such as the traveler’s name, travel dates, and expense details.
- **Finanzierungsdaten eingeben (sid-1um4-b0399020c4b54c4fa3a4761cf43b01e1)**: Entering financing data involves processing personal data such as the employee’s name, financial account details, and travel budget.
- **Daten zu Zahlungspartnern eingeben (sid-1up1-b0399020c4b54c4fa3a4761cf43b01e1)**: Entering payment partner data requires handling personal data such as the traveler’s name, bank account information, and payment details.
- **Notizen eingeben (sid-1uxn-b0399020c4b54c4fa3a4761cf43b01e1)**: Entering notes involves processing personal data such as the employee’s name, travel details, and any remarks about the trip.
- **Dokumente hochladen (sid-1v10-b0399020c4b54c4fa3a4761cf43b01e1)**: Uploading documents requires handling personal data such as travel receipts, passports, and other identity documents.
- **Antrag beenden und weiterleiten (sid-1v3m-b0399020c4b54c4fa3a4761cf43b01e1)**: Finishing and forwarding the application involves transmitting personal data such as the employee’s name, travel details, and approval status to the next handler.
- **Bankverbindung einsehen (abo9-b0399020c4b54c4fa3a4761cf43b01e1)**: Viewing bank details requires accessing personal data such as the employee’s bank account number and routing information.
- **Änderungen zur Bankverbindung eingeben (abpu-b0399020c4b54c4fa3a4761cf43b01e1)**: Editing bank details involves processing personal data such as the employee’s name, bank account number, and routing information.
- **Reiseverlauf anpassen (i46u-b0399020c4b54c4fa3a4761cf43b01e1)**: Adjusting the travel itinerary requires handling personal data such as the employee’s name, travel dates, and destinations.
- **Beförderungsmittel anpassen (sid-10lea-b0399020c4b54c4fa3a4761cf43b01e1)**: Adjusting transport means involves processing personal data such as the employee’s name, travel dates, and chosen transport method.
- **Hotelrechnung anpassen (sid-12so0-b0399020c4b54c4fa3a4761cf43b01e1)**: Adjusting the hotel invoice requires handling personal data such as the employee’s name, hotel stay dates, and payment details.
- **Übernachtung eingeben (sid-1dl33-b0399020c4b54c4fa3a4761cf43b01e1)**: Entering overnight stay details involves processing personal data such as the employee’s name, dates, and accommodation information.
- **Kürzungen angeben (sid-1egam-b0399020c4b54c4fa3a4761cf43b01e1)**: Specifying reductions again involves processing personal data such as the employee’s name and travel expense details.
- **Nebenkosten eingeben (sid-208pr-b0399020c4b54c4fa3a4761cf43b01e1)**: Entering additional costs requires handling personal data such as the employee’s name, travel dates, and cost details.
- **Wegstrecke angeben (sid-2ct8-ed2f4ad43858602ad6e00efd76b0f56e)**: Specifying the travel distance involves processing personal data such as the employee’s name, travel dates, and route information.
- **Detailsansicht anzeigen (lhyw-ed2f4ad43858602ad6e00efd76b0f56e)**: Displaying the details view requires accessing personal data such as the employee’s name, travel itinerary, and expense details.
- **PDF ausgeben (sm4r-ed2f4ad43858602ad6e00efd76b0f56e)**: Generating a PDF of the travel claim involves processing personal data such as the employee’s name, travel dates, expenses, and supporting documents.

</details>

## Test Case 129 - Maschinenstörung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/129/preview?correctIds=sid-46D4BFF2-48E5-459D-ACAA-C9ACCD42573B,sid-5E2A40A5-3435-4BEE-AEC5-D8CAF2A6EED5,sid-110D2497-3601-4119-A52C-F8473C270F6C,sid-AC55AE26-39A6-4C2B-8905-D08B345E0B28,sid-A4ADCACD-D8F9-48ED-A5A1-9999DE32288F,sid-14D44907-DCA4-4782-9A34-4A2F37F556FC,sid-C53BF8F1-90D8-4DC5-B780-93366AAC4C6E,sid-70BF76B1-C8CE-46FB-AEFD-031ABAB6C51B&falsePositiveIds=sid-0B974FB2-8D0F-4B56-B92F-9851968C8870,sid-C55FBA30-D3F9-4ED4-997E-C96545BAC686&falseNegativeIds=&salt=60504.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 8
- Amount of False Positives: 2
- Amount of False Negatives: 0

- **Expected:** Supportcall absetzen (sid-46D4BFF2-48E5-459D-ACAA-C9ACCD42573B), Störung mit Hilfe von Support beheben (sid-5E2A40A5-3435-4BEE-AEC5-D8CAF2A6EED5), Monteur anfordern (sid-110D2497-3601-4119-A52C-F8473C270F6C), Supportcall absetzen (sid-AC55AE26-39A6-4C2B-8905-D08B345E0B28), Monteur anfordern (sid-A4ADCACD-D8F9-48ED-A5A1-9999DE32288F), Störung mit Hilfe von Support beheben (sid-14D44907-DCA4-4782-9A34-4A2F37F556FC), Abschluss und Dokumentation im Maschinenlogbuch (sid-C53BF8F1-90D8-4DC5-B780-93366AAC4C6E), Techniker anfordern (sid-70BF76B1-C8CE-46FB-AEFD-031ABAB6C51B)
- **Actual:** Supportcall absetzen (sid-46D4BFF2-48E5-459D-ACAA-C9ACCD42573B), Störung mit Hilfe von Support beheben (sid-5E2A40A5-3435-4BEE-AEC5-D8CAF2A6EED5), Monteur anfordern (sid-110D2497-3601-4119-A52C-F8473C270F6C), Supportcall absetzen (sid-AC55AE26-39A6-4C2B-8905-D08B345E0B28), Monteur anfordern (sid-A4ADCACD-D8F9-48ED-A5A1-9999DE32288F), Störung mit Hilfe von Support beheben (sid-14D44907-DCA4-4782-9A34-4A2F37F556FC), Abschluss und Dokumentation im Maschinenlogbuch (sid-C53BF8F1-90D8-4DC5-B780-93366AAC4C6E), Techniker anfordern (sid-70BF76B1-C8CE-46FB-AEFD-031ABAB6C51B), Störung mit Assistent beheben (sid-0B974FB2-8D0F-4B56-B92F-9851968C8870), Störung mit Hilfe beheben (sid-C55FBA30-D3F9-4ED4-997E-C96545BAC686)
- **Result:** ❌ Failed

- **False Positives:** Störung mit Assistent beheben (sid-0B974FB2-8D0F-4B56-B92F-9851968C8870), Störung mit Hilfe beheben (sid-C55FBA30-D3F9-4ED4-997E-C96545BAC686)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.240
- **Context Utilization:** 0.609
- **Samples:** 10 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Supportcall absetzen (sid-46D4BFF2-48E5-459D-ACAA-C9ACCD42573B)**: The activity involves setting up a support call, which requires collecting and using contact information such as phone numbers or email addresses of the operator or customer, thereby processing personal data.
- **Störung mit Hilfe von Support beheben (sid-5E2A40A5-3435-4BEE-AEC5-D8CAF2A6EED5)**: This task involves coordinating with support to resolve an issue, which typically requires exchanging personal data like operator details and possibly customer information, thus processing personal data.
- **Monteur anfordern (sid-110D2497-3601-4119-A52C-F8473C270F6C)**: Requesting a technician requires identifying the technician and possibly sharing personal data such as name, contact details, and location, thereby processing personal data.
- **Supportcall absetzen (sid-AC55AE26-39A6-4C2B-8905-D08B345E0B28)**: The activity involves setting up a support call, which requires collecting and using contact information such as phone numbers or email addresses of the operator or customer, thereby processing personal data.
- **Monteur anfordern (sid-A4ADCACD-D8F9-48ED-A5A1-9999DE32288F)**: Requesting a technician requires identifying the technician and possibly sharing personal data such as name, contact details, and location, thereby processing personal data.
- **Störung mit Hilfe von Support beheben (sid-14D44907-DCA4-4782-9A34-4A2F37F556FC)**: This task involves coordinating with support to resolve an issue, which typically requires exchanging personal data like operator details and possibly customer information, thus processing personal data.
- **Abschluss und Dokumentation im Maschinenlogbuch (sid-C53BF8F1-90D8-4DC5-B780-93366AAC4C6E)**: This activity records the completion of a machine operation in the logbook, which typically includes operator identifiers, shift times, and possibly other personal data, thus processing personal data.
- **Techniker anfordern (sid-70BF76B1-C8CE-46FB-AEFD-031ABAB6C51B)**: Requesting a technician involves identifying the technician and sharing personal data such as contact details, thereby processing personal data.
- **Störung mit Assistent beheben (sid-0B974FB2-8D0F-4B56-B92F-9851968C8870)**: This task uses an assistant to resolve an issue, which typically requires personal data such as operator identity and contact information, thus processing personal data.
- **Störung mit Hilfe beheben (sid-C55FBA30-D3F9-4ED4-997E-C96545BAC686)**: This task involves using help to fix an issue, which typically requires exchanging personal data such as operator details, thus processing personal data.

</details>

## Test Case 97 - Anlegen der Lehrendendaten
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/97/preview?correctIds=sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D,sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE,sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B,sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3,sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7,sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0,sid-B17C2630-6153-4618-80B6-0247C52B79BF,sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80,sid-BC28D36C-E060-4E77-9073-54C345E0F808,sid-F9972CAB-1E32-49E4-B81D-70771BB88947,sid-C4B71B84-B173-44A2-B378-6A9B45B08D74,sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48,sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED,sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF,sid-EE199D2B-132E-4696-987B-5FFEBDD4E064,sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD,sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1&falsePositiveIds=&falseNegativeIds=&salt=1202.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 17
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Antrag auf Datenpflege
stellen (sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D), ob die
Daten der
Lehrperson schon angelegt
sind prüfen (sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE), Lehrenden- 
daten temporär im CMS anlegen (sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B), ob Abeitsvertrag vorliegt prüfen (sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3), Einstellungs-
antrag erstellen (sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7), legt Grundgerüst für Personal an (sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0), externe
Personendaten
pflegen (sid-B17C2630-6153-4618-80B6-0247C52B79BF), legt Personal mit allen notwendigen Daten an (sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80), Person die Rolle Lehrender zuweisen (sid-BC28D36C-E060-4E77-9073-54C345E0F808), Arbeitsvertrag erstellen (sid-F9972CAB-1E32-49E4-B81D-70771BB88947), Arbeitsvertrag  in die Personalakte legen (sid-C4B71B84-B173-44A2-B378-6A9B45B08D74), Person im SVA deaktivieren (sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48), Person auf ungültig setzten (sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED), Person im CMS 
deaktivieren (sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF), Lehrenden- 
daten im CMS abgleichen (sid-EE199D2B-132E-4696-987B-5FFEBDD4E064),  (IDM) Zugangsdaten
 für Lehrenden erzeugen (sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD), Arbeitsvertrag unterschreiben (sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1)
- **Actual:** ob die
Daten der
Lehrperson schon angelegt
sind prüfen (sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE), ob Abeitsvertrag vorliegt prüfen (sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3), legt Grundgerüst für Personal an (sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0), externe
Personendaten
pflegen (sid-B17C2630-6153-4618-80B6-0247C52B79BF), legt Personal mit allen notwendigen Daten an (sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80), Person die Rolle Lehrender zuweisen (sid-BC28D36C-E060-4E77-9073-54C345E0F808), Person auf ungültig setzten (sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED), Arbeitsvertrag  in die Personalakte legen (sid-C4B71B84-B173-44A2-B378-6A9B45B08D74), Person im SVA deaktivieren (sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48), Arbeitsvertrag erstellen (sid-F9972CAB-1E32-49E4-B81D-70771BB88947),  (IDM) Zugangsdaten
 für Lehrenden erzeugen (sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD), Antrag auf Datenpflege
stellen (sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D), Lehrenden- 
daten im CMS abgleichen (sid-EE199D2B-132E-4696-987B-5FFEBDD4E064), Lehrenden- 
daten temporär im CMS anlegen (sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B), Person im CMS 
deaktivieren (sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF), Einstellungs-
antrag erstellen (sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7), Arbeitsvertrag unterschreiben (sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.108
- **Context Utilization:** 0.302
- **Samples:** 17 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **ob die
Daten der
Lehrperson schon angelegt
sind prüfen (sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE)**: The task checks whether the personal data of a teacher has already been created, which involves reading personal data of an individual.
- **ob Abeitsvertrag vorliegt prüfen (sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3)**: The task verifies if an employment contract exists, which requires accessing personal data of an employee.
- **legt Grundgerüst für Personal an (sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0)**: The task creates the basic personnel structure, which involves creating records that contain personal data.
- **externe
Personendaten
pflegen (sid-B17C2630-6153-4618-80B6-0247C52B79BF)**: The task maintains external personnel data, directly processing personal data of individuals.
- **legt Personal mit allen notwendigen Daten an (sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80)**: The task creates personnel records with all necessary data, which includes personal data.
- **Person die Rolle Lehrender zuweisen (sid-BC28D36C-E060-4E77-9073-54C345E0F808)**: The task assigns a teaching role to a person, which involves handling personal data of that individual.
- **Person auf ungültig setzten (sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED)**: The task sets a person as invalid, which updates personal data records.
- **Arbeitsvertrag  in die Personalakte legen (sid-C4B71B84-B173-44A2-B378-6A9B45B08D74)**: The task stores the employment contract in the personnel file, which involves storing personal data.
- **Person im SVA deaktivieren (sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48)**: The task deactivates a person in the SVA system, which updates personal data.
- **Arbeitsvertrag erstellen (sid-F9972CAB-1E32-49E4-B81D-70771BB88947)**: The task creates an employment contract, which involves generating personal data records.
- ** (IDM) Zugangsdaten
 für Lehrenden erzeugen (sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD)**: The task generates access credentials for a teacher, which includes personal data such as login information.
- **Antrag auf Datenpflege
stellen (sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D)**: The task submits a request for data maintenance, which involves handling personal data of the requester.
- **Lehrenden- 
daten im CMS abgleichen (sid-EE199D2B-132E-4696-987B-5FFEBDD4E064)**: The task synchronises teacher data in the CMS, which processes personal data of teachers.
- **Lehrenden- 
daten temporär im CMS anlegen (sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B)**: The task temporarily creates teacher data in the CMS, which involves creating personal data records.
- **Person im CMS 
deaktivieren (sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF)**: The task deactivates a person in the CMS, which updates personal data records.
- **Einstellungs-
antrag erstellen (sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7)**: The task creates a hiring request, which involves processing personal data of the applicant.
- **Arbeitsvertrag unterschreiben (sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1)**: The task signs an employment contract, which involves processing personal data of the employee.

</details>

## Test Case 103 - Abschlussarbeit erfassen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/103/preview?correctIds=sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7,sid-E2A8380F-3287-4747-A608-9D64E6EA96F4,sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD,sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242,sid-E71C0317-6761-4D46-9963-98D20F6353AE,sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765,sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399,sid-6B454017-EB2A-4C27-A17D-E4CE3379315D,sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245,sid-16659179-643C-4968-9543-E807A8BD5E47,sid-5A34C81B-720B-43E5-8883-F5491379778F,sid-E087B843-EE5B-4CF7-ADD5-122171A9235C,sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A,sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0,sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D,sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72,sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A,sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8,sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC,sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A,sid-8F5478CC-BC17-4E18-AB76-F537290F687E,sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9,sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7,sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140,sid-7113D8F7-41CA-4A42-9B3F-6815107291EA,sid-832872EC-BDFE-4453-97FE-8235E8398554&falsePositiveIds=&falseNegativeIds=&salt=50443.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 26
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Prüfer 1
bestätigen (sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7), Prüfer 1  schriftlich bestätigen (sid-E2A8380F-3287-4747-A608-9D64E6EA96F4), Prüfer 2 schriftlich bestätigen  (sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD), Prüfer 2 bestätigen (sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242), Bestätigung erfassen (sid-E71C0317-6761-4D46-9963-98D20F6353AE), Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden (sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765), Abgabedatum ändern und Bestätigung verschicken (sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399), Antrag auf Frist-
verlängerung überprüfen (sid-6B454017-EB2A-4C27-A17D-E4CE3379315D), NB verbucht (sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245), Abgabedatum in CMS eingeben (sid-16659179-643C-4968-9543-E807A8BD5E47), Abschlussarbeit an Prüfer weitergeben (sid-5A34C81B-720B-43E5-8883-F5491379778F), bei externem Prüfer Note eintragen  (sid-E087B843-EE5B-4CF7-ADD5-122171A9235C), Gutachten erstellen und schriftlich weiterreichen (sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0), Gutachten erstellen und schriftlich weiterreichen (sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72), Gutachten erstellen,  in CMS hochladen (sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A), Bonierung bestätigen (sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8), bei externem Prüfer Note eintragen (sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC), ob Noten mehr als xx auseinander-
liegen prüfen (sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A), Endnote der Abschluss-arbeit eintragen (sid-8F5478CC-BC17-4E18-AB76-F537290F687E), Studierenden  über WH-Modalitäten informieren (sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9), für Abschluss-
arbeit anmelden (sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7), Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen (sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140), Antrag beim PA-Vor-sitzenden stellen (sid-7113D8F7-41CA-4A42-9B3F-6815107291EA), Bestätigung erfassen (sid-832872EC-BDFE-4453-97FE-8235E8398554)
- **Actual:** Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden (sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765), Abgabedatum ändern und Bestätigung verschicken (sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399), Abschlussarbeit an Prüfer weitergeben (sid-5A34C81B-720B-43E5-8883-F5491379778F), Abgabedatum in CMS eingeben (sid-16659179-643C-4968-9543-E807A8BD5E47), bei externem Prüfer Note eintragen  (sid-E087B843-EE5B-4CF7-ADD5-122171A9235C), bei externem Prüfer Note eintragen (sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC), ob Noten mehr als xx auseinander-
liegen prüfen (sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A), Endnote der Abschluss-arbeit eintragen (sid-8F5478CC-BC17-4E18-AB76-F537290F687E), Bestätigung erfassen (sid-832872EC-BDFE-4453-97FE-8235E8398554), Bestätigung erfassen (sid-E71C0317-6761-4D46-9963-98D20F6353AE), NB verbucht (sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245), Bonierung bestätigen (sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8), Studierenden  über WH-Modalitäten informieren (sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0), Prüfer 1  schriftlich bestätigen (sid-E2A8380F-3287-4747-A608-9D64E6EA96F4), Prüfer 1
bestätigen (sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7), Gutachten erstellen und schriftlich weiterreichen (sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A), Antrag auf Frist-
verlängerung überprüfen (sid-6B454017-EB2A-4C27-A17D-E4CE3379315D), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72), Prüfer 2 bestätigen (sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242), Prüfer 2 schriftlich bestätigen  (sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD), Gutachten erstellen und schriftlich weiterreichen (sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D), Gutachten erstellen,  in CMS hochladen (sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A), für Abschluss-
arbeit anmelden (sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7), Antrag beim PA-Vor-sitzenden stellen (sid-7113D8F7-41CA-4A42-9B3F-6815107291EA), Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen (sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.061
- **Context Utilization:** 0.173
- **Samples:** 26 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden (sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765)**: The activity "Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden" records a student’s formal registration and sends a confirmation with the submission date, which involves collecting, storing, and transmitting the student’s personal data such as name, contact details, and submission information.
- **Abgabedatum ändern und Bestätigung verschicken (sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399)**: The activity "Abgabedatum ändern und Bestätigung verschicken" updates the submission date and sends a confirmation, thereby modifying and transmitting the student’s personal data record.
- **Abschlussarbeit an Prüfer weitergeben (sid-5A34C81B-720B-43E5-8883-F5491379778F)**: The activity "Abschlussarbeit an Prüfer weitergeben" forwards the thesis to an examiner, transferring the student’s personal data and thesis content to another party.
- **Abgabedatum in CMS eingeben (sid-16659179-643C-4968-9543-E807A8BD5E47)**: The activity "Abgabedatum in CMS eingeben" inputs the submission date into the CMS, storing the student’s personal data in the system.
- **bei externem Prüfer Note eintragen  (sid-E087B843-EE5B-4CF7-ADD5-122171A9235C)**: The activity "bei externem Prüfer Note eintragen" records a grade for an external examiner, which involves handling the student’s personal data and the examiner’s personal data.
- **bei externem Prüfer Note eintragen (sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC)**: The activity "bei externem Prüfer Note eintragen" records a grade for an external examiner, which involves handling the student’s personal data and the examiner’s personal data.
- **ob Noten mehr als xx auseinander-
liegen prüfen (sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A)**: The activity "ob Noten mehr als xx auseinander liegen prüfen" checks whether grades differ beyond a threshold, which requires accessing the student’s personal data and grade records.
- **Endnote der Abschluss-arbeit eintragen (sid-8F5478CC-BC17-4E18-AB76-F537290F687E)**: The activity "Endnote der Abschluss-arbeit eintragen" records the final grade for the thesis, processing the student’s personal data.
- **Bestätigung erfassen (sid-832872EC-BDFE-4453-97FE-8235E8398554)**: The activity "Bestätigung erfassen" captures a confirmation, which involves recording the student’s personal data.
- **Bestätigung erfassen (sid-E71C0317-6761-4D46-9963-98D20F6353AE)**: The activity "Bestätigung erfassen" captures a confirmation, which involves recording the student’s personal data.
- **NB verbucht (sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245)**: The activity "NB verbucht" logs the grade book entry, storing the student’s personal data.
- **Bonierung bestätigen (sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8)**: The activity "Bonierung bestätigen" confirms a bonus, which involves processing the student’s personal data.
- **Studierenden  über WH-Modalitäten informieren (sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9)**: The activity "Studierenden über WH-Modalitäten informieren" informs students about WH modalities, sending personal data such as email addresses to the students.
- **Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0)**: The activity "Gutachten erstellen, in CMS hochladen und Note eintragen" creates a report, uploads it to CMS, and records a grade, all of which involve processing the student’s personal data.
- **Prüfer 1  schriftlich bestätigen (sid-E2A8380F-3287-4747-A608-9D64E6EA96F4)**: The activity "Prüfer 1 schriftlich bestätigen" confirms the examiner’s written confirmation, recording the examiner’s personal data.
- **Prüfer 1
bestätigen (sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7)**: The activity "Prüfer 1 bestätigen" confirms the examiner’s confirmation, processing the examiner’s personal data.
- **Gutachten erstellen und schriftlich weiterreichen (sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A)**: The activity "Gutachten erstellen und schriftlich weiterreichen" creates a report and forwards it, transferring the student’s personal data to the examiner.
- **Antrag auf Frist-
verlängerung überprüfen (sid-6B454017-EB2A-4C27-A17D-E4CE3379315D)**: The activity "Antrag auf Fristverlängerung überprüfen" reviews an extension request, which involves accessing the student’s personal data and request details.
- **Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72)**: The activity "Gutachten erstellen, in CMS hochladen und Note eintragen" creates a report, uploads it to CMS, and records a grade, processing the student’s personal data.
- **Prüfer 2 bestätigen (sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242)**: The activity "Prüfer 2 bestätigen" confirms the second examiner, recording the examiner’s personal data.
- **Prüfer 2 schriftlich bestätigen  (sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD)**: The activity "Prüfer 2 schriftlich bestätigen" confirms the second examiner’s written confirmation, processing the examiner’s personal data.
- **Gutachten erstellen und schriftlich weiterreichen (sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D)**: The activity "Gutachten erstellen und schriftlich weiterreichen" creates a report and forwards it, transferring the student’s personal data to the examiner.
- **Gutachten erstellen,  in CMS hochladen (sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A)**: The activity "Gutachten erstellen, in CMS hochladen" creates a report and uploads it to CMS, processing the student’s personal data.
- **für Abschluss-
arbeit anmelden (sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7)**: The activity "für Abschluss-arbeit anmelden" registers the thesis, collecting the student’s personal data and thesis details.
- **Antrag beim PA-Vor-sitzenden stellen (sid-7113D8F7-41CA-4A42-9B3F-6815107291EA)**: The activity "Antrag beim PA-Vorsitzenden stellen" submits an extension request, which involves collecting the student’s personal data and request details.
- **Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen (sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140)**: The activity "Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen" submits the thesis PDF to CMS, storing the student’s personal data and thesis content.

</details>

# Run 2

## Summary (GPT-OSS-20B)

## Summary
Total: 19
Passed: 9
Failed: 8
Error: 2
Total Retries: 0

### Metrics
Accuracy: 0.872
Precision: 0.875
Recall: 0.955
F1-Score: 0.913

### Confusion Matrix
True Positives: 126
False Positives: 18
False Negatives: 6
True Negatives: 38

### RAG Metrics (Ragas) — averaged across 15 test case(s)
Faithfulness: 0.219
Context Utilization: 0.320
Total Samples Scored: 144 (failed: 0)

## Model: GPT-OSS-20B

## Test Case 43 - Technische Wartung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/43/preview?correctIds=&falsePositiveIds=&falseNegativeIds=&salt=81005.0" alt="Test Case BPMN XML" />

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

## Test Case 70 - Wartungsplanung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/70/preview?correctIds=&falsePositiveIds=&falseNegativeIds=&salt=93399.0" alt="Test Case BPMN XML" />

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

## Test Case 40 - Online Bestellung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/40/preview?correctIds=Activity_1jmvap9&falsePositiveIds=Activity_1biw5cc,Activity_0w3m6fk,Activity_1ehspue&falseNegativeIds=&salt=34313.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 1
- Amount of False Positives: 3
- Amount of False Negatives: 0

- **Expected:** Bestellung abschicken (Activity_1jmvap9)
- **Actual:** Lieferadresse eingeben (Activity_1biw5cc), Zahlungsdaten angeben (Activity_0w3m6fk), Bestellung abschicken (Activity_1jmvap9), Bestellbestätigung anzeigen (Activity_1ehspue)
- **Result:** ❌ Failed

- **False Positives:** Lieferadresse eingeben (Activity_1biw5cc), Zahlungsdaten angeben (Activity_0w3m6fk), Bestellbestätigung anzeigen (Activity_1ehspue)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.333
- **Context Utilization:** 0.382
- **Samples:** 4 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Lieferadresse eingeben (Activity_1biw5cc)**: The activity "Lieferadresse eingeben" collects the customer's delivery address, which is personal data such as name, street, city, and postal code.
- **Zahlungsdaten angeben (Activity_0w3m6fk)**: The activity "Zahlungsdaten angeben" requires the customer to provide payment information such as credit‑card or bank account details, which are personal data.
- **Bestellung abschicken (Activity_1jmvap9)**: The activity "Bestellung abschicken" transmits the order details, including the customer's personal data (name, address, payment information), to the system for processing.
- **Bestellbestätigung anzeigen (Activity_1ehspue)**: The activity "Bestellbestätigung anzeigen" retrieves and displays the order confirmation, which includes personal data such as the customer's name and order details.

</details>

## Test Case 19 - Introduce patient to head physician
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/19/preview?correctIds=sid-1177E778-9B60-4A48-895E-82DAAC180C9E,sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B,sid-4FAB8B03-8311-42F4-91B2-BDC72772535E&falsePositiveIds=&falseNegativeIds=&salt=53668.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 3
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Introduce patient with all results to head physician (sid-1177E778-9B60-4A48-895E-82DAAC180C9E), Order surgery to be performed (sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B), Examine patient (sid-4FAB8B03-8311-42F4-91B2-BDC72772535E)
- **Actual:** Introduce patient with all results to head physician (sid-1177E778-9B60-4A48-895E-82DAAC180C9E), Examine patient (sid-4FAB8B03-8311-42F4-91B2-BDC72772535E), Order surgery to be performed (sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.889
- **Context Utilization:** 0.536
- **Samples:** 3 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Introduce patient with all results to head physician (sid-1177E778-9B60-4A48-895E-82DAAC180C9E)**: The task "Introduce patient with all results to head physician" involves the transfer and handling of the patient’s medical results, which are personal data under the GDPR. The activity explicitly deals with the patient’s health information, thereby constituting processing of personal data.
- **Examine patient (sid-4FAB8B03-8311-42F4-91B2-BDC72772535E)**: The task "Examine patient" is a medical examination that directly processes the patient’s health data. This activity collects, records, and uses personal health information, which is covered by the GDPR.
- **Order surgery to be performed (sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B)**: The task "Order surgery to be performed" involves ordering a medical procedure for a specific patient, which requires the use of the patient’s personal and health data. This constitutes processing of personal data under the GDPR.

</details>

## Test Case 57 - Dienstreiseabrechnung genehmigen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/57/preview?correctIds=xtre-ed2f4ad43858602ad6e00efd76b0f56e,xttr-ed2f4ad43858602ad6e00efd76b0f56e,xu5h-ed2f4ad43858602ad6e00efd76b0f56e,xu3y-ed2f4ad43858602ad6e00efd76b0f56e,xtts-ed2f4ad43858602ad6e00efd76b0f56e,xtrf-ed2f4ad43858602ad6e00efd76b0f56e,xu2f-ed2f4ad43858602ad6e00efd76b0f56e&falsePositiveIds=&falseNegativeIds=&salt=12008.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 7
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Dienstreiseabrechnung sichten (xtre-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung genehmigen (xttr-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung ablehnen (xu5h-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an andere Sachbearbeitung weiterleiten (xu3y-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnungsdaten anpassen (xtts-ed2f4ad43858602ad6e00efd76b0f56e), Kommentar eingeben (xtrf-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an Antragsteller zurücksenden (xu2f-ed2f4ad43858602ad6e00efd76b0f56e)
- **Actual:** Dienstreiseabrechnung sichten (xtre-ed2f4ad43858602ad6e00efd76b0f56e), Kommentar eingeben (xtrf-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung genehmigen (xttr-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnungsdaten anpassen (xtts-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an Antragsteller zurücksenden (xu2f-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an andere Sachbearbeitung weiterleiten (xu3y-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung ablehnen (xu5h-ed2f4ad43858602ad6e00efd76b0f56e)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.000
- **Context Utilization:** 0.322
- **Samples:** 7 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Dienstreiseabrechnung sichten (xtre-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Dienstreiseabrechnung sichten" involves reviewing a travel expense claim, which contains personal data such as employee name, travel details, expenses, and contact information. This activity processes personal data.
- **Kommentar eingeben (xtrf-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Kommentar eingeben" involves entering a comment on the travel expense claim, which is part of the processing of personal data related to the claim.
- **Abrechnung genehmigen (xttr-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnung genehmigen" involves approving the travel expense claim, which requires access to personal data of the employee and travel details.
- **Abrechnungsdaten anpassen (xtts-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnungsdaten anpassen" involves adjusting the claim data, which includes personal data such as employee information and expense details.
- **Abrechnung an Antragsteller zurücksenden (xu2f-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnung an Antragsteller zurücksenden" involves sending the claim back to the applicant, transmitting personal data to the data subject.
- **Abrechnung an andere Sachbearbeitung weiterleiten (xu3y-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnung an andere Sachbearbeitung weiterleiten" involves forwarding the claim to another department, transmitting personal data within the organization.
- **Abrechnung ablehnen (xu5h-ed2f4ad43858602ad6e00efd76b0f56e)**: The task "Abrechnung ablehnen" involves rejecting the claim, which requires processing personal data of the employee and travel details.

</details>

## Test Case 29 - Arzttermin buchen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/29/preview?correctIds=Activity_0gi1msm,Activity_1agrvaf,Activity_1molpic&falsePositiveIds=&falseNegativeIds=&salt=80482.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 3
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Patientendaten aufnehmen (Activity_0gi1msm), Termin bestätigen (Activity_1agrvaf), Erinnerung senden (Activity_1molpic)
- **Actual:** Patientendaten aufnehmen (Activity_0gi1msm), Termin bestätigen (Activity_1agrvaf), Erinnerung senden (Activity_1molpic)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.194
- **Context Utilization:** 0.315
- **Samples:** 3 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Patientendaten aufnehmen (Activity_0gi1msm)**: The activity 'Patientendaten aufnehmen' records patient data, which includes personal identifiers such as name, address, medical information, and therefore processes personal data.
- **Termin bestätigen (Activity_1agrvaf)**: The activity 'Termin bestätigen' confirms an appointment by accessing and using the patient's personal details to send confirmation, thus processing personal data.
- **Erinnerung senden (Activity_1molpic)**: The activity 'Erinnerung senden' sends a reminder to the patient using their contact information, which is personal data, so it processes personal data.

</details>

## Test Case 98 - Pflege der Lehrendendaten
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/98/preview?correctIds=sid-6AF85299-5179-4780-B917-67C3678C0062,sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B,sid-CCFEBADD-0645-4F1F-936A-F2094AC68729,sid-9C71399D-C2E4-45E3-8256-FBD48257840A,sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A,sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110,sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF,sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F&falsePositiveIds=&falseNegativeIds=&salt=97094.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 8
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Lehrendendaten
überprüfen (sid-6AF85299-5179-4780-B917-67C3678C0062), Lehrenden deaktivieren (sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B), Lehrenden aktivieren (sid-CCFEBADD-0645-4F1F-936A-F2094AC68729), Lehrenden entfernen (sid-9C71399D-C2E4-45E3-8256-FBD48257840A), Lehrenden entfernen (sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A), Lehrenden-
daten aktualisieren (sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110), Lehrenden-
daten aktualisieren (sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF), Art der Pflege
überprüfen (sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F)
- **Actual:** Lehrenden-
daten aktualisieren (sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF), Lehrenden entfernen (sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A), Lehrendendaten
überprüfen (sid-6AF85299-5179-4780-B917-67C3678C0062), Art der Pflege
überprüfen (sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F), Lehrenden deaktivieren (sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B), Lehrenden entfernen (sid-9C71399D-C2E4-45E3-8256-FBD48257840A), Lehrenden-
daten aktualisieren (sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110), Lehrenden aktivieren (sid-CCFEBADD-0645-4F1F-936A-F2094AC68729)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.198
- **Context Utilization:** 0.459
- **Samples:** 8 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Lehrenden-
daten aktualisieren (sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF)**: The task "Lehrenden-daten aktualisieren" updates teacher records, which contain personal data such as names, contact details, and employment information. Updating such records is a processing activity that manipulates personal data.
- **Lehrenden entfernen (sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A)**: The task "Lehrenden entfernen" deletes a teacher record, which involves erasing personal data such as identity, contact information, and employment details.
- **Lehrendendaten
überprüfen (sid-6AF85299-5179-4780-B917-67C3678C0062)**: The task "Lehrendendaten überprüfen" checks teacher data, which requires accessing personal data such as names, contact details, and employment records.
- **Art der Pflege
überprüfen (sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F)**: The task "Art der Pflege überprüfen" verifies the type of care for a person, which involves accessing personal data of the care recipient such as health status and care needs.
- **Lehrenden deaktivieren (sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B)**: The task "Lehrenden deaktivieren" disables a teacher account, which requires handling personal data such as login credentials and identity information.
- **Lehrenden entfernen (sid-9C71399D-C2E4-45E3-8256-FBD48257840A)**: The task "Lehrenden entfernen" (second instance) deletes a teacher record, erasing personal data such as identity and contact details.
- **Lehrenden-
daten aktualisieren (sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110)**: The task "Lehrenden-daten aktualisieren" updates teacher data, which involves processing personal data such as names, contact information, and employment details.
- **Lehrenden aktivieren (sid-CCFEBADD-0645-4F1F-936A-F2094AC68729)**: The task "Lehrenden aktivieren" activates a teacher account, which requires handling personal data such as identity and login credentials.

</details>

## Test Case 3 - Preparing surgery
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/3/preview?correctIds=sid-D0309BAA-2BC7-4866-BD2B-36789C42198D,sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745,sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B,sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA,sid-A76861E0-B426-4259-997B-A72EEB63E3A1,sid-87E2518C-79DE-403D-8A33-010389B828A6,sid-5DA96990-76B4-40D0-88B6-915685E7E279,sid-8018CC06-69E9-4E62-977B-E441C039AEF0,sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF&falsePositiveIds=sid-E05999ED-FBEC-4936-994E-EC0F5808EF69,sid-C17C448D-8303-4345-BD8A-C773D4A6A63B&falseNegativeIds=sid-6EFE25FB-0893-440F-A48A-876386148A36,sid-71AF5554-8150-4459-9577-961A5D078CB2,sid-8048EF97-74FF-4BD4-B97A-FB3D04E435E2&salt=46454.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 9
- Amount of False Positives: 2
- Amount of False Negatives: 3

- **Expected:** Collect and evaluate all results (sid-D0309BAA-2BC7-4866-BD2B-36789C42198D), Check results (sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745), Perform pre-operation discussion  (sid-6EFE25FB-0893-440F-A48A-876386148A36), Enter patient into surgery board (sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B), Enter surgery into Oplus for surgery plan (sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA), Ordain premedication (sid-A76861E0-B426-4259-997B-A72EEB63E3A1), Introduce patient to head physician  (sid-71AF5554-8150-4459-9577-961A5D078CB2), Cancel surgery (sid-8048EF97-74FF-4BD4-B97A-FB3D04E435E2), Give medicaments (sid-87E2518C-79DE-403D-8A33-010389B828A6), Check electrolytes (sid-5DA96990-76B4-40D0-88B6-915685E7E279), Perform colonoskopy (sid-8018CC06-69E9-4E62-977B-E441C039AEF0), Order preparations of surgery in kardex (sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF)
- **Actual:** Patient fasts (sid-E05999ED-FBEC-4936-994E-EC0F5808EF69), Give medicaments (sid-87E2518C-79DE-403D-8A33-010389B828A6), Perform colonoskopy (sid-8018CC06-69E9-4E62-977B-E441C039AEF0), Check electrolytes (sid-5DA96990-76B4-40D0-88B6-915685E7E279), Enter patient into surgery board (sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B), Collect and evaluate all results (sid-D0309BAA-2BC7-4866-BD2B-36789C42198D), Enter surgery into Oplus for surgery plan (sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA), Order preparations of surgery in kardex (sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF), Meet patient (sid-C17C448D-8303-4345-BD8A-C773D4A6A63B), Check results (sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745), Ordain premedication (sid-A76861E0-B426-4259-997B-A72EEB63E3A1)
- **Result:** ❌ Failed

- **False Positives:** Patient fasts (sid-E05999ED-FBEC-4936-994E-EC0F5808EF69), Meet patient (sid-C17C448D-8303-4345-BD8A-C773D4A6A63B)
- **False Negatives:** Perform pre-operation discussion  (sid-6EFE25FB-0893-440F-A48A-876386148A36), Introduce patient to head physician  (sid-71AF5554-8150-4459-9577-961A5D078CB2), Cancel surgery (sid-8048EF97-74FF-4BD4-B97A-FB3D04E435E2)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.318
- **Context Utilization:** 0.348
- **Samples:** 11 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Patient fasts (sid-E05999ED-FBEC-4936-994E-EC0F5808EF69)**: The activity 'Patient fasts' is a medical procedure performed on a patient, a natural person. The procedure is part of healthcare and therefore processes the patient's health data, which is personal data under GDPR.
- **Give medicaments (sid-87E2518C-79DE-403D-8A33-010389B828A6)**: The activity 'Give medicaments' administers medication to a patient, directly involving the patient's health data. This is processing of personal data.
- **Perform colonoskopy (sid-8018CC06-69E9-4E62-977B-E441C039AEF0)**: The activity 'Perform colonoskopy' is a medical examination of a patient, which generates and uses the patient's health data. This constitutes processing of personal data.
- **Check electrolytes (sid-5DA96990-76B4-40D0-88B6-915685E7E279)**: The activity 'Check electrolytes' involves laboratory testing of a patient's blood, producing health data that is personal data. This is processing.
- **Enter patient into surgery board (sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B)**: The activity 'Enter patient into surgery board' records the patient's identity and medical information into the surgery board, which is a data record. This is processing of personal data.
- **Collect and evaluate all results (sid-D0309BAA-2BC7-4866-BD2B-36789C42198D)**: The activity 'Collect and evaluate all results' gathers and assesses medical test results for a patient, which are personal data. This is processing.
- **Enter surgery into Oplus for surgery plan (sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA)**: The activity 'Enter surgery into Oplus for surgery plan' inputs the patient's surgery plan into the Oplus system, storing personal data. This is processing.
- **Order preparations of surgery in kardex (sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF)**: The activity 'Order preparations of surgery in kardex' records preparation details for a patient in the kardex, which includes personal data. This is processing.
- **Meet patient (sid-C17C448D-8303-4345-BD8A-C773D4A6A63B)**: The activity 'Meet patient' involves direct interaction with a patient, during which personal data is handled. This is processing.
- **Check results (sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745)**: The activity 'Check results' reviews medical test results for a patient, which are personal data. This is processing.
- **Ordain premedication (sid-A76861E0-B426-4259-997B-A72EEB63E3A1)**: The activity 'Ordain premedication' administers premedication to a patient, involving the patient's health data. This is processing.

</details>

## Test Case 59 - Neuen Mitarbeiter erfassen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/59/preview?correctIds=sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551,sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4,x7f-f3cda2a0d4d7330d9353884ec580e5e4,vv8-f3cda2a0d4d7330d9353884ec580e5e4,yys-f3cda2a0d4d7330d9353884ec580e5e4,sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4&falsePositiveIds=&falseNegativeIds=sid-7hno-95f654cf6849a76aaf94b5a98ebc5551&salt=35060.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 6
- Amount of False Positives: 0
- Amount of False Negatives: 1

- **Expected:** QIS-Nutzer anlegen (sid-7hno-95f654cf6849a76aaf94b5a98ebc5551), QIS-Rolle zuordnen (sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551), Stammsätze anlegen (sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4), Chipkarte zuordnen (x7f-f3cda2a0d4d7330d9353884ec580e5e4), Arbeitszeitmodell zuordnen (vv8-f3cda2a0d4d7330d9353884ec580e5e4), Arbeitszeit eintragen (yys-f3cda2a0d4d7330d9353884ec580e5e4), Abweichende Sollarbeitszeit eintragen (sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4)
- **Actual:** Arbeitszeitmodell zuordnen (vv8-f3cda2a0d4d7330d9353884ec580e5e4), Chipkarte zuordnen (x7f-f3cda2a0d4d7330d9353884ec580e5e4), Arbeitszeit eintragen (yys-f3cda2a0d4d7330d9353884ec580e5e4), Abweichende Sollarbeitszeit eintragen (sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4), Stammsätze anlegen (sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4), QIS-Rolle zuordnen (sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551)
- **Result:** ❌ Failed

- **False Positives:** None
- **False Negatives:** QIS-Nutzer anlegen (sid-7hno-95f654cf6849a76aaf94b5a98ebc5551)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.333
- **Context Utilization:** 0.464
- **Samples:** 6 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Arbeitszeitmodell zuordnen (vv8-f3cda2a0d4d7330d9353884ec580e5e4)**: The task "Arbeitszeitmodell zuordnen" assigns a working‑time model to an employee, which involves accessing and updating the employee’s personal working‑time records. This activity processes personal data.
- **Chipkarte zuordnen (x7f-f3cda2a0d4d7330d9353884ec580e5e4)**: The task "Chipkarte zuordnen" assigns a chip card to an employee, which requires handling the employee’s personal identification and card details. This is a processing of personal data.
- **Arbeitszeit eintragen (yys-f3cda2a0d4d7330d9353884ec580e5e4)**: The task "Arbeitszeit eintragen" records an employee’s working hours, which involves capturing the employee’s personal time‑keeping data. This activity processes personal data.
- **Abweichende Sollarbeitszeit eintragen (sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4)**: The task "Abweichende Sollarbeitszeit eintragen" records a deviation from the standard working time for an employee, which requires handling the employee’s personal working‑time data. This is a processing of personal data.
- **Stammsätze anlegen (sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4)**: The task "Stammsätze anlegen" creates master records for an employee, which involves storing the employee’s personal identifiers, contact details and employment information. This activity processes personal data.
- **QIS-Rolle zuordnen (sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551)**: The task "QIS‑Rolle zuordnen" assigns a role to an employee in the QIS system, which requires accessing the employee’s personal role and permission data. This activity processes personal data.

</details>

## Test Case 94 - Lehrveranstaltung koordinieren
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/94/preview?correctIds=sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651,sid-24B998D4-5424-41DD-A564-6CEACF2F115D,sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9,sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745,sid-B1E78F49-99A0-4AFA-8019-A4042F018C63,sid-EC199047-4E2E-45A5-9475-EEF8357F1B49&falsePositiveIds=sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89,sid-C4B1896E-4290-4049-9753-3C607BFAAA46,sid-3CD2926F-2770-49BB-AB78-E78E12F25367,sid-34FCE951-492D-4098-8450-5EBEF19E80D0,sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4,sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35&falseNegativeIds=sid-46DD6D1E-8D65-47A8-BEF3-A9FA5F7FD6BC&salt=14642.0" alt="Test Case BPMN XML" />

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
- **Faithfulness:** 0.028
- **Context Utilization:** 0.163
- **Samples:** 12 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-F448B14E-6D6A-4AD1-A5F2-11F833232D89)**: This task checks whether new events need to be created, which involves handling lecturer information and event details that are personal data.
- ** ob eine Änderung des Lehrenden in einer vorhandenen Veranstaltung nötig ist prüfen (sid-C4B1896E-4290-4049-9753-3C607BFAAA46)**: This task verifies whether a lecturer change is needed for an existing event, which requires processing lecturer personal data.
- **Lehrveran-staltung erstellen (sid-3CD2926F-2770-49BB-AB78-E78E12F25367)**: Creating a lecture event involves storing event details and lecturer information, both of which are personal data.
- **ob Lehrender in CMS verfügbar ist prüfen (sid-83BA7AB7-58E4-4EB1-9F3D-41100124A651)**: Checking lecturer availability in the CMS requires accessing lecturer personal data.
- **Lehrenden der Veranstaltung zuteilen (sid-24B998D4-5424-41DD-A564-6CEACF2F115D)**: Assigning lecturers to an event involves handling lecturer personal data.
- **aktuelle Veranstaltungs-Liste generieren (sid-34FCE951-492D-4098-8450-5EBEF19E80D0)**: Generating the current event list requires accessing event and lecturer personal data.
- **welche Änderungen an der Veranstaltung notwendig sind prüfen (sid-DD117211-5CF0-42FE-9E0D-81E0D98B07D9)**: Checking which changes to an event are needed involves processing event and lecturer personal data.
- **Änderungen vornehmen (sid-EC199047-4E2E-45A5-9475-EEF8357F1B49)**: Making changes to an event requires handling event and lecturer personal data.
- **ob neue Veranstaltungen angelegt werden müssen prüfen (sid-8CD2E544-C82F-444A-B20B-7BA424AE43E4)**: Checking whether new events need to be created in the Lehrender lane involves processing lecturer personal data.
- **ob eine Änderung für eine vorhandenen Veranstaltung notwendig ist prüfen (sid-B1E78F49-99A0-4AFA-8019-A4042F018C63)**: Checking if a change for an existing event is needed involves processing event and lecturer personal data.
- **eingeschränkt Änderungen vornehmen (sid-FB83D4A4-0C6B-4936-9B1C-F94569A6D745)**: Making restricted changes to an event requires handling event and lecturer personal data.
- **Änderungen,
die er nicht selbst vornehmen darf melden (sid-56B37B82-3EC3-42AA-B7EF-CBF5E67DDF35)**: Reporting changes that cannot be made involves handling event and lecturer personal data.

</details>

## Test Case 135 - Immatrikulation
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/135/preview?correctIds=sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277,sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB,sid-0728004E-21CC-4D5E-9D26-6FB3182102F5,sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D,sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A,sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF,sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A,sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A&falsePositiveIds=&falseNegativeIds=&salt=24076.0" alt="Test Case BPMN XML" />

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
- **Faithfulness:** 0.125
- **Context Utilization:** 0.129
- **Samples:** 8 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB)**: Bewerbung bearbeiten processes applicant personal data such as names, contact details, academic records, and other identifying information required for evaluating the application.
- **Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5)**: Studienplatz vergeben uses personal data of applicants to decide and assign study places, thereby processing identifiable information.
- **Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A)**: Bewerber immatrikulieren creates and stores enrollment records that contain personal data of the applicant, thus processing identifiable information.
- **traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D)**: Traditionelles Nachrück-verfahren processes personal data of applicants for replacement positions, involving collection and use of identifiable information.
- **Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277)**: Bewerbung durchführen involves the applicant submitting an application that contains personal data such as name, contact details, and academic information.
- **Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A)**: Koordinier-ungsphase 1 mit Entscheidungsphase coordinates decisions based on applicant personal data, thereby processing identifiable information.
- **Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A)**: Koordinierungsphase 2 continues coordination using applicant personal data, thus processing identifiable information.
- **Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF)**: Clearingphase likely involves the processing or removal of personal data records, thereby handling identifiable information.

</details>

## Test Case 132 - Rückmeldung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/132/preview?correctIds=sid-716BD875-CB9C-4173-91AE-20E2BEFB723E,sid-9E177368-C98D-401A-8179-38BEB5DF6390,sid-2A902299-023A-44C5-9A76-16F47701996C,sid-9A7FCC57-F21E-4924-9968-224952C660DD,sid-EF63A163-83B2-46A7-85E9-0089AC7D0786,sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66,sid-F8D21C49-24E6-479B-84DF-98B5D63593C8,sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F,sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB&falsePositiveIds=&falseNegativeIds=sid-82F7C1B0-B225-469A-8C21-15B72F0A53CF&salt=10808.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 9
- Amount of False Positives: 0
- Amount of False Negatives: 1

- **Expected:** Gebühren ermitteln (gebsql) (sid-716BD875-CB9C-4173-91AE-20E2BEFB723E), Informations-E-Mail erstellen und an Student senden
(Fristen und Zahlungs-
details) (sid-9E177368-C98D-401A-8179-38BEB5DF6390), Beiträge überweisen (sid-2A902299-023A-44C5-9A76-16F47701996C), 4.1 Gebührenmanagement (sid-82F7C1B0-B225-469A-8C21-15B72F0A53CF), Datensätze in das CMS einspielen (sid-9A7FCC57-F21E-4924-9968-224952C660DD), ob 
Ist = Soll
prüfen (sid-EF63A163-83B2-46A7-85E9-0089AC7D0786), Mahnschreiben erstellen und an Student senden (sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66), auf Rückmelde-sperre prüfen und Mängel beheben (sid-F8D21C49-24E6-479B-84DF-98B5D63593C8), Studierenden
informieren (sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F), Studenten exmatrikulieren (sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB)
- **Actual:** Gebühren ermitteln (gebsql) (sid-716BD875-CB9C-4173-91AE-20E2BEFB723E), Datensätze in das CMS einspielen (sid-9A7FCC57-F21E-4924-9968-224952C660DD), ob 
Ist = Soll
prüfen (sid-EF63A163-83B2-46A7-85E9-0089AC7D0786), auf Rückmelde-sperre prüfen und Mängel beheben (sid-F8D21C49-24E6-479B-84DF-98B5D63593C8), Studierenden
informieren (sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F), Mahnschreiben erstellen und an Student senden (sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66), Informations-E-Mail erstellen und an Student senden
(Fristen und Zahlungs-
details) (sid-9E177368-C98D-401A-8179-38BEB5DF6390), Studenten exmatrikulieren (sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB), Beiträge überweisen (sid-2A902299-023A-44C5-9A76-16F47701996C)
- **Result:** ❌ Failed

- **False Positives:** None
- **False Negatives:** 4.1 Gebührenmanagement (sid-82F7C1B0-B225-469A-8C21-15B72F0A53CF)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.037
- **Context Utilization:** 0.121
- **Samples:** 9 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Gebühren ermitteln (gebsql) (sid-716BD875-CB9C-4173-91AE-20E2BEFB723E)**: The task "Gebühren ermitteln (gebsql)" calculates fees for students, which requires access to students' personal data such as enrollment status and contact details.
- **Datensätze in das CMS einspielen (sid-9A7FCC57-F21E-4924-9968-224952C660DD)**: The task "Datensätze in das CMS einspielen" imports records into the CMS, which almost certainly contains personal data of students.
- **ob 
Ist = Soll
prüfen (sid-EF63A163-83B2-46A7-85E9-0089AC7D0786)**: The task "ob Ist = Soll prüfen" checks whether the billed amount matches the expected amount, which requires access to students' fee information – a form of personal data.
- **auf Rückmelde-sperre prüfen und Mängel beheben (sid-F8D21C49-24E6-479B-84DF-98B5D63593C8)**: The task "auf Rückmelde-sperre prüfen und Mängel beheben" checks for feedback locks and fixes defects, which involves handling students' personal data.
- **Studierenden
informieren (sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F)**: The task "Studierenden informieren" sends information to students, which requires the use of students' personal data such as names and contact details.
- **Mahnschreiben erstellen und an Student senden (sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66)**: The task "Mahnschreiben erstellen und an Student senden" creates reminder letters and sends them to students, which involves processing students' personal data.
- **Informations-E-Mail erstellen und an Student senden
(Fristen und Zahlungs-
details) (sid-9E177368-C98D-401A-8179-38BEB5DF6390)**: The task "Informations-E-Mail erstellen und an Student senden (Fristen und Zahlungs-details)" creates and sends an email containing personal data such as deadlines and payment details to a student.
- **Studenten exmatrikulieren (sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB)**: The task "Studenten exmatrikulieren" processes the removal of a student from enrollment, which requires handling the student's personal data.
- **Beiträge überweisen (sid-2A902299-023A-44C5-9A76-16F47701996C)**: The task "Beiträge überweisen" transfers contributions, which involves processing students' payment information – a form of personal data.

</details>

## Test Case 142 - Datenübermittlung Bewerber
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/142/preview?correctIds=sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561,sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67,sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21,sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE,sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0,sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76,sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156,sid-3492B288-6611-4891-954A-44FF10D5A30E,sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411,sid-5D601043-5FEE-4681-B123-44CE520423FF&falsePositiveIds=sid-8188439C-7014-4A94-ADE3-80E22199EEBF,sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7&falseNegativeIds=&salt=25703.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 10
- Amount of False Positives: 2
- Amount of False Negatives: 0

- **Expected:** Bewerber-account anlegen (sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561), vorhandene 
Qualifikationen eintragen (sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67), Qualifikationen (automatisch) prüfen (sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21), Angaben vervollständigen (sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE), Bewerbung auf Vollständigkeit prüfen (automatisch) (sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0), Antrag ausfüllen
und Unterlagen hinzufügen (sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76), nachzu-reichende Unterlagen im CMS
vermerken (sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156), Unterlagen auf Vollständigkeit prüfen und einscannen (sid-3492B288-6611-4891-954A-44FF10D5A30E), Unterlagen vervollständigen (sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411), Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen
 (sid-5D601043-5FEE-4681-B123-44CE520423FF)
- **Actual:** Bewerber-account anlegen (sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561), vorhandene 
Qualifikationen eintragen (sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67), Antrag ausfüllen
und Unterlagen hinzufügen (sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76), Angaben vervollständigen (sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE), Bewerbung abschließen (sid-8188439C-7014-4A94-ADE3-80E22199EEBF), Studiengang wählen (sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7), Unterlagen vervollständigen (sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411), Qualifikationen (automatisch) prüfen (sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21), Bewerbung auf Vollständigkeit prüfen (automatisch) (sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0), Unterlagen auf Vollständigkeit prüfen und einscannen (sid-3492B288-6611-4891-954A-44FF10D5A30E), Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen
 (sid-5D601043-5FEE-4681-B123-44CE520423FF), nachzu-reichende Unterlagen im CMS
vermerken (sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156)
- **Result:** ❌ Failed

- **False Positives:** Bewerbung abschließen (sid-8188439C-7014-4A94-ADE3-80E22199EEBF), Studiengang wählen (sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.197
- **Context Utilization:** 0.372
- **Samples:** 12 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Bewerber-account anlegen (sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561)**: The task "Bewerber-account anlegen" creates a new applicant account, which involves collecting and storing personal data such as the applicant’s name, contact details, and other identifying information.
- **vorhandene 
Qualifikationen eintragen (sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67)**: The task "vorhandene Qualifikationen eintragen" records the applicant’s existing qualifications, which are personal data that must be processed.
- **Antrag ausfüllen
und Unterlagen hinzufügen (sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76)**: The task "Antrag ausfüllen und Unterlagen hinzufügen" involves the applicant filling out an application form and attaching documents that contain personal data.
- **Angaben vervollständigen (sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE)**: The task "Angaben vervollständigen" completes the applicant’s information, which requires processing personal data.
- **Bewerbung abschließen (sid-8188439C-7014-4A94-ADE3-80E22199EEBF)**: The task "Bewerbung abschließen" finalises the application, which involves storing and possibly transmitting the applicant’s personal data.
- **Studiengang wählen (sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7)**: The task "Studiengang wählen" records the applicant’s choice of study programme, which is personal data.
- **Unterlagen vervollständigen (sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411)**: The task "Unterlagen vervollständigen" involves adding missing documents that contain personal data.
- **Qualifikationen (automatisch) prüfen (sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21)**: The task "Qualifikationen (automatisch) prüfen" automatically checks qualifications, which requires processing the applicant’s personal data.
- **Bewerbung auf Vollständigkeit prüfen (automatisch) (sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0)**: The task "Bewerbung auf Vollständigkeit prüfen (automatisch)" automatically verifies completeness of the application, which involves processing personal data.
- **Unterlagen auf Vollständigkeit prüfen und einscannen (sid-3492B288-6611-4891-954A-44FF10D5A30E)**: The task "Unterlagen auf Vollständigkeit prüfen und einscannen" scans documents that contain personal data, thereby processing that data.
- **Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen
 (sid-5D601043-5FEE-4681-B123-44CE520423FF)**: The task "Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen" creates a confirmation that references missing documents, which involves handling personal data.
- **nachzu-reichende Unterlagen im CMS
vermerken (sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156)**: The task "nachzu-reichende Unterlagen im CMS vermerken" records missing documents in the CMS, which involves processing personal data.

</details>

## Test Case 63 - Warehouse Optimization
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/63/preview?correctIds=sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4,sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E,sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB,sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6,sid-EB25994B-55A9-40B3-958B-DB47E77C8728&falsePositiveIds=sid-3A9B35BE-E0F1-4DA5-9758-EBF4D0A98D01,sid-9117EA73-8362-49FB-8CA6-83FE46F25522,sid-F2E68AC0-4245-44D7-9BFF-47015E384362&falseNegativeIds=&salt=11796.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 5
- Amount of False Positives: 3
- Amount of False Negatives: 0

- **Expected:** send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4), capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E), 
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB), forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6), make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728)
- **Actual:** capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E), forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6), make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728), send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4), commision ware (sid-3A9B35BE-E0F1-4DA5-9758-EBF4D0A98D01), send in stock (sid-9117EA73-8362-49FB-8CA6-83FE46F25522), send in stock (sid-F2E68AC0-4245-44D7-9BFF-47015E384362), 
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB)
- **Result:** ❌ Failed

- **False Positives:** commision ware (sid-3A9B35BE-E0F1-4DA5-9758-EBF4D0A98D01), send in stock (sid-9117EA73-8362-49FB-8CA6-83FE46F25522), send in stock (sid-F2E68AC0-4245-44D7-9BFF-47015E384362)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.197
- **Context Utilization:** 0.216
- **Samples:** 8 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **capture order (sid-1D1EF4B0-0651-4909-9B4D-0C8C61F4758E)**: The activity captures order details which include customer personal data such as name, address, contact information, and payment details. This is a direct collection and processing of personal data.
- **forward order (sid-5100EBA3-E14F-44FE-AF0A-3C8304FD35B6)**: The activity forwards the order to the next step, which involves transmitting personal data of the customer to another participant. This is a transmission of personal data.
- **make offer and sell (sid-EB25994B-55A9-40B3-958B-DB47E77C8728)**: The activity creates an offer and completes a sale, which involves processing personal data of the customer, such as contact details and payment information. This is a direct processing of personal data.
- **send to customer (sid-B605E9E8-4D14-48B5-AE6C-B62AE7B335E4)**: The activity sends information to the customer, which requires using the customer's personal data (contact details). This is a disclosure of personal data.
- **commision ware (sid-3A9B35BE-E0F1-4DA5-9758-EBF4D0A98D01)**: The activity commissions ware for shipment to the customer, which requires the customer's address and contact details. This is a transmission of personal data.
- **send in stock (sid-9117EA73-8362-49FB-8CA6-83FE46F25522)**: The activity sends the stock to the customer, which involves personal data such as shipping address. This is a transmission of personal data.
- **send in stock (sid-F2E68AC0-4245-44D7-9BFF-47015E384362)**: The activity sends the stock to the customer, which involves personal data such as shipping address. This is a transmission of personal data.
- **
analyze the customers (sid-70874A34-B162-4AB5-8AC0-DE8059C26FDB)**: The activity analyzes customer data, which includes personal data such as purchase history, demographics, and contact information. This is a processing of personal data.

</details>

## Test Case 103 - Abschlussarbeit erfassen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/103/preview?correctIds=sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7,sid-E2A8380F-3287-4747-A608-9D64E6EA96F4,sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD,sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242,sid-E71C0317-6761-4D46-9963-98D20F6353AE,sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765,sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399,sid-6B454017-EB2A-4C27-A17D-E4CE3379315D,sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245,sid-16659179-643C-4968-9543-E807A8BD5E47,sid-5A34C81B-720B-43E5-8883-F5491379778F,sid-E087B843-EE5B-4CF7-ADD5-122171A9235C,sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A,sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0,sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D,sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72,sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A,sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8,sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC,sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A,sid-8F5478CC-BC17-4E18-AB76-F537290F687E,sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9,sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7,sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140,sid-7113D8F7-41CA-4A42-9B3F-6815107291EA,sid-832872EC-BDFE-4453-97FE-8235E8398554&falsePositiveIds=&falseNegativeIds=&salt=15939.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 26
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Prüfer 1
bestätigen (sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7), Prüfer 1  schriftlich bestätigen (sid-E2A8380F-3287-4747-A608-9D64E6EA96F4), Prüfer 2 schriftlich bestätigen  (sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD), Prüfer 2 bestätigen (sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242), Bestätigung erfassen (sid-E71C0317-6761-4D46-9963-98D20F6353AE), Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden (sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765), Abgabedatum ändern und Bestätigung verschicken (sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399), Antrag auf Frist-
verlängerung überprüfen (sid-6B454017-EB2A-4C27-A17D-E4CE3379315D), NB verbucht (sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245), Abgabedatum in CMS eingeben (sid-16659179-643C-4968-9543-E807A8BD5E47), Abschlussarbeit an Prüfer weitergeben (sid-5A34C81B-720B-43E5-8883-F5491379778F), bei externem Prüfer Note eintragen  (sid-E087B843-EE5B-4CF7-ADD5-122171A9235C), Gutachten erstellen und schriftlich weiterreichen (sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0), Gutachten erstellen und schriftlich weiterreichen (sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72), Gutachten erstellen,  in CMS hochladen (sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A), Bonierung bestätigen (sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8), bei externem Prüfer Note eintragen (sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC), ob Noten mehr als xx auseinander-
liegen prüfen (sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A), Endnote der Abschluss-arbeit eintragen (sid-8F5478CC-BC17-4E18-AB76-F537290F687E), Studierenden  über WH-Modalitäten informieren (sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9), für Abschluss-
arbeit anmelden (sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7), Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen (sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140), Antrag beim PA-Vor-sitzenden stellen (sid-7113D8F7-41CA-4A42-9B3F-6815107291EA), Bestätigung erfassen (sid-832872EC-BDFE-4453-97FE-8235E8398554)
- **Actual:** Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden (sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765), Abgabedatum ändern und Bestätigung verschicken (sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399), Abschlussarbeit an Prüfer weitergeben (sid-5A34C81B-720B-43E5-8883-F5491379778F), Abgabedatum in CMS eingeben (sid-16659179-643C-4968-9543-E807A8BD5E47), bei externem Prüfer Note eintragen  (sid-E087B843-EE5B-4CF7-ADD5-122171A9235C), bei externem Prüfer Note eintragen (sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC), ob Noten mehr als xx auseinander-
liegen prüfen (sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A), Endnote der Abschluss-arbeit eintragen (sid-8F5478CC-BC17-4E18-AB76-F537290F687E), Bestätigung erfassen (sid-832872EC-BDFE-4453-97FE-8235E8398554), Bestätigung erfassen (sid-E71C0317-6761-4D46-9963-98D20F6353AE), NB verbucht (sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245), Bonierung bestätigen (sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8), Studierenden  über WH-Modalitäten informieren (sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0), Prüfer 1  schriftlich bestätigen (sid-E2A8380F-3287-4747-A608-9D64E6EA96F4), Prüfer 1
bestätigen (sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7), Gutachten erstellen und schriftlich weiterreichen (sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A), Antrag auf Frist-
verlängerung überprüfen (sid-6B454017-EB2A-4C27-A17D-E4CE3379315D), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72), Prüfer 2 bestätigen (sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242), Prüfer 2 schriftlich bestätigen  (sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD), Gutachten erstellen und schriftlich weiterreichen (sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D), Gutachten erstellen,  in CMS hochladen (sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A), für Abschluss-
arbeit anmelden (sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7), Antrag beim PA-Vor-sitzenden stellen (sid-7113D8F7-41CA-4A42-9B3F-6815107291EA), Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen (sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.040
- **Context Utilization:** 0.172
- **Samples:** 26 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden (sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765)**: The activity "Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden" collects registration information and sends a confirmation, thereby handling personal data such as student identifiers and contact details.
- **Abgabedatum ändern und Bestätigung verschicken (sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399)**: The activity "Abgabedatum ändern und Bestätigung verschicken" updates a submission date and sends a confirmation, which involves processing personal data such as the student's identity and submission details.
- **Abschlussarbeit an Prüfer weitergeben (sid-5A34C81B-720B-43E5-8883-F5491379778F)**: The activity "Abschlussarbeit an Prüfer weitergeben" forwards a thesis to an examiner, involving personal data of both the student and the examiner.
- **Abgabedatum in CMS eingeben (sid-16659179-643C-4968-9543-E807A8BD5E47)**: The activity "Abgabedatum in CMS eingeben" records a submission date in the CMS, processing personal data such as the student's identity and submission details.
- **bei externem Prüfer Note eintragen  (sid-E087B843-EE5B-4CF7-ADD5-122171A9235C)**: The activity "bei externem Prüfer Note eintragen" records a grade for a student, directly handling personal data such as the student's identity and academic performance.
- **bei externem Prüfer Note eintragen (sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC)**: The activity "bei externem Prüfer Note eintragen" (duplicate) also records a grade, processing personal data of the student.
- **ob Noten mehr als xx auseinander-
liegen prüfen (sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A)**: The activity "ob Noten mehr als xx auseinander liegen prüfen" checks grade differences, which requires accessing students' grades and personal identifiers.
- **Endnote der Abschluss-arbeit eintragen (sid-8F5478CC-BC17-4E18-AB76-F537290F687E)**: The activity "Endnote der Abschluss-arbeit eintragen" records the final grade for a thesis, directly handling personal data of the student.
- **Bestätigung erfassen (sid-832872EC-BDFE-4453-97FE-8235E8398554)**: The activity "Bestätigung erfassen" captures a confirmation, which involves personal data such as the student's identity and confirmation details.
- **Bestätigung erfassen (sid-E71C0317-6761-4D46-9963-98D20F6353AE)**: The activity "Bestätigung erfassen" (duplicate) also captures confirmation data, processing personal data of the student.
- **NB verbucht (sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245)**: The activity "NB verbucht" records a note, which involves personal data such as the student's grade and identity.
- **Bonierung bestätigen (sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8)**: The activity "Bonierung bestätigen" confirms a bonus for a student, processing personal data such as the student's identity and bonus details.
- **Studierenden  über WH-Modalitäten informieren (sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9)**: The activity "Studierenden über WH-Modalitäten informieren" informs students about modalities, which requires sending information to students using their personal data such as contact details.
- **Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0)**: The activity "Gutachten erstellen, in CMS hochladen und Note eintragen" creates a report, uploads it to the CMS, and records a grade, all of which involve personal data of the student and examiner.
- **Prüfer 1  schriftlich bestätigen (sid-E2A8380F-3287-4747-A608-9D64E6EA96F4)**: The activity "Prüfer 1 schriftlich bestätigen" confirms the examiner in writing, processing personal data such as the examiner's identity and confirmation details.
- **Prüfer 1
bestätigen (sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7)**: The activity "Prüfer 1 bestätigen" confirms the examiner, processing personal data such as the examiner's identity and confirmation details.
- **Gutachten erstellen und schriftlich weiterreichen (sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A)**: The activity "Gutachten erstellen und schriftlich weiterreichen" creates a report and forwards it, handling personal data of the student and examiner.
- **Antrag auf Frist-
verlängerung überprüfen (sid-6B454017-EB2A-4C27-A17D-E4CE3379315D)**: The activity "Antrag auf Fristverlängerung überprüfen" reviews an extension request, processing personal data such as the student's identity and request details.
- **Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72)**: The activity "Gutachten erstellen, in CMS hochladen und Note eintragen" (duplicate) creates a report, uploads it, and records a grade, all involving personal data of the student and examiner.
- **Prüfer 2 bestätigen (sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242)**: The activity "Prüfer 2 bestätigen" confirms the second examiner, processing personal data such as the examiner's identity and confirmation details.
- **Prüfer 2 schriftlich bestätigen  (sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD)**: The activity "Prüfer 2 schriftlich bestätigen" confirms the second examiner in writing, processing personal data such as the examiner's identity and confirmation details.
- **Gutachten erstellen und schriftlich weiterreichen (sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D)**: The activity "Gutachten erstellen und schriftlich weiterreichen" (duplicate) creates a report and forwards it, handling personal data of the student and examiner.
- **Gutachten erstellen,  in CMS hochladen (sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A)**: The activity "Gutachten erstellen, in CMS hochladen" creates a report and uploads it, processing personal data of the student and examiner.
- **für Abschluss-
arbeit anmelden (sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7)**: The activity "für Abschluss-arbeit anmelden" registers a thesis, collecting and storing personal data such as the student's identity and thesis details.
- **Antrag beim PA-Vor-sitzenden stellen (sid-7113D8F7-41CA-4A42-9B3F-6815107291EA)**: The activity "Antrag beim PA-Vorsitzenden stellen" submits a request to the chair, processing personal data such as the student's identity and request details.
- **Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen (sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140)**: The activity "Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen" submits a thesis and uploads it, processing personal data of the student and examiner.

</details>

## Test Case 129 - Maschinenstörung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/129/preview?correctIds=sid-46D4BFF2-48E5-459D-ACAA-C9ACCD42573B,sid-5E2A40A5-3435-4BEE-AEC5-D8CAF2A6EED5,sid-110D2497-3601-4119-A52C-F8473C270F6C,sid-AC55AE26-39A6-4C2B-8905-D08B345E0B28,sid-A4ADCACD-D8F9-48ED-A5A1-9999DE32288F,sid-14D44907-DCA4-4782-9A34-4A2F37F556FC,sid-C53BF8F1-90D8-4DC5-B780-93366AAC4C6E,sid-70BF76B1-C8CE-46FB-AEFD-031ABAB6C51B&falsePositiveIds=sid-0B974FB2-8D0F-4B56-B92F-9851968C8870,sid-C55FBA30-D3F9-4ED4-997E-C96545BAC686&falseNegativeIds=&salt=270.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 8
- Amount of False Positives: 2
- Amount of False Negatives: 0

- **Expected:** Supportcall absetzen (sid-46D4BFF2-48E5-459D-ACAA-C9ACCD42573B), Störung mit Hilfe von Support beheben (sid-5E2A40A5-3435-4BEE-AEC5-D8CAF2A6EED5), Monteur anfordern (sid-110D2497-3601-4119-A52C-F8473C270F6C), Supportcall absetzen (sid-AC55AE26-39A6-4C2B-8905-D08B345E0B28), Monteur anfordern (sid-A4ADCACD-D8F9-48ED-A5A1-9999DE32288F), Störung mit Hilfe von Support beheben (sid-14D44907-DCA4-4782-9A34-4A2F37F556FC), Abschluss und Dokumentation im Maschinenlogbuch (sid-C53BF8F1-90D8-4DC5-B780-93366AAC4C6E), Techniker anfordern (sid-70BF76B1-C8CE-46FB-AEFD-031ABAB6C51B)
- **Actual:** Supportcall absetzen (sid-46D4BFF2-48E5-459D-ACAA-C9ACCD42573B), Störung mit Hilfe von Support beheben (sid-5E2A40A5-3435-4BEE-AEC5-D8CAF2A6EED5), Monteur anfordern (sid-110D2497-3601-4119-A52C-F8473C270F6C), Supportcall absetzen (sid-AC55AE26-39A6-4C2B-8905-D08B345E0B28), Monteur anfordern (sid-A4ADCACD-D8F9-48ED-A5A1-9999DE32288F), Störung mit Hilfe von Support beheben (sid-14D44907-DCA4-4782-9A34-4A2F37F556FC), Abschluss und Dokumentation im Maschinenlogbuch (sid-C53BF8F1-90D8-4DC5-B780-93366AAC4C6E), Techniker anfordern (sid-70BF76B1-C8CE-46FB-AEFD-031ABAB6C51B), Störung mit Assistent beheben (sid-0B974FB2-8D0F-4B56-B92F-9851968C8870), Störung mit Hilfe beheben (sid-C55FBA30-D3F9-4ED4-997E-C96545BAC686)
- **Result:** ❌ Failed

- **False Positives:** Störung mit Assistent beheben (sid-0B974FB2-8D0F-4B56-B92F-9851968C8870), Störung mit Hilfe beheben (sid-C55FBA30-D3F9-4ED4-997E-C96545BAC686)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.250
- **Context Utilization:** 0.598
- **Samples:** 10 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Supportcall absetzen (sid-46D4BFF2-48E5-459D-ACAA-C9ACCD42573B)**: The activity "Supportcall absetzen" involves making a support call to a customer, which requires handling the customer's contact details and possibly other personal information, thereby processing personal data.
- **Störung mit Hilfe von Support beheben (sid-5E2A40A5-3435-4BEE-AEC5-D8CAF2A6EED5)**: The activity "Störung mit Hilfe von Support beheben" involves coordinating with support to resolve an issue, which requires accessing the customer's personal data, thereby processing personal data.
- **Monteur anfordern (sid-110D2497-3601-4119-A52C-F8473C270F6C)**: The activity "Monteur anfordern" involves requesting a technician, which typically includes transmitting the customer's personal data to the technician, thereby processing personal data.
- **Supportcall absetzen (sid-AC55AE26-39A6-4C2B-8905-D08B345E0B28)**: The activity "Supportcall absetzen" involves making a support call to a customer, which requires handling the customer's contact details and possibly other personal information, thereby processing personal data.
- **Monteur anfordern (sid-A4ADCACD-D8F9-48ED-A5A1-9999DE32288F)**: The activity "Monteur anfordern" involves requesting a technician, which typically includes transmitting the customer's personal data to the technician, thereby processing personal data.
- **Störung mit Hilfe von Support beheben (sid-14D44907-DCA4-4782-9A34-4A2F37F556FC)**: The activity "Störung mit Hilfe von Support beheben" involves coordinating with support to resolve an issue, which requires accessing the customer's personal data, thereby processing personal data.
- **Abschluss und Dokumentation im Maschinenlogbuch (sid-C53BF8F1-90D8-4DC5-B780-93366AAC4C6E)**: The activity "Abschluss und Dokumentation im Maschinenlogbuch" involves recording the resolution details in the machine logbook, which may include personal data of the operator or customer, thereby processing personal data.
- **Techniker anfordern (sid-70BF76B1-C8CE-46FB-AEFD-031ABAB6C51B)**: The activity "Techniker anfordern" involves requesting a technician, which typically includes transmitting the customer's personal data to the technician, thereby processing personal data.
- **Störung mit Assistent beheben (sid-0B974FB2-8D0F-4B56-B92F-9851968C8870)**: The activity "Störung mit Assistent beheben" involves using an assistant to resolve an issue, which may involve personal data of the operator or customer, thereby processing personal data.
- **Störung mit Hilfe beheben (sid-C55FBA30-D3F9-4ED4-997E-C96545BAC686)**: The activity "Störung mit Hilfe beheben" involves coordinating with support to resolve an issue, which requires accessing the customer's personal data, thereby processing personal data.

</details>

## Test Case 97 - Anlegen der Lehrendendaten
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/97/preview?correctIds=sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D,sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE,sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B,sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3,sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7,sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0,sid-B17C2630-6153-4618-80B6-0247C52B79BF,sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80,sid-BC28D36C-E060-4E77-9073-54C345E0F808,sid-F9972CAB-1E32-49E4-B81D-70771BB88947,sid-C4B71B84-B173-44A2-B378-6A9B45B08D74,sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48,sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED,sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF,sid-EE199D2B-132E-4696-987B-5FFEBDD4E064,sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD,sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1&falsePositiveIds=&falseNegativeIds=&salt=84623.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 17
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Antrag auf Datenpflege
stellen (sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D), ob die
Daten der
Lehrperson schon angelegt
sind prüfen (sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE), Lehrenden- 
daten temporär im CMS anlegen (sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B), ob Abeitsvertrag vorliegt prüfen (sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3), Einstellungs-
antrag erstellen (sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7), legt Grundgerüst für Personal an (sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0), externe
Personendaten
pflegen (sid-B17C2630-6153-4618-80B6-0247C52B79BF), legt Personal mit allen notwendigen Daten an (sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80), Person die Rolle Lehrender zuweisen (sid-BC28D36C-E060-4E77-9073-54C345E0F808), Arbeitsvertrag erstellen (sid-F9972CAB-1E32-49E4-B81D-70771BB88947), Arbeitsvertrag  in die Personalakte legen (sid-C4B71B84-B173-44A2-B378-6A9B45B08D74), Person im SVA deaktivieren (sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48), Person auf ungültig setzten (sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED), Person im CMS 
deaktivieren (sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF), Lehrenden- 
daten im CMS abgleichen (sid-EE199D2B-132E-4696-987B-5FFEBDD4E064),  (IDM) Zugangsdaten
 für Lehrenden erzeugen (sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD), Arbeitsvertrag unterschreiben (sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1)
- **Actual:** ob die
Daten der
Lehrperson schon angelegt
sind prüfen (sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE), ob Abeitsvertrag vorliegt prüfen (sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3), legt Grundgerüst für Personal an (sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0), externe
Personendaten
pflegen (sid-B17C2630-6153-4618-80B6-0247C52B79BF), legt Personal mit allen notwendigen Daten an (sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80), Person die Rolle Lehrender zuweisen (sid-BC28D36C-E060-4E77-9073-54C345E0F808), Person auf ungültig setzten (sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED), Arbeitsvertrag  in die Personalakte legen (sid-C4B71B84-B173-44A2-B378-6A9B45B08D74), Person im SVA deaktivieren (sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48), Arbeitsvertrag erstellen (sid-F9972CAB-1E32-49E4-B81D-70771BB88947),  (IDM) Zugangsdaten
 für Lehrenden erzeugen (sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD), Antrag auf Datenpflege
stellen (sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D), Lehrenden- 
daten im CMS abgleichen (sid-EE199D2B-132E-4696-987B-5FFEBDD4E064), Lehrenden- 
daten temporär im CMS anlegen (sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B), Person im CMS 
deaktivieren (sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF), Einstellungs-
antrag erstellen (sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7), Arbeitsvertrag unterschreiben (sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.137
- **Context Utilization:** 0.205
- **Samples:** 17 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **ob die
Daten der
Lehrperson schon angelegt
sind prüfen (sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE)**: The activity checks whether the teacher’s personal data has already been created, which involves accessing and evaluating personal information.
- **ob Abeitsvertrag vorliegt prüfen (sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3)**: The activity verifies the existence of an employment contract, a document that contains personal data of the employee.
- **legt Grundgerüst für Personal an (sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0)**: The activity creates the basic personnel structure, which includes storing personal data of staff members.
- **externe
Personendaten
pflegen (sid-B17C2630-6153-4618-80B6-0247C52B79BF)**: The activity maintains external personnel data, directly handling personal information of individuals.
- **legt Personal mit allen notwendigen Daten an (sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80)**: The activity creates a personnel record with all necessary data, which is personal data.
- **Person die Rolle Lehrender zuweisen (sid-BC28D36C-E060-4E77-9073-54C345E0F808)**: The activity assigns the teacher role to a person, using personal data to determine eligibility.
- **Person auf ungültig setzten (sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED)**: The activity marks a person as invalid, which changes the status of their personal data record.
- **Arbeitsvertrag  in die Personalakte legen (sid-C4B71B84-B173-44A2-B378-6A9B45B08D74)**: The activity stores the employment contract in the personnel file, handling personal data.
- **Person im SVA deaktivieren (sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48)**: The activity deactivates a person in the SVA system, which involves updating personal data records.
- **Arbeitsvertrag erstellen (sid-F9972CAB-1E32-49E4-B81D-70771BB88947)**: The activity creates an employment contract, a document that contains personal data of the employee.
- ** (IDM) Zugangsdaten
 für Lehrenden erzeugen (sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD)**: The activity generates login credentials for a teacher, which includes personal data such as identifiers and passwords.
- **Antrag auf Datenpflege
stellen (sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D)**: The activity submits a request for data maintenance, which involves the personal data of the requester.
- **Lehrenden- 
daten im CMS abgleichen (sid-EE199D2B-132E-4696-987B-5FFEBDD4E064)**: The activity synchronises teacher data in the CMS, directly handling personal data.
- **Lehrenden- 
daten temporär im CMS anlegen (sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B)**: The activity temporarily creates teacher data in the CMS, which involves personal data.
- **Person im CMS 
deaktivieren (sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF)**: The activity deactivates a person in the CMS, updating their personal data status.
- **Einstellungs-
antrag erstellen (sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7)**: The activity creates a hiring request, which includes personal data of the applicant.
- **Arbeitsvertrag unterschreiben (sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1)**: The activity signs an employment contract, a document containing personal data of the employee.

</details>

# Run 3

## Summary (GPT-OSS-20B)

## Summary
Total: 19
Passed: 7
Failed: 8
Error: 4
Total Retries: 0

### Metrics
Accuracy: 0.836
Precision: 0.851
Recall: 0.950
F1-Score: 0.898

### Confusion Matrix
True Positives: 114
False Positives: 20
False Negatives: 6
True Negatives: 19

### RAG Metrics (Ragas) — averaged across 14 test case(s)
Faithfulness: 0.158
Context Utilization: 0.303
Total Samples Scored: 134 (failed: 0)

## Model: GPT-OSS-20B

## Test Case 70 - Wartungsplanung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/70/preview?correctIds=&falsePositiveIds=&falseNegativeIds=&salt=56861.0" alt="Test Case BPMN XML" />

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

## Test Case 19 - Introduce patient to head physician
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/19/preview?correctIds=sid-1177E778-9B60-4A48-895E-82DAAC180C9E,sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B,sid-4FAB8B03-8311-42F4-91B2-BDC72772535E&falsePositiveIds=&falseNegativeIds=&salt=7617.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 3
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Introduce patient with all results to head physician (sid-1177E778-9B60-4A48-895E-82DAAC180C9E), Order surgery to be performed (sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B), Examine patient (sid-4FAB8B03-8311-42F4-91B2-BDC72772535E)
- **Actual:** Introduce patient with all results to head physician (sid-1177E778-9B60-4A48-895E-82DAAC180C9E), Examine patient (sid-4FAB8B03-8311-42F4-91B2-BDC72772535E), Order surgery to be performed (sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.611
- **Context Utilization:** 0.488
- **Samples:** 3 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Introduce patient with all results to head physician (sid-1177E778-9B60-4A48-895E-82DAAC180C9E)**: The activity "Introduce patient with all results to head physician" involves presenting patient medical results, which are personal health data.
- **Examine patient (sid-4FAB8B03-8311-42F4-91B2-BDC72772535E)**: The activity "Examine patient" is a medical examination that processes the patient's health information and personal data.
- **Order surgery to be performed (sid-915B12CE-53F4-4984-AB0F-1B301C9FBC3B)**: The activity "Order surgery to be performed" involves ordering a surgical procedure for a patient, which requires processing the patient's personal health data and related information.

</details>

## Test Case 29 - Arzttermin buchen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/29/preview?correctIds=Activity_0gi1msm,Activity_1agrvaf,Activity_1molpic&falsePositiveIds=Activity_0khomk4&falseNegativeIds=&salt=41411.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 3
- Amount of False Positives: 1
- Amount of False Negatives: 0

- **Expected:** Patientendaten aufnehmen (Activity_0gi1msm), Termin bestätigen (Activity_1agrvaf), Erinnerung senden (Activity_1molpic)
- **Actual:** Terminoptionen prüfen (Activity_0khomk4), Patientendaten aufnehmen (Activity_0gi1msm), Termin bestätigen (Activity_1agrvaf), Erinnerung senden (Activity_1molpic)
- **Result:** ❌ Failed

- **False Positives:** Terminoptionen prüfen (Activity_0khomk4)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.083
- **Context Utilization:** 0.319
- **Samples:** 4 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Terminoptionen prüfen (Activity_0khomk4)**: The activity "Terminoptionen prüfen" involves checking appointment options for a patient, which requires accessing the patient’s personal data such as name, contact details or patient ID to determine availability and schedule. This constitutes processing of personal data.
- **Patientendaten aufnehmen (Activity_0gi1msm)**: The activity "Patientendaten aufnehmen" directly collects patient data, which includes personal information such as name, address, medical history, and contact details. This is a clear processing of personal data.
- **Termin bestätigen (Activity_1agrvaf)**: The activity "Termin bestätigen" confirms an appointment, which requires using the patient’s personal data (contact information, appointment details) to send confirmation and update records. This is processing of personal data.
- **Erinnerung senden (Activity_1molpic)**: The activity "Erinnerung senden" sends a reminder to the patient, which involves using personal data such as the patient’s contact details and appointment information. This is processing of personal data.

</details>

## Test Case 59 - Neuen Mitarbeiter erfassen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/59/preview?correctIds=sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551,sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4,x7f-f3cda2a0d4d7330d9353884ec580e5e4,vv8-f3cda2a0d4d7330d9353884ec580e5e4,yys-f3cda2a0d4d7330d9353884ec580e5e4,sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4&falsePositiveIds=&falseNegativeIds=sid-7hno-95f654cf6849a76aaf94b5a98ebc5551&salt=24270.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 6
- Amount of False Positives: 0
- Amount of False Negatives: 1

- **Expected:** QIS-Nutzer anlegen (sid-7hno-95f654cf6849a76aaf94b5a98ebc5551), QIS-Rolle zuordnen (sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551), Stammsätze anlegen (sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4), Chipkarte zuordnen (x7f-f3cda2a0d4d7330d9353884ec580e5e4), Arbeitszeitmodell zuordnen (vv8-f3cda2a0d4d7330d9353884ec580e5e4), Arbeitszeit eintragen (yys-f3cda2a0d4d7330d9353884ec580e5e4), Abweichende Sollarbeitszeit eintragen (sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4)
- **Actual:** Arbeitszeitmodell zuordnen (vv8-f3cda2a0d4d7330d9353884ec580e5e4), Chipkarte zuordnen (x7f-f3cda2a0d4d7330d9353884ec580e5e4), Arbeitszeit eintragen (yys-f3cda2a0d4d7330d9353884ec580e5e4), Abweichende Sollarbeitszeit eintragen (sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4), Stammsätze anlegen (sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4), QIS-Rolle zuordnen (sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551)
- **Result:** ❌ Failed

- **False Positives:** None
- **False Negatives:** QIS-Nutzer anlegen (sid-7hno-95f654cf6849a76aaf94b5a98ebc5551)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.083
- **Context Utilization:** 0.311
- **Samples:** 6 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Arbeitszeitmodell zuordnen (vv8-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Arbeitszeitmodell zuordnen" assigns a working time model to an employee, which involves handling the employee’s personal data such as their identity and employment details.
- **Chipkarte zuordnen (x7f-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Chipkarte zuordnen" assigns a chip card to an employee, processing personal identifiers and card information linked to the employee.
- **Arbeitszeit eintragen (yys-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Arbeitszeit eintragen" records the employee’s working hours, directly handling personal time‑keeping data tied to the employee.
- **Abweichende Sollarbeitszeit eintragen (sid-1o83-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Abweichende Sollarbeitszeit eintragen" records deviations in standard working time for an employee, processing personal data about the employee’s work schedule.
- **Stammsätze anlegen (sid-2vka-f3cda2a0d4d7330d9353884ec580e5e4)**: The activity "Stammsätze anlegen" creates master records for employees, storing personal data such as employee identifiers and employment details.
- **QIS-Rolle zuordnen (sid-4wcu-95f654cf6849a76aaf94b5a98ebc5551)**: The activity "QIS-Rolle zuordnen" assigns a role to a user in the QIS system, processing personal data about the user’s identity and role assignment.

</details>

## Test Case 96 - Durchführung Vergabe
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/96/preview?correctIds=sid-769A281E-121A-4CE5-BCA5-133855F4EBC5,sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725,sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F,sid-513CC109-2A72-4E35-A35F-48407221396C&falsePositiveIds=&falseNegativeIds=&salt=60096.0" alt="Test Case BPMN XML" />

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
- **Context Utilization:** 0.388
- **Samples:** 4 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Zulassungs-kriterien automatisiert prüfen (sid-769A281E-121A-4CE5-BCA5-133855F4EBC5)**: The task "Zulassungs-kriterien automatisiert prüfen" automatically evaluates admission criteria that include personal attributes such as student status, gender, semester, and other identifying information. This processing of individual data is required to determine eligibility, thereby constituting GDPR‑relevant personal data processing.
- **Ranking  automatisiert durchführen (sid-A5BBCAF2-3C63-4BA2-B270-8FAF462DD725)**: The task "Ranking automatisiert durchführen" performs an automated ranking of applicants based on criteria that involve personal data such as roles, study programmes, semesters, and other identifying attributes. The ranking decision directly influences the admission status of individuals, making it a GDPR‑relevant processing activity.
- **Wartelistenplatz(SPZ)/Absage zuweisen/erteilen (sid-513CC109-2A72-4E35-A35F-48407221396C)**: The task "Wartelistenplatz(SPZ)/Absage zuweisen/erteilen" assigns waiting‑list spots or rejections to specific applicants. This requires the use of personal identifiers and other personal data to determine the outcome for each individual, thereby constituting GDPR‑relevant processing.
- **Zulassung (SPZ)/Zulassung zuweisen/erteilen (sid-49DD8FE6-3091-48F4-AC68-E22EFA18216F)**: The task "Zulassung (SPZ)/Zulassung zuweisen/erteilen" grants admission to individuals. The decision is based on personal data such as applicant identity, academic records, and other identifying information, making it a GDPR‑relevant processing activity.

</details>

## Test Case 57 - Dienstreiseabrechnung genehmigen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/57/preview?correctIds=xtre-ed2f4ad43858602ad6e00efd76b0f56e,xttr-ed2f4ad43858602ad6e00efd76b0f56e,xu5h-ed2f4ad43858602ad6e00efd76b0f56e,xu3y-ed2f4ad43858602ad6e00efd76b0f56e,xtts-ed2f4ad43858602ad6e00efd76b0f56e,xtrf-ed2f4ad43858602ad6e00efd76b0f56e,xu2f-ed2f4ad43858602ad6e00efd76b0f56e&falsePositiveIds=&falseNegativeIds=&salt=4887.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 7
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Dienstreiseabrechnung sichten (xtre-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung genehmigen (xttr-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung ablehnen (xu5h-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an andere Sachbearbeitung weiterleiten (xu3y-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnungsdaten anpassen (xtts-ed2f4ad43858602ad6e00efd76b0f56e), Kommentar eingeben (xtrf-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an Antragsteller zurücksenden (xu2f-ed2f4ad43858602ad6e00efd76b0f56e)
- **Actual:** Dienstreiseabrechnung sichten (xtre-ed2f4ad43858602ad6e00efd76b0f56e), Kommentar eingeben (xtrf-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung genehmigen (xttr-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnungsdaten anpassen (xtts-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an Antragsteller zurücksenden (xu2f-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung an andere Sachbearbeitung weiterleiten (xu3y-ed2f4ad43858602ad6e00efd76b0f56e), Abrechnung ablehnen (xu5h-ed2f4ad43858602ad6e00efd76b0f56e)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.200
- **Context Utilization:** 0.378
- **Samples:** 7 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Dienstreiseabrechnung sichten (xtre-ed2f4ad43858602ad6e00efd76b0f56e)**: The task 'Dienstreiseabrechnung sichten' involves reviewing a travel expense claim, which contains personal data such as employee identity, travel details, and expense amounts. Reviewing this information constitutes processing personal data.
- **Kommentar eingeben (xtrf-ed2f4ad43858602ad6e00efd76b0f56e)**: The task 'Kommentar eingeben' requires the employee to enter a comment regarding the travel expense claim. The comment is linked to the claim and therefore involves personal data such as employee identity and claim details.
- **Abrechnung genehmigen (xttr-ed2f4ad43858602ad6e00efd76b0f56e)**: The task 'Abrechnung genehmigen' is the approval of a travel expense claim. Approval requires access to and evaluation of personal data such as employee identity, travel details, and expense amounts.
- **Abrechnungsdaten anpassen (xtts-ed2f4ad43858602ad6e00efd76b0f56e)**: The task 'Abrechnungsdaten anpassen' involves modifying the data of a travel expense claim, which contains personal data such as employee identity and travel details. Adjusting these records is a form of processing personal data.
- **Abrechnung an Antragsteller zurücksenden (xu2f-ed2f4ad43858602ad6e00efd76b0f56e)**: The task 'Abrechnung an Antragsteller zurücksenden' sends the travel expense claim back to the applicant. Sending the claim involves transmitting personal data such as the applicant’s identity and claim details.
- **Abrechnung an andere Sachbearbeitung weiterleiten (xu3y-ed2f4ad43858602ad6e00efd76b0f56e)**: The task 'Abrechnung an andere Sachbearbeitung weiterleiten' forwards the travel expense claim to another department. Forwarding the claim requires handling personal data such as employee identity and claim details.
- **Abrechnung ablehnen (xu5h-ed2f4ad43858602ad6e00efd76b0f56e)**: The task 'Abrechnung ablehnen' rejects a travel expense claim. Rejecting the claim involves reviewing personal data such as employee identity and claim details, thereby processing personal data.

</details>

## Test Case 40 - Online Bestellung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/40/preview?correctIds=Activity_1jmvap9&falsePositiveIds=Activity_1biw5cc,Activity_0w3m6fk,Activity_1ehspue&falseNegativeIds=&salt=81357.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 1
- Amount of False Positives: 3
- Amount of False Negatives: 0

- **Expected:** Bestellung abschicken (Activity_1jmvap9)
- **Actual:** Lieferadresse eingeben (Activity_1biw5cc), Zahlungsdaten angeben (Activity_0w3m6fk), Bestellung abschicken (Activity_1jmvap9), Bestellbestätigung anzeigen (Activity_1ehspue)
- **Result:** ❌ Failed

- **False Positives:** Lieferadresse eingeben (Activity_1biw5cc), Zahlungsdaten angeben (Activity_0w3m6fk), Bestellbestätigung anzeigen (Activity_1ehspue)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.325
- **Context Utilization:** 0.267
- **Samples:** 4 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Lieferadresse eingeben (Activity_1biw5cc)**: The activity collects and processes personal data such as delivery address, name, and contact details.
- **Zahlungsdaten angeben (Activity_0w3m6fk)**: The activity processes personal data such as payment information, including credit card or bank details.
- **Bestellung abschicken (Activity_1jmvap9)**: The activity submits an order that includes personal data such as shipping address, payment details, and order information.
- **Bestellbestätigung anzeigen (Activity_1ehspue)**: The activity displays an order confirmation that contains personal data such as order details and customer information.

</details>

## Test Case 98 - Pflege der Lehrendendaten
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/98/preview?correctIds=sid-6AF85299-5179-4780-B917-67C3678C0062,sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B,sid-CCFEBADD-0645-4F1F-936A-F2094AC68729,sid-9C71399D-C2E4-45E3-8256-FBD48257840A,sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A,sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110,sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF,sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F&falsePositiveIds=&falseNegativeIds=&salt=55646.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 8
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Lehrendendaten
überprüfen (sid-6AF85299-5179-4780-B917-67C3678C0062), Lehrenden deaktivieren (sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B), Lehrenden aktivieren (sid-CCFEBADD-0645-4F1F-936A-F2094AC68729), Lehrenden entfernen (sid-9C71399D-C2E4-45E3-8256-FBD48257840A), Lehrenden entfernen (sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A), Lehrenden-
daten aktualisieren (sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110), Lehrenden-
daten aktualisieren (sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF), Art der Pflege
überprüfen (sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F)
- **Actual:** Lehrenden-
daten aktualisieren (sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF), Lehrenden entfernen (sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A), Lehrendendaten
überprüfen (sid-6AF85299-5179-4780-B917-67C3678C0062), Art der Pflege
überprüfen (sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F), Lehrenden deaktivieren (sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B), Lehrenden entfernen (sid-9C71399D-C2E4-45E3-8256-FBD48257840A), Lehrenden-
daten aktualisieren (sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110), Lehrenden aktivieren (sid-CCFEBADD-0645-4F1F-936A-F2094AC68729)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.125
- **Context Utilization:** 0.541
- **Samples:** 8 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Lehrenden-
daten aktualisieren (sid-8246F1A1-3540-48CD-AA7D-E982C554A4DF)**: The task "Lehrenden-daten aktualisieren" involves updating teacher records, which contain personal information such as names, contact details, employment status and other identifying data. This activity therefore processes personal data.
- **Lehrenden entfernen (sid-420F0086-521C-4E5F-9E93-8B863D4E3A1A)**: The task "Lehrenden entfernen" removes a teacher from the system, which requires accessing and handling personal data such as identity, employment history and contact information. This activity processes personal data.
- **Lehrendendaten
überprüfen (sid-6AF85299-5179-4780-B917-67C3678C0062)**: The task "Lehrendendaten überprüfen" checks teacher data for accuracy, which involves reading and possibly modifying personal information such as names, addresses, and employment details. This activity processes personal data.
- **Art der Pflege
überprüfen (sid-78B152B0-C7FA-423E-B79A-A0A7CBDCDD7F)**: The task "Art der Pflege überprüfen" verifies the type of care required, which is personal data about an individual’s health or care needs. This activity processes personal data.
- **Lehrenden deaktivieren (sid-AE1F535B-A3A3-4FD9-9FE2-68051514180B)**: The task "Lehrenden deaktivieren" deactivates a teacher’s account, which requires accessing personal data such as login credentials, identity, and employment status. This activity processes personal data.
- **Lehrenden entfernen (sid-9C71399D-C2E4-45E3-8256-FBD48257840A)**: The task "Lehrenden entfernen" (second instance) removes a teacher from the system, involving personal data such as identity, contact details, and employment records. This activity processes personal data.
- **Lehrenden-
daten aktualisieren (sid-9EB4EFF7-5015-4189-A1ED-E9E662CA7110)**: The task "Lehrenden-daten aktualisieren" updates teacher information, which includes personal data such as names, addresses, and employment details. This activity processes personal data.
- **Lehrenden aktivieren (sid-CCFEBADD-0645-4F1F-936A-F2094AC68729)**: The task "Lehrenden aktivieren" activates a teacher’s account, which requires handling personal data such as identity, contact information, and employment status. This activity processes personal data.

</details>

## Test Case 55 - Dienstreiseantrag stellen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/55/preview?correctIds=abo9-b0399020c4b54c4fa3a4761cf43b01e1,abpu-b0399020c4b54c4fa3a4761cf43b01e1,sid-1v10-b0399020c4b54c4fa3a4761cf43b01e1,sid-1v3m-b0399020c4b54c4fa3a4761cf43b01e1&falsePositiveIds=sid-1tzo-b0399020c4b54c4fa3a4761cf43b01e1,sid-1tzp-b0399020c4b54c4fa3a4761cf43b01e1,sid-1u3t-b0399020c4b54c4fa3a4761cf43b01e1,sid-1u4n-b0399020c4b54c4fa3a4761cf43b01e1,sid-1u8s-b0399020c4b54c4fa3a4761cf43b01e1,sid-1um4-b0399020c4b54c4fa3a4761cf43b01e1,sid-1up1-b0399020c4b54c4fa3a4761cf43b01e1,sid-1uxn-b0399020c4b54c4fa3a4761cf43b01e1,i46u-b0399020c4b54c4fa3a4761cf43b01e1,sid-1egam-b0399020c4b54c4fa3a4761cf43b01e1,lhyw-ed2f4ad43858602ad6e00efd76b0f56e,sm4r-ed2f4ad43858602ad6e00efd76b0f56e&falseNegativeIds=&salt=83384.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 4
- Amount of False Positives: 12
- Amount of False Negatives: 0

- **Expected:** Bankverbindung einsehen (abo9-b0399020c4b54c4fa3a4761cf43b01e1), Änderungen zur Bankverbindung eingeben (abpu-b0399020c4b54c4fa3a4761cf43b01e1), Dokumente hochladen (sid-1v10-b0399020c4b54c4fa3a4761cf43b01e1), Antrag beenden und weiterleiten (sid-1v3m-b0399020c4b54c4fa3a4761cf43b01e1)
- **Actual:** Genehmigten Dienstreiseantrag suchen (sid-1tzo-b0399020c4b54c4fa3a4761cf43b01e1), Reisedaten anpassen (sid-1tzp-b0399020c4b54c4fa3a4761cf43b01e1), Ausländischen Reiseverlauf anpassen (sid-1u3t-b0399020c4b54c4fa3a4761cf43b01e1), Kürzungen angeben (sid-1u4n-b0399020c4b54c4fa3a4761cf43b01e1), Fahrkosten anpassen (sid-1u8s-b0399020c4b54c4fa3a4761cf43b01e1), Finanzierungsdaten eingeben (sid-1um4-b0399020c4b54c4fa3a4761cf43b01e1), Daten zu Zahlungspartnern eingeben (sid-1up1-b0399020c4b54c4fa3a4761cf43b01e1), Notizen eingeben (sid-1uxn-b0399020c4b54c4fa3a4761cf43b01e1), Dokumente hochladen (sid-1v10-b0399020c4b54c4fa3a4761cf43b01e1), Antrag beenden und weiterleiten (sid-1v3m-b0399020c4b54c4fa3a4761cf43b01e1), Bankverbindung einsehen (abo9-b0399020c4b54c4fa3a4761cf43b01e1), Änderungen zur Bankverbindung eingeben (abpu-b0399020c4b54c4fa3a4761cf43b01e1), Reiseverlauf anpassen (i46u-b0399020c4b54c4fa3a4761cf43b01e1), Kürzungen angeben (sid-1egam-b0399020c4b54c4fa3a4761cf43b01e1), Detailsansicht anzeigen (lhyw-ed2f4ad43858602ad6e00efd76b0f56e), PDF ausgeben (sm4r-ed2f4ad43858602ad6e00efd76b0f56e)
- **Result:** ❌ Failed

- **False Positives:** Genehmigten Dienstreiseantrag suchen (sid-1tzo-b0399020c4b54c4fa3a4761cf43b01e1), Reisedaten anpassen (sid-1tzp-b0399020c4b54c4fa3a4761cf43b01e1), Ausländischen Reiseverlauf anpassen (sid-1u3t-b0399020c4b54c4fa3a4761cf43b01e1), Kürzungen angeben (sid-1u4n-b0399020c4b54c4fa3a4761cf43b01e1), Fahrkosten anpassen (sid-1u8s-b0399020c4b54c4fa3a4761cf43b01e1), Finanzierungsdaten eingeben (sid-1um4-b0399020c4b54c4fa3a4761cf43b01e1), Daten zu Zahlungspartnern eingeben (sid-1up1-b0399020c4b54c4fa3a4761cf43b01e1), Notizen eingeben (sid-1uxn-b0399020c4b54c4fa3a4761cf43b01e1), Reiseverlauf anpassen (i46u-b0399020c4b54c4fa3a4761cf43b01e1), Kürzungen angeben (sid-1egam-b0399020c4b54c4fa3a4761cf43b01e1), Detailsansicht anzeigen (lhyw-ed2f4ad43858602ad6e00efd76b0f56e), PDF ausgeben (sm4r-ed2f4ad43858602ad6e00efd76b0f56e)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.031
- **Context Utilization:** 0.112
- **Samples:** 16 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Genehmigten Dienstreiseantrag suchen (sid-1tzo-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Genehmigten Dienstreiseantrag suchen" involves searching for an approved business travel request, which requires accessing records that contain employee personal data such as name, ID, contact details and travel dates.
- **Reisedaten anpassen (sid-1tzp-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Reisedaten anpassen" requires retrieving and modifying travel data that is linked to the employee, thereby processing personal data such as dates, destinations and contact information.
- **Ausländischen Reiseverlauf anpassen (sid-1u3t-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Ausländischen Reiseverlauf anpassen" involves adjusting foreign travel itineraries that are tied to the employee, thus processing personal data such as travel dates, destinations and employee identifiers.
- **Kürzungen angeben (sid-1u4n-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Kürzungen angeben" consults the travel request record to indicate cost reductions, which contains personal data of the employee, thereby processing that data.
- **Fahrkosten anpassen (sid-1u8s-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Fahrkosten anpassen" requires accessing the employee’s travel cost information, which is personal data, and updating it.
- **Finanzierungsdaten eingeben (sid-1um4-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Finanzierungsdaten eingeben" involves entering financing information such as bank account numbers and payment details, which are personal financial data.
- **Daten zu Zahlungspartnern eingeben (sid-1up1-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Daten zu Zahlungspartnern eingeben" processes payment partner data that includes personal data such as bank account or payment details of the employee.
- **Notizen eingeben (sid-1uxn-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Notizen eingeben" allows the user to add notes that may contain personal data about the employee or the travel request, thereby processing that data.
- **Dokumente hochladen (sid-1v10-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Dokumente hochladen" involves uploading documents that are likely to contain personal data such as travel authorisations, passports or other identification documents.
- **Antrag beenden und weiterleiten (sid-1v3m-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Antrag beenden und weiterleiten" finalises the travel request and forwards it to another party, which involves transmitting personal data of the employee and travel details.
- **Bankverbindung einsehen (abo9-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Bankverbindung einsehen" displays the employee’s bank account information, which is personal financial data.
- **Änderungen zur Bankverbindung eingeben (abpu-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Änderungen zur Bankverbindung eingeben" processes changes to the employee’s bank details, thereby handling personal financial data.
- **Reiseverlauf anpassen (i46u-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Reiseverlauf anpassen" involves adjusting the travel itinerary, which is linked to the employee’s personal data such as dates, destinations and identifiers.
- **Kürzungen angeben (sid-1egam-b0399020c4b54c4fa3a4761cf43b01e1)**: The activity "Kürzungen angeben" (second occurrence) consults the travel request record to indicate cost reductions, processing personal data of the employee.
- **Detailsansicht anzeigen (lhyw-ed2f4ad43858602ad6e00efd76b0f56e)**: The activity "Detailsansicht anzeigen" displays detailed information about the travel request, which includes personal data of the employee, thus processing that data.
- **PDF ausgeben (sm4r-ed2f4ad43858602ad6e00efd76b0f56e)**: The activity "PDF ausgeben" generates a PDF that likely contains personal data such as employee details and travel information, thereby processing that data.

</details>

## Test Case 3 - Preparing surgery
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/3/preview?correctIds=sid-D0309BAA-2BC7-4866-BD2B-36789C42198D,sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745,sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B,sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA,sid-A76861E0-B426-4259-997B-A72EEB63E3A1,sid-87E2518C-79DE-403D-8A33-010389B828A6,sid-5DA96990-76B4-40D0-88B6-915685E7E279,sid-8018CC06-69E9-4E62-977B-E441C039AEF0,sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF&falsePositiveIds=sid-E05999ED-FBEC-4936-994E-EC0F5808EF69,sid-C17C448D-8303-4345-BD8A-C773D4A6A63B&falseNegativeIds=sid-6EFE25FB-0893-440F-A48A-876386148A36,sid-71AF5554-8150-4459-9577-961A5D078CB2,sid-8048EF97-74FF-4BD4-B97A-FB3D04E435E2&salt=87422.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 9
- Amount of False Positives: 2
- Amount of False Negatives: 3

- **Expected:** Collect and evaluate all results (sid-D0309BAA-2BC7-4866-BD2B-36789C42198D), Check results (sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745), Perform pre-operation discussion  (sid-6EFE25FB-0893-440F-A48A-876386148A36), Enter patient into surgery board (sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B), Enter surgery into Oplus for surgery plan (sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA), Ordain premedication (sid-A76861E0-B426-4259-997B-A72EEB63E3A1), Introduce patient to head physician  (sid-71AF5554-8150-4459-9577-961A5D078CB2), Cancel surgery (sid-8048EF97-74FF-4BD4-B97A-FB3D04E435E2), Give medicaments (sid-87E2518C-79DE-403D-8A33-010389B828A6), Check electrolytes (sid-5DA96990-76B4-40D0-88B6-915685E7E279), Perform colonoskopy (sid-8018CC06-69E9-4E62-977B-E441C039AEF0), Order preparations of surgery in kardex (sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF)
- **Actual:** Patient fasts (sid-E05999ED-FBEC-4936-994E-EC0F5808EF69), Give medicaments (sid-87E2518C-79DE-403D-8A33-010389B828A6), Perform colonoskopy (sid-8018CC06-69E9-4E62-977B-E441C039AEF0), Check electrolytes (sid-5DA96990-76B4-40D0-88B6-915685E7E279), Enter patient into surgery board (sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B), Collect and evaluate all results (sid-D0309BAA-2BC7-4866-BD2B-36789C42198D), Enter surgery into Oplus for surgery plan (sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA), Order preparations of surgery in kardex (sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF), Meet patient (sid-C17C448D-8303-4345-BD8A-C773D4A6A63B), Check results (sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745), Ordain premedication (sid-A76861E0-B426-4259-997B-A72EEB63E3A1)
- **Result:** ❌ Failed

- **False Positives:** Patient fasts (sid-E05999ED-FBEC-4936-994E-EC0F5808EF69), Meet patient (sid-C17C448D-8303-4345-BD8A-C773D4A6A63B)
- **False Negatives:** Perform pre-operation discussion  (sid-6EFE25FB-0893-440F-A48A-876386148A36), Introduce patient to head physician  (sid-71AF5554-8150-4459-9577-961A5D078CB2), Cancel surgery (sid-8048EF97-74FF-4BD4-B97A-FB3D04E435E2)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.356
- **Context Utilization:** 0.406
- **Samples:** 11 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Patient fasts (sid-E05999ED-FBEC-4936-994E-EC0F5808EF69)**: The activity involves a patient undergoing a medical procedure, which requires recording and handling of the patient's health data, thus processing personal data.
- **Give medicaments (sid-87E2518C-79DE-403D-8A33-010389B828A6)**: The activity involves administering medication to a patient, requiring knowledge of the patient's medical condition and medication history, thus processing personal data.
- **Perform colonoskopy (sid-8018CC06-69E9-4E62-977B-E441C039AEF0)**: The activity involves performing a colonoscopy on a patient, which requires handling the patient's health data, thus processing personal data.
- **Check electrolytes (sid-5DA96990-76B4-40D0-88B6-915685E7E279)**: The activity involves checking electrolytes for a patient, which requires handling the patient's lab results, thus processing personal data.
- **Enter patient into surgery board (sid-97A5C3AA-D6BB-4803-A0AC-D63762A3A69B)**: The activity involves entering the patient into the surgery board, which records the patient's details, thus processing personal data.
- **Collect and evaluate all results (sid-D0309BAA-2BC7-4866-BD2B-36789C42198D)**: The activity involves collecting and evaluating all results for a patient, which requires handling the patient's lab results, thus processing personal data.
- **Enter surgery into Oplus for surgery plan (sid-857018B9-BAD3-4C01-903B-0A1CFA8E00EA)**: The activity involves entering the surgery into Oplus for the surgery plan, which records the patient's details, thus processing personal data.
- **Order preparations of surgery in kardex (sid-E0ED1A9A-8DA8-4613-8907-8B910EC27DDF)**: The activity involves ordering preparations of surgery in the kardex, which records patient details, thus processing personal data.
- **Meet patient (sid-C17C448D-8303-4345-BD8A-C773D4A6A63B)**: The activity involves meeting a patient, which requires handling the patient's personal information, thus processing personal data.
- **Check results (sid-EC03143D-3700-4B0F-A8C9-4C0A360D3745)**: The activity involves checking results for a patient, which requires handling the patient's lab results, thus processing personal data.
- **Ordain premedication (sid-A76861E0-B426-4259-997B-A72EEB63E3A1)**: The activity involves ordaining premedication for a patient, which requires handling the patient's medical information, thus processing personal data.

</details>

## Test Case 135 - Immatrikulation
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/135/preview?correctIds=sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277,sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB,sid-0728004E-21CC-4D5E-9D26-6FB3182102F5,sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D,sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A,sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF,sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A,sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A&falsePositiveIds=&falseNegativeIds=&salt=24094.0" alt="Test Case BPMN XML" />

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
- **Faithfulness:** 0.000
- **Context Utilization:** 0.101
- **Samples:** 8 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Bewerbung bearbeiten (sid-5CCCA870-162C-45A8-88E5-FAF663A44DAB)**: Bewerbung bearbeiten processes personal data because it handles student applications, which contain personal information such as names, contact details, and academic records.
- **Studienplatz
vergeben (sid-0728004E-21CC-4D5E-9D26-6FB3182102F5)**: Studienplatz vergeben processes personal data because it assigns study places based on applicant information, involving evaluation of personal data such as academic records and contact details.
- **Bewerber immatrikulieren (sid-A8E4767C-7AB6-4B9B-8F89-71970FB52A3A)**: Bewerber immatrikulieren processes personal data because it enrolls applicants, creating student records that contain personal data such as names, addresses, and academic history.
- **traditionelles Nachrück-verfahren (sid-5767BBF4-4398-4E10-8A08-3BAD565EE85D)**: traditionelles Nachrück-verfahren processes personal data because it manages a traditional replacement procedure, which requires handling personal data of applicants such as names and contact information.
- **Bewerbung durchführen (sid-5BA5FD19-0416-425C-AF53-1EDD7A3BE277)**: Bewerbung durchführen processes personal data because it involves applicants submitting applications, which include personal data such as names, contact details, and academic information.
- **Koordinier-ungsphase 1 mit Entscheidungsphase (sid-24B68543-3A87-4465-B34C-2E97A2B1AD1A)**: Koordinier-ungsphase 1 mit Entscheidungsphase processes personal data because it coordinates decisions about admissions, using personal data of applicants such as academic records and contact information.
- **Koordinierungsphase 2 (sid-65665F98-3FDB-4B73-8D2D-A30385D23F6A)**: Koordinierungsphase 2 processes personal data because it continues coordination of admission decisions, handling personal data such as applicant details and evaluation results.
- **Clearingphase (sid-248EC068-F0EE-4B10-A7CE-91A2553EE2DF)**: Clearingphase processes personal data because it clears data related to admissions, which involves handling personal data such as applicant records and admission decisions.

</details>

## Test Case 97 - Anlegen der Lehrendendaten
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/97/preview?correctIds=sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D,sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE,sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B,sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3,sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7,sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0,sid-B17C2630-6153-4618-80B6-0247C52B79BF,sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80,sid-BC28D36C-E060-4E77-9073-54C345E0F808,sid-F9972CAB-1E32-49E4-B81D-70771BB88947,sid-C4B71B84-B173-44A2-B378-6A9B45B08D74,sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48,sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED,sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF,sid-EE199D2B-132E-4696-987B-5FFEBDD4E064,sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD,sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1&falsePositiveIds=&falseNegativeIds=&salt=39690.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 17
- Amount of False Positives: 0
- Amount of False Negatives: 0

- **Expected:** Antrag auf Datenpflege
stellen (sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D), ob die
Daten der
Lehrperson schon angelegt
sind prüfen (sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE), Lehrenden- 
daten temporär im CMS anlegen (sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B), ob Abeitsvertrag vorliegt prüfen (sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3), Einstellungs-
antrag erstellen (sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7), legt Grundgerüst für Personal an (sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0), externe
Personendaten
pflegen (sid-B17C2630-6153-4618-80B6-0247C52B79BF), legt Personal mit allen notwendigen Daten an (sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80), Person die Rolle Lehrender zuweisen (sid-BC28D36C-E060-4E77-9073-54C345E0F808), Arbeitsvertrag erstellen (sid-F9972CAB-1E32-49E4-B81D-70771BB88947), Arbeitsvertrag  in die Personalakte legen (sid-C4B71B84-B173-44A2-B378-6A9B45B08D74), Person im SVA deaktivieren (sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48), Person auf ungültig setzten (sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED), Person im CMS 
deaktivieren (sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF), Lehrenden- 
daten im CMS abgleichen (sid-EE199D2B-132E-4696-987B-5FFEBDD4E064),  (IDM) Zugangsdaten
 für Lehrenden erzeugen (sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD), Arbeitsvertrag unterschreiben (sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1)
- **Actual:** ob die
Daten der
Lehrperson schon angelegt
sind prüfen (sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE), ob Abeitsvertrag vorliegt prüfen (sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3), legt Grundgerüst für Personal an (sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0), externe
Personendaten
pflegen (sid-B17C2630-6153-4618-80B6-0247C52B79BF), legt Personal mit allen notwendigen Daten an (sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80), Person die Rolle Lehrender zuweisen (sid-BC28D36C-E060-4E77-9073-54C345E0F808), Person auf ungültig setzten (sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED), Arbeitsvertrag  in die Personalakte legen (sid-C4B71B84-B173-44A2-B378-6A9B45B08D74), Person im SVA deaktivieren (sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48), Arbeitsvertrag erstellen (sid-F9972CAB-1E32-49E4-B81D-70771BB88947),  (IDM) Zugangsdaten
 für Lehrenden erzeugen (sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD), Antrag auf Datenpflege
stellen (sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D), Lehrenden- 
daten im CMS abgleichen (sid-EE199D2B-132E-4696-987B-5FFEBDD4E064), Lehrenden- 
daten temporär im CMS anlegen (sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B), Person im CMS 
deaktivieren (sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF), Einstellungs-
antrag erstellen (sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7), Arbeitsvertrag unterschreiben (sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1)
- **Result:** ✅ Passed

- **False Positives:** None
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.103
- **Context Utilization:** 0.280
- **Samples:** 17 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **ob die
Daten der
Lehrperson schon angelegt
sind prüfen (sid-CD793E5C-FFB9-4BF5-BD1B-3ADF428335DE)**: The task "ob die Daten der Lehrperson schon angelegt sind prüfen" checks whether personal data of a teacher has already been created, which involves reading personal data records.
- **ob Abeitsvertrag vorliegt prüfen (sid-6AA5AC11-6A0A-4921-8B96-D4D9FCD948F3)**: The task "ob Arbeitsvertrag vorliegt prüfen" verifies the existence of an employment contract, which contains personal data of the employee.
- **legt Grundgerüst für Personal an (sid-B7E55198-A614-492F-9A4B-2D9246EFA6A0)**: The task "legt Grundgerüst für Personal an" creates a basic personnel record, thereby collecting and storing personal data.
- **externe
Personendaten
pflegen (sid-B17C2630-6153-4618-80B6-0247C52B79BF)**: The task "externe Personendaten pflegen" updates external personal data, which is a direct manipulation of personal data.
- **legt Personal mit allen notwendigen Daten an (sid-9B154C3B-8259-498C-B4E3-0C10ECCC2A80)**: The task "legt Personal mit allen notwendigen Daten an" creates a full personnel record with all required personal data.
- **Person die Rolle Lehrender zuweisen (sid-BC28D36C-E060-4E77-9073-54C345E0F808)**: The task "Person die Rolle Lehrender zuweisen" assigns a teaching role to a person, which requires accessing and updating personal data.
- **Person auf ungültig setzten (sid-AC8CDA05-96A0-4AD6-AFD3-79B6C09299ED)**: The task "Person auf ungültig setzten" marks a person record as invalid, which involves modifying personal data.
- **Arbeitsvertrag  in die Personalakte legen (sid-C4B71B84-B173-44A2-B378-6A9B45B08D74)**: The task "Arbeitsvertrag in die Personalakte legen" stores an employment contract in the personnel file, storing personal data.
- **Person im SVA deaktivieren (sid-A635B7A4-DFC8-4F84-A1B5-8608DA0D8A48)**: The task "Person im SVA deaktivieren" deactivates a person in the system, which requires accessing personal data.
- **Arbeitsvertrag erstellen (sid-F9972CAB-1E32-49E4-B81D-70771BB88947)**: The task "Arbeitsvertrag erstellen" creates an employment contract containing personal data.
- ** (IDM) Zugangsdaten
 für Lehrenden erzeugen (sid-A7FF9AC4-06AC-4254-B8AE-D2F16CE633CD)**: The task " (IDM) Zugangsdaten für Lehrenden erzeugen" generates login credentials for a teacher, which involves handling personal data such as identifiers and authentication information.
- **Antrag auf Datenpflege
stellen (sid-9B34A11E-6848-4A84-8B0D-CA879A74FA9D)**: The task "Antrag auf Datenpflege stellen" submits a request to maintain personal data, which is a processing activity.
- **Lehrenden- 
daten im CMS abgleichen (sid-EE199D2B-132E-4696-987B-5FFEBDD4E064)**: The task "Lehrenden-daten im CMS abgleichen" compares teacher data in the CMS, which reads and potentially updates personal data.
- **Lehrenden- 
daten temporär im CMS anlegen (sid-D8A10025-FFCC-4B03-B9A3-80F5D9A09B3B)**: The task "Lehrenden-daten temporär im CMS anlegen" creates temporary teacher records, processing personal data.
- **Person im CMS 
deaktivieren (sid-9A8FB332-959F-44B2-9642-6ACE8A61A7DF)**: The task "Person im CMS deaktivieren" deactivates a person in the CMS, which involves modifying personal data.
- **Einstellungs-
antrag erstellen (sid-37DF5D30-DB0A-46C1-8DD4-B057BC3C79A7)**: The task "Einstellungs-antrag erstellen" creates a hiring application that contains personal data of the applicant.
- **Arbeitsvertrag unterschreiben (sid-BC3C94FB-BCF0-4F3B-A8BB-1BCF319983D1)**: The task "Arbeitsvertrag unterschreiben" records the signing of an employment contract, which involves processing personal data.

</details>

## Test Case 132 - Rückmeldung
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/132/preview?correctIds=sid-716BD875-CB9C-4173-91AE-20E2BEFB723E,sid-9E177368-C98D-401A-8179-38BEB5DF6390,sid-2A902299-023A-44C5-9A76-16F47701996C,sid-9A7FCC57-F21E-4924-9968-224952C660DD,sid-EF63A163-83B2-46A7-85E9-0089AC7D0786,sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66,sid-F8D21C49-24E6-479B-84DF-98B5D63593C8,sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F,sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB&falsePositiveIds=&falseNegativeIds=sid-82F7C1B0-B225-469A-8C21-15B72F0A53CF&salt=82026.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 9
- Amount of False Positives: 0
- Amount of False Negatives: 1

- **Expected:** Gebühren ermitteln (gebsql) (sid-716BD875-CB9C-4173-91AE-20E2BEFB723E), Informations-E-Mail erstellen und an Student senden
(Fristen und Zahlungs-
details) (sid-9E177368-C98D-401A-8179-38BEB5DF6390), Beiträge überweisen (sid-2A902299-023A-44C5-9A76-16F47701996C), 4.1 Gebührenmanagement (sid-82F7C1B0-B225-469A-8C21-15B72F0A53CF), Datensätze in das CMS einspielen (sid-9A7FCC57-F21E-4924-9968-224952C660DD), ob 
Ist = Soll
prüfen (sid-EF63A163-83B2-46A7-85E9-0089AC7D0786), Mahnschreiben erstellen und an Student senden (sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66), auf Rückmelde-sperre prüfen und Mängel beheben (sid-F8D21C49-24E6-479B-84DF-98B5D63593C8), Studierenden
informieren (sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F), Studenten exmatrikulieren (sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB)
- **Actual:** Gebühren ermitteln (gebsql) (sid-716BD875-CB9C-4173-91AE-20E2BEFB723E), Datensätze in das CMS einspielen (sid-9A7FCC57-F21E-4924-9968-224952C660DD), ob 
Ist = Soll
prüfen (sid-EF63A163-83B2-46A7-85E9-0089AC7D0786), auf Rückmelde-sperre prüfen und Mängel beheben (sid-F8D21C49-24E6-479B-84DF-98B5D63593C8), Studierenden
informieren (sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F), Mahnschreiben erstellen und an Student senden (sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66), Informations-E-Mail erstellen und an Student senden
(Fristen und Zahlungs-
details) (sid-9E177368-C98D-401A-8179-38BEB5DF6390), Studenten exmatrikulieren (sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB), Beiträge überweisen (sid-2A902299-023A-44C5-9A76-16F47701996C)
- **Result:** ❌ Failed

- **False Positives:** None
- **False Negatives:** 4.1 Gebührenmanagement (sid-82F7C1B0-B225-469A-8C21-15B72F0A53CF)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.000
- **Context Utilization:** 0.107
- **Samples:** 9 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Gebühren ermitteln (gebsql) (sid-716BD875-CB9C-4173-91AE-20E2BEFB723E)**: The task "Gebühren ermitteln (gebsql)" determines fee amounts based on student records, which requires access to personal data such as student identifiers and financial information.
- **Datensätze in das CMS einspielen (sid-9A7FCC57-F21E-4924-9968-224952C660DD)**: The task "Datensätze in das CMS einspielen" uploads student records into the CMS, directly handling personal data such as names, addresses and academic details.
- **ob 
Ist = Soll
prüfen (sid-EF63A163-83B2-46A7-85E9-0089AC7D0786)**: The task "ob Ist = Soll prüfen" checks whether the recorded fee matches the expected amount, which requires reading personal data from student accounts.
- **auf Rückmelde-sperre prüfen und Mängel beheben (sid-F8D21C49-24E6-479B-84DF-98B5D63593C8)**: The task "auf Rückmelde-sperre prüfen und Mängel beheben" evaluates whether a student’s account is blocked and fixes any issues, which involves accessing personal data such as student identifiers and account status.
- **Studierenden
informieren (sid-BCA06EE4-E4F2-4D50-A83A-ED000E85FA9F)**: The task "Studierenden informieren" sends notifications to students, which requires the use of personal data such as names, email addresses and student IDs.
- **Mahnschreiben erstellen und an Student senden (sid-6005A74C-7AEC-4C2B-825C-6D4936FDFC66)**: The task "Mahnschreiben erstellen und an Student senden" creates a reminder letter and sends it to a student, directly processing personal data such as contact details and outstanding amounts.
- **Informations-E-Mail erstellen und an Student senden
(Fristen und Zahlungs-
details) (sid-9E177368-C98D-401A-8179-38BEB5DF6390)**: The task "Informations-E-Mail erstellen und an Student senden (Fristen und Zahlungsdetails)" composes and sends an email containing personal data such as student identifiers, deadlines and payment details.
- **Studenten exmatrikulieren (sid-542A5436-6B2F-45C7-8B2F-9D37D2EE4BDB)**: The task "Studenten exmatrikulieren" removes a student from the registry, which requires handling personal data such as names, student IDs and academic records.
- **Beiträge überweisen (sid-2A902299-023A-44C5-9A76-16F47701996C)**: The task "Beiträge überweisen" transfers contributions, which involves processing payment information and personal identifiers of the student.

</details>

## Test Case 142 - Datenübermittlung Bewerber
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/142/preview?correctIds=sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561,sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67,sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21,sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE,sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0,sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76,sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156,sid-3492B288-6611-4891-954A-44FF10D5A30E,sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411,sid-5D601043-5FEE-4681-B123-44CE520423FF&falsePositiveIds=sid-8188439C-7014-4A94-ADE3-80E22199EEBF,sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7&falseNegativeIds=&salt=59925.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 10
- Amount of False Positives: 2
- Amount of False Negatives: 0

- **Expected:** Bewerber-account anlegen (sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561), vorhandene 
Qualifikationen eintragen (sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67), Qualifikationen (automatisch) prüfen (sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21), Angaben vervollständigen (sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE), Bewerbung auf Vollständigkeit prüfen (automatisch) (sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0), Antrag ausfüllen
und Unterlagen hinzufügen (sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76), nachzu-reichende Unterlagen im CMS
vermerken (sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156), Unterlagen auf Vollständigkeit prüfen und einscannen (sid-3492B288-6611-4891-954A-44FF10D5A30E), Unterlagen vervollständigen (sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411), Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen
 (sid-5D601043-5FEE-4681-B123-44CE520423FF)
- **Actual:** Bewerber-account anlegen (sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561), vorhandene 
Qualifikationen eintragen (sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67), Antrag ausfüllen
und Unterlagen hinzufügen (sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76), Angaben vervollständigen (sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE), Bewerbung abschließen (sid-8188439C-7014-4A94-ADE3-80E22199EEBF), Studiengang wählen (sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7), Unterlagen vervollständigen (sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411), Qualifikationen (automatisch) prüfen (sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21), Bewerbung auf Vollständigkeit prüfen (automatisch) (sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0), Unterlagen auf Vollständigkeit prüfen und einscannen (sid-3492B288-6611-4891-954A-44FF10D5A30E), Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen
 (sid-5D601043-5FEE-4681-B123-44CE520423FF), nachzu-reichende Unterlagen im CMS
vermerken (sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156)
- **Result:** ❌ Failed

- **False Positives:** Bewerbung abschließen (sid-8188439C-7014-4A94-ADE3-80E22199EEBF), Studiengang wählen (sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7)
- **False Negatives:** None

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.194
- **Context Utilization:** 0.363
- **Samples:** 12 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Bewerber-account anlegen (sid-0F4B1BFE-73D9-41D0-A434-5826D5D29561)**: The task "Bewerber-account anlegen" creates a new applicant account, which requires storing personal information such as name, contact details and other identifiers. This is a direct processing of personal data.
- **vorhandene 
Qualifikationen eintragen (sid-BF915FB3-1C77-4169-9D4A-2D3BAB5F9D67)**: The task "vorhandene Qualifikationen eintragen" records existing qualifications of the applicant, which are personal data (educational records). This is a processing activity.
- **Antrag ausfüllen
und Unterlagen hinzufügen (sid-C90C4A6F-9029-4943-AC38-1AAA562F4E76)**: The task "Antrag ausfüllen und Unterlagen hinzufügen" involves the applicant filling out an application form and attaching documents that contain personal data such as identity, contact and academic information.
- **Angaben vervollständigen (sid-D43C3E31-7C71-45E2-A1C8-52A18FC22EBE)**: The task "Angaben vervollständigen" completes the applicant’s information, which includes personal data such as name, address and other identifiers.
- **Bewerbung abschließen (sid-8188439C-7014-4A94-ADE3-80E22199EEBF)**: The task "Bewerbung abschließen" finalises the application, which involves handling the applicant’s personal data and confirming its receipt.
- **Studiengang wählen (sid-45A9A5E5-C52B-4F5F-983B-8A24DA9B88D7)**: The task "Studiengang wählen" records the applicant’s choice of study program, which is personal data linked to the applicant.
- **Unterlagen vervollständigen (sid-68BD3816-7D38-49B5-A2B7-01F3BACC4411)**: The task "Unterlagen vervollständigen" involves adding missing documents that contain personal data such as identity proofs and academic records.
- **Qualifikationen (automatisch) prüfen (sid-2877D4FE-E9E9-42B0-85A2-CA7E01FB3B21)**: The task "Qualifikationen (automatisch) prüfen" automatically checks the applicant’s qualifications, which requires reading personal data such as academic records.
- **Bewerbung auf Vollständigkeit prüfen (automatisch) (sid-4030BAF4-4632-4F14-86E2-7DB48C2AEFD0)**: The task "Bewerbung auf Vollständigkeit prüfen (automatisch)" automatically verifies that the application contains all required personal data, thus processing that data.
- **Unterlagen auf Vollständigkeit prüfen und einscannen (sid-3492B288-6611-4891-954A-44FF10D5A30E)**: The task "Unterlagen auf Vollständigkeit prüfen und einscannen" scans documents that contain personal data, thereby processing that data.
- **Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen
 (sid-5D601043-5FEE-4681-B123-44CE520423FF)**: The task "Bestätigung ggf. mit Hinweis auf nachzureichende Unterlagen erstellen" creates a confirmation message that references missing documents, which involves handling personal data.
- **nachzu-reichende Unterlagen im CMS
vermerken (sid-9DA0D02B-3D74-435B-9C8D-FF5E64446156)**: The task "nachzu-reichende Unterlagen im CMS vermerken" records missing documents in the CMS, which involves storing personal data about the applicant.

</details>

## Test Case 103 - Abschlussarbeit erfassen
<img src="https://gripl.mertendieckmann.de/api/dataset/testcase/103/preview?correctIds=sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7,sid-E2A8380F-3287-4747-A608-9D64E6EA96F4,sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD,sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242,sid-E71C0317-6761-4D46-9963-98D20F6353AE,sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765,sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399,sid-6B454017-EB2A-4C27-A17D-E4CE3379315D,sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245,sid-16659179-643C-4968-9543-E807A8BD5E47,sid-5A34C81B-720B-43E5-8883-F5491379778F,sid-E087B843-EE5B-4CF7-ADD5-122171A9235C,sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A,sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0,sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D,sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72,sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A,sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8,sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC,sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A,sid-8F5478CC-BC17-4E18-AB76-F537290F687E,sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9,sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7,sid-7113D8F7-41CA-4A42-9B3F-6815107291EA,sid-832872EC-BDFE-4453-97FE-8235E8398554&falsePositiveIds=&falseNegativeIds=sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140&salt=35992.0" alt="Test Case BPMN XML" />

- Amount of Correct Activities: 25
- Amount of False Positives: 0
- Amount of False Negatives: 1

- **Expected:** Prüfer 1
bestätigen (sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7), Prüfer 1  schriftlich bestätigen (sid-E2A8380F-3287-4747-A608-9D64E6EA96F4), Prüfer 2 schriftlich bestätigen  (sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD), Prüfer 2 bestätigen (sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242), Bestätigung erfassen (sid-E71C0317-6761-4D46-9963-98D20F6353AE), Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden (sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765), Abgabedatum ändern und Bestätigung verschicken (sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399), Antrag auf Frist-
verlängerung überprüfen (sid-6B454017-EB2A-4C27-A17D-E4CE3379315D), NB verbucht (sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245), Abgabedatum in CMS eingeben (sid-16659179-643C-4968-9543-E807A8BD5E47), Abschlussarbeit an Prüfer weitergeben (sid-5A34C81B-720B-43E5-8883-F5491379778F), bei externem Prüfer Note eintragen  (sid-E087B843-EE5B-4CF7-ADD5-122171A9235C), Gutachten erstellen und schriftlich weiterreichen (sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0), Gutachten erstellen und schriftlich weiterreichen (sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72), Gutachten erstellen,  in CMS hochladen (sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A), Bonierung bestätigen (sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8), bei externem Prüfer Note eintragen (sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC), ob Noten mehr als xx auseinander-
liegen prüfen (sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A), Endnote der Abschluss-arbeit eintragen (sid-8F5478CC-BC17-4E18-AB76-F537290F687E), Studierenden  über WH-Modalitäten informieren (sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9), für Abschluss-
arbeit anmelden (sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7), Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen (sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140), Antrag beim PA-Vor-sitzenden stellen (sid-7113D8F7-41CA-4A42-9B3F-6815107291EA), Bestätigung erfassen (sid-832872EC-BDFE-4453-97FE-8235E8398554)
- **Actual:** Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden (sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765), Abgabedatum ändern und Bestätigung verschicken (sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399), Abschlussarbeit an Prüfer weitergeben (sid-5A34C81B-720B-43E5-8883-F5491379778F), Abgabedatum in CMS eingeben (sid-16659179-643C-4968-9543-E807A8BD5E47), bei externem Prüfer Note eintragen  (sid-E087B843-EE5B-4CF7-ADD5-122171A9235C), bei externem Prüfer Note eintragen (sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC), ob Noten mehr als xx auseinander-
liegen prüfen (sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A), Endnote der Abschluss-arbeit eintragen (sid-8F5478CC-BC17-4E18-AB76-F537290F687E), Bestätigung erfassen (sid-832872EC-BDFE-4453-97FE-8235E8398554), Bestätigung erfassen (sid-E71C0317-6761-4D46-9963-98D20F6353AE), NB verbucht (sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245), Bonierung bestätigen (sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8), Studierenden  über WH-Modalitäten informieren (sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0), Prüfer 1  schriftlich bestätigen (sid-E2A8380F-3287-4747-A608-9D64E6EA96F4), Prüfer 1
bestätigen (sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7), Gutachten erstellen und schriftlich weiterreichen (sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A), Antrag auf Frist-
verlängerung überprüfen (sid-6B454017-EB2A-4C27-A17D-E4CE3379315D), Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72), Prüfer 2 bestätigen (sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242), Prüfer 2 schriftlich bestätigen  (sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD), Gutachten erstellen und schriftlich weiterreichen (sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D), Gutachten erstellen,  in CMS hochladen (sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A), für Abschluss-
arbeit anmelden (sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7), Antrag beim PA-Vor-sitzenden stellen (sid-7113D8F7-41CA-4A42-9B3F-6815107291EA)
- **Result:** ❌ Failed

- **False Positives:** None
- **False Negatives:** Abschlussarbeit im Studiensekretariat abgeben und als PDF in CMS hochladen (sid-CA58F9D1-84AB-4224-8D20-3D19EEF94140)

- **Amount of Retries:** 0

### RAG Metrics (Ragas)
- **Faithfulness:** 0.033
- **Context Utilization:** 0.184
- **Samples:** 25 (failed: 0)

<details>
<summary><h3>Reasoning of the LLM</h3></summary>

- **Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden (sid-88C6B7D8-07C6-43E4-873B-12DDC5A8B765)**: The activity "Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden" collects and records student registration details and sends a confirmation, which involves processing personal data such as student identity and submission date.
- **Abgabedatum ändern und Bestätigung verschicken (sid-688E18ED-E16E-4D7A-89AB-4D3CE8F8B399)**: The activity "Abgabedatum ändern und Bestätigung verschicken" updates a submission date and sends a confirmation, thereby modifying and communicating personal data related to a student.
- **Abschlussarbeit an Prüfer weitergeben (sid-5A34C81B-720B-43E5-8883-F5491379778F)**: The activity "Abschlussarbeit an Prüfer weitergeben" forwards a thesis to a reviewer, which involves transmitting personal data of the student and the reviewer.
- **Abgabedatum in CMS eingeben (sid-16659179-643C-4968-9543-E807A8BD5E47)**: The activity "Abgabedatum in CMS eingeben" records a submission date in the CMS, storing personal data of the student.
- **bei externem Prüfer Note eintragen  (sid-E087B843-EE5B-4CF7-ADD5-122171A9235C)**: The activity "bei externem Prüfer Note eintragen" records a grade for an external reviewer, which involves processing personal data of the student and the reviewer.
- **bei externem Prüfer Note eintragen (sid-544CE696-A8AB-4BE4-91B8-AE78B1C6D6BC)**: The activity "bei externem Prüfer Note eintragen" (duplicate) records a grade for an external reviewer, processing personal data of the student and reviewer.
- **ob Noten mehr als xx auseinander-
liegen prüfen (sid-2A03D864-2A83-42D3-BFBA-2650DA95FE2A)**: The activity "ob Noten mehr als xx auseinander liegen prüfen" checks if grades differ beyond a threshold, which requires accessing and analysing personal data of students.
- **Endnote der Abschluss-arbeit eintragen (sid-8F5478CC-BC17-4E18-AB76-F537290F687E)**: The activity "Endnote der Abschluss-arbeit eintragen" records the final grade of a thesis, processing personal data of the student.
- **Bestätigung erfassen (sid-832872EC-BDFE-4453-97FE-8235E8398554)**: The activity "Bestätigung erfassen" records a confirmation, which involves storing personal data of the student or reviewer.
- **Bestätigung erfassen (sid-E71C0317-6761-4D46-9963-98D20F6353AE)**: The activity "Bestätigung erfassen" (duplicate) records a confirmation, processing personal data of the student or reviewer.
- **NB verbucht (sid-07310CC7-05D1-46D0-8A3F-99E01C9D5245)**: The activity "NB verbucht" records a grade entry, which processes personal data of the student.
- **Bonierung bestätigen (sid-12E37560-FFE4-41C6-B24A-2BFBA50A24A8)**: The activity "Bonierung bestätigen" confirms a bonus, which involves processing personal data of the student.
- **Studierenden  über WH-Modalitäten informieren (sid-B63A17CF-BA04-495C-80BB-C9652F98B8F9)**: The activity "Studierenden über WH-Modalitäten informieren" informs students about workshop modalities, which requires sending personal data such as contact details to the students.
- **Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-FB94E060-4192-4639-A4AB-C91FCEA0B3C0)**: The activity "Gutachten erstellen, in CMS hochladen und Note eintragen" creates a report, uploads it to the CMS, and records a grade, all of which process personal data of the student and reviewers.
- **Prüfer 1  schriftlich bestätigen (sid-E2A8380F-3287-4747-A608-9D64E6EA96F4)**: The activity "Prüfer 1 schriftlich bestätigen" records a written confirmation from the first reviewer, processing personal data of the reviewer and the student.
- **Prüfer 1
bestätigen (sid-EDABAD89-25EF-481E-A295-D7EB931E8AF7)**: The activity "Prüfer 1 bestätigen" confirms the first reviewer, processing personal data of the reviewer and the student.
- **Gutachten erstellen und schriftlich weiterreichen (sid-6BF5D87C-55FD-41D1-A9BA-7073B6EBC28A)**: The activity "Gutachten erstellen und schriftlich weiterreichen" creates a report and forwards it, processing personal data of the student and reviewers.
- **Antrag auf Frist-
verlängerung überprüfen (sid-6B454017-EB2A-4C27-A17D-E4CE3379315D)**: The activity "Antrag auf Fristverlängerung überprüfen" reviews a deadline extension request, which involves accessing and evaluating personal data of the student.
- **Gutachten erstellen,  in CMS hochladen und Note eintragen (sid-1185EEDC-E888-4C51-999D-4EE4FFB0FB72)**: The activity "Gutachten erstellen, in CMS hochladen und Note eintragen" (Prüfer2) creates a report, uploads it, and records a grade, processing personal data of the student and reviewers.
- **Prüfer 2 bestätigen (sid-3DCA2BFA-31B4-45E0-8B4A-88045DD99242)**: The activity "Prüfer 2 bestätigen" confirms the second reviewer, processing personal data of the reviewer and the student.
- **Prüfer 2 schriftlich bestätigen  (sid-B4F710FC-6783-4282-A31E-A35CA7EE5CFD)**: The activity "Prüfer 2 schriftlich bestätigen" records a written confirmation from the second reviewer, processing personal data of the reviewer and the student.
- **Gutachten erstellen und schriftlich weiterreichen (sid-8BAB5D12-3602-4F26-8BE0-9D0330B2074D)**: The activity "Gutachten erstellen und schriftlich weiterreichen" (Prüfer2) creates a report and forwards it, processing personal data of the student and reviewers.
- **Gutachten erstellen,  in CMS hochladen (sid-25D6B9EC-1B45-4B3A-AEBD-07FEDB25A36A)**: The activity "Gutachten erstellen, in CMS hochladen" creates a report and uploads it, processing personal data of the student and reviewers.
- **für Abschluss-
arbeit anmelden (sid-F61DFEB0-F9C4-419F-94F0-D75831E449C7)**: The activity "für Abschluss-arbeit anmelden" registers a thesis, collecting and recording student information, which processes personal data.
- **Antrag beim PA-Vor-sitzenden stellen (sid-7113D8F7-41CA-4A42-9B3F-6815107291EA)**: The activity "Antrag beim PA-Vorsitzenden stellen" submits a request to the chair, which involves handling personal data of the student.

</details>