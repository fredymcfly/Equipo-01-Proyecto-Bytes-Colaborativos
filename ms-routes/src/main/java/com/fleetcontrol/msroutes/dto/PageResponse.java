package com.fleetcontrol.msroutes.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page with structure required by the common contract
 * @param content
 * @param page
 * @param size
 * @param totalElements
 * @param totalPages
 * @param <T>
 */
public record PageResponse<T>(
  List<T> content,
  int page,
  int size,
  long totalElements,
  int totalPages
) {


  public static <T> PageResponse<T> from(Page<T> page){
    return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
    );
  }
}

/*
* I use this PageResponse because serializing Spring's Page object directly produces a different structure,
* while the contract requires content, page, size, totalElements, and totalPages.
* The response follows the example provided in the PDF, so it does not expose createdBy, createdAt, or updatedAt.
* */