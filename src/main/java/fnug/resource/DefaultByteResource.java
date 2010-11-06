package fnug.resource;

import java.util.List;

public class DefaultByteResource extends AbstractResource implements HasBundle {

    private Bundle bundle;
    private byte[] bytes;
    private long lastModified;

    public DefaultByteResource(Bundle bundle, String path, byte[] bytes, long lastModified) {
        super(bundle.getName() + "/", path);
        this.bundle = bundle;
        this.bytes = bytes;
        this.lastModified = lastModified;
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    protected Entry readEntry() {
        return new Entry(readLastModified(), bytes);
    }

    @Override
    protected long readLastModified() {
        return lastModified;
    }

    @Override
    public List<String> findRequiresTags() {
        throw new UnsupportedOperationException("Can't find @requires in byte resource");
    }

}
