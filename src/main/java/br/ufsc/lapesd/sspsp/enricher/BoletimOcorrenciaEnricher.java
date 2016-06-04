package br.ufsc.lapesd.sspsp.enricher;

import br.com.ivansalvadori.ssp.sp.cleaner.BoletimOcorrencia;
import br.ufsc.lapesd.sspsp.sink.EnrichmentSink;

import java.io.File;
import java.io.IOException;

public interface BoletimOcorrenciaEnricher<Enrichment> extends AutoCloseable {
    Enrichment enrich(BoletimOcorrencia bo) throws Exception;
    EnrichmentSink<Enrichment> createEnrichmentSink(File output) throws IOException;
    void setCache(File location) throws IOException;
}
