# GRIPL Labeling Guide

**Version:** 1.0  **Stand:** Oktober 2026

Dieser Guide beschreibt, wie BPMN Elemente für die Ground Truth von GRIPL gelabelt werden. Ziel ist, dass verschiedene Labeler zu möglichst gleichen Ergebnissen kommen und jede Entscheidung nachvollziehbar bleibt.

---

## 1. Zweck und Geltungsbereich

GRIPL nutzt die Labels als Ground Truth, um LLMs und Endpunkte miteinander zu vergleichen. Gelabelt wird deshalb immer die **größtmögliche Menge**: alle relevanten Elemente mit allen zutreffenden Klassen. Vereinfachte Sichten wie „binär“ oder „nur Aktivitäten“ leitet die Evaluation automatisch daraus ab.

### Zu labelnde Elementtypen

| Elementtyp | Wird gelabelt? | Regel |
| --- | --- | --- |
| Aktivitäten (Tasks) | ✅ Ja | Wenn personenbezogene Daten verarbeitet werden |
| Events | ✅ Ja | Ausgehende Message Events sind `TRANSFERAL`, eingehende `COLLECTION` (siehe Abgrenzungsregel „Senden oder Empfangen“ in Abschnitt 4) |
| Data Objects | ✅ Ja | Wenn sie personenbezogene Daten enthalten; nur die Reference labeln (siehe Abschnitt 6) |
| Data Stores | ✅ Ja | Wenn sie personenbezogene Daten enthalten; Klasse nach den Regeln in Abschnitt 4 (meist `STORAGE` oder `ACCESS`, je nach Zugriff auch `MODIFICATION` oder `DELETION`); nur die Reference labeln (siehe Abschnitt 6) |
| Gateways | ⚠️ Nur bedingt | Nur wenn die Entscheidung selbst auf personenbezogenen Daten beruht (z. B. „Kreditwürdig?“) |
| Subprozesse | ⚠️ Nur bedingt | Siehe Abschnitt 6 |
| Sequence Flows, Pools, Lanes, Text Annotationen | ❌ Nein | Nie labeln, sonst erscheinen sie in der Evaluation als False Negatives unter „Other“. Lanes und Text Annotationen aber als Kontext nutzen (siehe Abschnitt 6) |

> **Technischer Hinweis:** Bis der Editor angepasst ist, **immer mit dem Multiclass Endpunkt labeln.** Im Binär Modus gehen die Klassen verloren.

---

## 2. Definition: Wann ist ein Element relevant?

Ein Element ist **relevant (DSGVO kritisch)**, wenn es eine Verarbeitung personenbezogener Daten im Sinne der DSGVO darstellt oder direkt darüber entscheidet.

- **Personenbezogene Daten** sind alle Informationen, die sich auf eine identifizierte oder identifizierbare natürliche Person beziehen (Art. 4 Nr. 1 DSGVO), z. B. Name, Adresse, Kundennummer, Gesundheitsdaten.
- **Verarbeitung** ist jeder Vorgang im Zusammenhang mit solchen Daten, etwa Erheben, Speichern, Abfragen, Verwenden, Übermitteln oder Löschen (Art. 4 Nr. 2 DSGVO).
- **Besondere Kategorien** (Art. 9 DSGVO), etwa Gesundheitsdaten oder religiöse Überzeugungen, sind immer relevant, sobald sie verarbeitet werden. Im Feld `reason` vermerken.

### Die sieben Verarbeitungsklassen

Jedes relevante Element bekommt **eine oder mehrere** dieser Klassen. Die Definitionen in der mittleren Spalte stimmen wörtlich mit dem Prompt des Multiclass Endpunkts überein. Die rechte Spalte präzisiert sie für das Labeln.

