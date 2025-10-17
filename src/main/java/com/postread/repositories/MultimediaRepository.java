package com.postread.repositories;

import com.postread.data.Multimedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultimediaRepository extends JpaRepository<Multimedia, Long> {
    List<Multimedia> findByArticleId(Long articleId);
    void deleteByArticleId(Long articleId);
}
