package xyz.dowenwork.npl.dmseg.elasticsearch;

import xyz.dowenwork.npl.dmseg.core.SegmenterHolder;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>create at 16-11-15</p>
 *
 * @author liufl
 * @since 5.0.0.0
 */
public interface DmSegmenterHolder {
    AtomicReference<SegmenterHolder> SEGMENTER_HOLDER = new AtomicReference<>();
}