| Klasse | Definition | Präzisierung fürs Labeln |
| --- | --- | --- |
| `COLLECTION` | Gathering or entering personal data | Personenbezogene Daten kommen neu in den Prozess: Sie werden erfragt, empfangen, gemessen oder durch eine Untersuchung erzeugt. |
| `STORAGE` | Storing or maintaining personal data | Daten werden dauerhaft festgehalten, z. B. in einem System, einer Akte, einem Board oder einem Archiv. |
| `USAGE` | Using, analysing or processing personal data | Daten werden für einen Zweck ausgewertet: geprüft, bewertet, berechnet, als Grundlage für eine Entscheidung oder Planung genutzt. |
| `TRANSFERAL` | Sending or sharing personal data with another party or system | Daten werden aktiv an jemand anderen übergeben: senden, weiterleiten, benachrichtigen, ausdrucken zur Weitergabe. |
| `MODIFICATION` | Updating, correcting or changing personal data | Vorhandene Daten oder ihr Status werden geändert, ergänzt oder korrigiert. |
| `DELETION` | Deleting, anonymising or pseudonymising personal data | Daten werden gelöscht, entfernt, anonymisiert oder pseudonymisiert. |
| `ACCESS` | Retrieving, viewing or making personal data available | Daten werden gesucht, abgerufen, angezeigt oder zum Abruf bereitgestellt, ohne dass die Aktivität sie selbst auswertet. |

Die Klassen orientieren sich an den Verarbeitungsvorgängen nach Art. 4 Nr. 2 DSGVO. Wie mehrere Klassen kombiniert werden, regelt Abschnitt 4.

---

## 3. Entscheidungsregeln

Für jedes Element in dieser Reihenfolge prüfen:

1. **Ist der Elementtyp zugelassen?** (siehe Tabelle in Abschnitt 1)
   Nein → nicht labeln.
2. **Verarbeitet das Element überhaupt personenbezogene Daten oder wird auf ihrer Grundlage entschieden?**
   Nein → nicht labeln.
3. **Kommen dabei neue Daten in den Prozess?** → `COLLECTION`
4. **Werden Daten festgehalten?** Bei einem neuen Datensatz → `STORAGE`, bei einem bestehenden → `MODIFICATION`
5. **Werden Daten ausgewertet, oder dient eine Entscheidung auf Basis der Daten dem Zweck?** → `USAGE`
6. **Werden Daten gesucht oder angezeigt, ohne sie auszuwerten?** → `ACCESS`
7. **Gehen Daten aktiv an jemand anderen?** → `TRANSFERAL`
8. **Werden Daten gelöscht oder anonymisiert?** → `DELETION`
9. **Begründung (`reason`) eintragen.**
   Pflicht. Jede vergebene Klasse mit einem Halbsatz belegen.

**Im Zweifel:** Das Element als **relevant** labeln. Die Entscheidung in jedem Fall im Feld `reason` begründen.

---

## 4. Klassen im Detail

### Beispiele aus dem Datensatz

Legende: ✔ richtig gelabelt · ✘ nicht diese Klasse bzw. nicht relevant · ⚠ bisher falsch gelabelt oder Sonderregel beachten

#### `COLLECTION`

- ✔ Patientendaten aufnehmen, Obtain consent for chemotherapy, Conduct anamnesis and physical examination, Interview applicant, Receive blood analysis results
- ✔ Untersuchungen erzeugen neue Gesundheitsdaten: Perform ultrasound scan, Examine blood sample
- ✘ Take blood sample: Die Probe ist Material, noch keine Information. Erst die Untersuchung der Probe ist `COLLECTION`.
- ⚠ Lieferadresse eingeben, Zahlungsdaten angeben waren bisher nicht gelabelt, sind aber eindeutig `COLLECTION`.

#### `STORAGE`

- ✔ Create patient Kardex, Arbeitsvertrag in die Personalakte legen, Kunde anlegen, Bestellinfos speichern, Arbeitszeit eintragen
- ✔ complete and archive patient record: Archivieren ist `STORAGE`.
- ✘ Wartungsplan speichern, Bericht archivieren: Diese Daten haben keinen Personenbezug.

#### `USAGE`

- ✔ Kreditwürdigkeit prüfen, Check results, 2.9 Ranglistenerstellung für NC-Studiengänge, Arrange further examinations
- ✔ Entscheidungen über eine Person: Decide whether patient should be operated anyway, bisher nicht gelabelt
- ✘ Warenbestand prüfen, Select a Pizza: kein Personenbezug

