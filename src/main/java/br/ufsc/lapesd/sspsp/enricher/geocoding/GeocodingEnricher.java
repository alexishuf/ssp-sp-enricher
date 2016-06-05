package br.ufsc.lapesd.sspsp.enricher.geocoding;

import br.com.ivansalvadori.ssp.sp.cleaner.BoletimOcorrencia;
import br.ufsc.lapesd.sspsp.cache.JsonCacheDir;
import br.ufsc.lapesd.sspsp.enricher.BoletimOcorrenciaEnricher;
import br.ufsc.lapesd.sspsp.enricher.NamedEnricher;
import br.ufsc.lapesd.sspsp.enricher.PermanentEnricherException;
import br.ufsc.lapesd.sspsp.sink.BoletimOcorrenciaJsonSink;
import br.ufsc.lapesd.sspsp.sink.EnrichmentSink;
import com.google.appengine.repackaged.com.google.api.client.util.Charsets;
import com.google.appengine.repackaged.com.google.common.io.Resources;
import com.google.common.base.Preconditions;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.errors.OverQueryLimitException;
import com.google.maps.model.GeocodingResult;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@NamedEnricher("geocoding")
public class GeocodingEnricher implements BoletimOcorrenciaEnricher<GeocodingResultContainer> {
    private GeoApiContext apiContext = new GeoApiContext();
    JsonCacheDir<BoletimOcorrencia, GeocodingResultContainer> cache = null;

    public GeocodingEnricher() throws IOException {
        String key = Resources.toString(Resources.getResource("googleMapsApiKey"), Charsets.UTF_8);
        apiContext.setApiKey(key);
    }

    @Override
    public void setCache(File location) throws IOException {
        if (location == null) {
            cache = null;
        } else {
            setupCacheLocation(location);

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
    }

    private void setupCacheLocation(File location) throws IOException {
        File can = location.getCanonicalFile();
        Preconditions.checkArgument(!(can.exists() && !can.isDirectory()),
                "Location %s already exists and is not a directory, it can't be used as " +
                        "cache dir.", can.getPath());
        if (!can.exists() && !can.mkdirs())
            throw new IOException(String.format("Couldn't mkdirs %1$s", can.getPath()));
    }

    @Override
    public GeocodingResultContainer enrich(BoletimOcorrencia bo) throws Exception {
        if (bo.getLocal() == null) return null;

        if (cache != null) {
            GeocodingResultContainer cached = cache.fetch(bo, GeocodingResultContainer.class);
            if (cached != null) return cached;
        }
        GeocodingApiRequest request = GeocodingApi.geocode(apiContext, bo.getLocal());
        try {
            GeocodingResult[] results = request.await();
            GeocodingResultContainer container = new GeocodingResultContainer(results);
            cache.save(bo, container);
            return container;
        } catch (OverQueryLimitException ex) {
            throw new PermanentEnricherException(ex.getMessage(), ex);
        }
    }

    @Override
    public EnrichmentSink<GeocodingResultContainer> createEnrichmentSink(File output) throws IOException {
        return new BoletimOcorrenciaJsonSink<>(output);
    }

    @Override
    public void close() throws Exception { }
}
