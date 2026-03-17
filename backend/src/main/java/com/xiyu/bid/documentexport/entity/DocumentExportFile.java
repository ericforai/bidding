package com.xiyu.bid.documentexport.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "document_export_files", indexes = {
        @Index(name = "idx_document_export_file_export", columnList = "export_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentExportFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "export_id", nullable = false, unique = true)
    private Long exportId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}
