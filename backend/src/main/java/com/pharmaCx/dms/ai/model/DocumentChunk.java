package com.pharmaCx.dms.ai.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A chunk of text extracted from a ControlledDocument, stored with its
 * vector embedding for semantic (RAG) search.
 *
 * Indexing is ALWAYS read-only with respect to the source file — this model
 * stores derived data only. The source file on disk is never modified by the
 * AI layer.
 */
@Document(collection = "document_chunks")
@CompoundIndex(name = "doc_chunk_idx", def = "{'documentId': 1, 'chunkIndex': 1, 'source': 1}")
public class DocumentChunk {

    @Id
    private String id;

    @Indexed
    private String documentId;

    private String documentTitle;
    private String documentNumber;
    private String documentStatus;
    private String documentTypeId;
    private String unitId;
    private String source; // INTERNAL or EXTERNAL

    private int chunkIndex;

    /** Plain text of this chunk (~500 words). */
    private String text;

    /**
     * Vector embedding produced by nomic-embed-text (768 dimensions).
     * Used for cosine similarity search at query time.
     */
    private List<Double> embedding;

    private Instant indexedAt;

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getDocumentStatus() { return documentStatus; }
    public void setDocumentStatus(String documentStatus) { this.documentStatus = documentStatus; }

    public String getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(String documentTypeId) { this.documentTypeId = documentTypeId; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<Double> getEmbedding() { return embedding; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }

    public Instant getIndexedAt() { return indexedAt; }
    public void setIndexedAt(Instant indexedAt) { this.indexedAt = indexedAt; }
}
