package fnug;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fnug.config.BundleConfig;

public class DefaultResourceCollectionTest {

    protected int readLastModifiedCount;

    @Before
    public void before() {
        readLastModifiedCount = 0;
    }

    @Test
    public void testDefaultResourceCollection() throws Exception {

        Bundle bundle = makeBundle(false);

        DefaultResourceCollection c = new DefaultResourceCollection(bundle, new Resource[] {
                makeResource("test/js-resource2.js", false),
                makeResource("test/js-resource1.js", false),
                makeResource("nonexistant.js", false),
                makeResource("test/css-resource1.css", false),
                makeResource("test/css-resource2.css", false)
        }, null) {
            @Override
            protected long readLastModified() {
                readLastModifiedCount++;
                return super.readLastModified();
            }
        };

        String fullPath = c.getFullPath();

        Assert.assertEquals(-559262445, new String(c.getBytes()).hashCode());
        Assert.assertSame(c.getBytes(), c.getJs());

        Assert.assertEquals(822055981, new String(c.getCss()).hashCode());

        byte[] js = c.getJs();
        byte[] css = c.getCss();
        byte[] compressedJs = c.getCompressedJs();
        byte[] compressedCss = c.getCompressedCss();

        Assert.assertEquals("var a=function(){alert(\"this is jozt a test\")},b=function(){a()},c=function(){b()};\n",
                new String(compressedJs));
        Assert.assertSame(compressedJs, c.getCompressedJs());

        Assert.assertEquals("\n" +
                "a{color:red}\n" +
                "p{margin-top:14px 14px 14px 14px}body{background:black;color:white;font-size:14em}",
                new String(compressedCss));
        Assert.assertSame(compressedCss, c.getCompressedCss());

        Assert.assertSame(js, c.getJs());
        Assert.assertSame(css, c.getCss());

        Assert.assertEquals(1, readLastModifiedCount);

        Assert.assertFalse(c.checkModified());
        Assert.assertFalse(c.checkModified());
        Assert.assertFalse(c.checkModified());

        Assert.assertEquals(fullPath, c.getFullPath());
        
        Assert.assertEquals(1, readLastModifiedCount);

    }

    @Test
    public void testCheckModified() throws Exception {

        Bundle bundle = makeBundle(true);

        DefaultResourceCollection c = new DefaultResourceCollection(bundle, new Resource[] {
                makeResource("test/js-resource2.js", true),
                makeResource("test/js-resource1.js", false),
                makeResource("nonexistant.js", false),
                makeResource("test/css-resource1.css", false),
                makeResource("test/css-resource2.css", false)
        }, null) {
            @Override
            protected long readLastModified() {
                readLastModifiedCount++;
                return super.readLastModified();
            }
        };

        String fullPath = c.getFullPath();
        
        byte[] js = c.getJs();
        byte[] css = c.getCss();
        byte[] compressedJs = c.getCompressedJs();
        byte[] compressedCss = c.getCompressedCss();

        Assert.assertEquals(1, readLastModifiedCount);

        Assert.assertTrue(c.checkModified());
        Assert.assertTrue(c.checkModified());
        Assert.assertTrue(c.checkModified());

        Assert.assertEquals(4, readLastModifiedCount);

        Assert.assertNotSame(js, c.getJs());
        Assert.assertNotSame(css, c.getCss());
        Assert.assertNotSame(compressedJs, c.getCompressedJs());
        Assert.assertNotSame(compressedCss, c.getCompressedCss());
        
        Assert.assertFalse(fullPath.equals(c.getFullPath()));

    }

    private Resource makeResource(String path, final boolean forceModified) {
        return new DefaultResource("/", path) {
            @Override
            protected long readLastModified() {
                return forceModified ? (long) (System.currentTimeMillis() + (Math.random() * 100000)) :
                        super.readLastModified();
            }
        };
    }

    private Bundle makeBundle(final boolean checkModified) {
        return new Bundle() {

            @Override
            public BundleConfig getConfig() {
                return new BundleConfig() {

                    @Override
                    public String name() {
                        return "testbundleconfig";
                    }

                    @Override
                    public Pattern[] matches() {
                        return null;
                    }

                    @Override
                    public boolean jsLint() {
                        return false;
                    }

                    @Override
                    public String[] jsCompileArgs() {
                        return null;
                    }

                    @Override
                    public String[] files() {
                        return null;
                    }

                    @Override
                    public Resource configResource() {
                        return null;
                    }

                    @Override
                    public boolean checkModified() {
                        return checkModified;
                    }

                    @Override
                    public String basePath() {
                        return "/";
                    }
                };
            }

            @Override
            public String getName() {
                return "testbundle";
            }

            @Override
            public Resource resolve(String path) {
                return null;
            }

            @Override
            public ResourceCollection[] getResourceCollections() {
                return null;
            }
        };
    }

}