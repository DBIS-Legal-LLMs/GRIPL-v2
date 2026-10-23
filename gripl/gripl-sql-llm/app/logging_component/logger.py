import traceback
from pathlib import Path
import csv


class Logger:

    def __init__(self, file_name: str):
        self.log_directory = Path(__file__).parents[1] / "eval"
        self.file_name = file_name

    def log(self,
            user_prompt: str,
            system_prompt: str,
            answer,
            ):

        try:
            file_path = self.log_directory / self.file_name
            file_path.parent.mkdir(parents=True, exist_ok=True)
            file_exists = file_path.exists()

            with open(
                    file_path,
                    mode="a",
                    newline="",
                    encoding="utf-8"
            ) as csv_file:

                writer = csv.writer(csv_file)

                if not file_exists:
                    writer.writerow([
                        "system_prompt",
                        "user_prompt",
                        "output"
                    ])

                writer.writerow([
                    system_prompt,
                    user_prompt,
                    answer
                ])

        except Exception as e:
            print("error in logging ")
            print(traceback.format_exc())

    def log_results(self, gold_results, predicted_results):
        try:
            file_path = self.log_directory / self.file_name

            file_exists = file_path.exists()

            with open(
                    file_path,
                    mode="a",
                    newline="",
                    encoding="utf-8"
            ) as csv_file:

                writer = csv.writer(csv_file)

                if not file_exists:
                    writer.writerow([
                        "gold_results",
                        "predicted_results",
                    ])

                writer.writerow([
                    gold_results,
                    predicted_results,
                ])

        except Exception as e:
            print("error in logging ")
            print(traceback.format_exc())


