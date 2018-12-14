# NGB Singularity management commands reference

Once image is built - use the following commands to start/manage/stop:

Start singularity instance from NGB image
```
mkdir -p logs contents H2
singularity instance start  -B ./logs/:/opt/ngb/logs/ \
                            -B ./contents/:/opt/ngb/contents/ \
                            -B ./H2/:/opt/ngb/H2/ \
                            ./ngb.sinularity.img \
                            ngb
```

List singularity instances
```
singularity instance list
```

Check that NGB is started
```
curl http://localhost:8080/catgenome/ -v
```

Connect to the running service's shell
```
singularity shell instance://ngb
```

Execute NGB CLI command
```
singularity exec instance://ngb ngb lr
```

Kill NGB instance
```
singularity instance stop ngb
```
