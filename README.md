GC tuning and investigation

Settings:

### test1
-Xmx490m\
-Xmn400m\
-XX:SurvivorRatio=1\
-XX:+UseParallelGC\
-Xlog:gc:./mem_usage.log

### test2

-Xmx200m\
-XX:+UseParallelGC\
-Xlog:gc:./mem_usage-1.log