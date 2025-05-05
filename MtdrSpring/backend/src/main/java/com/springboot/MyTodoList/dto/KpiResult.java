package com.springboot.MyTodoList.dto;

public class KpiResult {
    private KpiData data;
    private ChartData charts;

    public KpiResult() {
    }

    public KpiResult(KpiData data, ChartData charts) {
        this.data = data;
        this.charts = charts;
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
}