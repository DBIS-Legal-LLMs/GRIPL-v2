"use client"

import {usePathname} from "next/navigation";
import {generateBreadcrumbs} from "@/lib/breadcrumbs";
import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from "@/components/ui/breadcrumb";
import Link from "next/link";
import React, {useEffect, useState} from "react";
import {Crumb} from "@/models/Crumb";
import {EvaluationData} from "@/models/dto/EvaluationData";
import {authenticatedFetch} from "@/lib/authenticated-fetch";
import {
    PROCESS_MODEL_RENAMED_EVENT,
    ProcessModelDetail,
    ProcessModelRenamedDetail
} from "@/models/dto/ProcessModel";

const nameMap = {
    "process-analysis": "Process Analysis",
    labeling: "Labeling",
    thesis: "Thesis",
    "custom-endpoints": "Custom Endpoints",
    settings: "Account Settings",
}

export default function AppBreadCrumbs() {

    const pathname = usePathname()
    const [crumbs, setCrumbs] = useState<Crumb[]>(generateBreadcrumbs(pathname, nameMap))

    useEffect(() => {

        const newCrumbs = generateBreadcrumbs(pathname, nameMap);

        // If the user is on the labeling page and has a test case selected the name of the test case should be displayed instead of the id
        if (newCrumbs[0].label === nameMap["labeling"] && newCrumbs.length === 2) {
            authenticatedFetch(`/api/dataset/testcase/${newCrumbs[1].label}`).then(response => {
                if (!response.ok) {
                    throw new Error(`Failed to fetch test case with id ${newCrumbs[1].label}: ${response.statusText}`);
                }
                return response.json()
            }).then((data: EvaluationData) => {
                const updatedCrumbs = newCrumbs.map((crumb, index) => {
                    if (index === 1) {
                        return {
                            ...crumb,
                            label: `${data.name} (${data.id})`,
                        };
                    }
                    return crumb;
                });
                setCrumbs(updatedCrumbs);
            }).catch(error => {
                console.error("Error fetching dataset name:", error);
                setCrumbs(newCrumbs)
            })
            return
        }

        // If the user has a process model selected, show its name instead of the id
        if (newCrumbs[0].label === nameMap["process-analysis"] && newCrumbs.length === 2) {
            fetch(`/api/process-models/${newCrumbs[1].label}`).then(response => {
                if (!response.ok) {
                    throw new Error(`Failed to fetch process model with id ${newCrumbs[1].label}: ${response.statusText}`);
                }
                return response.json()
            }).then((data: ProcessModelDetail) => {
                const updatedCrumbs = newCrumbs.map((crumb, index) => {
                    if (index === 1) {
                        return {
                            ...crumb,
                            label: `${data.name} (${data.id})`,
                        };
                    }
                    return crumb;
                });
                setCrumbs(updatedCrumbs);
            }).catch(error => {
                console.error("Error fetching process model name:", error);
                setCrumbs(newCrumbs)
            })
            return
        }

        setCrumbs(newCrumbs)
    }, [pathname])

    // A rename in the process model detail view doesn't change the pathname,
    // so the effect above wouldn't refetch — update the crumb label directly.
    useEffect(() => {
        const handleRenamed = (event: Event) => {
            const {id, name} = (event as CustomEvent<ProcessModelRenamedDetail>).detail
            const newCrumbs = generateBreadcrumbs(pathname, nameMap)
            if (newCrumbs[0].label !== nameMap["process-analysis"] || newCrumbs.length !== 2) return
            if (newCrumbs[1].label !== String(id)) return
            setCrumbs(newCrumbs.map((crumb, index) =>
                index === 1 ? {...crumb, label: `${name} (${id})`} : crumb
            ))
        }
        window.addEventListener(PROCESS_MODEL_RENAMED_EVENT, handleRenamed)
        return () => window.removeEventListener(PROCESS_MODEL_RENAMED_EVENT, handleRenamed)
    }, [pathname])

    return <Breadcrumb>
        <BreadcrumbList>
            {crumbs.map((crumb, index) => (
                <React.Fragment key={index}>
                    <BreadcrumbItem>
                        <Link href={crumb.href}>{crumb.label}</Link>
                    </BreadcrumbItem>
                    {index < crumbs.length - 1 && <BreadcrumbSeparator />}
                </React.Fragment>
            ))}
        </BreadcrumbList>
    </Breadcrumb>
}