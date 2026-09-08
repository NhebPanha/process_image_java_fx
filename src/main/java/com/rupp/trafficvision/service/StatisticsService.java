package com.rupp.trafficvision.service;

import com.rupp.trafficvision.statistics.StatisticsManager;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service orchestrating statistics tracking and event dispatch to UI charts and counters.
 */
public class StatisticsService {

    private final StatisticsManager statisticsManager = new StatisticsManager();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public void addListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    /**
     * Dispatches notification to all registered UI listeners on the JavaFX Application Thread.
     */
    public void notifyStatisticsChanged() {
        Platform.runLater(() -> {
            for (Runnable listener : changeListeners) {
                listener.run();
            }
        });
    }

    public void reset() {
        statisticsManager.reset();
        notifyStatisticsChanged();
    }
}
