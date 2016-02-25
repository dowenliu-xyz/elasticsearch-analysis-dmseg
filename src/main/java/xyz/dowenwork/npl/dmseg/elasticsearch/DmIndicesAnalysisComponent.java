package xyz.dowenwork.npl.dmseg.elasticsearch;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.AttributeFactory;
import org.apache.zookeeper.data.Stat;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.PreBuiltTokenizerFactoryFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import xyz.dowenwork.npl.dmseg.core.SegmenterHolder;
import xyz.dowenwork.npl.dmseg.dict.loader.DictResource;
import xyz.dowenwork.npl.dmseg.dict.loader.DictWordReaderFactory;
import xyz.dowenwork.npl.dmseg.dict.loader.RamHashedDictionaryLoader;
import xyz.dowenwork.npl.dmseg.lucene.DmTokenizer;
import xyz.dowenwork.npl.dmseg.lucene.DmTokenizerType;
import xyz.dowenwork.npl.dmseg.lucene.ZkDictResource;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * <p>create at 16-2-24</p>
 *
 * @author liufl
 * @since 1.0.0
 */
public class DmIndicesAnalysisComponent extends AbstractComponent {
    private static final SegmenterHolder segmenterHolder = new SegmenterHolder();

    @Inject
    public DmIndicesAnalysisComponent(final Settings settings, IndicesAnalysisService indicesAnalysisService) {
        super(settings);
        URL jarLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
        final Path pluginHome = Paths.get(jarLocation.getPath()).getParent();
        String[] extDicts = settings.getAsArray("dm.ext_dicts");
        String zkDictProfile = settings.get("dm.zk_dict_profile");
        boolean zk = true;
        String zkAddress = null;
        String dictPath = null;
        String zkExtDicts = null;
        if (StringUtils.isNoneBlank(zkDictProfile)) {
            logger.debug("loading dm zk dict profile: {}", zkDictProfile);
            Properties p = new Properties();
            try {
                p.load(Files.newInputStream(pluginHome.resolve("config/" + zkDictProfile)));
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
        DictWordReaderFactory wordReaderFactory = new DictWordReaderFactory() {
            @Override
            public Reader createReader(String resource) throws IOException {
                return Files.newBufferedReader(pluginHome.resolve("config/" + resource), Charset.defaultCharset());
            }
        };
        for (String extDict : extDicts) {
            segmenterHolder.getDictResources().add(new DictResource(extDict, "local", wordReaderFactory,
                    RamHashedDictionaryLoader.getInstance()));
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
        indicesAnalysisService.tokenizerFactories().put("dm_query", new PreBuiltTokenizerFactoryFactory(
                new TokenizerFactory() {
                    @Override
                    public String name() {
                        return "dm_query";
                    }

                    @Override
                    public Tokenizer create() {
                        return new DmTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY, DmTokenizerType.QUERY,
                                segmenterHolder);
                    }
                }));

        indicesAnalysisService.tokenizerFactories().put("dm_index", new PreBuiltTokenizerFactoryFactory(
                new TokenizerFactory() {
                    @Override
                    public String name() {
                        return "dm_index";
                    }

                    @Override
                    public Tokenizer create() {
                        return new DmTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY, DmTokenizerType.INDEX,
                                segmenterHolder);
                    }
                }));
    }
}
