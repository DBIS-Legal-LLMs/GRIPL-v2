import CustomEndpointListItem from "@/components/custom-endpoints/custom-endpoint-list-item"
import {CustomAnalysisEndpoint} from "@/models/dto/CustomAnalysisEndpoint"

interface CustomEndpointListProps {
    endpoints: CustomAnalysisEndpoint[];
}

export default function CustomEndpointList({endpoints}: CustomEndpointListProps) {
    if (endpoints.length === 0) {
        return <p className="text-sm text-muted-foreground">No custom analysis endpoints uploaded yet.</p>
    }

    return <div className="flex flex-col gap-4">
        {endpoints.map(endpoint => (
            <CustomEndpointListItem endpoint={endpoint} key={endpoint.id}/>
        ))}
    </div>
}
