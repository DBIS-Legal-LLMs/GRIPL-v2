import sqlite3
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DB_PATH = PROJECT_ROOT / "test.db"

categories = [
    "Collection",
    "Storage",
    "Usage",
    "Transferal",
    "Modification",
    "Deletion",
    "Access",
]

articles_data = [
    {
        "article_number": "4",
        "paragraph": "1",
        "literature": None,
        "text": "„personenbezogene Daten“ alle Informationen, die sich auf eine identifizierte oder identifizierbare natürliche Person (im Folgenden „betroffene Person“) beziehen; als identifizierbar wird eine natürliche Person angesehen, die direkt oder indirekt, insbesondere mittels Zuordnung zu einer Kennung wie einem Namen, zu einer Kennnummer, zu Standortdaten, zu einer Online-Kennung oder zu einem oder mehreren besonderen Merkmalen, die Ausdruck der physischen, physiologischen, genetischen, psychischen, wirtschaftlichen, kulturellen oder sozialen Identität dieser natürlichen Person sind, identifiziert werden kann."
    },
    {
        "article_number": "4",
        "paragraph": "2",
        "literature": None,
        "text": "„Verarbeitung“ jeden mit oder ohne Hilfe automatisierter Verfahren ausgeführten Vorgang oder jede solche Vorgangsreihe im Zusammenhang mit personenbezogenen Daten wie das Erheben, das Erfassen, die Organisation, das Ordnen, die Speicherung, die Anpassung oder Veränderung, das Auslesen, das Abfragen, die Verwendung, die Offenlegung durch Übermittlung, Verbreitung oder eine andere Form der Bereitstellung, den Abgleich oder die Verknüpfung, die Einschränkung, das Löschen oder die Vernichtung."
    },
    {
        "article_number": "4",
        "paragraph": "3",
        "literature": None,
        "text": "„Einschränkung der Verarbeitung“ die Markierung gespeicherter personenbezogener Daten mit dem Ziel, ihre künftige Verarbeitung einzuschränken."
    },
    {
        "article_number": "4",
        "paragraph": "4",
        "literature": None,
        "text": "„Profiling“ jede Art der automatisierten Verarbeitung personenbezogener Daten, die darin besteht, dass diese personenbezogenen Daten verwendet werden, um bestimmte persönliche Aspekte, die sich auf eine natürliche Person beziehen, zu bewerten, insbesondere um Aspekte bezüglich Arbeitsleistung, wirtschaftliche Lage, Gesundheit, persönliche Vorlieben, Interessen, Zuverlässigkeit, Verhalten, Aufenthaltsort oder Ortswechsel dieser natürlichen Person zu analysieren oder vorherzusagen."
    },
    {
        "article_number": "4",
        "paragraph": "5",
        "literature": None,
        "text": "„Pseudonymisierung“ die Verarbeitung personenbezogener Daten in einer Weise, dass die personenbezogenen Daten ohne Hinzuziehung zusätzlicher Informationen nicht mehr einer spezifischen betroffenen Person zugeordnet werden können, sofern diese zusätzlichen Informationen gesondert aufbewahrt werden und technischen und organisatorischen Maßnahmen unterliegen, die gewährleisten, dass die personenbezogenen Daten nicht einer identifizierten oder identifizierbaren natürlichen Person zugewiesen werden."
    },
    {
        "article_number": "4",
        "paragraph": "6",
        "literature": None,
        "text": "„Dateisystem“ jede strukturierte Sammlung personenbezogener Daten, die nach bestimmten Kriterien zugänglich sind, unabhängig davon, ob diese Sammlung zentral, dezentral oder nach funktionalen oder geografischen Gesichtspunkten geordnet geführt wird."
    },
    {
        "article_number": "4",
        "paragraph": "7",
        "literature": None,
        "text": "„Verantwortlicher“ die natürliche oder juristische Person, Behörde, Einrichtung oder andere Stelle, die allein oder gemeinsam mit anderen über die Zwecke und Mittel der Verarbeitung von personenbezogenen Daten entscheidet; sind die Zwecke und Mittel dieser Verarbeitung durch das Unionsrecht oder das Recht der Mitgliedstaaten vorgegeben, so kann der Verantwortliche beziehungsweise können die bestimmten Kriterien seiner Benennung nach dem Unionsrecht oder dem Recht der Mitgliedstaaten vorgesehen werden."
    },
    {
        "article_number": "4",
        "paragraph": "8",
        "literature": None,
        "text": "„Auftragsverarbeiter“ eine natürliche oder juristische Person, Behörde, Einrichtung oder andere Stelle, die personenbezogene Daten im Auftrag des Verantwortlichen verarbeitet."
    },
    {
        "article_number": "4",
        "paragraph": "9",
        "literature": None,
        "text": "„Empfänger“ eine natürliche oder juristische Person, Behörde, Einrichtung oder andere Stelle, der personenbezogene Daten offengelegt werden, unabhängig davon, ob es sich bei ihr um einen Dritten handelt oder nicht. Behörden, die im Rahmen eines bestimmten Untersuchungsauftrags nach dem Unionsrecht oder dem Recht der Mitgliedstaaten möglicherweise personenbezogene Daten erhalten, gelten jedoch nicht als Empfänger."
    },
    {
        "article_number": "4",
        "paragraph": "10",
        "literature": None,
        "text": "„Dritter“ eine natürliche oder juristische Person, Behörde, Einrichtung oder andere Stelle, außer der betroffenen Person, dem Verantwortlichen, dem Auftragsverarbeiter und den Personen, die unter der unmittelbaren Verantwortung des Verantwortlichen oder des Auftragsverarbeiters befugt sind, die personenbezogenen Daten zu verarbeiten."
    },
    {
        "article_number": "4",
        "paragraph": "11",
        "literature": None,
        "text": "„Einwilligung“ der betroffenen Person jede freiwillig für den bestimmten Fall, in informierter Weise und unmissverständlich abgegebene Willensbekundung in Form einer Erklärung oder einer sonstigen eindeutigen bestätigenden Handlung, mit der die betroffene Person zu verstehen gibt, dass sie mit der Verarbeitung der sie betreffenden personenbezogenen Daten einverstanden ist."
    },
    {
        "article_number": "4",
        "paragraph": "12",
        "literature": None,
        "text": "„Verletzung des Schutzes personenbezogener Daten“ eine Verletzung der Sicherheit, die, ob unbeabsichtigt oder unrechtmäßig, zur Vernichtung, zum Verlust, zur Veränderung, oder zur unbefugten Offenlegung von beziehungsweise zum unbefugten Zugang zu personenbezogenen Daten führt, die übermittelt, gespeichert oder auf sonstige Weise verarbeitet wurden."
    },
    {
        "article_number": "4",
        "paragraph": "13",
        "literature": None,
        "text": "„genetische Daten“ personenbezogene Daten zu den ererbten oder erworbenen genetischen Eigenschaften einer natürlichen Person, die eindeutige Informationen über die Physiologie oder die Gesundheit dieser natürlichen Person liefern und insbesondere aus der Analyse einer biologischen Probe der betreffenden natürlichen Person gewonnen wurden."
    },
    {
        "article_number": "4",
        "paragraph": "14",
        "literature": None,
        "text": "„biometrische Daten“ mit speziellen technischen Verfahren gewonnene personenbezogene Daten zu den physischen, physiologischen oder verhaltenstypischen Merkmalen einer natürlichen Person, die die eindeutige Identifizierung dieser natürlichen Person ermöglichen oder bestätigen, wie Gesichtsbilder oder daktyloskopische Daten."
    },
    {
        "article_number": "4",
        "paragraph": "15",
        "literature": None,
        "text": "„Gesundheitsdaten“ personenbezogene Daten, die sich auf die körperliche oder geistige Gesundheit einer natürlichen Person, einschließlich der Erbringung von Gesundheitsdienstleistungen, beziehen und aus denen Informationen über deren Gesundheitszustand hervorgehen."
    },
    {
        "article_number": "4",
        "paragraph": "16",
        "literature": "a",
        "text": "„Hauptniederlassung“ im Falle eines Verantwortlichen mit Niederlassungen in mehr als einem Mitgliedstaat den Ort seiner Hauptverwaltung in der Union, es sei denn, die Entscheidungen hinsichtlich der Zwecke und Mittel der Verarbeitung personenbezogener Daten werden in einer anderen Niederlassung des Verantwortlichen in der Union getroffen und diese Niederlassung ist befugt, diese Entscheidungen umsetzen zu lassen; in diesem Fall gilt die Niederlassung, die derartige Entscheidungen trifft, als Hauptniederlassung."
    },
    {
        "article_number": "4",
        "paragraph": "16",
        "literature": "b",
        "text": "„Hauptniederlassung“ im Falle eines Auftragsverarbeiters mit Niederlassungen in mehr als einem Mitgliedstaat den Ort seiner Hauptverwaltung in der Union oder, sofern der Auftragsverarbeiter keine Hauptverwaltung in der Union hat, die Niederlassung des Auftragsverarbeiters in der Union, in der die Verarbeitungstätigkeiten im Rahmen der Tätigkeiten einer Niederlassung eines Auftragsverarbeiters hauptsächlich stattfinden, soweit der Auftragsverarbeiter spezifischen Pflichten aus dieser Verordnung unterliegt."
    },
    {
        "article_number": "4",
        "paragraph": "17",
        "literature": None,
        "text": "„Vertreter“ eine in der Union niedergelassene natürliche oder juristische Person, die von dem Verantwortlichen oder Auftragsverarbeiter gemäß Artikel 27 schriftlich bestellt wurde und den Verantwortlichen oder Auftragsverarbeiter in Bezug auf ihre jeweiligen Pflichten nach dieser Verordnung vertritt."
    },
    {
        "article_number": "4",
        "paragraph": "18",
        "literature": None,
        "text": "„Unternehmen“ eine natürliche oder juristische Person, die eine wirtschaftliche Tätigkeit ausübt, unabhängig von ihrer Rechtsform, einschließlich Personengesellschaften oder Vereinigungen, die regelmäßig einer wirtschaftlichen Tätigkeit nachgehen."
    },
    {
        "article_number": "4",
        "paragraph": "19",
        "literature": None,
        "text": "„Unternehmensgruppe“ eine Gruppe, die aus einem herrschenden Unternehmen und den von diesem abhängigen Unternehmen besteht."
    },
    {
        "article_number": "4",
        "paragraph": "20",
        "literature": None,
        "text": "„verbindliche interne Datenschutzvorschriften“ die Maßnahmen zum Schutz personenbezogener Daten, zu deren Einhaltung sich ein in einem Mitgliedstaat niedergelassener Verantwortlicher oder Auftragsverarbeiter für Übermittlungen oder eine Reihe von Übermittlungen personenbezogener Daten an einen Verantwortlichen oder Auftragsverarbeiter in einem Drittland innerhalb einer Unternehmensgruppe oder einer Gruppe von Unternehmen, die eine gemeinsame wirtschaftliche Tätigkeit ausüben, verpflichtet."
    },
    {
        "article_number": "4",
        "paragraph": "21",
        "literature": None,
        "text": "„Aufsichtsbehörde“ eine von einem Mitgliedstaat gemäß Artikel 51 eingerichtete unabhängige staatliche Behörde."
    },
    {
        "article_number": "4",
        "paragraph": "22",
        "literature": "a",
        "text": "„betroffene Aufsichtsbehörde“ eine Aufsichtsbehörde, die von der Verarbeitung personenbezogener Daten betroffen ist, weil der Verantwortliche oder der Auftragsverarbeiter im Hoheitsgebiet des Mitgliedstaats dieser Aufsichtsbehörde niedergelassen ist."
    },
    {
        "article_number": "4",
        "paragraph": "22",
        "literature": "b",
        "text": "„betroffene Aufsichtsbehörde“ eine Aufsichtsbehörde, die von der Verarbeitung personenbezogener Daten betroffen ist, weil diese Verarbeitung erhebliche Auswirkungen auf betroffene Personen mit Wohnsitz im Mitgliedstaat dieser Aufsichtsbehörde hat oder haben kann."
    },
    {
        "article_number": "4",
        "paragraph": "22",
        "literature": "c",
        "text": "„betroffene Aufsichtsbehörde“ eine Aufsichtsbehörde, die von der Verarbeitung personenbezogener Daten betroffen ist, weil eine Beschwerde bei dieser Aufsichtsbehörde eingereicht wurde."
    },
    {
        "article_number": "4",
        "paragraph": "23",
        "literature": "a",
        "text": "„grenzüberschreitende Verarbeitung“ eine Verarbeitung personenbezogener Daten, die im Rahmen der Tätigkeiten von Niederlassungen eines Verantwortlichen oder eines Auftragsverarbeiters in der Union in mehr als einem Mitgliedstaat erfolgt, wenn der Verantwortliche oder Auftragsverarbeiter in mehr als einem Mitgliedstaat niedergelassen ist."
    },
    {
        "article_number": "4",
        "paragraph": "23",
        "literature": "b",
        "text": "„grenzüberschreitende Verarbeitung“ eine Verarbeitung personenbezogener Daten, die im Rahmen der Tätigkeiten einer einzelnen Niederlassung eines Verantwortlichen oder eines Auftragsverarbeiters in der Union erfolgt, die jedoch erhebliche Auswirkungen auf betroffene Personen in mehr als einem Mitgliedstaat hat oder haben kann."
    },
    {
        "article_number": "4",
        "paragraph": "24",
        "literature": None,
        "text": "„erheblicher und begründeter Einwand“ einen Einwand gegen einen Entwurf eines Beschlusses der federführenden Aufsichtsbehörde, der entweder deutlich macht, welche Gefahren die Grundrechte und Grundfreiheiten der betroffenen Personen mit sich bringen, oder der, soweit der Entwurf eines Beschlusses keinen oder keinen ordnungsgemäßen Vollzug des Unionsrechts vorsieht, darlegt, dass dieser Beschluss dem Unionsrecht oder dem Recht eines Mitgliedstaates zuwiderläuft."
    },
    {
        "article_number": "4",
        "paragraph": "25",
        "literature": None,
        "text": "„Dienst der Informationsgesellschaft“ eine Dienstleistung im Sinne von Artikel 1 Nummer 2 der Richtlinie (EU) 2015/1535 des Europäischen Parlaments und des Rates."
    },
    {
        "article_number": "4",
        "paragraph": "26",
        "literature": None,
        "text": "„internationale Organisation“ eine Organisation und die ihr nachgelagerten Einrichtungen, die durch ein Abkommen zwischen zwei oder mehr Ländern oder auf der Grundlage eines solchen Abkommens geschaffen wurden."
    }
]

