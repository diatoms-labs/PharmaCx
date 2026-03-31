package com.pharmaCx.dms.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = "document_number_sequences")
public class DocumentNumberSequence {

    @Id
    private String id;

    @Indexed(unique = true)
    private String key; // e.g. "SOP-QA"

    private long currentNumber;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getCurrentNumber() {
        return currentNumber;
    }

    public void setCurrentNumber(long currentNumber) {
        this.currentNumber = currentNumber;
    }

    @Override
    public String toString() {
        return "DocumentNumberSequence{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", currentNumber=" + currentNumber +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentNumberSequence that = (DocumentNumberSequence) o;
        return currentNumber == that.currentNumber &&
                Objects.equals(id, that.id) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, key, currentNumber);
    }
}
