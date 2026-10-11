package com.fleetcontrol.msvehicles.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Paginated response envelope defined by the shared contract. */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, long totalPages) {

  /** Builds the envelope from a Spring Data page. */
  public static <T> PageResponse<T> from(Page<T> source) {
    return new PageResponse<>(
        source.getContent(),
        source.getNumber(),
        source.getSize(),
        source.getTotalElements(),
        source.getTotalPages());
  }
}
