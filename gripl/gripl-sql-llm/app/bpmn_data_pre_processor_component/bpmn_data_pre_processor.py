import xml.etree.ElementTree as ET
import re
import traceback

class BPMNDataPreProcessor:

    def __init__(self,):
        self.activity_types = {
            "task",
            "userTask",
            "serviceTask",
            "manualTask",
            "scriptTask",
            "businessRuleTask",
            "sendTask",
            "receiveTask",
            "callActivity",
            "subProcess"
        }


    def get_only_activity_field_of_bpmn_file(self,
                                             bpmn_content,
                                             ):
        try:
            root = ET.fromstring(bpmn_content)

            activities = []

            for element in root.iter():

                tag_name = element.tag.split("}")[-1]

                if tag_name in self.activity_types:
                    activity = ET.tostring(
                        element,
                        encoding="unicode"
                    )

                    activities.append(activity)
            return activities
        except Exception as e:
            print(traceback.format_exc())
            print(e)
            return []

    def get_sid_from_activity_field_of_bpmn_file(self,
                                                 activity_field,
                                                 )->str:
        try:

            match = re.search(r'id="(sid-[^"]+)"', activity_field)
            if match:
                return match.group(1)
            return ""


        except Exception as e:
            print(e)
            return ""

    def get_name_from_activity_field_of_bpmn_file(
            self,
            activity_field,
    ) -> str:
        try:
            match = re.search(r'name="([^"]*)"', activity_field)

            if match:
                return match.group(1)

            return ""

        except Exception as e:
            print(e)
            return ""