#### `TRANSFERAL`

- ✔ eRezept an Apotheke schicken, Send admission report to insurance, Send invoice by email, inform relatives and familiy physician
- ✔ Auch interne Übergaben: Abrechnung an andere Sachbearbeitung weiterleiten
- ✔ Ausdrucken zur Weitergabe: Print badges with personal data, Print discharge papers
- ✘ Send patient to surgical suite, Transfer patient back to surgical ward: Hier wird ein Mensch transportiert, keine Daten.

#### `MODIFICATION`

- ✔ Änderungen zur Bankverbindung eingeben, Zeitguthaben ändern, Lehrendendaten aktualisieren, Studiengangswechsel durchführen
- ✔ Reisedaten anpassen, Hotelrechnung anpassen waren bisher nicht gelabelt. Sie betreffen aber die Dienstreise einer bestimmten Person und sind damit `MODIFICATION`.
- ✘ Firmware aktualisieren
- ✘ Standardvertrag anpassen, außer wenn ein Vertrag mit einer bestimmten Person gemeint ist und keine Vorlage

#### `DELETION`

- ✔ Zeiterfassungsdaten löschen, Annahme löschen (nicht zulassungsfreie Studiengänge), Lehrenden entfernen
- ✘ Remove drainages and threads: eine körperliche Handlung
- ⚠ Für Exmatrikulation, Stornierung und Abmeldung gilt die Abgrenzungsregel unten.

#### `ACCESS`

- ✔ Bankverbindung einsehen, QIS-Dienstreiseabrechnung suchen, Genehmigten Dienstreiseantrag suchen, Abfrage aller Klausurteilnehmer im CMS …, read working time
- ✘ Interview applicant: Das ist `COLLECTION`, kein `ACCESS`.
- ✘ Review documents: Das ist `USAGE`.

### Abgrenzungsregeln

**`ACCESS` oder `USAGE`:** Besteht die Aktivität darin, Daten zu holen oder anzusehen, ist es `ACCESS`. Wird mit den Daten etwas bewertet oder entschieden, ist es `USAGE`. Dass man zum Auswerten Daten lesen muss, ist selbstverständlich; dafür kommt kein zusätzliches `ACCESS` dazu.
*Bankverbindung einsehen → `ACCESS` · Check results → `USAGE` · Abrechnungsdaten einsehen und prüfen → `ACCESS` + `USAGE`, weil beides ausdrücklich genannt ist.*

**`COLLECTION` oder `STORAGE`:** Kommt die Information in dieser Aktivität zum ersten Mal in den Prozess, ist es `COLLECTION`. Wird etwas bereits Vorhandenes in ein System oder eine Akte übertragen, ist es `STORAGE`. Geschieht beides in einem Schritt, sind es beide Klassen.
*Patientendaten aufnehmen → `COLLECTION` · QIS-Nutzer anlegen (aus bereits erhobenen Bewerbungsdaten) → `STORAGE` · Enter surgery into Oplus for surgery plan → `STORAGE` (wenn der Termin vorher festgelegt wurde) · Termin erfassen (Termin wird dabei vereinbart) → `COLLECTION` + `STORAGE`*

**`STORAGE` oder `MODIFICATION`:** Ein neuer Datensatz ist `STORAGE`. Ein bestehender Datensatz, der geändert, ergänzt oder vervollständigt wird, ist `MODIFICATION`.
*Create patient Kardex → `STORAGE` · Update Kardex with planned lab examination → `MODIFICATION`*

**`TRANSFERAL` oder `ACCESS`:** Aktiv an einen bestimmten Empfänger übergeben (Push) ist `TRANSFERAL`. Zum Abruf freischalten oder bereitstellen (Pull) ist `ACCESS`.
*Send results to ward → `TRANSFERAL` · Prüfungstermine zur Anmeldung freigeben → `ACCESS`*

