package com.postread.services;

import com.postread.data.Tag;
import com.postread.repositories.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    @Transactional
    public Tag findOrCreateTag(String tagName) {
        String normalizedName = normalizeTagName(tagName);
        String slug = generateSlug(normalizedName);

        return tagRepository.findByName(normalizedName)
                .orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(normalizedName);
                    newTag.setSlug(slug);
                    return tagRepository.save(newTag);
                });
    }

    @Transactional
    public Set<Tag> findOrCreateTags(Set<String> tagNames) {
        return tagNames.stream()
                .map(this::findOrCreateTag)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<Tag> extractTagsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new HashSet<>();
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        Set<String> tagNames = new HashSet<>();
        while (matcher.find()) {
            tagNames.add(matcher.group(1));
        }

        return tagNames.stream()
                .limit(3)
                .map(this::findOrCreateTag)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<Tag> searchTags(String query) {
        if (query == null || query.trim().isEmpty()) {
            return tagRepository.findAll().stream().limit(10).collect(Collectors.toList());
        }
        return tagRepository.searchTags(query.trim());
    }

    @Transactional(readOnly = true)
    public List<Tag> getPopularTags(int limit) {
        return tagRepository.findAll().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String normalizeTagName(String tagName) {
        return tagName.trim().toLowerCase();
    }

    private String generateSlug(String tagName) {
        return tagName.toLowerCase()
                .replaceAll("[^a-z0-9а-яё]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}