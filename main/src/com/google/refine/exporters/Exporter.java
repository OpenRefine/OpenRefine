package com.google.refine.exporters;


public interface Exporter {
    /**
     * @return MIME content type handled by exporter
     */
    public String getContentType();
}
