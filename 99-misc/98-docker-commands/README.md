# Docker Commnands

## Docker Core

### Show Log

```
docker logs -f <container-name>
```

### Stopping a container

To stop a container, run the following command

```
docker stop <container-name>
```

The container is only stopped, but will still be around and could be started again. You can use `docker ps -a` to view also containers, which are stopped.

### Running a command inside a running container

To run a command in a running container

```
docker exec -ti <container-name> bash
```

### Remove dangling volumes

```
docker volume rm (docker volume ls -qf dangling=true)
```


## Docker Compose

```
docker-compose logs -f
```

