package fnug.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fnug.resource.AbstractAggregatedResource;
import fnug.resource.Resource;

public class AbstractAggregatedResourceTest {

    private int checkModifiedCount;

    @Before
    public void before() {
        checkModifiedCount = 0;
    }

    @Test
    public void testConstructor() {

        try {
            new TestAggResource("", "foo.js", null, null);
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // ok
        }

        try {
            new TestAggResource("/", "/foo.js", null, null);
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // ok
        }

    }

    @Test
    public void testLastModified() {

        TestAggResource r1 = new TestAggResource("/", "foo.js", new Resource[] {}, new Resource[] {
                makeResource("1", 1),
                makeResource("2", 2) });

        Assert.assertEquals(2l, r1.getLastModified());

        r1 = new TestAggResource("/", "foo.js", new Resource[] { makeResource("1", 1),
                makeResource("2", 2) }, new Resource[] { makeResource("3", 3), makeResource("4", 4) });

        Assert.assertEquals(2, checkModifiedCount);

        Assert.assertEquals(4l, r1.getLastModified());
        Assert.assertEquals(4l, r1.getLastModified());
        Assert.assertEquals(4l, r1.getLastModified());

        Assert.assertEquals(6, checkModifiedCount);

    }

    @Test
    public void testCheckModified() {

        TestAggResource r1 = new TestAggResource("/", "foo.js", new Resource[] {}, new Resource[] {
                makeResource("1", 1),
                makeResource("2", 2) });

        Assert.assertEquals(2l, r1.getLastModified());

        r1 = new TestAggResource("/", "foo.js", new Resource[] { makeResource("1", 1),
                makeResource("2", 2) }, new Resource[] { makeResource("3", 3), makeResource("4", 4) });

        Assert.assertEquals(2, checkModifiedCount);

        Assert.assertEquals(4l, r1.getLastModified());

        Assert.assertEquals(6, checkModifiedCount);
        
        Assert.assertFalse(r1.checkModified());
        Assert.assertFalse(r1.checkModified());
        Assert.assertFalse(r1.checkModified());
        
        Assert.assertEquals(18, checkModifiedCount);

    }

    @Test
    public void testBuildAggregate() {

        TestAggResource r1 = new TestAggResource("/", "foo.js", new Resource[] { makeResource("1", 1),
                makeResource("2", 2) }, new Resource[] { makeResource("3", 3), makeResource("4", 4) });

        Assert.assertEquals("12", new String(r1.getBytes()));

        Assert.assertEquals(1, r1.buildAggregateCount);

        Assert.assertEquals("12", new String(r1.getBytes()));

        Assert.assertEquals(1, r1.buildAggregateCount);

    }

    private Resource makeResource(final String path, final long lastModified) {
        return new Resource() {

            @Override
            public boolean isJs() {
                return false;
            }

            @Override
            public boolean isCss() {
                return false;
            }

            @Override
            public String getPath() {
                return path;
            }

            @Override
            public long getLastModified() {
                return lastModified;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public byte[] getBytes() {
                return path.getBytes();
            }

            @Override
            public boolean checkModified() {
                checkModifiedCount++;
                return false;
            }

            @Override
            public List<String> findRequiresTags() {
                return null;
            }

            @Override
            public String getBasePath() {
                return null;
            }

            @Override
            public String getFullPath() {
                return null;
            }
        };
    }

    private class TestAggResource extends AbstractAggregatedResource {

        private Resource[] aggregates;
        private Resource[] dependencies;
        int buildAggregateCount = 0;

        protected TestAggResource(String basePath, String path, Resource[] aggregates, Resource[] dependencies) {
            super(basePath, path);
            this.aggregates = aggregates;
            this.dependencies = dependencies;
        }

        @Override
        protected byte[] buildAggregate() {
            buildAggregateCount++;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (Resource r : aggregates) {
                try {
                    baos.write(r.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return baos.toByteArray();
        }

        @Override
        public Resource[] getAggregates() {
            return aggregates;
        }

        @Override
        public Resource[] getDependencies() {
            return dependencies;
        }

        @Override
        public List<String> findRequiresTags() {
            return null;
        }

    }

}
