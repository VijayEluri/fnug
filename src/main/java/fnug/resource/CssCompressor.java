package fnug.resource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import andyr.compressor.AndyRYUICssCompressor;

public class CssCompressor implements Compressor {

    public byte[] compress(byte[] input) {

        if (input == null) {
            return null;
        }
        
        StringReader reader = null;
        try {
            reader = new StringReader(new String(input, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            // unlikely.
        }

        StringWriter writer = new StringWriter();

        try {
            AndyRYUICssCompressor cmp = new AndyRYUICssCompressor(reader);
            cmp.compress(writer, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            return writer.toString().getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }
}
