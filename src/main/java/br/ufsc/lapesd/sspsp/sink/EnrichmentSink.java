package br.ufsc.lapesd.sspsp.sink;

import br.com.ivansalvadori.ssp.sp.cleaner.BoletimOcorrencia;

public interface EnrichmentSink<Enrichment> {
    void offerEnrichment(BoletimOcorrencia bo, Enrichment enrichment) throws Exception;
}