criteria_data = [
    {
        "short_name": "Collection and entry of personal data",
        "description": "Activities that collect or capture personal information, for example entering contact details, addresses, payment information, job applications, health information, student enrolments, membership data, tax declarations, registration forms or other forms with personally identifiable information.",
        "category_name": "Collection"
    },
    {
        "short_name": "Creation, storage and updating of records",
        "description": "Activities that create, save or update records containing personal data, such as opening customer accounts, storing order or appointment details, creating personnel files, enrolling students, setting up insurance cases or filing a medical record.",
        "category_name": "Storage"
    },
    {
        "short_name": "Transmission or disclosure of personal data",
        "description": "Activities that send, print or otherwise disclose personal data to another participant, system or third party. Examples include printing shipping labels or prescriptions, sending orders or personal data to logistics partners, pharmacies, insurers or authorities, generating payroll reports for external providers, notifying universities about student records, transmitting tax or social security data, sending confirmations or queries that rely on a person's contact details, or transferring data to non‑EU locations.",
        "category_name": "Transferal"
    },
    {
        "short_name": "Payments and financial transactions",
        "description": "Activities that process personal financial data, such as initiating or verifying payments, processing bank account or credit‑card information, executing payroll, handling reimbursements or insurance payouts, managing expense claims or collecting membership fees.",
        "category_name": "Transferal"
    },
    {
        "short_name": "Use of health, biometric or other special categories of data",
        "description": "Activities that handle medical diagnoses, prescriptions, insurance claims, disability information, photos of damages or patients, biometric identifiers (fingerprints, facial images, voice), racial or ethnic data, political opinions, religious beliefs or union membership. Processing these 'special categories' always triggers GDPR relevance.",
        "category_name": "Usage"
    },
    {
        "short_name": "Audio/Video and communications",
        "description": "Activities that initiate or join audio or video calls, record calls or meetings, capture surveillance footage, or communicate directly with a data subject via email, chat, SMS or other channels. Simply using a person's contact data to send reminders, marketing messages or notifications is processing.",
        "category_name": "Usage"
    },
    {
        "short_name": "Profiling, scoring and decision‑making",
        "description": "Activities that analyse or evaluate a person's performance, behaviour or characteristics for purposes such as credit scoring, hiring, admissions, insurance underwriting, marketing segmentation, customer value analysis or automated decision‑making.",
        "category_name": "Usage"
    },
    {
        "short_name": "Logging, tracking and location data",
        "description": "Activities that log user activity, record access or usage data, track geolocation (e.g. telematics, fleet or mobile tracking), monitor attendance or timekeeping, or collect IP addresses or device identifiers.",
        "category_name": "Access"
    },
    {
        "short_name": "Consent and data‑subject rights",
        "description": "Activities that obtain, record or manage consent; respond to requests for access, rectification, restriction, erasure, data portability or objections; or document lawful bases for processing.",
        "category_name": "Access"
    },
    {
        "short_name": "Deletion, anonymisation or pseudonymisation",
        "description": "Activities that erase, anonymise or pseudonymise personal data, even if the goal is to remove identifiers, because these operations manipulate personal data.",
        "category_name": "Deletion"
    }
]

