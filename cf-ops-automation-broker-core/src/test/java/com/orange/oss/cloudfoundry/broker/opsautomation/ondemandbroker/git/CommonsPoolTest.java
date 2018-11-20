package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.eclipse.jgit.api.Git;
import org.junit.Test;

public class CommonsPoolTest {

    @Test
    public void test() throws Exception {
        KeyedPooledObjectFactory<Context, Git> factory = new KeyedPooledObjectFactory<Context, Git>() {
            @Override
            public PooledObject<Git> makeObject(Context key) throws Exception {
                return null;
            }

            @Override
            public void destroyObject(Context key, PooledObject<Git> p) throws Exception {

            }

            @Override
            public boolean validateObject(Context key, PooledObject<Git> p) {
                return false;
            }

            @Override
            public void activateObject(Context key, PooledObject<Git> p) throws Exception {

            }

            @Override
            public void passivateObject(Context key, PooledObject<Git> p) throws Exception {

            }
        };
        Context ctx = new Context();
        KeyedObjectPool<Context, Git> pool =  new GenericKeyedObjectPool<Context, Git>(factory);
        Git git = pool.borrowObject(ctx);
    }
}
