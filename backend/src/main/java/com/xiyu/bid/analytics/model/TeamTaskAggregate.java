package com.xiyu.bid.analytics.model;

public class TeamTaskAggregate {
    private long totalTaskCount;
    private long completedTaskCount;
    private long overdueTaskCount;

    public static TeamTaskAggregate empty() {
        return new TeamTaskAggregate();
    }

    public long totalTaskCount() {
        return totalTaskCount;
    }

    public void setTotalTaskCount(long totalTaskCount) {
        this.totalTaskCount = totalTaskCount;
    }

    public long completedTaskCount() {
        return completedTaskCount;
    }

    public void setCompletedTaskCount(long completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }

    public long overdueTaskCount() {
        return overdueTaskCount;
    }

    public void setOverdueTaskCount(long overdueTaskCount) {
        this.overdueTaskCount = overdueTaskCount;
    }
}