reason_mapping = {

    "Collects personal data – gathers information about natural persons.": [
        "Collection"
    ],

    " Collects personal data – gathers information about natural persons (PersonalID, departmentID, Time schedule)": [
        "Collection"
    ],

    "Collects personal data – gathers information about natural persons (e.g., name, email).": [
        "Collection"
    ],

    "Collects personal data – gathers information about natural persons (speech).": [
        "Collection"
    ],

    "Collects personal health data – gathers health information about natural persons.": [
        "Collection"
    ],

    "Collects personal health data – gathers health information about natural persons.\nStores personal data – saves information in databases, systems, or files.": [
        "Collection",
        "Storage"
    ],

    "Patient transport data is filled into a form which includes personal data such as weight, height, name, age etc": [
        "Collection"
    ],

    "Stores personal data – saves information in databases, systems, or files.": [
        "Storage"
    ],

    " Stores personal data – saves information in databases, systems, or files.": [
        "Storage"
    ],

    "Stores personal data – saves information in databases, systems, or files.\n    ": [
        "Storage"
    ],

    "Stores personal data – saves information in databases, systems, or files.\nAccesses personal data – retrieves or makes data available to users or systems.": [
        "Storage",
        "Access"
    ],

    "Storage of personal employment related data": [
        "Storage"
    ],

    "Stores personal data – saves information in databases, systems, or files. If the customer is a natural person or the organization has a contact person.": [
        "Storage"
    ],

    "Uses personal data – processes data for operational, analytical, or business purposes.": [
        "Usage"
    ],

    "ses personal data – processes data for operational, analytical, or business purposes.": [
        "Usage"
    ],

    "Uses personal data – processes data for operational, analytical, or business purposes\n": [
        "Usage"
    ],

    "Uses personal data – processes data for operational, analytical, or business purposes. If the customer is a natural person or the organization has a contact person.": [
        "Usage"
    ],

    "Uses personal data – processes data for operational, analytical, or business purposes. If the customer is a natural person or a n organization with a contact person. If the customer is a natural person or a n organization with a contact person.": [
        "Usage"
    ],

    "Uses personal data – processes data for operational, analytical, or business purposes": [
        "Usage"
    ],

    "Uses personal data (to decide medication), Stores personal data (in medical records)": [
        "Usage",
        "Storage"
    ],

    "Uses personal data – processes data for operational, analytical, or business purposes.\nCollects personal health data – gathers health information about natural persons.": [
        "Usage",
        "Collection"
    ],

    "Transfers personal data – shares data internally, with third parties, or externally": [
        "Transferal"
    ],

    "Transfers personal data – shares data internally, with third parties, or externally.": [
        "Transferal"
    ],

    "ransfers personal data – shares data internally, with third parties, or externally.": [
        "Transferal"
    ],

    "Submit personal data to another institution (court)": [
        "Transferal"
    ],

    "Modifies personal data – updates, corrects, or changes stored data.": [
        "Modification"
    ],

    "Modifies personal data – updates, corrects, or changes stored data.\n    ": [
        "Modification"
    ],

    "Accesses personal data – updates, corrects, or changes stored data.\n    ": [
        "Modification"
    ],

    "Deletes personal data – removes or anonymizes data from storage.": [
        "Deletion"
    ],

    "Accesses personal data – retrieves or makes data available to users or systems": [
        "Access"
    ],

    "Accesses personal data – retrieves or makes data available to users or systems.": [
        "Access"
    ],

    "ccesses personal data – retrieves or makes data available to users or systems.": [
        "Access"
    ],

    "Needs access to patients medical test results which include personal medical data": [
        "Access"
    ],

    "Access customer's financial data": [
        "Access"
    ],

    "Precise information, including patient age, gender, Accesses personal data – retrieves or makes data available to users or systems.": [
        "Access"
    ]
}

