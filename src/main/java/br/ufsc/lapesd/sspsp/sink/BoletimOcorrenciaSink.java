package br.ufsc.lapesd.sspsp.sink;

import br.com.ivansalvadori.ssp.sp.cleaner.BoletimOcorrencia;
import com.google.common.base.Preconditions;

import java.io.*;

public abstract class BoletimOcorrenciaSink<Enrichment> implements EnrichmentSink<Enrichment> {
    private final File dir;
    private final String suffix;

    protected BoletimOcorrenciaSink(File dir, String suffix) throws IOException {
        try {
            dir = dir.getCanonicalFile();
            Preconditions.checkArgument(!(dir.exists() && !dir.isDirectory()));
        } catch (IOException ignored) { }
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Failed to crete dir " + dir.getPath());

        this.dir = dir;
        this.suffix = suffix;
    }

    @Override
    public void offerEnrichment(BoletimOcorrencia bo, Enrichment enrichment) throws IOException {
        File file = new File(dir, bo.getIdBO() + suffix);
        try (FileOutputStream stream = new FileOutputStream(file)) {
            write(stream, enrichment);
        }
    }

    abstract protected void write(OutputStream stream, Enrichment enrichment) throws IOException;
}
