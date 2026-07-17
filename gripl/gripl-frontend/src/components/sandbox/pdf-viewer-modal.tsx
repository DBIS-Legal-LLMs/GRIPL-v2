"use client";

import { useEffect, useState } from "react";
import { Document, Page, pdfjs } from "react-pdf";
import "react-pdf/dist/Page/AnnotationLayer.css";
import "react-pdf/dist/Page/TextLayer.css";

import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/toast";
import { extractErrorDetails, toErrorMessage } from "@/lib/http-error";
import { PdfLocateResponse } from "@/models/dto/AnalysisDto";

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
    "pdfjs-dist/build/pdf.worker.min.mjs",
    import.meta.url
).toString();

// Direct base URL for standalone dev (CORS is open on the RAG service);
// falls back to the /rag proxy path used behind Traefik / the Next.js rewrite.
const RAG_BASE = process.env.NEXT_PUBLIC_RAG_BASE_URL || "/rag";

interface PdfViewerModalProps {
    open: boolean;
    onClose: () => void;
    documentName: string;
    exactText: string;
}

export default function PdfViewerModal({ open, onClose, documentName, exactText }: PdfViewerModalProps) {
    const [numPages, setNumPages] = useState<number>(0);
    const [currentPage, setCurrentPage] = useState<number>(1);
    const [locateResult, setLocateResult] = useState<PdfLocateResponse | null>(null);
    const [pageWidth, setPageWidth] = useState<number>(600);
    const { showError } = useToast();

    const pdfUrl = `${RAG_BASE}/api/pdf/${encodeURIComponent(documentName)}`;

    useEffect(() => {
        if (!open) return;
        setLocateResult(null);
        setCurrentPage(1);

        if (!exactText || exactText.length < 10) return;

        fetch(`${RAG_BASE}/api/pdf/${encodeURIComponent(documentName)}/locate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: exactText }),
        })
            .then(async (res) => {
                if (!res.ok) {
                    showError("Could not locate excerpt in PDF", await extractErrorDetails(res));
                    return null;
                }
                return res.json();
            })
            .then((data: PdfLocateResponse | null) => {
                if (data) {
                    setLocateResult(data);
                    if (data.page !== null) {
                        setCurrentPage(data.page + 1);
                    }
                }
            })
            .catch((err) => {
                showError("Could not locate excerpt in PDF", toErrorMessage(err));
            });
    }, [open, documentName, exactText]);

    const handlePageLoadSuccess = (page: { width: number }) => {
        setPageWidth(page.width);
    };

    // Only show highlights when the user is viewing the exact page where the text was found
    const onTargetPage =
        locateResult?.page !== null &&
        locateResult?.page !== undefined &&
        currentPage === locateResult.page + 1;

    const highlights = onTargetPage && locateResult!.page_width > 0 ? locateResult!.rects : [];
    const scale = locateResult && locateResult.page_width > 0 ? pageWidth / locateResult.page_width : 1;

    return (
        <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
            <DialogContent className="max-w-4xl w-full h-[90vh] flex flex-col p-0 gap-0">
                <DialogHeader className="px-4 pt-4 pb-2 shrink-0">
                    <DialogTitle className="text-sm font-medium truncate">
                        {documentName.replace(/[_-]/g, " ")}
                    </DialogTitle>
                    {exactText && (
                        <blockquote className="border-l-2 border-primary/30 pl-2 italic text-xs text-muted-foreground line-clamp-2 mt-1">
                            {exactText.slice(0, 200)}
                        </blockquote>
                    )}
                </DialogHeader>

                {/* PDF render area */}
                <div className="flex-1 overflow-auto flex justify-center bg-muted/30 px-4">
                    <div className="relative">
                        <Document
                            file={pdfUrl}
                            onLoadSuccess={({ numPages: n }) => setNumPages(n)}
                            loading={<p className="text-sm text-muted-foreground p-8">Loading PDF…</p>}
                            error={<p className="text-sm text-destructive p-8">Failed to load PDF.</p>}
                        >
                            <Page
                                pageNumber={currentPage}
                                width={Math.min(window.innerWidth * 0.8, 760)}
                                onLoadSuccess={handlePageLoadSuccess}
                                renderTextLayer
                                renderAnnotationLayer={false}
                            />
                        </Document>

                        {/* Highlight overlays — only rendered on the target page */}
                        {highlights.map((r, i) => (
                            <div
                                key={i}
                                className="absolute pointer-events-none bg-yellow-300/40 border border-yellow-400/60"
                                style={{
                                    left: `${(r.x0 / locateResult!.page_width) * pageWidth}px`,
                                    top: `${(r.y0 / locateResult!.page_height) * scale * locateResult!.page_height}px`,
                                    width: `${((r.x1 - r.x0) / locateResult!.page_width) * pageWidth}px`,
                                    height: `${((r.y1 - r.y0) / locateResult!.page_height) * scale * locateResult!.page_height}px`,
                                }}
                            />
                        ))}
                    </div>
                </div>

                {/* Page navigation */}
                <div className="shrink-0 flex items-center justify-center gap-3 px-4 py-3 border-t">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                        disabled={currentPage <= 1}
                    >
                        ‹ Prev
                    </Button>
                    <span className="text-sm text-muted-foreground">
                        Page {currentPage} of {numPages || "…"}
                    </span>
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setCurrentPage((p) => Math.min(numPages, p + 1))}
                        disabled={currentPage >= numPages}
                    >
                        Next ›
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    );
}
