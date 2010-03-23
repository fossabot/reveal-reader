package com.jackcholt.reveal;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool for measuring the timing of events, with the ability to print a summary when all is done.
 */
public class TimingTool {
    /**
     * One event in the list of events tracked by this timing tool.
     */
    private class TimingEvent {
        private String tag;
        private long time;

        public TimingEvent(String tag) {
            this.tag = tag;
            this.time = System.currentTimeMillis();
        }
    };

    private long initialTime = 0L;
    private List<TimingEvent> events;

    public TimingTool() {
        init();
    }

    public void init() {
        initialTime = System.currentTimeMillis();
        events = new ArrayList<TimingEvent>();
    }
    /**
     * Adds an event to the timing list.
     * 
     * @param name
     */
    public void addEvent(String tag) {
        events.add(new TimingEvent(tag));
    }

    /**
     * Get the number of milliseconds elapsed since this tool's creation.
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - initialTime;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        long lastTime = initialTime;
        int maxLength = 0;

        for (TimingEvent event : events) {
            if (event.tag.length() > maxLength) {
                maxLength = event.tag.length();
            }
        }

        for (TimingEvent event : events) {
            sb.append(event.tag);
            padSpaces(sb, maxLength - event.tag.length());
            sb.append(": ");
            String currentInterval = "" + (event.time - lastTime);
            String totalInterval = "" + (event.time - initialTime);
            padSpaces(sb, 5 - currentInterval.length());
            sb.append(currentInterval);
            sb.append("ms [");
            padSpaces(sb, 5 - totalInterval.length());
            sb.append(totalInterval);
            sb.append("ms total]\n");
            lastTime = event.time;
        }

        return sb.toString();
    }

    /**
     * Pad the given string with the given number of spaces.
     * 
     * @param sb A string builder.
     * @param count The number of spaces to pad.
     */
    private void padSpaces(StringBuilder sb, int count) {
        while (count > 0) {
            sb.append(' ');
            --count;
        }
    }
}