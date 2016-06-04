package br.ufsc.lapesd.sspsp.cache;

import java.io.IOException;

public interface Cache<Key, Value> {
    void save(Key key, Value object) throws Exception;
    Value fetch(Key key, Class<Value> clazz) throws Exception;
}