**Exmatrikulation, Stornierung, Abmeldung:** Hier ändert sich zunächst nur ein Status. Das ist `MODIFICATION`. `DELETION` nur dann, wenn ausdrücklich gelöscht oder entfernt wird.
*3.3 Exmatrikulation → `MODIFICATION` · Exma bis 30.11./31.05. Annahme löschen → `DELETION` · Stornierung per E-Mail senden → `TRANSFERAL`; die Stornierung selbst ist nur ein Statuswechsel*

**Anonymisierung:** Werden personenbezogene Daten anonymisiert, ist das `DELETION`, auch wenn das Ziel Datenschutz ist. Werden bereits anonyme Daten nur verwendet, ist die Aktivität nicht relevant.
*Anonymisierte Statistik aktualisieren → `DELETION` + `USAGE`, wenn die Statistik dabei aus Patientendaten erzeugt wird; sonst nicht relevant.*

**Senden oder Empfangen:** Gelabelt wird aus der Sicht dessen, der die Aktivität ausführt. Senden ist `TRANSFERAL`, Empfangen ist `COLLECTION`.
*eRezept an Apotheke schicken → `TRANSFERAL` · Receive blood analysis results → `COLLECTION`*

### Mehrere Klassen gleichzeitig

**Grundsatz:** Vergib jede Klasse für einen Verarbeitungsschritt, den die Aktivität ausdrücklich nennt oder der zwingend dazugehört. Wer etwas in ein System einträgt, speichert es zwangsläufig. Klassen für Schritte, die nur vielleicht passieren, vergibst du nicht. Die Evaluation zählt jede Klasse einzeln, eine fehlende Klasse senkt also den Recall.

| Aktivität | Klassen | Begründung |
| --- | --- | --- |
| Collect and evaluate all results | `COLLECTION` + `USAGE` | sammeln + auswerten |
| Conduct anamnesis and physical examination | `COLLECTION` + `USAGE` | erfragen und untersuchen + ärztlich beurteilen |
| Enter dates into ward board and Kardex | `STORAGE` | Termine liegen schon vor und werden nur eingetragen |
| complete and archive patient record | `MODIFICATION` + `STORAGE` | eine vorhandene Akte vervollständigen + archivieren |
| Anmeldung formal erfassen und Bestätigung mit Abgabedatum versenden | `COLLECTION` + `STORAGE` + `TRANSFERAL` | erfassen + festhalten + versenden |
| Abgabedatum ändern und Bestätigung verschicken | `MODIFICATION` + `TRANSFERAL` | ändern + versenden |
| Rangliste aus Zu- und Absagen zum NR aktualisieren | `USAGE` + `MODIFICATION` | Zu- und Absagen auswerten + bestehende Rangliste ändern |
| Verfahren eröffnen und Doktorand über Fristen informieren | `STORAGE` + `TRANSFERAL` | Verfahren anlegen + Person benachrichtigen |

---

## 5. Weitere Beispiele und Grenzfälle

### Weitere Elementtypen (relevant)

| Element | Typ | Klasse(n) | Begründung |
| --- | --- | --- | --- |
| „Kundendatenbank“ | Data Store | `STORAGE` | Speichert personenbezogene Daten |
| „Adresse an Versanddienstleister senden“ | Message Event | `TRANSFERAL` | Weitergabe an einen Dritten |
| „Kreditwürdig?“ | Gateway | `USAGE` | Entscheidung beruht auf personenbezogenen Daten |

### Negativbeispiele (nicht relevant)

| Element | Typ | Begründung |
| --- | --- | --- |
| „Ware packen“ | Task | Keine personenbezogenen Daten |
| „Kommissionieren“ | Task | Keine personenbezogenen Daten |
| „Lagerbestand > 0?“ | Gateway | Entscheidung beruht nicht auf personenbezogenen Daten |

### Grenzfälle

#### Formulare der betroffenen Person

