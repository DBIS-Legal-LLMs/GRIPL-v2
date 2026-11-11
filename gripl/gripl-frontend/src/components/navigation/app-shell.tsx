"use client"

import React, { ReactNode } from "react";
import { usePathname } from "next/navigation";
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import AppSidebar from "@/components/navigation/app-sidebar";
import AppBreadCrumbs from "@/components/navigation/app-breadcrumbs";
import { ThemeToggle } from "@/components/ui/theme-toggle";

interface AppShellProps {
    children: ReactNode;
}

export default function AppShell({ children }: AppShellProps) {
    const pathname = usePathname();
    const isLoginPage = pathname === "/login" || pathname?.startsWith("/login/");

    if (isLoginPage) {
        return (
            <main className="relative h-screen w-full">
                {children}
            </main>
        );
    }

    return (
        <SidebarProvider className="h-full w-full">
            <AppSidebar />
            <SidebarInset className="h-full w-full">
                <header className="h-16 flex-shrink-0 px-2 flex flex-row items-center justify-between space-x-4 bg-sidebar sticky top-0 z-10">
                    <div className="flex flex-row space-x-4 items-center">
                        <SidebarTrigger/>
                        <AppBreadCrumbs />
                    </div>
                    <ThemeToggle />
                </header>
                {children}
            </SidebarInset>
        </SidebarProvider>
    );
}
