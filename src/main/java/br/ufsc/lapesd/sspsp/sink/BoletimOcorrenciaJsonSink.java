package br.ufsc.lapesd.sspsp.sink;

import com.google.common.base.Charsets;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class BoletimOcorrenciaJsonSink<Enrichment> extends BoletimOcorrenciaSink<Enrichment> {
    public BoletimOcorrenciaJsonSink(File dir) throws IOException {
        super(dir, ".json");
    }

    @Override
    protected void write(OutputStream stream, Enrichment enrichment) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(stream, Charsets.UTF_8)) {
            new Gson().toJson(enrichment, writer);
        }
    }
}