| Element | Typ | Entscheidung | Begründung |
| --- | --- | --- | --- |
| „Formular ausfüllen“ (betroffene Person gibt eigene Daten ein) | Task | Nicht relevant | Die Person verarbeitet nur ihre eigenen Daten, der Verantwortliche hat sie noch nicht |
| „Formular absenden“ (durch die betroffene Person) | Task / Event | Nicht relevant | Die Daten haben den Verantwortlichen noch nicht erreicht |
| „Formular empfangen“ (durch den Verantwortlichen) | Task / Event | Relevant, `COLLECTION` | Ab hier erhebt der Verantwortliche die Daten |

**Grundregel:** Kritisch wird es erst, sobald die Daten beim Verantwortlichen (Controller) ankommen. Was die betroffene Person vorher mit ihren eigenen Daten tut, wird nicht gelabelt.

Diese Ausnahme gilt **nur für die betroffene Person selbst**. Gibt eine dritte Person die Daten eines anderen ein, etwa Lieferadresse oder Zahlungsdaten, ist das eine Verarbeitung fremder personenbezogener Daten und damit relevant (`COLLECTION`), z. B. „Lieferadresse eingeben“ und „Zahlungsdaten angeben“.

#### Körperliche Handlungen an einer Person

Eine Handlung an einer Person ist nicht automatisch eine Datenverarbeitung. Sie ist nur relevant, wenn dabei **Daten entstehen** (z. B. Gesundheitsdaten bei einer Untersuchung) oder wenn sie **auf Grundlage personenbezogener Daten entschieden** wird (z. B. Medikamentengabe anhand der Patientenakte).

| Element | Entscheidung | Begründung |
| --- | --- | --- |
| „Wash patient“ | Nicht relevant | Rein körperliche Handlung, keine Daten entstehen |
| „Shave patient's abdomen“ | Nicht relevant | Rein körperliche Handlung, keine Daten entstehen |
| „Transport patient to examination“ | Nicht relevant | Rein körperliche Handlung, keine Daten entstehen |
| „Cover patient“ | Nicht relevant | Rein körperliche Handlung, keine Daten entstehen |
| „Calm Customer“ | Nicht relevant | Zwischenmenschliche Handlung, keine Daten entstehen |
| „Perform colonoskopy“ | Relevant, `COLLECTION` | Untersuchung erzeugt Gesundheitsdaten (Art. 9 DSGVO) |
| „Perform ultrasound scan“ | Relevant, `COLLECTION` | Untersuchung erzeugt Gesundheitsdaten (Art. 9 DSGVO) |
| „Give medicaments“ | Relevant, `USAGE` | Medikation wird anhand der Patientenakte festgelegt |

#### Kommunikation mit einer Person

Ein Gespräch ist nicht automatisch eine Datenverarbeitung. Es ist relevant, sobald dabei **personenbezogene Daten preisgegeben werden**, z. B. wenn eine Ärztin mit Angehörigen über den Zustand der Patientin spricht. Ein bloßer Kontakt oder eine Terminabsprache ohne Weitergabe solcher Daten wird nicht gelabelt.

| Element | Entscheidung | Begründung |
| --- | --- | --- |
| „Meet patient“ | Nicht relevant | Kontaktaufnahme, keine Daten werden preisgegeben |
| „Termin anfragen“ | Nicht relevant | Terminabsprache, keine Daten werden preisgegeben |
| „Videotermin beitreten“ | Nicht relevant | Beitritt zum Gespräch, keine Daten werden preisgegeben |
| „Talk to relatives“ | Relevant, `TRANSFERAL` | Gesundheitsdaten werden an Dritte weitergegeben |
| „Answer questions“ | Relevant, *Klasse prüfen* | Bei der Beantwortung werden personenbezogene Daten preisgegeben |

#### Entscheidungen über eine Person

Eine Bewertung oder Entscheidung über eine bestimmte Person ist relevant, sobald sie **auf deren personenbezogenen Daten beruht**. Die Daten werden dabei verwendet, die Klasse ist daher `USAGE`. Das entspricht der Regel für Gateways (siehe „Kreditwürdig?“) und für „Give medicaments“. Entscheidungen, die nicht auf Daten einer Person beruhen (z. B. „Lagerbestand > 0?“), bleiben nicht relevant.

