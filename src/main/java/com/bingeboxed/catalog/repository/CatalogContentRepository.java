package com.bingeboxed.catalog.repository;

import com.bingeboxed.catalog.entity.CatalogContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatalogContentRepository extends JpaRepository<CatalogContent, Long> {

    Optional<CatalogContent> findByTmdbId(Integer tmdbId);
}