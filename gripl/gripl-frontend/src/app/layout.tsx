import React from "react";
import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { ThemeProvider } from 'next-themes'
import "@/style/globals.css";
import "@/style/bpmn-js/bpmn-js.css";
import "@/style/bpmn-js/font-bpmn.css";
import "@/style/bpmn-js/diagram-js.css";
import {AnalysisEndpointProvider} from "@/components/providers/analysis-endpoint-provider";
import {EvaluationJobProvider} from "@/components/providers/evaluation-job-provider";
import {KgStatusBanner} from "@/components/providers/kg-status-banner";
import {ToastProvider} from "@/components/ui/toast";
import {AuthProvider} from "@/context/auth-context";
import AppShell from "@/components/navigation/app-shell";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "GRIPL",
  description: "GDPR Risk Identification in Processes using Large Language Models"
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {

  return <html lang="en" suppressHydrationWarning>
    <head>
        <title>GRIPL</title>
    </head>
    <body className={`${geistSans.variable} ${geistMono.variable} antialiased h-screen w-screen`} suppressHydrationWarning>
      <ThemeProvider
        attribute="class"
        defaultTheme="system"
        enableSystem={true}
      >
        <KgStatusBanner />
        <ToastProvider>
          <AuthProvider>
          <AnalysisEndpointProvider>
            <EvaluationJobProvider>
              <AppShell>
                {children}
              </AppShell>
            </EvaluationJobProvider>
          </AnalysisEndpointProvider>
          </AuthProvider>
        </ToastProvider>
      </ThemeProvider>
    </body>
  </html>
}