| Element | Entscheidung | Begründung |
| --- | --- | --- |
| „Decide whether patient should be operated anyway“ | Relevant, `USAGE` | Entscheidung beruht auf Gesundheitsdaten (Art. 9 DSGVO) |
| „Check results“ | Relevant, `USAGE` | Untersuchungsergebnisse der Person werden bewertet |
| „Collect and evaluate all results“ | Relevant, `COLLECTION` + `USAGE` | Ergebnisse werden gesammelt und bewertet (siehe Abschnitt 4) |

#### Gleicher Name, unterschiedlicher Kontext

Elemente mit gleichem Namen können unterschiedlich gelabelt werden, auch innerhalb desselben Modells. **Der Kontext hat Vorrang vor dem Namen**, also Lane, verknüpfte Data Objects, Text Annotationen und Prozessbeschreibung (siehe auch „Vage Aktivitätsnamen“ in Abschnitt 6). Weicht das Label von einem gleichnamigen Element ab, muss das Feld `reason` erklären, welcher Kontext den Unterschied ausmacht.

| Element | Entscheidung | Begründung |
| --- | --- | --- |
| „Unterlagen vervollständigen“ | Je nach Kontext | In einem Modell relevant, in einem anderen nicht; Unterschied im `reason` begründen |
| „write summary“ | Je nach Kontext | Mehrere Vorkommen im Modell, jedes einzeln anhand des Kontexts labeln |
| „fill formular“ | Je nach Kontext | Mehrere Vorkommen im Modell, jedes einzeln anhand des Kontexts labeln |
| „Problemermittlung“ | Je nach Kontext | Mehrere Vorkommen im Modell, jedes einzeln anhand des Kontexts labeln |

#### Produktion und Wartung

Prozesse aus Produktion, Lager und Wartung verarbeiten meist Maschinen oder Bestandsdaten. Diese sind **nur relevant, wenn sich die Daten einer bestimmten Person zuordnen lassen** (Art. 4 Nr. 1 DSGVO). Das betrifft vor allem Beschäftigtendaten, etwa wenn ein Protokoll festhält, wer eine Maschine bedient oder eine Schicht geleitet hat. Modelle ganz ohne relevante Elemente sind dabei ausdrücklich möglich (z. B. Lager Inventur, Wartungsplanung oder Maschinenbetrieb).

| Element | Entscheidung | Begründung |
| --- | --- | --- |
| „CM-Daten senden“ | Je nach Kontext | Reine Zustandsdaten der Maschine sind nicht relevant; relevant (`TRANSFERAL`) nur, wenn sie einer Person zugeordnet sind, z. B. dem Bediener |
| Schichtprotokolle | Je nach Kontext | Relevant, wenn Namen oder Kennungen der Beschäftigten enthalten sind |
| Bedienerdaten | Relevant | Beziehen sich auf eine bestimmte Person (Beschäftigtendaten) |

---

## 6. Sonderregeln

**Subprozesse:** Die enthaltenen Tasks werden gelabelt. Den Subprozess selbst nur dann, wenn er zugeklappt ist und keine sichtbaren Kinder hat. (Im aktuellen Datensatz betrifft das 30 Subprozess Labels, die entsprechend geprüft werden sollten.)

**Vage Aktivitätsnamen** (z. B. „Anfrage bearbeiten“): Den gesamten verfügbaren Kontext heranziehen, also Lane, verknüpfte Data Objects, Text Annotationen und Prozessbeschreibung. Das LLM erhält diese Informationen ebenfalls, die Labels beruhen damit auf derselben Grundlage.

**Text Annotationen:** Text Annotationen werden **nie gelabelt**, aber immer als Kontext gelesen. Oft steht dort genau die entscheidende Information. Beispiel: Eine Annotation listet „Name, address, date of birth, gender, citizenship, confession, health insurance“ auf. Daraus geht hervor, dass das zugehörige Element personenbezogene Daten verarbeitet, darunter mit der Konfession sogar eine besondere Kategorie nach Art. 9 DSGVO. Im aktuellen Datensatz gibt es 262 Text Annotationen.

