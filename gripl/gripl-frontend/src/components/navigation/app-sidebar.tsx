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
import {ChartBarDecreasing, LogIn, LogOut, Tag, User, Workflow} from "lucide-react";
import React, {ReactNode} from "react";
import Image from "next/image";
import {Label} from "@/components/ui/label";
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select";
import {useAuth} from "@/context/auth-context";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface Page {
    href: string;
    label: string;
    icon: ReactNode;
}

export default function AppSidebar() {
    const {
        selectedEndpoint,
        setSelectedEndpoint,
        availableEndpoints,
        backendEndpoint,
    } = useAnalysisEndpoint();

    const { logout, token, username } = useAuth();

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
        <SidebarFooter className="px-3 pt-1 pb-6 border-t space-y-3">
            <div className="space-y-2">
                <div className="space-y-1">
                    <Label className="text-xs font-medium">Global Analysis Endpoint</Label>
                    <div className="w-[250px] max-w-full overflow-hidden">
                        <Select value={selectedEndpoint} onValueChange={setSelectedEndpoint}>
                            <SelectTrigger className="h-8 w-full overflow-hidden text-xs">
                                <SelectValue placeholder="Select endpoint" />
                            </SelectTrigger>
                            <SelectContent className="w-[250px] max-w-[250px]">
                                {availableEndpoints.map((endpoint) => (
                                    <SelectItem key={endpoint.endpoint} value={endpoint.endpoint} className="max-w-[242px]">
                                        <span className="block w-full truncate">{endpoint.name}</span>
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                </div>
                <p className="text-[11px] text-muted-foreground break-all">{backendEndpoint}</p>
            </div>

            {/* Auth row — independent of the analysis-endpoint controls above.
                Logged out: a link to log in / register. Logged in: the username,
                opening a menu with Log out. */}
            <SidebarMenu>
                <SidebarMenuItem>
                    {token ? (
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <SidebarMenuButton>
                                    <User />
                                    <p className="truncate">{username ?? "Account"}</p>
                                </SidebarMenuButton>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent side="top" align="start" className="w-[--radix-popper-anchor-width]">
                                <DropdownMenuLabel className="truncate">{username ?? "Signed in"}</DropdownMenuLabel>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem onClick={logout}>
                                    <LogOut />
                                    Log out
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    ) : (
                        <SidebarMenuButton asChild>
                            <Link href="/login">
                                <LogIn />
                                <p>Log in / Register</p>
                            </Link>
                        </SidebarMenuButton>
                    )}
                </SidebarMenuItem>
            </SidebarMenu>
        </SidebarFooter>
    </Sidebar>
}
