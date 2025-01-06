"use client";

export default function DocumentList({ data }: { data: any }) {
    console.log("data", data);
    if (data) {
        return data.map((document: any) => <div key={document.kwargs.metadata._id}>{document.kwargs.page_content}</div>);
    }
    return null;
}