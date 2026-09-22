"use server"

export default async function deleteProcessModel(id: number) {

    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/process-models/${id}`, {
        method: "DELETE",
    });

    if (!response.ok) {
        throw new Error(`Error deleting process model: ${response.statusText}`);
    }

    return;
}
