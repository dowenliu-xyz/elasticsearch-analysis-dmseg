package xyz.dowenwork.npl.dmseg.elasticsearch;

import org.elasticsearch.common.inject.AbstractModule;

/**
 * <p>create at 16-2-24</p>
 *
 * @author liufl
 * @since 1.0.0
 */
public class DmIndicesAnalysisModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DmIndicesAnalysisComponent.class).asEagerSingleton();
    }
}
