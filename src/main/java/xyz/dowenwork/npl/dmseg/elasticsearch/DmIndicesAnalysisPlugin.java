package xyz.dowenwork.npl.dmseg.elasticsearch;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.Plugin;

import java.util.Collection;
import java.util.Collections;

/**
 * <p>create at 16-2-24</p>
 *
 * @author liufl
 * @since 1.0.0
 */
public class DmIndicesAnalysisPlugin extends Plugin {
    @Override
    public String name() {
        return "dm-analysis";
    }

    @Override
    public String description() {
        return "dm segment support for elasticsearch";
    }

    @Override
    public Collection<Module> nodeModules() {
        return Collections.<Module>singleton(new DmIndicesAnalysisModule());
    }
}
