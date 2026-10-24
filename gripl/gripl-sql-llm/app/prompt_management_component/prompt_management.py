class PromptManagement:

    def __init__(self,):
        pass

    def load_prompt_file(self, file_path: str) -> str:
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            return content
        except FileNotFoundError:
            return ""

    def fill_prompt(self, file_path: str, **kwargs) -> str:

        raw_prompt = self.load_prompt_file(file_path)
        try:
            return raw_prompt.format(**kwargs)
        except KeyError as e:
            print("error in schema formating")
            print(e)
            return ""