connection = sqlite3.connect(DB_PATH)
cursor = connection.cursor()

try:
    cursor.execute("PRAGMA foreign_keys = OFF")

    cursor.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name='gdpr_articles'")
    row = cursor.fetchone()
    if row:
        create_sql = row[0]
        if "UNIQUE(article_number)" in create_sql or "UNIQUE (article_number)" in create_sql:
            cursor.execute("ALTER TABLE gdpr_articles RENAME TO gdpr_articles_old")
            cursor.execute("""
                CREATE TABLE gdpr_articles (
                    id INTEGER PRIMARY KEY,
                    article_number TEXT NOT NULL,
                    paragraph TEXT,
                    literature TEXT,
                    text TEXT NOT NULL,
                    UNIQUE(article_number, paragraph, literature)
                )
            """)
            cursor.execute("""
                INSERT INTO gdpr_articles (id, article_number, paragraph, literature, text)
                SELECT id, article_number, paragraph, literature, text FROM gdpr_articles_old
            """)
            cursor.execute("DROP TABLE gdpr_articles_old")

    for cat in categories:
        cursor.execute("INSERT OR IGNORE INTO category (name) VALUES (?)", (cat,))
    connection.commit()

    for article in articles_data:
        cursor.execute(
            "INSERT OR IGNORE INTO gdpr_articles (article_number, paragraph, literature, text) VALUES (?, ?, ?, ?)",
            (article["article_number"], article["paragraph"], article["literature"], article["text"])
        )
    connection.commit()

    cursor.execute("SELECT id, name FROM category")
    category_map = {row[1]: row[0] for row in cursor.fetchall()}

    for crit in criteria_data:
        cat_id = category_map.get(crit["category_name"])
        if cat_id is None:
            continue
        cursor.execute(
            "INSERT OR IGNORE INTO gdpr_criteria (category_id, short_name, description) VALUES (?, ?, ?)",
            (cat_id, crit["short_name"], crit["description"])
        )
    connection.commit()

    cursor.execute("PRAGMA foreign_keys = ON")

    cursor.execute("SELECT id, name FROM category")
    category_map = {row[1]: row[0] for row in cursor.fetchall()}

    for reason_text, category_names_for_reason in reason_mapping.items():

        if not reason_text.strip():
            continue

        cursor.execute(
            """
            INSERT
            OR IGNORE INTO reason (reason)
                VALUES (?)
            """,
            (reason_text,)
        )

        cursor.execute(
            """
            SELECT id
            FROM reason
            WHERE reason = ?
            """,
            (reason_text,)
        )

        reason_row = cursor.fetchone()

        if reason_row is None:
            continue

        reason_id = reason_row[0]

        for category_name in category_names_for_reason:

            category_id = category_map.get(category_name)

            if category_id is None:
                print(
                    f"️ Kategorie nicht gefunden: {category_name}"
                )
                continue

            cursor.execute(
                """
                INSERT
                OR IGNORE INTO category_reason_association
                    (category_id, reason_id)
                    VALUES (?, ?)
                """,
                (category_id, reason_id)
            )

    connection.commit()


except Exception as e:
    connection.rollback()
    print(f"Fehler: {e}")

finally:
    connection.close()