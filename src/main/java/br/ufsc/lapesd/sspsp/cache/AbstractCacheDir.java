package br.ufsc.lapesd.sspsp.cache;

import com.google.common.base.Preconditions;

import java.io.*;
import java.util.function.Function;

public abstract class AbstractCacheDir<Key, Value> implements Cache<Key, Value> {
    private final File dir;
    private final Function<Key, String> getFilename;

    protected AbstractCacheDir(File dir, Function<Key, String> getFilename) {
        Preconditions.checkNotNull(dir);
        Preconditions.checkNotNull(getFilename);
        this.dir = dir;
        this.getFilename = getFilename;
    }

    @Override
    public void save(Key key, Value value) throws IOException {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(value);

        File file = new File(dir, getFilename.apply(key));
        try (FileOutputStream stream = new FileOutputStream(file)) {
            write(stream, value);
        }
    }

    @Override
    public Value fetch(Key key, Class<Value> clazz) throws IOException {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(clazz);

        File file = new File(dir, getFilename.apply(key));
        if (!file.exists()) return null;
        try (FileInputStream stream = new FileInputStream(file)) {
            return read(stream, clazz);
        }
    }

    abstract protected Value read(InputStream stream, Class<Value> clazz) throws IOException;
    abstract protected void write(OutputStream stream, Value value) throws IOException;
}
