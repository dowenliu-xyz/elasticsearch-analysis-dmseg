package xyz.dowenwork.npl.dmseg.elasticsearch;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenizerFactory;
import xyz.dowenwork.npl.dmseg.core.SegmenterHolder;
import xyz.dowenwork.npl.dmseg.dict.loader.DictResource;
import xyz.dowenwork.npl.dmseg.dict.loader.DictWordReaderFactory;
import xyz.dowenwork.npl.dmseg.dict.loader.RamHashedDictionaryLoader;
import xyz.dowenwork.npl.dmseg.lucene.ZkDictResource;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * <p>create at 16-11-14</p>
 *
 * @author liufl
 * @since 6.2.0.0
 */
public abstract class DmTokenizerFactory extends AbstractTokenizerFactory {
    public DmTokenizerFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        synchronized (DmSegmenterHolder.SEGMENTER_HOLDER) {
            if (DmSegmenterHolder.SEGMENTER_HOLDER.get() == null) {
                SegmenterHolder segmenterHolder = new SegmenterHolder();
                Path configDir = environment.configFile().resolve(DmIndicesAnalysisPlugin.PLUGIN_NAME);
                Path configDirInPlugin = PathUtils.get(
                        new File(
                                getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
                        ).getParent(), "config"
                ).toAbsolutePath();
                String[] extDicts = settings.getAsArray("dm.ext_dicts");
                DictWordReaderFactory wordReaderFactory = resource -> {
                    try {
                        return Files.newBufferedReader(configDir.resolve(resource),
                                Charset.defaultCharset());
                    } catch (Exception e) {
                        return Files.newBufferedReader(configDirInPlugin.resolve(resource),
                                Charset.defaultCharset());
                    }
                };
                for (String extDict : extDicts) {
                    segmenterHolder.getDictResources().add(new DictResource(extDict, "local", wordReaderFactory,
                            RamHashedDictionaryLoader.getInstance()));
                }
                String zkDictProfile = settings.get("dm.zk_dict_profile");
                boolean zk = true;
                String zkAddress = null;
                String dictPath = null;
                String zkExtDicts = null;
                if (StringUtils.isNoneBlank(zkDictProfile)) {
                    logger.debug("loading dm zk dict profile: {}", zkDictProfile);
                    Properties p = new Properties();
                    try {
                        p.load(Files.newInputStream(configDir.resolve(zkDictProfile)));
                        logger.debug("load dm zk dict profile OK: {}", zkDictProfile);
                        zkAddress = Validate.notEmpty(p.getProperty("zkAddress"), "zkAddress is essential!");
                        logger.info("zk address : {}", zkAddress);
                        dictPath = Validate.notEmpty(p.getProperty("dictPath"), "dictPath should not be empty!");
                        logger.info("zk dict ZNode path: {}", dictPath);
                        zkExtDicts = p.getProperty("extDicts");
                        logger.info("zk dicts: {}", zkExtDicts);
                    } catch (Exception e) {
                        logger.warn("load dm zk dict profile [{}] fail, zk dict support disabled.", e, zkDictProfile);
                        zk = false;
                    }
                } else {
                    logger.info("with on zk dict profile, zk dict support disabled.");
                    zk = false;
                }
                if (zk) {
                    try {
                        CuratorFramework client = CuratorFrameworkFactory.builder().connectString(zkAddress)
                                .retryPolicy(new ExponentialBackoffRetry(1000, 5)).build();
                        logger.debug("connect to ZooKeeper {} ...", zkAddress);
                        client.start();
                        Stat stat = client.checkExists().forPath(dictPath);
                        if (stat == null) {
                            logger.debug("dict path ZNode {} not exists, create.", dictPath);
                            client.create().creatingParentsIfNeeded().forPath(dictPath, "".getBytes());// 明确指定新建节点内容为空
                        }
                        for (String zkExtDict : zkExtDicts.trim().split(",")) {
                            segmenterHolder.getDictResources()
                                    .add(new ZkDictResource(zkExtDict, RamHashedDictionaryLoader.getInstance(), client,
                                            dictPath));
                        }
                    } catch (Exception e) {
                        logger.warn("与ZooKeeper交互失败，将禁用ZK字典加载", e);
                    }
                }
                segmenterHolder.getSegmenter();
                DmSegmenterHolder.SEGMENTER_HOLDER.set(segmenterHolder);
            }
        }
    }
}
