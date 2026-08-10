"use client"

import { useEffect, useState } from "react"
import dynamic from "next/dynamic"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import type ApexCharts from "apexcharts"
import {AggregatedEvaluationResults} from "@/models/evaluation/AggregatedEvaluationResult";
import ChartMenu from "@/components/evaluation/charts/common/chart-menu";

const Chart = dynamic(() => import("react-apexcharts"), { ssr: false })

interface TestcaseResultsStackedProps {
    aggregatedEvaluationResults: AggregatedEvaluationResults,
    repetisions: number
}

export default function TestcaseResultsStacked({ aggregatedEvaluationResults, repetisions }: TestcaseResultsStackedProps) {
    const [isClient, setIsClient] = useState(false)
    const [customColors, setCustomColors] = useState({
        passed: "#22c55e",
        failed: "#ef4444",
        error: "#fb923c",
    })
    const foregroundColor = "hsl(var(--foreground))"
    const mutedForegroundColor = "hsl(var(--muted-foreground))"
    const borderColor = "hsl(var(--border))"

    useEffect(() => {
        setIsClient(true)
    }, [])

    const chartId = `testcase-results-stacked-chart`

    const categories = Object.keys(aggregatedEvaluationResults)
    const passedData = categories.map((name) => aggregatedEvaluationResults[name].avgPassed)
    const failedData = categories.map((name) => aggregatedEvaluationResults[name].avgFailed)
    const errorData = categories.map((name) => aggregatedEvaluationResults[name].avgErrors)

    const series = [
        {
            name: "Passed",
            data: passedData,
        },
        {
            name: "Failed",
            data: failedData,
        },
        {
            name: "Error",
            data: errorData,
        },
    ]

    const options: ApexCharts.ApexOptions = {
        chart: {
            id: chartId,
            type: "bar",
            height: 500,
            stacked: true,
            toolbar: { show: false },
            animations: { enabled: false}
        },
        plotOptions: {
            bar: {
                horizontal: false,
                borderRadius: 0,
                dataLabels: {
                    total: {
                        enabled: true,
                        style: {
                            fontSize: "10px",
                            fontWeight: 600,
                            color: foregroundColor,
                        },
                    },
                },
            },
        },
        colors: [customColors.passed, customColors.failed, customColors.error],
        dataLabels: {
            enabled: true,
            style: {
                colors: [foregroundColor],
            },
        },
        xaxis: {
            categories,
            labels: {
                rotate: -42,
                rotateAlways: true,
                style: { fontSize: "10px", colors: mutedForegroundColor },
            },
            axisBorder: { show: true, color: borderColor },
            axisTicks: { show: true },
        },
        yaxis: {
            title: {
                text: "Mean number of test cases",
                style: { fontSize: "10px", fontWeight: 400, color: mutedForegroundColor },
            },
            labels: { style: { fontSize: "10px", colors: mutedForegroundColor } },
            axisBorder: { show: true, color: borderColor },
        },
        grid: {
            borderColor,
            strokeDashArray: 4,
            xaxis: { lines: { show: false } },
            yaxis: { lines: { show: true } },
        },
        legend: {
            position: "right",
            offsetY: 40,
            labels: {
                colors: foregroundColor,
            },
            markers: {
                shape: "square",
            },
        },
        tooltip: {
            y: {
                formatter: (val) => val.toFixed(1),
            },
        },
    }

    if (!isClient) {
        return (
            <Card>
                <CardHeader>
                    <CardTitle>Test results per model</CardTitle>
                    <CardDescription>Number of passed, failed and erroring test cases acrocc {repetisions} runs</CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="h-[300px] flex items-center justify-center">
                        <p className="text-muted-foreground">Loading chart...</p>
                    </div>
                </CardContent>
            </Card>
        )
    }

    return (
        <>
            <Card>
                <CardHeader>
                    <div className="flex items-start justify-between">
                        <div>
                            <CardTitle>Test results per model</CardTitle>
                            <CardDescription>Number of passed, failed and erroring test cases across {repetisions} runs</CardDescription>
                        </div>
                        <ChartMenu chartId={chartId} />
                    </div>
                </CardHeader>
                <CardContent>
                    <Chart options={options} series={series} type="bar" height={300} />
                </CardContent>
            </Card>
        </>
    )
}
