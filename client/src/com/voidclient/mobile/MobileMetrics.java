package com.voidclient.mobile;
import java.util.*;
/** Bounded local timing samples, not a device-FPS claim. No event coordinates or typed content. */
public final class MobileMetrics {
    private static final long[] paint=new long[256],queue=new long[256];
    private static int paints,queues;private static long paintStart,cancelled;
    public static synchronized void beginPaint(){paintStart=System.nanoTime();}
    public static synchronized void endPaint(){if(paintStart!=0){paint[paints++&255]=System.nanoTime()-paintStart;paintStart=0;}}
    public static synchronized void queueAge(long timeMillis){queue[queues++&255]=Math.max(0,System.nanoTime()/1000000-timeMillis)*1000000;}
    public static synchronized void cancel(){cancelled++;}
    private static Map<String,Object> distribution(long[] values,int count){int n=Math.min(256,count);long[] sorted=Arrays.copyOf(values,n);Arrays.sort(sorted);Map<String,Object> out=new LinkedHashMap<>();out.put("samples",n);if(n>0){out.put("p50_ms",sorted[(n-1)/2]/1e6);out.put("p95_ms",sorted[(int)((n-1)*.95)]/1e6);out.put("p99_ms",sorted[(int)((n-1)*.99)]/1e6);}return out;}
    public static synchronized Map<String,Object> snapshot(){Map<String,Object> out=new LinkedHashMap<>();out.put("interfaceTraversalCpu",distribution(paint,paints));out.put("commandQueueAge",distribution(queue,queues));out.put("cancellations",cancelled);Runtime r=Runtime.getRuntime();out.put("heapUsedBytes",r.totalMemory()-r.freeMemory());out.put("heapLimitBytes",r.maxMemory());out.put("scope","Local CPU/queue timings; not Android frame latency, GPU, thermal or native-memory measurement");return out;}
    private MobileMetrics(){}
}
