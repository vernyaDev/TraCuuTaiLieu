package vn.utc.tracuu.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {

    List<Chunk> findByChunkSetIdAndDocumentIdOrderByOrdinal(Long chunkSetId, Long documentId);

    /** Các đoạn liền kề quanh một đoạn — nguồn cho màn xem tài liệu quanh một trích dẫn. */
    @Query("""
           select c from Chunk c
           where c.chunkSetId = :setId and c.documentId = :docId
             and c.ordinal between :from and :to
           order by c.ordinal
           """)
    List<Chunk> findWindow(@Param("setId") Long setId, @Param("docId") Long docId,
                           @Param("from") int from, @Param("to") int to);

    @Modifying
    @Query("delete from Chunk c where c.documentId = :docId")
    int deleteByDocumentId(@Param("docId") Long docId);

    @Modifying
    @Query("delete from Chunk c where c.chunkSetId = :setId and c.documentId = :docId")
    int deleteByChunkSetAndDocument(@Param("setId") Long setId, @Param("docId") Long docId);

    long countByChunkSetId(Long chunkSetId);
}
