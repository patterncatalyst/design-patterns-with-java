package com.patterncatalyst.designpatterns.ch13.shipping;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A small, named registry of {@link PackagingTemplate} exemplars. A
 * warehouse keeps one configured template per package kind and gets a new,
 * independent package by cloning the matching entry rather than by
 * re-deriving its defaults at every call site.
 */
public final class PackagingCatalog {

    private final Map<String, PackagingTemplate> templates = new HashMap<>();

    public void register(String name, PackagingTemplate exemplar) {
        templates.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(exemplar, "exemplar"));
    }

    /**
     * Clones the named exemplar and hands the caller the result. The
     * registry's own copy is never handed out and never mutated; every call
     * with the same name returns a fresh, independent {@link
     * PackagingTemplate}.
     */
    public PackagingTemplate create(String name) {
        PackagingTemplate exemplar = templates.get(name);
        if (exemplar == null) {
            throw new NoSuchElementException("PackagingCatalog: no template registered for " + name);
        }
        return exemplar.copy();
    }
}
