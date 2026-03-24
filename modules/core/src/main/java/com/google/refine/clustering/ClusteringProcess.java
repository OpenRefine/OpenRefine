package com.google.refine.clustering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.browsing.Engine;
import com.google.refine.process.LongRunningProcess;

/**
 * Wraps a clustering computation as a {@link LongRunningProcess} so it can be queued, cancelled, and report progress.
 */
public class ClusteringProcess extends LongRunningProcess {

    private static final Logger logger = LoggerFactory.getLogger("ClusteringProcess");

    private final Clusterer _clusterer;
    private final Engine _engine;
    private volatile boolean _done = false;
    private volatile boolean _failed = false;

    public ClusteringProcess(Clusterer clusterer, Engine engine, String description) {
        super(description);
        _clusterer = clusterer;
        _engine = engine;
    }

    @Override
    public void cancel() {
        _clusterer.cancel();
        super.cancel();
    }

    @Override
    protected Runnable getRunnable() {
        return () -> {
            try {
                _clusterer.computeClusters(_engine);
                if (!_canceled) {
                    _progress = 100;
                    _done = true;
                }
            } catch (Exception e) {
                logger.error("Clustering failed", e);
                _failed = true;
                if (_manager != null) {
                    _manager.onFailedProcess(ClusteringProcess.this, e);
                    return;
                }
            }
            if (_manager != null) {
                _manager.onDoneProcess(ClusteringProcess.this);
            }
        };
    }

    /**
     * Periodically called to copy progress from the clusterer.
     */
    public void updateProgress() {
        if (!_canceled && !_done) {
            _progress = _clusterer.getProgress();
        }
    }

    public Clusterer getClusterer() {
        return _clusterer;
    }

    public boolean isCompleted() {
        return _done;
    }

    public boolean isFailed() {
        return _failed;
    }
}
