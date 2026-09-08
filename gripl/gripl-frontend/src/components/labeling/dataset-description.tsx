"use client"

import React, {useState} from "react";
import {CardDescription} from "@/components/ui/card";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";

interface DatasetDescriptionProps {
    description: string;
}

const MAX_LENGTH = 80;

export default function DatasetDescription({description}: DatasetDescriptionProps) {
    const [isOpen, setIsOpen] = useState(false);

    if (description.length <= MAX_LENGTH) {
        return <CardDescription className="line-clamp-1 max-w-md">{description}</CardDescription>;
    }

    const preview = description.slice(0, MAX_LENGTH).trimEnd() + "…";

    return <Dialog open={isOpen} onOpenChange={setIsOpen}>
        <div className="flex items-center gap-2">
            <CardDescription className="line-clamp-1 max-w-md">{preview}</CardDescription>
            <DialogTrigger asChild>
                <Button variant="link" size="sm" className="px-0 whitespace-nowrap">
                    more
                </Button>
            </DialogTrigger>
        </div>
        <DialogContent>
            <DialogHeader>
                <DialogTitle>Dataset Description</DialogTitle>
            </DialogHeader>
            <p className="text-sm text-muted-foreground">{description}</p>
        </DialogContent>
    </Dialog>;
}
