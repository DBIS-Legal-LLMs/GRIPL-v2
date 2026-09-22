"use server"

export default async function deleteCustomEndpoint(id: number) {

    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/custom-analysis-endpoints/${id}`, {
        method: "DELETE",
    });

    if (!response.ok) {
        throw new Error(`Error deleting custom analysis endpoint: ${response.statusText}`);
    }

    return;
}
