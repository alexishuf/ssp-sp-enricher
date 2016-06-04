package br.ufsc.lapesd.sspsp.cache;


import com.google.common.base.Charsets;
import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Function;

public class JsonCacheDir<Key, Value> extends AbstractCacheDir<Key,Value> {
    public JsonCacheDir(File dir, Function<Key, String> getFilename) {
        super(dir, getFilename);
    }

    @Override
    protected Value read(InputStream stream, Class<Value> clazz) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream, Charsets.UTF_8)) {
            return new Gson().fromJson(reader, clazz);
        }
    }

    @Override
    protected void write(OutputStream stream, Value value) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(stream, Charsets.UTF_8)) {
            new Gson().toJson(value, writer);
        }
    }
}
