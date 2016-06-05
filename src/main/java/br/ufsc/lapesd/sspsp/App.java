package br.ufsc.lapesd.sspsp;

import br.com.ivansalvadori.ssp.sp.cleaner.BoletimOcorrencia;
import br.ufsc.lapesd.sspsp.enricher.BoletimOcorrenciaEnricher;
import br.ufsc.lapesd.sspsp.enricher.NamedEnricher;
import br.ufsc.lapesd.sspsp.enricher.PermanentEnricherException;
import br.ufsc.lapesd.sspsp.sink.EnrichmentSink;
import com.google.common.base.Charsets;
import com.google.common.reflect.ClassPath;
import com.google.gson.Gson;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class App {
    @Option(name = "--cache", aliases = {"-c"}, usage = "Cache location to be used by the " +
            "enricher. Contents and type (file/directory) are enricher-dependant.")
    private File cache;

    @Option(name = "--output", aliases = {"-o"}, usage = "Destination path forwarded to the " +
            "enricher. Whether it is a file or directory is enricher-dependant.")
    private File output;

    @Option(name = "--enricher", aliases = {"-e"}, required = true)
    private String enricherName;

    @Argument(required = true, usage = "List of BO json files or directories with these files")
    private File[] inputs;

    public static void main( String[] args ) {
        App app = new App();
        CmdLineParser parser = new CmdLineParser(app);
        try {
            parser.parseArgument(args);
            app.run();
        } catch (CmdLineException | PermanentEnricherException e) {
            e.printStackTrace();
        }
    }

    private void run() throws PermanentEnricherException {
        BoletimOcorrenciaEnricher enricher = setupEnricher();
        if (enricher == null) return;

        EnrichmentSink sink;
        try {
            sink = enricher.createEnrichmentSink(output);
        } catch (IOException e) {
            System.err.printf("Failed to create enrichment sink at " + output.getPath());
            e.printStackTrace();
            return;
        }

        for (File input_ : inputs) {
            File input;
            try {
                input = input_.getCanonicalFile();
            } catch (IOException e) {
                System.err.printf("Couldn't get canonical file for %1$s", input_.getPath());
                continue;
            }
            if (input.isDirectory() && input.exists()) {
                File[] files = getBOs(input);
                for (File file : files) {
                    processBO(enricher, sink, file);
                }
            } else if (input.exists()) {
                processBO(enricher, sink, input);
            }
        }
    }

    private BoletimOcorrenciaEnricher setupEnricher()  {
        BoletimOcorrenciaEnricher enricher = newEnricher(enricherName);
        if (enricher == null) return null;
        try {
            enricher.setCache(cache);
        } catch (IOException e) {
            System.err.printf("Enricher %1$s refused %2$s as cache location.\n", enricherName,
                    cache);
            e.printStackTrace();
            try {
                enricher.close();
            } catch (Exception closeEx) {
                closeEx.printStackTrace();
            }
            return null;
        }
        return enricher;
    }

    private void processBO(BoletimOcorrenciaEnricher enricher,
                           EnrichmentSink sink, File file) throws PermanentEnricherException {
        BoletimOcorrencia bo = null;
        try {
            FileInputStream stream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(stream, Charsets.UTF_8);
            bo = new Gson().fromJson(reader, BoletimOcorrencia.class);
        } catch (IOException e) {
            System.err.printf("Failed to read json from %1$s. Reason: %2$s. Skipping.\n",
                    file.getAbsolutePath(), e.getMessage());
        }
        try {
            if (bo != null) {
                Object enrichment = enricher.enrich(bo);
                if (enrichment != null)
                    sink.offerEnrichment(bo, enrichment);

            }
        } catch (PermanentEnricherException e) {
            throw e;
        } catch (Exception e) {
            System.err.printf("Exception while enriching BO %1$s: %2$s\n", bo.getIdBO(),
                    e.getMessage());
            e.printStackTrace();
        }
    }

    private File[] getBOs(File dir) {
        return dir.listFiles(new FileFilter() {
            private final Pattern pattern = Pattern.compile(".*\\.json$");

            public boolean accept(File file) {
                Matcher matcher = pattern.matcher(file.getName());
                return matcher.matches();
            }
        });
    }

    private BoletimOcorrenciaEnricher newEnricher(String enricherName) {
        List<? extends Class<?>> classes;
        try {
            classes = ClassPath.from(getClass().getClassLoader())
                    .getTopLevelClassesRecursive("br.ufsc.lapesd.sspsp.enricher")
                    .stream().map(ClassPath.ClassInfo::load).collect(Collectors.toList());
        } catch (IOException e) {
            System.err.printf("Problem scanning the classpath\n");
            e.printStackTrace();
            return null;
        }
        for (Class<?> aClass : classes) {
            NamedEnricher ann = aClass.getAnnotation(NamedEnricher.class);
            if (ann != null && ann.value().equals(enricherName)) {
                try {
                    Constructor<?> constructor = aClass.getConstructor();
                    return (BoletimOcorrenciaEnricher) constructor.newInstance();
                } catch (NoSuchMethodException e) {
                    System.err.printf("%1$s has no empty constructor\n", aClass.getName());
                    return null;
                } catch (InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
                    System.err.printf("Error instantiating the Enricher implementation\n");
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }
}
