package com.fleetcontrol.msroutes.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Page response with the structure required by the common contract.
 *
 * @param <T> type of the page content
 * @param content page content
 * @param page zero-based page number
 * @param size number of elements per page
 * @param totalElements total number of elements
 * @param totalPages total number of pages
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  /**
   * Creates a response from a Spring Data page.
   *
   * @param page Spring Data page
   * @param <T> type of the page content
   * @return response containing the page data
   */
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}

/*
 * Serializing Spring's Page directly produces a different structure, while the
 * contract requires content, page, size, totalElements, and totalPages.
 */
