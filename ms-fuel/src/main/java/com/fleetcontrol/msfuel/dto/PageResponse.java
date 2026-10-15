package com.fleetcontrol.msfuel.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Página de resultados con los campos del contrato común.
 *
 * <p>No se devuelve {@link Page} directamente porque serializa {@code number} y metadatos de más.
 *
 * @param content elementos de la página
 * @param page número de página, desde cero
 * @param size tamaño de la página
 * @param totalElements total de elementos de la consulta
 * @param totalPages total de páginas
 * @param <T> tipo de cada elemento
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  /** Proyecta una página de Spring Data al contrato común. */
  public static <T> PageResponse<T> from(Page<T> source) {
    return new PageResponse<>(
        source.getContent(),
        source.getNumber(),
        source.getSize(),
        source.getTotalElements(),
        source.getTotalPages());
  }
}
