# GRIPL – Benutzerhandbuch

**GRIPL** (*GDPR Risk Identification in Processes using Large Language Models*) ist ein Werkzeug, mit dem Geschäftsprozessmodelle (BPMN 2.0) automatisch darauf untersucht werden, an welchen Stellen personenbezogene Daten im Sinne der DSGVO verarbeitet werden. Die Analyse übernimmt ein Large Language Model (LLM), optional unterstützt durch eine Wissensbasis mit Rechtstexten (RAG).

Dieses Handbuch erklärt die Funktionen der Anwendung aus Sicht der Nutzerinnen und Nutzer. Technische Details zu Installation und Betrieb sind nicht Teil dieses Dokuments und finden sich unter https://github.com/DBIS-Legal-LLMs/GRIPL-v2.

---

## Inhalt

1. [Überblick und Navigation](#1-überblick-und-navigation)
2. [Grundbegriffe](#2-grundbegriffe)
3. [Process Analysis – Prozessmodelle analysieren](#3-process-analysis--prozessmodelle-analysieren)
4. [Labeling – eigene Testdatensätze erstellen](#4-labeling--eigene-testdatensätze-erstellen)
5. [Evaluation – LLMs systematisch vergleichen](#5-evaluation--llms-systematisch-vergleichen)
6. [Custom Endpoints – eigene Prompts verwenden](#6-custom-endpoints--eigene-prompts-verwenden)
7. [Der BPMN-Editor](#7-der-bpmn-editor)
8. [Typische Arbeitsabläufe](#8-typische-arbeitsabläufe)
9. [Häufige Fragen und Probleme](#9-häufige-fragen-und-probleme)

---

## 1. Überblick und Navigation

Nach dem Öffnen von GRIPL im Browser landen Sie direkt auf der Seite **Process Analysis**. Am linken Rand befindet sich die Seitenleiste mit allen Bereichen der Anwendung:

| Bereich | Wofür? |
|---|---|
| **Process Analysis** | BPMN-Modelle hochladen und vom LLM auf DSGVO-relevante Aktivitäten prüfen lassen |
| **Labeling** | Eigene Datensätze anlegen und Prozessmodelle manuell mit der „richtigen Lösung“ versehen |
| **Evaluation** | Ein oder mehrere LLMs gegen die gelabelten Datensätze testen und Kennzahlen vergleichen |
| **Custom Endpoints** | Eigene Prompts als zusätzliche Analysevariante hinterlegen |

![image-20261012085153446](C:\Users\MagdalenavonSchwerin\AppData\Roaming\Typora\typora-user-images\image-20261012085153446.png)

### Global Analysis Endpoint

Unten in der Seitenleiste finden Sie das Auswahlfeld **Global Analysis Endpoint**. Hier legen Sie fest, *welche Analysevariante* (also welcher Prompt bzw. welches Verfahren) verwendet wird. Die Auswahl gilt für die gesamte Anwendung – für Analysen ebenso wie für den Labeling-Modus – und bleibt im Browser gespeichert.

Standardmäßig stehen zwei Varianten zur Verfügung:

- **Zero-shot Baseline** – *binäre* Analyse: Jede Aktivität wird als „kritisch“ oder „nicht kritisch“ eingestuft.
- **Multiclass Analysis** – *Mehrklassen*-Analyse: Kritische Aktivitäten werden zusätzlich einer oder mehreren DSGVO-Verarbeitungskategorien zugeordnet (siehe [Abschnitt 2](#2-grundbegriffe)).

Eigene Varianten, die unter **Custom Endpoints** angelegt wurden, erscheinen ebenfalls in dieser Liste.

![image-20261012085228117](C:\Users\MagdalenavonSchwerin\AppData\Roaming\Typora\typora-user-images\image-20261012085228117.png)

### Hinweisbanner zur Wissensbasis

Erscheint am oberen Bildschirmrand ein gelbes Banner mit dem Hinweis, dass der *GDPR knowledge graph* leer ist, liefert die RAG-Funktion derzeit keine Rechtstexte. Analysen ohne RAG funktionieren weiterhin. Wenden Sie sich in diesem Fall an die Betreuung des Projekts.

---

## 2. Grundbegriffe

**BPMN-Modell / Prozessmodell**
Ein Geschäftsprozess, modelliert im Standard BPMN 2.0 (Dateiendung `.bpmn` oder `.xml`). Solche Dateien lassen sich z. B. mit Camunda Modeler, bpmn.io oder oder direkt im GRIPL-Editor.

**Element / Aktivität**
Ein einzelner Baustein im Modell, z. B. eine Aufgabe („Bewerbung prüfen“), ein Ereignis, ein Gateway oder ein Datenobjekt.

**DSGVO-kritisch**
Eine Aktivität ist kritisch, wenn in ihr personenbezogene Daten verarbeitet werden.

**Binär vs. Multiclass**
Im *binären* Modus lautet die Frage nur: kritisch ja/nein? Im *Multiclass*-Modus wird zusätzlich angegeben, *wie* Daten verarbeitet werden. Dabei gibt es sieben Kategorien:

| Kategorie | Bedeutung | Farbe |
|---|---|---|
| **Collection** | Personenbezogene Daten werden erhoben (z. B. Name, E-Mail, Gesundheitsdaten) | grün |
| **Storage** | Daten werden in Datenbanken, Systemen oder Dateien gespeichert | blau |
| **Usage** | Daten werden für operative oder analytische Zwecke genutzt | gelb/orange |
| **Transferal** | Daten werden an Dritte oder nach extern weitergegeben | violett |
| **Modification** | Gespeicherte Daten werden aktualisiert, korrigiert oder verändert | hellblau |
| **Deletion** | Daten werden gelöscht oder anonymisiert | rot |
| **Access** | Daten werden abgerufen oder Personen/Systemen zugänglich gemacht | türkis |

Eine Aktivität kann mehreren Kategorien gleichzeitig angehören.

**RAG (Retrieval-Augmented Generation)**
Bei aktivierter RAG-Option sucht GRIPL vor der Analyse passende Passagen aus Rechtstexten (u. a. DSGVO) in einer Wissensbasis heraus und gibt sie dem LLM als zusätzlichen Kontext mit. Das LLM kann sich in seiner Begründung dann auf konkrete Textstellen beziehen.

**Datensatz / Testfall**
Ein *Testfall* ist ein Prozessmodell, bei dem von Menschen festgelegt wurde, welche Aktivitäten kritisch sind (die „Musterlösung“ bzw. *Ground Truth*). Mehrere Testfälle werden in einem *Datensatz* gebündelt.

---

## 3. Process Analysis – Prozessmodelle analysieren

Dies ist der zentrale Bereich, um eigene Prozessmodelle vom LLM prüfen zu lassen.

![image-20261012085350711](C:\Users\MagdalenavonSchwerin\AppData\Roaming\Typora\typora-user-images\image-20261012085350711.png)

### 3.1 Analyse-Einstellungen festlegen (einmalig)

Bevor die erste Analyse gestartet werden kann, muss angegeben werden, welches LLM verwendet werden soll. Solange das nicht geschehen ist, wird ein gelber Hinweis angezeigt und der Button **Analysis Settings** ist farbig hervorgehoben.

1. Klicken Sie auf **Analysis Settings**.
2. Füllen Sie die Felder aus:

| Feld | Bedeutung |
|---|---|
| **LLM Base URL** | Adresse des LLM-Anbieters, z. B. `https://api.openai.com/v1` oder `https://openrouter.ai/api/v1`. Vorschläge erscheinen beim Klick ins Feld. |
| **Model Name** | Name des Modells beim Anbieter, z. B. `gpt-4o-mini`. |
| **API Key** | Zugangsschlüssel für den Anbieter. Falls die Betreuung serverseitig Schlüssel hinterlegt hat, können Sie stattdessen einen Platzhalter wie `${OPENAI_API_KEY}` aus der Vorschlagsliste wählen. |
| **Seed** | Optionale Zahl für reproduzierbarere Ergebnisse. Über den Würfel-Button kann eine Zufallszahl erzeugt werden. Nicht jedes Modell unterstützt einen Seed. |
| **Temperature** | Steuert die „Kreativität“ des Modells. Niedrige Werte (z. B. 0) liefern konsistentere Antworten. Leer lassen = Standardwert des Modells. |
| **Top P** | Alternative Stellschraube für die Zufälligkeit. Im Zweifel leer lassen. |
| **Use RAG for the Analysis** | Schaltet die Einbindung von Rechtstexten ein oder aus. |
| **RAG Search Mode** | Nur bei aktivem RAG: Suchstrategie in der Wissensbasis (*Naive*, *Local*, *Global*, *Hybrid*). *Hybrid* ist ein guter Standard. |

3. Klicken Sie auf **Save Settings**.

Die Einstellungen werden im Browser gespeichert und gelten für alle folgenden Analysen – sowohl aus der Liste heraus als auch aus der Detailansicht eines Modells.

> **Hinweis:** Ist als Global Analysis Endpoint ein Custom Endpoint ausgewählt, ist die RAG-Einstellung durch diesen Endpoint fest vorgegeben und kann hier nicht verändert werden.

### 3.2 Prozessmodelle hochladen

1. Klicken Sie auf **Upload Process Models**.
2. Wählen Sie eine oder mehrere `.bpmn`- oder `.xml`-Dateien aus.
3. Die Modelle erscheinen anschließend oben in der Liste mit dem Status *Not analyzed*.

### 3.3 Analyse starten

1. Setzen Sie bei den gewünschten Modellen links ein Häkchen. Mit **Select all** werden alle Modelle auf einmal ausgewählt.
2. Klicken Sie auf **Analyze Selected (n)**.

Die Analyse läuft im Hintergrund. Sie können die Seite währenddessen verlassen. Der Status jedes Modells wird automatisch aktualisiert:

| Status | Bedeutung |
|---|---|
| *Not analyzed* | Noch nicht analysiert |
| *Queued* | Wartet auf Bearbeitung |
| *Analyzing…* | Wird gerade analysiert |
| *x/y Elements critical* (grüner Haken) | Fertig – x von y Elementen wurden als kritisch eingestuft |
| *Analysis failed* (rotes Symbol) | Fehler – beim Darüberfahren mit der Maus wird die Fehlermeldung angezeigt |

Ein Modell kann jederzeit erneut analysiert werden, z. B. mit einem anderen LLM oder Endpoint. Das neue Ergebnis ersetzt das vorherige.

### 3.4 Ergebnisse herunterladen und Modelle löschen

- **Download-Symbol** in einer Zeile: lädt das Analyseergebnis dieses einen Modells als JSON-Datei herunter (nur bei abgeschlossener Analyse).
- **Download Report (n)**: lädt die Ergebnisse aller ausgewählten Modelle als eine gemeinsame JSON-Datei herunter. Noch nicht analysierte Modelle werden darin mit einem entsprechenden Hinweis aufgeführt.
- **Papierkorb-Symbol** in einer Zeile: löscht dieses Modell.
- **Delete Selected**: löscht alle ausgewählten Modelle.

> ⚠️ Gelöschte Modelle samt Analyseergebnissen können nicht wiederhergestellt werden.

### 3.5 Detailansicht eines Modells

Ein Klick auf den Namen eines Modells öffnet die Detailansicht. Dort wird das Prozessmodell grafisch angezeigt.

![image-20261012085504797](C:\Users\MagdalenavonSchwerin\AppData\Roaming\Typora\typora-user-images\image-20261012085504797.png)

**Farbliche Markierung im Diagramm**
Nach der Analyse sind alle als kritisch erkannten Elemente farbig hinterlegt. Im binären Modus erscheinen sie rot, im Multiclass-Modus in der Farbe ihrer Kategorie (siehe Tabelle in [Abschnitt 2](#2-grundbegriffe)). Elemente mit mehreren Kategorien sind rosa hinterlegt und dick violett umrandet.

**Aktionen oben rechts**
- **Analyze for GDPR** – startet die Analyse für dieses Modell mit den aktuellen Analysis Settings und dem aktuell gewählten Global Analysis Endpoint.
- **Download Report (Json)** – lädt das Ergebnis als JSON-Datei herunter.

**Ergebnisbereich unten**
Der Ergebnisbereich lässt sich über die Kopfzeile ein- und ausklappen.

- **AI Reasoning / AI Model Reasoning** – Tabelle aller kritischen Aktivitäten mit der Begründung des LLM. Im Multiclass-Modus werden zusätzlich die Kategorien als farbige Abzeichen angezeigt.
  Wird im Diagramm ein Element angeklickt, ist die zugehörige Zeile in der Tabelle hervorgehoben.

- **References** (nur mit RAG) – Unter einer Begründung können die Rechtstext-Zitate aufgeklappt werden, auf die sich das LLM stützt. Ein Klick auf den Dokumentnamen öffnet das Originaldokument (PDF) an der entsprechenden Stelle.

- **Retrieved Knowledge** (nur mit RAG) – zeigt, welches Wissen aus der Wissensbasis abgerufen wurde:
  - Oben können Sie zwischen **All activities** und einzelnen Aktivitäten wechseln.
  - Links ist ein **Wissensgraph** zu sehen: Die Kreise sind Begriffe (z. B. Rechtsvorschriften, Pflichten, Akteure), die Linien deren Beziehungen. Fahren Sie mit der Maus über einen Kreis oder eine Linie, um Details zu sehen. Kreise lassen sich verschieben, die Ansicht lässt sich zoomen.
  - Rechts unter **Legal Excerpts** stehen die abgerufenen Textauszüge. Ein Klick auf den Dokumentnamen öffnet das PDF.

> **Hinweis:** Das Diagramm in der Detailansicht kann zwar bearbeitet und über **Export** heruntergeladen werden, Änderungen werden aber **nicht** in GRIPL gespeichert. Analysiert wird immer die hochgeladene Version. Wenn Sie ein geändertes Modell analysieren möchten, exportieren Sie es und laden Sie es in der Übersicht erneut hoch.

---

## 4. Labeling – eigene Testdatensätze erstellen

Im Labeling-Bereich legen Sie fest, welche Aktivitäten eines Prozessmodells *tatsächlich* DSGVO-kritisch sind. Diese gelabelten Modelle (Testfälle) dienen als Musterlösung, gegen die die LLM-Ergebnisse in der **Evaluation** verglichen werden.

> 💡 Ausführliche inhaltliche Regeln, *wann* eine Aktivität als kritisch gilt und welche Kategorie passt, finden Sie im separaten Dokument [GRIPL_Labeling_Guide.md](GRIPL_Labeling_Guide.md).

![image-20261012085738160](C:\Users\MagdalenavonSchwerin\AppData\Roaming\Typora\typora-user-images\image-20261012085738160.png)

### 4.1 Datensatz anlegen

1. Klicken Sie oben rechts auf **Create Dataset**.
2. Geben Sie einen **Name** und optional eine **Description** ein.
3. Bestätigen Sie mit **Create**.

Ein Klick auf einen Datensatz klappt ihn auf und zeigt alle enthaltenen Testfälle als Kacheln mit Vorschaubild.

### 4.2 Testfall anlegen

1. Klicken Sie neben dem gewünschten Datensatz auf **Create Test Case**.
2. Geben Sie einen Namen ein und bestätigen Sie mit **Create**.
3. Der Labeling-Editor öffnet sich mit einem leeren Diagramm.

Jetzt können Sie entweder:
- das Prozessmodell direkt im Editor modellieren, oder
- über **Upload** in der Werkzeugleiste eine vorhandene BPMN-Datei laden.

### 4.3 Aktivitäten labeln

Der Labeling-Editor besteht aus dem Diagramm, der Karte **Labeltools** oben rechts und der Seitenleiste **Labeled BPMN Elements** am rechten Rand.

1. Aktivieren Sie in den Labeltools den Schalter **Label Mode**. Das Diagramm erhält einen roten Rahmen und kann nun nicht mehr verändert werden – so werden versehentliche Änderungen verhindert.
2. Klicken Sie im Diagramm auf eine Aktivität. Rechts erscheint eine Karte mit dem Namen der Aktivität.
3. Vergeben Sie das Label:
   - **Binärer Modus:** Klicken Sie auf den Button *Not Critical*, um ihn auf *Critical* umzuschalten (und umgekehrt).
   - **Multiclass-Modus:** Klicken Sie eine oder mehrere der sieben Kategorien an. Beim Darüberfahren mit der Maus wird eine kurze Erklärung der Kategorie angezeigt. Ein erneuter Klick entfernt die Kategorie wieder.
4. Optional: Tragen Sie unter **Notes** eine Begründung oder Anmerkung ein.
5. Wiederholen Sie das für alle relevanten Aktivitäten.
6. Klicken Sie auf **Save Test Case**.

![image-20261012085821823](C:\Users\MagdalenavonSchwerin\AppData\Roaming\Typora\typora-user-images\image-20261012085821823.png)

Welcher Modus aktiv ist, richtet sich nach dem **Global Analysis Endpoint** in der Seitenleiste und wird in den Labeltools unter *Endpoint mode* angezeigt. Wählen Sie also *vor* dem Labeln den passenden Endpoint (z. B. *Multiclass Analysis* für Kategorie-Labels).

> ⚠️ Wechseln Sie bei einem Multiclass-gelabelten Testfall in den binären Modus, wird bei Aktivitäten mit mehreren Kategorien nur die erste beibehalten. Speichern Sie in diesem Fall nicht, wenn Sie die Kategorien behalten möchten.

**Seitenleiste „Labeled BPMN Elements“**
Hier sind alle bereits gelabelten Elemente mit Name, ID, Kategorien und Notiz aufgelistet. Elemente mit mehreren Kategorien sind mit *Multiple* gekennzeichnet. Über das Papierkorb-Symbol lässt sich ein Label entfernen. Die Leiste kann über den Pfeil eingeklappt werden.

> 💾 **Wichtig:** Änderungen werden erst mit **Save Test Case** gespeichert. Der Button ist nur aktiv, wenn es ungespeicherte Änderungen gibt. Nach erfolgreichem Speichern erscheint eine Bestätigung.

---

## 5. Evaluation – LLMs systematisch vergleichen

In der Evaluation lassen Sie ein oder mehrere LLMs alle Testfälle der ausgewählten Datensätze analysieren. GRIPL vergleicht die Ergebnisse automatisch mit Ihren Labels und berechnet Qualitätskennzahlen.

![image-20261012085915344](C:\Users\MagdalenavonSchwerin\AppData\Roaming\Typora\typora-user-images\image-20261012085915344.png)

### 5.1 Evaluation konfigurieren

Die Konfiguration gliedert sich in drei Bereiche.

**a) Default Settings – Standardeinstellungen**

| Einstellung | Bedeutung |
|---|---|
| **Default Evaluation Endpoint** | Welche Analysevariante standardmäßig genutzt wird: eine vorhandene (*Use preset*) oder eine eigene URL (*Custom endpoint*). |
| **Number of Repetitions** | Wie oft die gesamte Evaluation wiederholt wird. Mehrere Durchläufe zeigen, wie stabil die Ergebnisse eines Modells sind (Mittelwert ± Standardabweichung). |
| **Max Concurrent LLM Requests** | Wie viele Anfragen gleichzeitig an das LLM gesendet werden. Bei Fehlern wegen zu vieler Anfragen (Rate Limits) den Wert verringern. |
| **Seed** | Optionale Zahl für reproduzierbarere Ergebnisse. |
| **Evaluate Activities Only** | Bewertet nur Aktivitäten; Ereignisse, Gateways sowie Datenobjekte und -speicher werden ignoriert. |
| **RAG Configuration → Use RAG / RAG Mode** | Rechtstexte einbeziehen und Suchmodus wählen (*Hybrid* wird empfohlen). |
| **Evaluate RAG Quality (Ragas)** | Bewertet zusätzlich, wie gut das LLM die abgerufenen Rechtstexte nutzt (siehe [5.4](#54-ergebnisse-verstehen)). |

**b) Models Configuration – zu testende Modelle**

Über **Add Model** fügen Sie beliebig viele Modelle hinzu. Für jedes Modell geben Sie an:

- **Label** – ein frei wählbarer Anzeigename (z. B. „GPT-4o mini, Temp 0“). Unter diesem Namen erscheint das Modell in den Ergebnissen.
- **LLM Base URL**, **LLM Model Name**, **API Key** – wie bei den Analysis Settings (siehe [3.1](#31-analyse-einstellungen-festlegen-einmalig)).
- **Temperature**, **Top P** – optional.
- **LLM Response Timeout (seconds)** – wie lange auf eine Antwort gewartet wird.
- **Endpoint** – *Use default* (Standard aus Bereich a), *Preset* (andere vorhandene Variante) oder *Custom*.

So lässt sich z. B. dasselbe Modell mit und ohne RAG oder mit unterschiedlichen Prompts vergleichen.

**c) Datasets – Testdaten auswählen**

Wählen Sie unter **Select Datasets** einen oder mehrere Datensätze aus dem Labeling-Bereich aus.

**Konfiguration speichern und laden**
Mit **Download YAML Config** speichern Sie die aktuelle Konfiguration als Datei, mit **Import YAML Config** laden Sie sie wieder. So können Sie Experimente im Team teilen und später exakt wiederholen.

> ⚠️ In der heruntergeladenen YAML-Datei können API-Schlüssel enthalten sein. Teilen Sie die Datei nicht öffentlich.

### 5.2 Evaluation starten

Klicken Sie auf **Start Evaluation**. Während der Ausführung zeigt der Button an, welches Modell gerade welchen Testfall bearbeitet und wie viele Testfälle bereits erledigt sind. Je nach Anzahl der Modelle, Testfälle und Wiederholungen kann das einige Zeit dauern.

### 5.3 Berichte speichern und laden

Nach Abschluss stehen folgende Buttons zur Verfügung:

- **Download Markdown Report** – lesbarer Bericht mit allen Kennzahlen, z. B. für die Projektdokumentation.
- **Download JSON Report** – vollständige Ergebnisdaten.
- **Upload JSON Report** – lädt einen früher gespeicherten JSON-Bericht wieder in die Ansicht, ohne die Evaluation erneut auszuführen.

> 💡 Laden Sie den JSON-Bericht nach jeder Evaluation herunter. Die Ergebnisse sind sonst nach dem Neuladen der Seite nicht mehr sichtbar.

### 5.4 Ergebnisse verstehen

Unter **Complete Result Overview** werden die Ergebnisse dargestellt.

**Evaluation Metadata** fasst die Rahmendaten zusammen: getestete Modelle (mit Temperature und Top P), Datensätze, Anzahl der Testfälle, Endpoint, Anzahl der Durchläufe, Seed und Zeitpunkt.

**Kennzahlen**

| Kennzahl | Frage, die sie beantwortet |
|---|---|
| **Precision** | Von allen Aktivitäten, die das LLM als kritisch markiert hat – wie viele waren tatsächlich kritisch? (Wenige Fehlalarme = hohe Precision) |
| **Recall** | Von allen tatsächlich kritischen Aktivitäten – wie viele hat das LLM gefunden? (Wenig Übersehenes = hoher Recall) |
| **F1-Score** | Kombination aus Precision und Recall in einer Zahl |
| **Accuracy** (binär) | Anteil der insgesamt richtig eingestuften Elemente |
| **Exact Match Accuracy** (Multiclass) | Anteil der Elemente, bei denen die vorhergesagten Kategorien *exakt* mit dem Label übereinstimmen |
| **Retries** | Wie oft eine Anfrage wiederholt werden musste, z. B. weil die Antwort des LLM nicht korrekt formatiert war |
| **Context Utilization** (Ragas) | Wie gut nutzt das LLM die abgerufenen Rechtstexte? |
| **Faithfulness** (Ragas) | Stützt sich die Begründung tatsächlich auf die abgerufenen Texte, oder erfindet das LLM etwas? |

**Aggregated Results** – Bei mehreren Durchläufen zeigen Diagramme Mittelwert und Standardabweichung je Modell. Über das Menü (⋮) an jedem Diagramm können Sie es als **PNG herunterladen** oder die **Farben anpassen** (Configure Colors).

**Classification Filter** (nur Multiclass) – Damit lassen sich die Kennzahlen auf einzelne Kategorien beschränken, z. B. „Wie gut erkennt das Modell *Transferal*?“. Zusätzlich gibt es eine Auswertung **Per-class Metrics**.

**Results by Run** – Über Reiter navigieren Sie zu einem bestimmten **Durchlauf**, dann zu einem **Modell** und schließlich zu einem **Datensatz**. Dort sehen Sie:

- **Evaluation Summary** mit Anzahl bestandener (*Passed*), nicht bestandener (*Failed*) und fehlerhafter (*Errors*) Testfälle sowie die Kennzahlen dieses Durchlaufs, auch aufgeschlüsselt nach Elementtyp (**Metrics by Element Type**).
- Eine Karte pro **Testfall**, die sich aufklappen lässt:
  - **Expected / Detected / Correct** – wie viele Elemente erwartet, erkannt und richtig erkannt wurden.
  - **Vorschau** des Prozessmodells mit Markierungen.
  - **Vergleichstabelle** – erwartete und erkannte Einstufung je Element nebeneinander.
  - **Error Analysis** – **False Positives** (fälschlich als kritisch markiert) und **False Negatives** (übersehen).
  - **Reasoning** – Begründungen des LLM.
  - Bei RAG: die abgerufenen Rechtstexte und, falls aktiviert, die Ragas-Metriken.

Testfälle, bei denen ein technischer Fehler auftrat (z. B. Zeitüberschreitung), werden als rote Fehlerkarte mit der Fehlermeldung angezeigt.

---

## 6. Custom Endpoints – eigene Prompts verwenden

Hier können Sie eigene Analysevarianten anlegen, z. B. um verschiedene Prompt-Formulierungen miteinander zu vergleichen.

![image-20261012090021430](C:\Users\MagdalenavonSchwerin\AppData\Roaming\Typora\typora-user-images\image-20261012090021430.png)

### 6.1 Vorhandene Prompts ansehen

Unter **Built-in Endpoints** sind die mitgelieferten Varianten aufgeführt. Ein Klick klappt den vollständigen Prompt auf. Diese Prompts sind ein guter Ausgangspunkt für eigene Varianten, können aber nicht bearbeitet oder gelöscht werden.

### 6.2 Eigenen Endpoint anlegen

1. Klicken Sie auf **Upload Endpoint**.
2. Füllen Sie die Felder aus:
   - **Name** – z. B. „Strenger DSGVO-Prüfer“.
   - **Response shape** – *Binary* (kritisch / nicht kritisch) oder *Multiclass* (mit DSGVO-Kategorien).
   - **Use RAG** und ggf. **RAG search mode** – ob Rechtstexte einbezogen werden sollen.
   - **Prompt** – die Anweisung an das LLM.
3. Klicken Sie auf **Create**.

Der neue Endpoint erscheint sofort in der Liste (mit Abzeichen für Binary/Multiclass und RAG) sowie im Auswahlfeld **Global Analysis Endpoint** und in der Evaluation unter *Preset Endpoint*.

> **Hinweis:** Die RAG-Einstellung eines Custom Endpoints ist fest. Sie kann später in den Analysis Settings nicht überschrieben werden. Für eine Variante mit und eine ohne RAG legen Sie zwei Endpoints an.

---

## 7. Der BPMN-Editor

In der Detailansicht (Process Analysis) und im Labeling-Editor wird derselbe grafische Editor verwendet.

**Werkzeugleiste oben**

| Button | Funktion |
|---|---|
| 🔍+ / 🔍− | Vergrößern / Verkleinern |
| **100%** | Ansicht zurücksetzen |
| ↶ / ↷ | Rückgängig / Wiederherstellen |
| **Create New** | Leeres Diagramm beginnen |
| **Upload** | BPMN-Datei in den Editor laden |
| **Export** | Aktuelles Diagramm als `.bpmn`-Datei herunterladen |

**Im Diagramm**
- Mit gedrückter Maustaste auf freier Fläche verschieben Sie die Ansicht, mit dem Mausrad zoomen Sie.
- Ein Klick auf ein Element wählt es aus.
- Über die Palette am linken Rand lassen sich neue Elemente (Aufgaben, Ereignisse, Gateways, Datenobjekte …) per Drag & Drop einfügen.
- Ein Doppelklick auf ein Element erlaubt es, den Namen zu ändern.

> 💡 Damit das LLM gute Ergebnisse liefern kann, sollten Aktivitäten **aussagekräftig benannt** sein (z. B. „Bewerberdaten an Hochschule übermitteln“ statt „Schritt 3“). Auch Datenobjekte und Lanes/Pools helfen dem Modell, den Kontext zu verstehen.

---

## 8. Typische Arbeitsabläufe

### A) „Ich möchte wissen, wo in meinem Prozess personenbezogene Daten verarbeitet werden.“

1. **Global Analysis Endpoint** wählen (z. B. *Multiclass Analysis*).
2. **Process Analysis** → **Analysis Settings** ausfüllen und speichern.
3. **Upload Process Models** → Datei auswählen.
4. Modell anhaken → **Analyze Selected**.
5. Nach Abschluss auf das Modell klicken und Markierungen sowie Begründungen prüfen.
6. Optional: **Download Report** für die Dokumentation.

### B) „Wir möchten testen, wie gut verschiedene LLMs unsere Prozesse bewerten.“

1. Im **Labeling** einen Datensatz anlegen.
2. Für jedes Prozessmodell einen Testfall anlegen, das Modell hochladen, im **Label Mode** labeln und **speichern**.
   *Tipp:* Lassen Sie mehrere Teammitglieder unabhängig voneinander labeln und gleichen Sie Abweichungen anschließend ab.
3. In der **Evaluation** die zu vergleichenden Modelle mit **Add Model** hinzufügen und den Datensatz auswählen.
4. **Number of Repetitions** z. B. auf 3 setzen, um die Stabilität der Ergebnisse zu sehen.
5. **Download YAML Config** – damit lässt sich das Experiment später wiederholen.
6. **Start Evaluation**.
7. Nach Abschluss **Download JSON Report** und **Download Markdown Report**.
8. Ergebnisse vergleichen; in der **Error Analysis** nachsehen, *welche* Aktivitäten falsch eingestuft wurden.

### C) „Wir möchten herausfinden, ob ein anderer Prompt bessere Ergebnisse liefert.“

1. Unter **Custom Endpoints** einen Built-in Prompt aufklappen und als Vorlage nutzen.
2. Mit **Upload Endpoint** eine angepasste Variante anlegen.
3. In der **Evaluation** dasselbe LLM zweimal hinzufügen – einmal mit dem Standard-Endpoint, einmal mit dem eigenen Endpoint (*Preset*). Vergeben Sie unterscheidbare Labels.
4. Evaluation starten und Kennzahlen vergleichen.

---

## 9. Häufige Fragen und Probleme

**„Analyze Selected“ ist ausgegraut.**
Es ist kein Modell ausgewählt. Modelle, die sich gerade in der Warteschlange befinden oder analysiert werden, können nicht ausgewählt werden.

**Die Analyse endet mit „Analysis failed“.**
Fahren Sie mit der Maus über den Status, um die Fehlermeldung zu sehen. Häufige Ursachen: falscher API-Key, falscher Modellname, falsche Base URL oder aufgebrauchtes Guthaben beim LLM-Anbieter. Prüfen Sie die **Analysis Settings** und starten Sie die Analyse erneut.

**Es werden keine Rechtstexte / kein Wissensgraph angezeigt.**
Prüfen Sie, ob RAG in den Analysis Settings bzw. im Custom Endpoint aktiviert ist. Wird oben ein gelbes Banner angezeigt, ist die Wissensbasis derzeit leer – wenden Sie sich an die Betreuung.

**Im Labeling kann ich keine Kategorien auswählen, nur „Critical“.**
Als Global Analysis Endpoint ist eine binäre Variante ausgewählt. Wechseln Sie in der Seitenleiste auf einen Multiclass-Endpoint.

**Im Labeling lässt sich das Diagramm nicht mehr bearbeiten.**
Der **Label Mode** ist aktiv. Schalten Sie ihn aus, um das Modell zu bearbeiten.

**Meine Labels sind weg.**
Vermutlich wurde nicht auf **Save Test Case** geklickt, bevor die Seite verlassen wurde.

**Meine Änderungen am Diagramm in der Detailansicht wurden nicht analysiert.**
Die Detailansicht speichert keine Änderungen. Exportieren Sie das Diagramm und laden Sie es erneut hoch (siehe [3.5](#35-detailansicht-eines-modells)).

**Die Evaluation bricht ab oder viele Testfälle zeigen Fehler.**
Verringern Sie **Max Concurrent LLM Requests** (Rate Limits des Anbieters) oder erhöhen Sie das **LLM Response Timeout**.

**Die Evaluationsergebnisse sind nach dem Neuladen der Seite verschwunden.**
Laden Sie den zuvor gespeicherten JSON-Bericht über **Upload JSON Report** wieder hoch. Laden Sie Berichte deshalb immer direkt nach einer Evaluation herunter.

**Die Ergebnisse unterscheiden sich bei gleicher Eingabe.**
LLMs arbeiten nicht vollständig deterministisch. Eine niedrige **Temperature** und ein fester **Seed** verringern die Schwankungen; mehrere **Repetitions** in der Evaluation machen sie messbar.
