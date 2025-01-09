"use client";
import { DocumentMetadata } from "./DocumentCard"; // DocumentMetadata 임포트

interface RetrievedDocument {
    kwargs: {
      metadata: DocumentMetadata;
      page_content: string;
    };
  }

export default function DocumentList({ data }: { data: RetrievedDocument[] | null }) {
    if (data) {
        return data.map((document) => (
          <div key={document.kwargs.metadata.file_name}>
            {document.kwargs.page_content}
          </div>
        ));
      }
      return null;
}