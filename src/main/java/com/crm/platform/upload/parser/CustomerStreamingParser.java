package com.crm.platform.upload.parser;

import java.io.InputStream;
import java.util.function.Consumer;

public interface CustomerStreamingParser {
    void parse(InputStream inputStream, Consumer<ParsedRow> rowConsumer) throws Exception;
}
