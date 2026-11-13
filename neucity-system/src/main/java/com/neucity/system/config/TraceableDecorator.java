/*
 * Copyright (c) 2022. CloudStar.Co.Ltd. All rights reserved.
 */

package com.neucity.system.config;


import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.async.TraceRunnable;
import org.springframework.core.task.TaskDecorator;

public class TraceableDecorator implements TaskDecorator {

    private Tracer tracer;
    private SpanNamer spanNamer;

    public TraceableDecorator(Tracer tracer, SpanNamer spanNamer) {
        this.tracer = tracer;
        this.spanNamer = spanNamer;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        return new Thread(new TraceRunnable(tracer,spanNamer,runnable));
    }

}
