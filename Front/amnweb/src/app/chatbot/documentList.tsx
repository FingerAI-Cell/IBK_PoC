"use client";
import { DocumentCard, DocumentMetadata } from "./DocumentCard";

interface RetrievedDocument {
  kwargs: {
    metadata: DocumentMetadata;
    page_content: string;
  };
}

export default function DocumentList({ data }: { data: RetrievedDocument[] | null }) {
  if (!data) return null;
  
  return (
    <div className="space-y-4">
      {data.map((document) => (
        <div key={`${document.kwargs.metadata.file_name}-${document.kwargs.metadata.page_number}`}>
          <DocumentCard metadata={document.kwargs.metadata} />
          <div className="mt-2 p-4 bg-gray-50 rounded">
            <p className="text-sm text-gray-700">{document.kwargs.page_content}</p>
          </div>
        </div>
      ))}
    </div>
  );
}