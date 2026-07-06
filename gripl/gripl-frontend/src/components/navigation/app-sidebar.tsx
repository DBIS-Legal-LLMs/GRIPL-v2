"use client"

import {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarHeader,
    SidebarMenu, SidebarMenuButton,
    SidebarMenuItem
} from "@/components/ui/sidebar";
import Link from "next/link";
import {ChartBarDecreasing, Tag, Workflow} from "lucide-react";
import React, {ReactNode} from "react";
import Image from "next/image";
import {Switch} from "@/components/ui/switch";
import {Label} from "@/components/ui/label";
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider";

interface Page {
    href: string;
    label: string;
    icon: ReactNode;
}

export default function AppSidebar() {
    const {isMulticlass, setMode, backendEndpoint} = useAnalysisEndpoint();

    const pages = [
        {
            href: "/",
            label: "Sandbox",
            icon: <Workflow />
        },
        {
            href: "/labeling",
            label: "Labeling",
            icon: <Tag />
        },
        {
            href: "/evaluation",
            label: "Evaluation",
            icon: <ChartBarDecreasing />
        }
    ] as Page[]

    return <Sidebar>
        <SidebarHeader className="flex flex-row justify-start items-center space-x-2 pl-3 pt-3">
            <Image src="/logo.png" alt="GRIPL App Icon" width={100} height={100} className="w-32 h-auto" />
        </SidebarHeader>
        <SidebarContent className="p-2">
            <SidebarMenu>
                { pages.map((page) => {
                    return <SidebarMenuItem key={page.href}>
                        <SidebarMenuButton asChild>
                            <Link href={page.href}>
                                { page.icon }
                                <p>{page.label}</p>
                            </Link>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                })}
            </SidebarMenu>
        </SidebarContent>
        <SidebarFooter className="p-3 border-t">
            <div className="space-y-2">
                <div className="flex items-center justify-between gap-2">
                    <Label htmlFor="sidebar-endpoint-switch" className="text-xs font-medium">Multiclass Endpoint</Label>
                    <Switch
                        id="sidebar-endpoint-switch"
                        checked={isMulticlass}
                        onCheckedChange={(checked) => setMode(checked ? "multiclass" : "binary")}
                    />
                </div>
                <p className="text-[11px] text-muted-foreground break-all">{backendEndpoint}</p>
            </div>
        </SidebarFooter>
    </Sidebar>
}