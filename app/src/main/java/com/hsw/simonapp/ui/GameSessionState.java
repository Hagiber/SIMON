package com.hsw.simonapp.ui;

public final class GameSessionState {

    private boolean running;
    private boolean hasLastResult;
    private int lastResult;
    private int record;
    private int currentResult;

    public GameSessionState(boolean hasLastResult, int lastResult, int record) {
        this(false, 0, hasLastResult, lastResult, record);
    }

    public GameSessionState(boolean running, int currentResult,
            boolean hasLastResult, int lastResult, int record) {
        this.running = running;
        this.currentResult = running ? Math.max(0, currentResult) : 0;
        this.hasLastResult = hasLastResult;
        this.lastResult = Math.max(0, lastResult);
        this.record = Math.max(0, record);
        if (this.hasLastResult && this.lastResult > this.record) {
            this.record = this.lastResult;
        }
    }

    public void startRun() {
        running = true;
        currentResult = 0;
    }

    public boolean recordButtonPress() {
        if (!running) {
            return false;
        }
        currentResult++;
        return true;
    }

    public boolean finishRun() {
        if (!running) {
            return false;
        }
        running = false;
        hasLastResult = true;
        lastResult = currentResult;
        record = Math.max(record, currentResult);
        return true;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean hasLastResult() {
        return hasLastResult;
    }

    public int getLastResult() {
        return lastResult;
    }

    public int getRecord() {
        return record;
    }

    public int getCurrentResult() {
        return currentResult;
    }
}
