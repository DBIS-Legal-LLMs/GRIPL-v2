import pandas as pd

class DataLoader:

    def __init__(self,
                 file_path: str):
        self.file_path=file_path

    def load_evaluation_data_set_as_pd(self, )-> pd.DataFrame:
        return pd.read_csv(self.file_path)