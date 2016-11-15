package xyz.dowenwork.npl.dmseg.elasticsearch;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.AttributeFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import xyz.dowenwork.npl.dmseg.lucene.DmTokenizer;
import xyz.dowenwork.npl.dmseg.lucene.DmTokenizerType;

/**
 * <p>create at 16-11-15</p>
 *
 * @author liufl
 * @since 5.0.0.0
 */
public class DmQueryTokenizerFactory extends DmTokenizerFactory {
    public DmQueryTokenizerFactory(IndexSettings indexSettings,
            Environment environment, String name,
            Settings settings) {
        super(indexSettings, environment, name, settings);
    }

    @Override
    public Tokenizer create() {
        return new DmTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY, DmTokenizerType.QUERY,
                DmSegmenterHolder.SEGMENTER_HOLDER.get());
    }
}