**Gateways:** Gateways verarbeiten selten selbst Daten, sondern entscheiden nur über den weiteren Ablauf. Sie werden deshalb nur gelabelt, wenn die Entscheidungsgrundlage personenbezogen ist.

**Data Objects und Data Stores: References labeln, nicht Definitionen.** Gelabelt wird immer die Reference (`dataObjectReference` bzw. `dataStoreReference`), nie die zugrunde liegende Definition (`dataObject` bzw. `dataStore`). Die Evaluation zählt ausschließlich References. Wird ein Store oder ein Data Object mehrfach referenziert, wird **jede Reference einzeln** gelabelt.

---

## 7. Ablauf des Labelings

- **Unabhängigkeit:** Alle Labeler arbeiten unabhängig voneinander und sehen die Labels der anderen nicht.
- **Tool:** Gelabelt wird mit **GRIPL v2**.
- **Kalibrierung:** Es gibt keine Kalibrierungsrunde. Grundlage für alle Entscheidungen ist ausschließlich dieser Guide.

---

## 8. Umgang mit Uneinigkeit

Das finale Label entsteht per **Mehrheitsentscheid**. Bei drei Labelern gilt also, was mindestens zwei vergeben haben. Das gilt einzeln für jede Entscheidung:

- **Relevanz:** Ein Element ist relevant, wenn die Mehrheit es als relevant gelabelt hat.
- **Klassen:** Jede Klasse wird separat abgestimmt. Ein Element erhält eine Klasse, wenn die Mehrheit sie vergeben hat.

Damit es keine Patt Situationen gibt, wird mit einer ungeraden Anzahl an Labelern gearbeitet.

Ergibt sich bei einem relevanten Element für **keine Klasse eine Mehrheit**, wird die Klasse in einer kurzen Abstimmung zwischen den Labelern festgelegt und im Feld `reason` begründet.

---

## 9. Qualitätssicherung

Ein Teil des Datensatzes wird von mehreren Labelern unabhängig gelabelt. Die Übereinstimmung wird mit **Fleiss' κ** gemessen (bei zwei Labelern alternativ Cohen's κ). Die Begründungen im Feld `reason` dienen dabei dem Abgleich zwischen Labelern und der späteren Fehleranalyse.

> **TODO:** Umfang der Mehrfachlabels und Ergebnis der Übereinstimmung ergänzen.

---

## 10. Anonymisierung der Modelle

Einige Modelle enthalten echte Angaben, mit denen sich eine bestimmte Person oder Firma eindeutig zuordnen lässt. Diese Angaben werden entfernt, bevor gelabelt wird und bevor das LLM die Modelle erhält. So arbeiten Labeler und Modell weiterhin mit demselben Kontext (siehe Abschnitt 6), und der Datensatz kann veröffentlicht werden.

### Was wird anonymisiert?

| Bereich | Beispiele |
| --- | --- |
| Personen | Namen, Kürzel und Unterschriften, E-Mail Adressen, Telefonnummern, Anschriften, Personal, Kunden oder Matrikelnummern, Geburtsdaten |
| Firmen und Organisationen | Firmen und Kliniknamen, Namen von Hochschulen oder Behörden, Standorte, Logos, interne Projekt oder Abteilungsbezeichnungen, die nur in dieser Organisation vorkommen |
| Indirekte Hinweise | Kombinationen, die zusammen eindeutig sind, z. B. „Leiterin der Kardiologie am Standort X“ |

Anonymisiert wird **jeder Text im Modell**: Elementnamen, Lanes und Pools, Data Objects und Data Stores, Text Annotationen, Dokumentationsfelder und die Prozessbeschreibung.

### Was bleibt erhalten?

- **Rollen** wie „Arzt“, „Sachbearbeitung“ oder „Bewerber“.
- **Datenkategorien** wie „Name, address, date of birth“. Sie beschreiben, *welche Art* von Daten verarbeitet wird, und sind oft genau die Information, die für das Labeln entscheidend ist. Nur konkrete Werte wie „Max Müller“ werden ersetzt.
- **Verbreitete Standardsoftware** wie QIS, CMS oder Kardex, solange sie keine bestimmte Organisation erkennbar macht.
- **Element IDs** bleiben unverändert, da die Evaluation die Labels über die IDs zuordnet.

