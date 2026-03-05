import pandas as pd
import os

def extract_bpmn_from_csv(csv_path, output_directory="extracted_models"):
    # Erstellt den Zielordner, falls er noch nicht existiert
    if not os.path.exists(output_directory):
        os.makedirs(output_directory)
        print(f"Ordner '{output_directory}' wurde erstellt.")

    # CSV laden
    try:
        df = pd.read_csv(csv_path)
    except Exception as e:
        print(f"Fehler beim Laden der CSV: {e}")
        return

    # Check, ob die Spalten existieren
    required_columns = ['name', 'bpmn_xml']
    if not all(col in df.columns for col in required_columns):
        print(f"Fehler: Die CSV muss die Spalten {required_columns} enthalten.")
        return

    for index, row in df.iterrows():
        filename = f"{row['name']}.bpmn"
        xml_content = row['bpmn_xml']
        
        # Pfad säubern (falls der Name ungültige Zeichen für Dateisysteme enthält)
        filename = "".join([c for c in filename if c.isalnum() or c in (' ', '.', '_')]).strip()
        file_path = os.path.join(output_directory, filename)

        # XML in Datei schreiben
        try:
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(str(xml_content))
            print(f"Gespeichert: {filename}")
        except Exception as e:
            print(f"Fehler beim Speichern von {filename}: {e}")

# Skript starten
if __name__ == "__main__":
    # Ermittelt das Verzeichnis, in dem dieses Skript liegt
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # Verbindet den Ordnerpfad mit deinem Dateinamen
    csv_filename = "evaluation_data.csv" # <--- Hier GENAU prüfen (Groß-/Kleinschreibung!)
    full_path = os.path.join(script_dir, csv_filename)
    
    extract_bpmn_from_csv(full_path)