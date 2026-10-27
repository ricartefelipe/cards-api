package com.altbank.cardsapi.application;

/**
 * Pagina de consulta normalizada: page base 0, size limitado a um teto para evitar
 * alocacao ilimitada. Valores invalidos sao corrigidos para defaults seguros.
 */
public record PageRequest(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
    }

    public static PageRequest of(Integer page, Integer size) {
        return new PageRequest(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    public int offset() {
        return page * size;
    }

    public int limit() {
        return size;
    }
}
