package com.emc.greenplum.hadoop;

import com.emc.greenplum.hadoop.plugins.HdfsFileSystem;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;
import org.xeustechnologies.jcl.JclUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: pivotal
 * Date: 7/11/12
 * Time: 2:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class HdfsPluginLoader {
    private static Map<HdfsVersion, JarClassLoader> jarCache = new ConcurrentHashMap<HdfsVersion, JarClassLoader>();
    private HdfsVersion version;

    HdfsPluginLoader(HdfsVersion version) {
        this.version = version;
    }

    public HdfsFileSystem loadPlugin() {
        if(jarCache.containsKey(version)) {
            return loadObjectFromPlugin(jarCache.get(version));
        } else {
            return loadPluginFromScratch();
        }
    }

    private HdfsFileSystem loadPluginFromScratch() {
        JarClassLoader jarClassLoader = new JarClassLoader();
        jarCache.put(version, jarClassLoader);

        return loadObjectFromPlugin(jarClassLoader);
    }

    private HdfsFileSystem loadObjectFromPlugin(JarClassLoader jarClassLoader) {
        jarClassLoader.add(getClass().getClassLoader().getResource(version.getPluginJar()));

        for(String dependency: version.getDependencies()) {
            jarClassLoader.add(getClass().getClassLoader().getResource(dependency));
        }

        JclObjectFactory objectFactory = JclObjectFactory.getInstance();
        Object hdfsObject = objectFactory.create(jarClassLoader, "com.emc.greenplum.hadoop.plugins.HdfsFileSystemImpl");

        HdfsFileSystem fileSystem = (HdfsFileSystem) JclUtils.toCastable(hdfsObject, HdfsFileSystem.class);
        fileSystem.setClassLoader(jarClassLoader);

        return fileSystem;
    }
}
