package xyz.dowenwork.npl.dmseg.elasticsearch;

import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>create at 16-2-24</p>
 *
 * @author liufl
 * @since 1.0.0
 */
public class DmIndicesAnalysisPlugin extends Plugin implements AnalysisPlugin {
    static final String PLUGIN_NAME = "dmseg";

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> getTokenizers() {
        Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> factories = new HashMap<>();

        factories.put("dm_query", DmQueryTokenizerFactory::new);
        factories.put("dm_index", DmIndexTokenizerFactory::new);

        return factories;
    }
}
