package br.ufsc.lapesd.sspsp.enricher.geocoding;

import br.com.ivansalvadori.ssp.sp.cleaner.BoletimOcorrencia;
import br.ufsc.lapesd.sspsp.cache.JsonCacheDir;
import br.ufsc.lapesd.sspsp.enricher.BoletimOcorrenciaEnricher;
import br.ufsc.lapesd.sspsp.enricher.NamedEnricher;
import br.ufsc.lapesd.sspsp.sink.EnrichmentSink;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@NamedEnricher("CacheFiller")
public class CacheFiller implements BoletimOcorrenciaEnricher {
    private File cacheDir = null;
    private File outputDir = null;
    private JsonCacheDir<BoletimOcorrencia, GeocodingResultContainer> cache = null;

    @Override
    public Object enrich(BoletimOcorrencia bo) throws Exception {
        if (outputDir == null || cacheDir == null) return new Object();
        File outFile = new File(outputDir, bo.getIdBO() + ".json");
        if (!outFile.exists()) return new Object();
        if (cache.fetch(bo, GeocodingResultContainer.class) != null) return new Object();

        System.out.printf("Caching %1$s -- %2$s\n", bo.getIdBO(), bo.getLocal());
        FileInputStream inputStream = new FileInputStream(outFile);
        InputStreamReader reader = new InputStreamReader(inputStream, Charsets.UTF_8);
        GeocodingResultContainer container;
        container = new Gson().fromJson(reader, GeocodingResultContainer.class);
        cache.save(bo, container);
        return new Object();
    }

    @Override
    public EnrichmentSink createEnrichmentSink(File output) throws IOException {
        this.outputDir = output;
        return (bo, o) -> { /* no op */ };
    }

    @Override
    public void setCache(File location) throws IOException {
        Preconditions.checkArgument(location.exists() && location.isDirectory());
        this.cacheDir = location;

        cache = new JsonCacheDir<>(location, bo -> {
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException ignored) {}
            assert md != null;

            byte[] digest = md.digest(bo.getLocal().getBytes());
            return Base64.getEncoder().encodeToString(digest).replace('/', '=');
        });
    }

    @Override
    public void close() throws Exception {

    }
}
