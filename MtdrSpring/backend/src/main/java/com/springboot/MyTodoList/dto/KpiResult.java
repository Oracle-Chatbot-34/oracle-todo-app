package com.springboot.MyTodoList.dto;

public class KpiResult {
    private KpiData data;
    private ChartData charts;
    private String insights;

    public KpiResult() {
    }

    public KpiResult(KpiData data, ChartData charts, String insights) {
        this.data = data;
        this.charts = charts;
        this.insights = insights;
    }

    public KpiData getData() {
        return data;
    }

    public void setData(KpiData data) {
        this.data = data;
    }

    public ChartData getCharts() {
        return charts;
    }

    public void setCharts(ChartData charts) {
        this.charts = charts;
    }

    public String getInsights() {
        return insights;
    }

    public void setInsights(String insights) {
        this.insights = insights;
    }
}