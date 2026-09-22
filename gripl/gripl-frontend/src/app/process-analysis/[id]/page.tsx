import {notFound} from "next/navigation";
import getProcessModel from "@/actions/get-process-model";
import ProcessModelDetailView from "@/components/process-analysis/process-model-detail-view";

interface ProcessModelPageProps {
    params: Promise<{ id: string }>;
}

export default async function ProcessModelPage({params}: ProcessModelPageProps) {
    const {id} = await params;
    const model = await getProcessModel(Number(id));

    if (!model) {
        notFound();
    }

    return <ProcessModelDetailView initialModel={model}/>
}