### Wie wird ersetzt?

Konkrete Angaben werden durch **Platzhalter** ersetzt, die innerhalb eines Modells konsistent sind: dieselbe Person oder Firma bekommt im ganzen Modell denselben Platzhalter.

| Vorher | Nachher |
| --- | --- |
| „E-Mail an Max Müller senden“ | „E-Mail an [PERSON_1] senden“ |
| Lane „Müller GmbH Einkauf“ | Lane „[FIRMA_1] Einkauf“ |
| „Rechnung an klinik-musterstadt@example.de schicken“ | „Rechnung an [EMAIL_1] schicken“ |

Die Anonymisierung darf das Label nicht verändern. Ein Element, das vorher personenbezogene Daten verarbeitet hat, tut das auch nach der Ersetzung. „E-Mail an [PERSON_1] senden“ bleibt also `TRANSFERAL`.

### Feld `reason`

Auch im Feld `reason` dürfen keine echten Namen oder Firmenangaben stehen. Dort immer die Rolle oder den Platzhalter verwenden.

---

## 11. Versionshistorie

| Version | Datum | Änderung |
| --- | --- | --- |
| 1.0 | 01.10.2026 | Erste Fassung |

---

## Anhang: Technischer Hintergrund zu GRIPL

GRIPL bietet verschiedene Endpunkte an. Das Paradigma der Klassifikationspipeline wird über drei Stellschrauben bestimmt:

- **Granularität:** binär oder Multiclass mit sieben Processing Types
- **Kontext:** nur Parameterwissen des LLM oder RAG
- **BPMN Struktur:** nur Aktivitäten oder erweiterte Elemente (Aktivitäten, Events, Gateways, Data Objects und Data Stores)

### Verfügbare Kombinationen

| Ausgabe | RAG | Elemente | Status | Auswahl |
| --- | --- | --- | --- | --- |
| binär | nein | nur Aktivitäten | ✅ Baseline | `/binary` + `activitiesOnly=true` |
| binär | nein | alle | ✅ | `/binary` |
| binär | ja | nur Aktivitäten | ✅ | `/binary` + `useRag=true` + `activitiesOnly=true` |
| binär | ja | alle | ✅ | `/binary` + `useRag=true` |
| Multiclass | nein | nur Aktivitäten | ✅ | `/multiclass` |
| Multiclass | nein | alle | ❌ | Der Prompt enthält ausdrücklich „Ignore all other element types“. |
| Multiclass | ja | nur Aktivitäten | ❌ | Keine RAG-Anbindung im `MulticlassBpmnAnalyzer` |
| Multiclass | ja | alle | ❌ | Nicht implementiert |

**Zu beachten:**

- Alle Varianten bauen auf demselben Ansatz auf. Das BPMN wird zu einer Elementliste aufbereitet und mit einem ausführlichen Prompt aus `resources/prompts/` analysiert. Einen eigenen Baseline-Endpunkt gibt es nicht mehr. Die Baseline ist die einfachste Konfiguration von `/binary`.
- `/binary` nutzt standardmäßig **alle Elemente**. Für die Baseline-Definition „nur Aktivitäten“ muss `activitiesOnly=true` gesetzt werden.
- Ohne Angabe ist RAG ausgeschaltet (`useRag=false`). Mit `useRag=true` gilt standardmäßig `ragMode=hybrid`; außerdem sind `naive`, `local` und `global` möglich.
- Der Multiclass-Endpunkt klassifiziert aktuell nur Aktivitäten. Die Ground Truth für weitere Elementtypen wird trotzdem schon jetzt mit Klassen angelegt, damit sie später nutzbar ist.
- **Multiclass-Läufe immer mit „Activities only“ auswerten.** Sonst zählen erwartete Labels auf Nicht-Aktivitäten als False Negatives, und der Recall fällt künstlich niedrig aus.
